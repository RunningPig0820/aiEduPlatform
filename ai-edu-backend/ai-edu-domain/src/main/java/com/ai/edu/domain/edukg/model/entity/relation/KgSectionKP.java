package com.ai.edu.domain.edukg.model.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 小节-知识点关联实体
 */
@TableName("t_kg_section_kp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSectionKP {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("section_uri")
    private String sectionUri;

    @TableField("kp_uri")
    private String kpUri;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static KgSectionKP create(String sectionUri, String kpUri, Integer orderIndex) {
        KgSectionKP relation = new KgSectionKP();
        relation.sectionUri = sectionUri;
        relation.kpUri = kpUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
