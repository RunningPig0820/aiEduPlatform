package com.ai.edu.domain.edukg.model.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 章节-小节关联实体
 */
@TableName("t_kg_chapter_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapterSection {

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

    public static KgChapterSection create(String chapterUri, String sectionUri, Integer orderIndex) {
        KgChapterSection relation = new KgChapterSection();
        relation.chapterUri = chapterUri;
        relation.sectionUri = sectionUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
