package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.KgChapter;
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
 * 知识图谱-章节持久化对象
 */
@TableName("t_kg_chapter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapterPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("topic")
    private String topic;

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

    public static KgChapterPo from(KgChapter entity) {
        if (entity == null) return null;
        KgChapterPo po = new KgChapterPo();
        po.id = entity.getId();
        po.uri = entity.getUri();
        po.label = entity.getLabel();
        po.topic = entity.getTopic();
        po.status = entity.getStatus();
        po.mergedToUri = entity.getMergedToUri();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgChapter toEntity() {
        KgChapter entity = EntityFactory.create(KgChapter.class);
        entity.setId(this.id);
        entity.setUri(this.uri);
        entity.setLabel(this.label);
        entity.setTopic(this.topic);
        entity.setStatus(this.status);
        entity.setMergedToUri(this.mergedToUri);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgChapterPo> fromList(List<KgChapter> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgChapterPo::from).collect(Collectors.toList());
    }

    public static List<KgChapter> toEntityList(List<KgChapterPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgChapterPo::toEntity).collect(Collectors.toList());
    }
}
