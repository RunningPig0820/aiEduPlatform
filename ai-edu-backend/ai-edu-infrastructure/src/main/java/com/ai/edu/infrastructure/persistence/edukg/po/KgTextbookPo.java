package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;
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
 * 知识图谱-教材持久化对象
 */
@TableName("t_kg_textbook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbookPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("grade")
    private String grade;

    @TableField("stage")
    private String stage;

    @TableField("edition")
    private String edition;

    @TableField("order_index")
    private Integer orderIndex = 0;

    @TableField("subject")
    private String subject;

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

    public static KgTextbookPo from(KgTextbook entity) {
        if (entity == null) return null;
        KgTextbookPo po = new KgTextbookPo();
        po.id = entity.getId();
        po.uri = entity.getUri();
        po.label = entity.getLabel();
        po.grade = entity.getGrade();
        po.stage = entity.getStage();
        po.edition = entity.getEdition();
        po.orderIndex = entity.getOrderIndex();
        po.subject = entity.getSubject();
        po.status = entity.getStatus();
        po.mergedToUri = entity.getMergedToUri();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgTextbook toEntity() {
        KgTextbook entity = EntityFactory.create(KgTextbook.class);
        entity.setId(this.id);
        entity.setUri(this.uri);
        entity.setLabel(this.label);
        entity.setGrade(this.grade);
        entity.setStage(this.stage);
        entity.setEdition(this.edition);
        entity.setOrderIndex(this.orderIndex);
        entity.setSubject(this.subject);
        entity.setStatus(this.status);
        entity.setMergedToUri(this.mergedToUri);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgTextbookPo> fromList(List<KgTextbook> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgTextbookPo::from).collect(Collectors.toList());
    }

    public static List<KgTextbook> toEntityList(List<KgTextbookPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgTextbookPo::toEntity).collect(Collectors.toList());
    }
}
