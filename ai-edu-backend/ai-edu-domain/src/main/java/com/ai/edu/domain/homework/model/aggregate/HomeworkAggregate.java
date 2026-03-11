package com.ai.edu.domain.homework.model.aggregate;

import com.ai.edu.domain.homework.model.entity.Homework;
import lombok.Getter;

/**
 * 作业聚合根
 */
@Getter
public class HomeworkAggregate {

    private final Homework homework;

    public HomeworkAggregate(Homework homework) {
        this.homework = homework;
    }

    public static HomeworkAggregate create(Long studentId, Long questionId, String imageUrl) {
        Homework homework = Homework.create(studentId, questionId, imageUrl);
        return new HomeworkAggregate(homework);
    }

    public Long getId() {
        return homework.getId();
    }

    public String getStatus() {
        return homework.getStatus();
    }

    public void markAsProcessing() {
        homework.markAsProcessing();
    }

    public void complete(Integer score, String feedback) {
        homework.complete(score, feedback);
    }
}