package com.ai.edu.domain.question.repository;

import com.ai.edu.domain.question.model.entity.Question;
import java.util.List;
import java.util.Optional;

/**
 * 题目仓储接口
 */
public interface QuestionRepository {

    Question save(Question question);

    Optional<Question> findById(Long id);

    List<Question> findByKnowledgePointId(Long knowledgePointId);

    List<Question> findByDifficulty(String difficulty);

    List<Question> findByQuestionType(String questionType);

    void deleteById(Long id);
}