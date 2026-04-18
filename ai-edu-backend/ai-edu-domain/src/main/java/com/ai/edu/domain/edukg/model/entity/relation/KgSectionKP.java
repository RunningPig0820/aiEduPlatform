package com.ai.edu.domain.edukg.model.entity.relation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 小节-知识点关联实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSectionKP {

    private Long id;

    private String sectionUri;

    private String kpUri;

    private Integer orderIndex;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgSectionKP create(String sectionUri, String kpUri, Integer orderIndex) {
        KgSectionKP relation = new KgSectionKP();
        relation.sectionUri = sectionUri;
        relation.kpUri = kpUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
