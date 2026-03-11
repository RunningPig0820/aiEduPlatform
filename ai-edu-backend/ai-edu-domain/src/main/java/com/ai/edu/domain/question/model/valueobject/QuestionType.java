package com.ai.edu.domain.question.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 题目类型值对象
 */
@Getter
@EqualsAndHashCode
public class QuestionType implements ValueObject {

    private final String value;

    private QuestionType(String value) {
        this.value = value;
    }

    public static QuestionType singleChoice() {
        return new QuestionType("SINGLE_CHOICE");
    }

    public static QuestionType multipleChoice() {
        return new QuestionType("MULTIPLE_CHOICE");
    }

    public static QuestionType fillInBlank() {
        return new QuestionType("FILL_IN_BLANK");
    }

    public static QuestionType shortAnswer() {
        return new QuestionType("SHORT_ANSWER");
    }

    public static QuestionType essay() {
        return new QuestionType("ESSAY");
    }

    public static QuestionType of(String value) {
        return new QuestionType(value);
    }

    public boolean isObjective() {
        return "SINGLE_CHOICE".equals(value) ||
               "MULTIPLE_CHOICE".equals(value) ||
               "FILL_IN_BLANK".equals(value);
    }

    public boolean isSubjective() {
        return "SHORT_ANSWER".equals(value) || "ESSAY".equals(value);
    }
}