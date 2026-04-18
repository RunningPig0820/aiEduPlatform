package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
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
 * 小节-知识点关联持久化对象
 */
@TableName("t_kg_section_kp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSectionKPPo {

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

    public static KgSectionKPPo from(KgSectionKP entity) {
        if (entity == null) return null;
        KgSectionKPPo po = new KgSectionKPPo();
        po.id = entity.getId();
        po.sectionUri = entity.getSectionUri();
        po.kpUri = entity.getKpUri();
        po.orderIndex = entity.getOrderIndex();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgSectionKP toEntity() {
        KgSectionKP entity = EntityFactory.create(KgSectionKP.class);
        entity.setId(this.id);
        entity.setSectionUri(this.sectionUri);
        entity.setKpUri(this.kpUri);
        entity.setOrderIndex(this.orderIndex);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgSectionKPPo> fromList(List<KgSectionKP> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgSectionKPPo::from).collect(Collectors.toList());
    }

    public static List<KgSectionKP> toEntityList(List<KgSectionKPPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgSectionKPPo::toEntity).collect(Collectors.toList());
    }
}
