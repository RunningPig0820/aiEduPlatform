package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
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
 * 教材-章节关联持久化对象
 */
@TableName("t_kg_textbook_chapter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbookChapterPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("textbook_uri")
    private String textbookUri;

    @TableField("chapter_uri")
    private String chapterUri;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static KgTextbookChapterPo from(KgTextbookChapter entity) {
        if (entity == null) return null;
        KgTextbookChapterPo po = new KgTextbookChapterPo();
        po.id = entity.getId();
        po.textbookUri = entity.getTextbookUri();
        po.chapterUri = entity.getChapterUri();
        po.orderIndex = entity.getOrderIndex();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgTextbookChapter toEntity() {
        KgTextbookChapter entity = EntityFactory.create(KgTextbookChapter.class);
        entity.setId(this.id);
        entity.setTextbookUri(this.textbookUri);
        entity.setChapterUri(this.chapterUri);
        entity.setOrderIndex(this.orderIndex);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgTextbookChapterPo> fromList(List<KgTextbookChapter> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgTextbookChapterPo::from).collect(Collectors.toList());
    }

    public static List<KgTextbookChapter> toEntityList(List<KgTextbookChapterPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgTextbookChapterPo::toEntity).collect(Collectors.toList());
    }
}
