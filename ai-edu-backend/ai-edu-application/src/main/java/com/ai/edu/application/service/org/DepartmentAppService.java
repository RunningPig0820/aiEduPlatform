package com.ai.edu.application.service.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.org.DepartmentDTO;
import com.ai.edu.application.dto.org.command.CreateDepartmentCommand;
import com.ai.edu.application.dto.org.command.DepartmentQueryCommand;
import com.ai.edu.application.dto.org.command.UpdateDepartmentCommand;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.DepartmentQueryParam;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门应用服务
 * 处理部门的创建、更新、查询等用例
 */
@Slf4j
@Service
public class DepartmentAppService {

    @Resource
    private DepartmentRepository departmentRepository;

    /**
     * 创建部门
     */
    @DS("org")
    @Transactional
    public DepartmentDTO createDepartment(Long schoolId, CreateDepartmentCommand command) {
        log.info("创建部门: schoolId={}, name={}, parentId={}", schoolId, command.getName(), command.getParentId());

        // 1. 检查名称是否已存在
        if (departmentRepository.existsBySchoolIdAndName(SchoolId.of(schoolId), command.getName())) {
            throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "部门名称已存在");
        }

        // 2. 创建部门实体
        Department department;
        if (command.getParentId() == null) {
            // 创建根部门
            department = Department.createRoot(
                    SchoolId.of(schoolId),
                    command.getName(),
                    command.getSortOrder(),
                    command.getDescription()
            );
        } else {
            // 创建子部门
            Department parentDepartment = departmentRepository.findById(DepartmentId.of(command.getParentId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "上级部门不存在"));

            // 校验父部门属于同一学校
            if (!parentDepartment.getSchoolIdValue().equals(schoolId)) {
                throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "上级部门不属于该学校");
            }

            department = Department.createChild(
                    SchoolId.of(schoolId),
                    command.getName(),
                    parentDepartment,
                    command.getSortOrder(),
                    command.getDescription()
            );
        }

        // 3. 保存
        Department savedDepartment = departmentRepository.save(department);

        log.info("部门创建成功: id={}, name={}", savedDepartment.getIdValue(), savedDepartment.getName());
        return toDTO(savedDepartment, null);
    }

    /**
     * 更新部门
     */
    @DS("org")
    @Transactional
    public DepartmentDTO updateDepartment(Long schoolId, Long id, UpdateDepartmentCommand command) {
        log.info("更新部门: schoolId={}, id={}, name={}, parentId={}", schoolId, id, command.getName(), command.getParentId());

        // 1. 查找部门
        Department department = departmentRepository.findById(DepartmentId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        // 2. 校验部门属于该学校
        if (!department.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        // 3. 检查名称冲突（如果修改了名称）
        if (command.getName() != null && !department.getName().equals(command.getName())) {
            if (departmentRepository.existsBySchoolIdAndName(SchoolId.of(schoolId), command.getName())) {
                throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "部门名称已存在");
            }
        }

        // 4. 更新上级部门（如果需要）
        if (command.getParentId() != null) {
            // 防止自引用
            if (command.getParentId().equals(id)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "上级部门不能为自身");
            }

            Department newParent = departmentRepository.findById(DepartmentId.of(command.getParentId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "上级部门不存在"));

            // 校验父部门属于同一学校
            if (!newParent.getSchoolIdValue().equals(schoolId)) {
                throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "上级部门不属于该学校");
            }

            // 更新父部门和路径
            department.updateParent(newParent);

            // 更新子孙部门路径
            departmentRepository.updateDescendantsPath(DepartmentId.of(id), department.getDepartmentPath());
        }

        // 5. 更新基本信息
        department.update(
                command.getName() != null ? command.getName() : department.getName(),
                command.getSortOrder(),
                command.getDescription() != null ? command.getDescription() : department.getDescription()
        );

        // 6. 保存
        departmentRepository.save(department);

        // 7. 获取父部门名称
        String parentName = null;
        if (department.getParentId() != null) {
            Department parent = departmentRepository.findById(DepartmentId.of(department.getParentId())).orElse(null);
            parentName = parent != null ? parent.getName() : null;
        }

        log.info("部门更新成功: id={}", id);
        return toDTO(department, parentName);
    }

    /**
     * 删除部门
     */
    @DS("org")
    @Transactional
    public void deleteDepartment(Long schoolId, Long id) {
        log.info("删除部门: schoolId={}, id={}", schoolId, id);

        // 1. 查找部门
        Department department = departmentRepository.findById(DepartmentId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        // 2. 校验部门属于该学校
        if (!department.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        // 3. 检查是否有子部门
        if (departmentRepository.hasChildren(DepartmentId.of(id))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在子部门，需先删除子部门");
        }

        // 4. 删除
        departmentRepository.deleteById(DepartmentId.of(id));

        log.info("部门删除成功: id={}", id);
    }

    /**
     * 获取部门详情
     */
    public DepartmentDTO getDepartmentById(Long schoolId, Long id) {
        log.info("获取部门详情: schoolId={}, id={}", schoolId, id);

        Department department = departmentRepository.findById(DepartmentId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        // 校验部门属于该学校
        if (!department.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        // 获取父部门名称
        String parentName = null;
        if (department.getParentId() != null) {
            Department parent = departmentRepository.findById(DepartmentId.of(department.getParentId())).orElse(null);
            parentName = parent != null ? parent.getName() : null;
        }

        return toDTO(department, parentName);
    }

    /**
     * 获取部门树
     */
    public List<DepartmentDTO> getDepartmentTree(Long schoolId) {
        log.info("获取部门树: schoolId={}", schoolId);

        // 1. 获取所有部门
        List<Department> allDepartments = departmentRepository.findBySchoolId(SchoolId.of(schoolId));

        // 2. 构建树形结构
        return buildTree(allDepartments);
    }

    /**
     * 分页查询部门
     */
    public PageResult<DepartmentDTO> queryDepartments(Long schoolId, DepartmentQueryCommand query) {
        log.info("分页查询部门: schoolId={}, id={}, name={}, parentId={}, pageNum={}, pageSize={}",
                schoolId, query.getId(), query.getName(), query.getParentId(), query.getPageNum(), query.getPageSize());

        DepartmentQueryParam param = DepartmentQueryParam.builder()
                .schoolId(schoolId)
                .id(query.getId())
                .name(query.getName())
                .parentId(query.getParentId())
                .rootOnly(query.getRootOnly())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();

        com.ai.edu.domain.shared.valueobject.PageResult<Department> result = departmentRepository.queryPage(param);

        List<DepartmentDTO> list = result.getList().stream()
                .map(dept -> {
                    String parentName = null;
                    if (dept.getParentId() != null) {
                        Department parent = departmentRepository.findById(DepartmentId.of(dept.getParentId())).orElse(null);
                        parentName = parent != null ? parent.getName() : null;
                    }
                    return toDTO(dept, parentName);
                })
                .collect(Collectors.toList());

        return PageResult.<DepartmentDTO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(result.getPageNum())
                .pageSize(result.getPageSize())
                .pages(result.getPages())
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为DTO
     */
    private DepartmentDTO toDTO(Department department, String parentName) {
        return DepartmentDTO.builder()
                .id(department.getIdValue())
                .schoolId(department.getSchoolIdValue())
                .name(department.getName())
                .parentId(department.getParentId())
                .parentName(parentName)
                .departmentPath(department.getDepartmentPath())
                .sortOrder(department.getSortOrder())
                .description(department.getDescription())
                .isRoot(department.isRoot())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }

    /**
     * 构建部门树
     */
    private List<DepartmentDTO> buildTree(List<Department> allDepartments) {
        // 按 parentId 分组
        Map<Long, List<Department>> parentMap = new HashMap<>();
        for (Department dept : allDepartments) {
            Long parentId = dept.getParentId() != null ? dept.getParentId() : 0L;
            parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dept);
        }

        // 构建根部门树
        List<Department> rootDepartments = parentMap.getOrDefault(0L, new ArrayList<>());
        return rootDepartments.stream()
                .map(dept -> buildTreeNode(dept, parentMap))
                .collect(Collectors.toList());
    }

    /**
     * 构建树节点
     */
    private DepartmentDTO buildTreeNode(Department department, Map<Long, List<Department>> parentMap) {
        DepartmentDTO dto = toDTO(department, null);

        // 添加子部门
        List<Department> children = parentMap.getOrDefault(department.getIdValue(), new ArrayList<>());
        if (!children.isEmpty()) {
            dto.setChildren(children.stream()
                    .map(child -> buildTreeNode(child, parentMap))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}