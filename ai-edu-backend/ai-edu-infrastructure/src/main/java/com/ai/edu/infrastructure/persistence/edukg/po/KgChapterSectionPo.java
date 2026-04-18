package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
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
 * 章节-小节关联持久化对象
 */
@TableName("t_kg_chapter_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapterSectionPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("chapter_uri")
    private String chapterUri;

    @TableField("section_uri")
    private String sectionUri;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static KgChapterSectionPo from(KgChapterSection entity) {
        if (entity == null) return null;
        KgChapterSectionPo po = new KgChapterSectionPo();
        po.id = entity.getId();
        po.chapterUri = entity.getChapterUri();
        po.sectionUri = entity.getSectionUri();
        po.orderIndex = entity.getOrderIndex();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgChapterSection toEntity() {
        KgChapterSection entity = EntityFactory.create(KgChapterSection.class);
        entity.setId(this.id);
        entity.setChapterUri(this.chapterUri);
        entity.setSectionUri(this.sectionUri);
        entity.setOrderIndex(this.orderIndex);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgChapterSectionPo> fromList(List<KgChapterSection> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgChapterSectionPo::from).collect(Collectors.toList());
    }

    public static List<KgChapterSection> toEntityList(List<KgChapterSectionPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgChapterSectionPo::toEntity).collect(Collectors.toList());
    }
}
