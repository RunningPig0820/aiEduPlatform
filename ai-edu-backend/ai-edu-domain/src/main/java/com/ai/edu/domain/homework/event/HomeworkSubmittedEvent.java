package com.ai.edu.domain.homework.event;

import com.ai.edu.domain.shared.event.DomainEvent;
import com.ai.edu.domain.homework.model.entity.Homework;

/**
 * 作业提交事件
 */
public class HomeworkSubmittedEvent extends DomainEvent {

    private final Long homeworkId;
    private final Long studentId;
    private final String imageUrl;

    public HomeworkSubmittedEvent(Homework homework) {
        super();
        this.homeworkId = homework.getId();
        this.studentId = homework.getStudentId();
        this.imageUrl = homework.getImageUrl();
    }

    public Long getHomeworkId() {
        return homeworkId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}