package com.ai.edu.application.assembler.org;

import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.user.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教职工 DTO 转换器
 * 负责领域对象到 DTO 的转换
 */
@Slf4j
@Component
public class OrgTeacherAssembler {

    /**
     * 转换单个教职工实体为 DTO（包含用户基本信息）
     */
    public OrgTeacherDTO toDTO(OrgTeacher teacher, User user, String departmentName) {
        OrgTeacherDTO dto = OrgTeacherDTO.builder()
                .id(teacher.getIdValue())
                .userId(teacher.getUserId())
                .schoolId(teacher.getSchoolIdValue())
                .departmentId(teacher.getDepartmentId())
                .departmentName(departmentName)
                .createdBy(teacher.getCreatedBy())
                .modifiedBy(teacher.getModifiedBy())
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();

        // 补充用户基本信息
        if (user != null) {
            dto.setName(user.getRealName());
            dto.setPhone(user.getPhone());
        }

        return dto;
    }

    /**
     * 批量转换教职工列表（聚合用户信息和部门名称）
     */
    public List<OrgTeacherDTO> toDTOList(
            List<OrgTeacher> teachers,
            Map<Long, User> userMap,
            Map<Long, String> departmentNameMap) {

        return teachers.stream()
                .map(teacher -> {
                    User user = userMap.get(teacher.getUserId());
                    String departmentName = departmentNameMap.get(teacher.getDepartmentId());
                    return toDTO(teacher, user, departmentName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换教职工实体为 DTO（仅包含组织域信息）
     */
    public OrgTeacherDTO toDTO(OrgTeacher teacher, String departmentName) {
        return toDTO(teacher, null, departmentName);
    }
}