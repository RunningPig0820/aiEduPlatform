package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.AdminClassStudentDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassStudentCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassStudentCommand;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.acl.ParentBinding;
import com.ai.edu.domain.organization.acl.ParentInfo;
import com.ai.edu.domain.organization.acl.StudentInfo;
import com.ai.edu.domain.organization.gateway.OrgUserGateway;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.StudentClassStatus;
import com.ai.edu.domain.organization.model.valueobject.enums.DepartmentTypeEnum;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.organization.repository.DepartmentEduRepository;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政班学生应用服务
 *
 * 处理行政班学生的添加、查询等用例。
 * 负责跨域聚合协调：通过 Gateway 获取用户域数据，合并返回完整信息。
 *
 * 核心添加流程（6步）：
 * 1. 验证行政班班级节点（组织域）
 * 2. 创建/查询学生用户（用户域，via Gateway）
 * 3. 遍历创建/查询家长用户（用户域，via Gateway）
 * 4. 创建 StudentClass 关联（组织域）
 * 5. 绑定家长关联 ParentProfile（用户域，via Gateway）
 * 6. 聚合返回 DTO
 *
 * Application Service 职责：
 * - 编排 Domain Service 和 Repository
 * - 通过 Gateway 调用其他域
 * - 聚合多域数据返回 DTO
 */
@Slf4j
@Service
public class AdminClassStudentAppService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private DepartmentEduRepository departmentEduRepository;

    @Resource
    private StudentClassRepository studentClassRepository;

    @Resource
    private OrgUserGateway userGateway;  // 防腐层：跨域调用用户域

    /**
     * 添加学生到行政班班级节点
     *
     * 跨域编排流程：
     * - 步骤 1（组织域）：验证班级节点
     * - 步骤 2-3（用户域，via Gateway）：创建/查询学生和家长用户
     * - 步骤 4（组织域，@DS("org") @Transactional）：创建 StudentClass 关联
     * - 步骤 5（用户域，via Gateway）：绑定家长关联
     * - 步骤 6：聚合返回 DTO
     *
     * 注意：步骤 2-3 和步骤 4 不在同一事务（不同域/不同数据库），最终一致性。
     */
    public AdminClassStudentDTO createStudent(Long schoolId, Long deptId, Long currentUserId,
                                               CreateAdminClassStudentCommand command) {
        log.info("添加行政班学生: schoolId={}, deptId={}, phone={}, name={}",
                schoolId, deptId, command.getPhone(), command.getName());

        // Step 1: 验证行政班班级节点（组织域）
        Department dept = validateClassNode(deptId, schoolId);

        // Step 2: 创建/查询学生用户（用户域，via Gateway）
        StudentInfo studentInfo = userGateway.findOrCreateStudent(
                command.getName(), command.getPhone(), command.getIdCard());
        log.info("学生用户处理完成: userId={}", studentInfo.getUserId());

        // Step 3: 遍历创建/查询家长用户（用户域，via Gateway）
        List<ParentBinding> parentBindings = new ArrayList<>();
        if (command.getParents() != null && !command.getParents().isEmpty()) {
            for (var parentCmd : command.getParents()) {
                ParentInfo parentInfo = userGateway.findOrCreateParent(
                        parentCmd.getName(), parentCmd.getPhone());
                parentBindings.add(new ParentBinding(parentInfo.getUserId(), parentCmd.getRelationship()));
                log.info("家长用户处理完成: userId={}, relationship={}", parentInfo.getUserId(), parentCmd.getRelationship());
            }
        }

        // Step 4: 创建 StudentClass 关联（组织域，独立事务）
        StudentClass studentClass = createStudentRelationInTx(
                deptId, studentInfo.getUserId(), command.getStudentNo(), currentUserId);

        // Step 5: 绑定家长关联（用户域，via Gateway）
        if (!parentBindings.isEmpty()) {
            userGateway.bindStudentParents(studentInfo.getUserId(), parentBindings);
        }

        // Step 6: 聚合返回
        List<ParentInfo> savedParents = userGateway.findParentsByStudentUserId(studentInfo.getUserId());

        log.info("行政班学生添加成功: studentClassId={}, studentUserId={}, deptId={}",
                studentClass.getId(), studentInfo.getUserId(), deptId);

        return buildDTO(studentClass, studentInfo, dept.getName(), savedParents);
    }

    /**
     * 查询部门下所有班级的学生列表（聚合查询）
     *
     * 递归查询当前部门及其所有子孙部门中的 CLASS 节点，
     * 聚合返回所有班级下的在读学生。
     */
    public List<AdminClassStudentDTO> listStudentsByClass(Long deptId, Long schoolId) {
        log.info("查询行政班学生列表: deptId={}, schoolId={}", deptId, schoolId);

        // 1. 获取当前部门
        Department dept = departmentRepository.findById(DepartmentId.of(deptId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID, "部门不存在"));

        // 2. 收集所有 CLASS 节点：自身(如果是CLASS) + 所有子孙中的CLASS
        List<Long> classDeptIds = new ArrayList<>();
        if (isClassNode(deptId)) {
            classDeptIds.add(deptId);
        }
        List<Department> descendants = departmentRepository.findAllDescendants(DepartmentId.of(deptId));
        for (Department descendant : descendants) {
            if (isClassNode(descendant.getIdValue())) {
                classDeptIds.add(descendant.getIdValue());
            }
        }

        if (classDeptIds.isEmpty()) {
            log.info("未找到 CLASS 节点: deptId={}", deptId);
            return List.of();
        }
        log.info("找到 {} 个 CLASS 节点: {}", classDeptIds.size(), classDeptIds);

        // 3. 查询所有 CLASS 节点下的 StudentClass 关联
        List<StudentClass> allRelations = new ArrayList<>();
        for (Long cid : classDeptIds) {
            allRelations.addAll(studentClassRepository.findByClassId(ClassId.of(cid)));
        }
        List<StudentClass> activeRelations = allRelations.stream()
                .filter(r -> !r.isDeleted() && r.isActive())
                .toList();

        if (activeRelations.isEmpty()) {
            return List.of();
        }

        // 4. 构建 deptId → deptName 映射
        var deptNameMap = buildDeptNameMap(classDeptIds);

        // 5. 批量查询学生用户信息
        List<Long> userIds = activeRelations.stream()
                .map(StudentClass::getStudentIdValue)
                .distinct()
                .toList();
        var teacherInfoMap = userGateway.findTeachersByIds(userIds).stream()
                .collect(Collectors.toMap(t -> t.getUserId(), t -> t));

        // 6. 聚合返回
        List<AdminClassStudentDTO> result = new ArrayList<>();
        for (StudentClass sc : activeRelations) {
            var teacherInfo = teacherInfoMap.get(sc.getStudentIdValue());
            List<ParentInfo> parents = userGateway.findParentsByStudentUserId(sc.getStudentIdValue());

            StudentInfo studentInfo = null;
            if (teacherInfo != null) {
                studentInfo = new StudentInfo(
                        teacherInfo.getUserId(), teacherInfo.getName(), teacherInfo.getPhone(), "");
            }

            String className = deptNameMap.getOrDefault(sc.getClassIdValue(), "");
            result.add(buildDTO(sc, studentInfo, className, parents));
        }

        return result;
    }

    /**
     * 判断部门是否是 CLASS 节点
     */
    private boolean isClassNode(Long deptId) {
        return departmentEduRepository.findByDeptId(deptId)
                .map(edu -> DeptEduTypeEnum.CLASS.equals(edu.getDeptType()))
                .orElse(false);
    }

    /**
     * 构建 deptId → deptName 映射
     */
    private java.util.Map<Long, String> buildDeptNameMap(List<Long> deptIds) {
        java.util.Map<Long, String> map = new java.util.HashMap<>();
        for (Long id : deptIds) {
            departmentRepository.findById(DepartmentId.of(id))
                    .ifPresent(d -> map.put(id, d.getName()));
        }
        return map;
    }

    /**
     * 更新行政班学生信息
     *
     * 组织域只支持修改学号和状态（毕业/转出/恢复在读）。
     * 用户基本信息修改在用户中心处理。
     */
    public AdminClassStudentDTO updateStudent(Long schoolId, Long currentUserId,
                                               UpdateAdminClassStudentCommand command) {
        log.info("更新行政班学生: id={}, studentNo={}, status={}", command.getId(), command.getStudentNo(), command.getStatus());

        StudentClass studentClass = updateStudentInTx(command, currentUserId);

        // 聚合返回
        Department dept = departmentRepository.findById(DepartmentId.of(studentClass.getClassIdValue()))
                .orElse(null);
        String deptName = dept != null ? dept.getName() : null;

        List<StudentInfo> studentInfos = findStudentInfoList(List.of(studentClass.getStudentIdValue()));
        StudentInfo studentInfo = studentInfos.isEmpty() ? null : studentInfos.get(0);

        List<ParentInfo> parents = userGateway.findParentsByStudentUserId(studentClass.getStudentIdValue());

        return buildDTO(studentClass, studentInfo, deptName, parents);
    }

    /**
     * 删除行政班学生关联（逻辑删除）
     *
     * 只删除组织域的 StudentClass 关联，用户域数据保留不受影响。
     */
    @DS("org")
    @Transactional
    public void deleteStudent(Long schoolId, Long id) {
        log.info("删除行政班学生关联: schoolId={}, id={}", schoolId, id);

        StudentClass studentClass = studentClassRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学生关联不存在"));

        // 注意：不能用 studentClass.delete() + save()。MyBatis-Plus 全局配置了 logic-delete-field: deleted，
        // updateById 会跳过逻辑删除字段的更新，is_deleted 不会落库，软删除实际不生效（删除后无法重新添加同一学生）。
        // 必须走 deleteById：插件自动转 UPDATE SET is_deleted=1 WHERE id=? AND is_deleted=0。
        studentClassRepository.deleteById(id);

        log.info("行政班学生关联删除成功: id={}, studentUserId={}", id, studentClass.getStudentIdValue());
    }

    // ==================== 私有方法：组织域事务 ====================

    @DS("org")
    @Transactional
    public StudentClass updateStudentInTx(UpdateAdminClassStudentCommand command, Long currentUserId) {
        StudentClass studentClass = studentClassRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学生关联不存在"));

        if (command.getStudentNo() != null) {
            studentClass.setStudentNo(command.getStudentNo());
        }

        if (command.getStatus() != null) {
            switch (command.getStatus()) {
                case "GRADUATED" -> studentClass.graduate();
                case "TRANSFERRED" -> studentClass.transfer();
                case "ACTIVE" -> studentClass.activate();
                default -> throw new BusinessException(ErrorCode.INVALID_PARAMS,
                        "无效的状态值: " + command.getStatus() + "，有效值: ACTIVE, GRADUATED, TRANSFERRED");
            }
        }

        studentClassRepository.save(studentClass);
        return studentClass;
    }

    private List<StudentInfo> findStudentInfoList(List<Long> userIds) {
        if (userIds.isEmpty()) return List.of();
        return userGateway.findTeachersByIds(userIds).stream()
                .map(t -> new StudentInfo(t.getUserId(), t.getName(), t.getPhone(), ""))
                .toList();
    }

    // ==================== 私有方法：验证 ====================

    /**
     * 验证行政班班级节点
     *
     * 条件：
     * 1. Department 存在且未删除
     * 2. Department.departmentType == ADMIN_CLASS
     * 3. DepartmentEdu 存在且 deptType == CLASS
     * 4. Department.schoolId == schoolId
     */
    private Department validateClassNode(Long deptId, Long schoolId) {
        // 1. 查 Department
        Department dept = departmentRepository.findById(DepartmentId.of(deptId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID, "行政班节点不存在"));

        // 2. 校验 schoolId
        if (!dept.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID, "行政班节点不属于该学校");
        }

        // 3. 校验类型为 ADMIN_CLASS
        if (!DepartmentTypeEnum.ADMIN_CLASS.getValue().equals(dept.getDepartmentTypeValue())) {
            throw new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID, "该节点不是行政班类型");
        }

        // 4. 校验 DeptEduType 为 CLASS
        DepartmentEdu edu = departmentEduRepository.findByDeptId(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID, "行政班节点缺少教育扩展属性"));

        if (!DeptEduTypeEnum.CLASS.equals(edu.getDeptType())) {
            throw new BusinessException(ErrorCode.ADMIN_CLASS_NODE_INVALID,
                    "该行政班节点不是班级类型，无法添加学生。当前类型：" + edu.getDeptType().getDescription());
        }

        return dept;
    }

    // ==================== 私有方法：组织域事务 ====================

    /**
     * 在组织域事务中创建 StudentClass 关联
     */
    @DS("org")
    @Transactional
    public StudentClass createStudentRelationInTx(Long deptId, Long studentUserId,
                                                   String studentNo, Long currentUserId) {
        ClassId classId = ClassId.of(deptId);  // classId 实际指向 Department.id
        UserId studentId = UserId.of(studentUserId);

        // 检查重复：学生是否已在该行政班中
        if (studentClassRepository.existsByStudentIdAndClassId(studentId, classId)) {
            throw new BusinessException(ErrorCode.STUDENT_ALREADY_IN_ADMIN_CLASS, "学生已在该行政班中");
        }

        // 创建 StudentClass 关联
        StudentClass studentClass = StudentClass.create(studentId, classId);
        if (studentNo != null && !studentNo.isBlank()) {
            studentClass.setStudentNo(studentNo);
        }
        studentClassRepository.save(studentClass);

        log.info("StudentClass 关联创建成功: id={}, studentId={}, deptId={}, studentNo={}",
                studentClass.getId(), studentUserId, deptId, studentNo);
        return studentClass;
    }

    // ==================== 私有方法：DTO 构建 ====================

    /**
     * 构建聚合 DTO
     */
    private AdminClassStudentDTO buildDTO(StudentClass studentClass, StudentInfo studentInfo,
                                           String deptName, List<ParentInfo> parents) {
        AdminClassStudentDTO.AdminClassStudentDTOBuilder builder = AdminClassStudentDTO.builder()
                .id(studentClass.getId())
                .studentUserId(studentClass.getStudentIdValue())
                .studentNo(studentClass.getStudentNo())
                .deptId(studentClass.getClassIdValue())
                .deptName(deptName)
                .joinDate(studentClass.getJoinDate())
                .status(studentClass.getStatusValue());

        if (studentInfo != null) {
            builder.name(studentInfo.getName())
                   .phone(studentInfo.getPhone())
                   .maskedIdCard(studentInfo.getMaskedIdCard());
        }

        if (parents != null && !parents.isEmpty()) {
            builder.parents(parents.stream()
                    .map(p -> AdminClassStudentDTO.ParentInfoDTO.builder()
                            .userId(p.getUserId())
                            .name(p.getName())
                            .phone(p.getPhone())
                            .relationship(p.getRelationship())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
