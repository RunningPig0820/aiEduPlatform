package com.ai.edu.infrastructure.persistence.edukg.respository;

import com.ai.edu.domain.edukg.model.valueobject.KgTreeNode;
import com.ai.edu.domain.edukg.repository.KgOverviewTreeRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgOverviewTreeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 教材树浏览仓储实现（kg 镜像只读，MyBatis 直接映射 {@link KgTreeNode}）。
 */
@Repository
public class KgOverviewTreeRepositoryImpl implements KgOverviewTreeRepository {

    @Resource
    private KgOverviewTreeMapper kgOverviewTreeMapper;

    @Override
    public List<KgTreeNode> findGradesByStage(String stage) {
        return kgOverviewTreeMapper.selectGradesByStage(stage);
    }

    @Override
    public List<KgTreeNode> findTextbooksByStage(String stage, String grade) {
        return kgOverviewTreeMapper.selectTextbooksByStageAndGrade(stage, grade);
    }

    @Override
    public List<KgTreeNode> findChaptersByTextbookUri(String textbookUri) {
        return kgOverviewTreeMapper.selectChaptersByTextbookUri(textbookUri);
    }

    @Override
    public List<KgTreeNode> findSectionsByChapterUri(String chapterUri) {
        return kgOverviewTreeMapper.selectSectionsByChapterUri(chapterUri);
    }

    @Override
    public List<KgTreeNode> findKnowledgePointsBySectionUri(String sectionUri) {
        return kgOverviewTreeMapper.selectKnowledgePointsBySectionUri(sectionUri);
    }
}
