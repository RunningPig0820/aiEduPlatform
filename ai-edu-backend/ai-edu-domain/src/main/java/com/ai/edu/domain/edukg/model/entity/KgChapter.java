package com.ai.edu.domain.edukg.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识图谱-章节实体
 */
@TableName("t_kg_chapter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapter {

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

    public static KgChapter create(String uri, String label) {
        KgChapter chapter = new KgChapter();
        chapter.uri = uri;
        chapter.label = label;
        chapter.status = "active";
        return chapter;
    }

    public void updateTopic(String topic) {
        this.topic = topic;
    }
}
