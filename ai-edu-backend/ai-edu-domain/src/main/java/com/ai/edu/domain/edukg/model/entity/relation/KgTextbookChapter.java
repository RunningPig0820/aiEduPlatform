package com.ai.edu.domain.edukg.model.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 教材-章节关联实体
 */
@TableName("t_kg_textbook_chapter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbookChapter {

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

    public static KgTextbookChapter create(String textbookUri, String chapterUri, Integer orderIndex) {
        KgTextbookChapter relation = new KgTextbookChapter();
        relation.textbookUri = textbookUri;
        relation.chapterUri = chapterUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
