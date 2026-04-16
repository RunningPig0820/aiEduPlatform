package com.ai.edu.domain.edukg.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识图谱-小节实体
 */
@TableName("t_kg_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("status")
    private String status;

    @TableField("merged_to_uri")
    private String mergedToUri;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static KgSection create(String uri, String label) {
        KgSection section = new KgSection();
        section.uri = uri;
        section.label = label;
        section.status = "active";
        return section;
    }
}
