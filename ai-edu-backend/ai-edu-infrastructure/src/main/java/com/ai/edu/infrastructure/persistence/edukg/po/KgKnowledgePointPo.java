package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
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
 * 知识图谱-知识点持久化对象
 */
@TableName("t_kg_knowledge_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgKnowledgePointPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("difficulty")
    private String difficulty;

    @TableField("importance")
    private String importance;

    @TableField("cognitive_level")
    private String cognitiveLevel;

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

    public static KgKnowledgePointPo from(KgKnowledgePoint entity) {
        if (entity == null) return null;
        KgKnowledgePointPo po = new KgKnowledgePointPo();
        po.id = entity.getId();
        po.uri = entity.getUri();
        po.label = entity.getLabel();
        po.difficulty = entity.getDifficulty();
        po.importance = entity.getImportance();
        po.cognitiveLevel = entity.getCognitiveLevel();
        po.status = entity.getStatus();
        po.mergedToUri = entity.getMergedToUri();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgKnowledgePoint toEntity() {
        KgKnowledgePoint entity = EntityFactory.create(KgKnowledgePoint.class);
        entity.setId(this.id);
        entity.setUri(this.uri);
        entity.setLabel(this.label);
        entity.setDifficulty(this.difficulty);
        entity.setImportance(this.importance);
        entity.setCognitiveLevel(this.cognitiveLevel);
        entity.setStatus(this.status);
        entity.setMergedToUri(this.mergedToUri);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgKnowledgePointPo> fromList(List<KgKnowledgePoint> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgKnowledgePointPo::from).collect(Collectors.toList());
    }

    public static List<KgKnowledgePoint> toEntityList(List<KgKnowledgePointPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgKnowledgePointPo::toEntity).collect(Collectors.toList());
    }
}
