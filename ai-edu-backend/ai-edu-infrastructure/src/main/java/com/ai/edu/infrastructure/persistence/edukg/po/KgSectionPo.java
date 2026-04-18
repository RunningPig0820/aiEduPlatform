package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.infrastructure.persistence.edukg.util.EntityFactory;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识图谱-小节持久化对象
 */
@TableName("t_kg_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSectionPo {

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

    public static KgSectionPo from(KgSection entity) {
        if (entity == null) return null;
        KgSectionPo po = new KgSectionPo();
        po.id = entity.getId();
        po.uri = entity.getUri();
        po.label = entity.getLabel();
        po.status = entity.getStatus();
        po.mergedToUri = entity.getMergedToUri();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgSection toEntity() {
        KgSection entity = EntityFactory.create(KgSection.class);
        entity.setId(this.id);
        entity.setUri(this.uri);
        entity.setLabel(this.label);
        entity.setStatus(this.status);
        entity.setMergedToUri(this.mergedToUri);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgSectionPo> fromList(List<KgSection> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgSectionPo::from).collect(Collectors.toList());
    }

    public static List<KgSection> toEntityList(List<KgSectionPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgSectionPo::toEntity).collect(Collectors.toList());
    }
}
