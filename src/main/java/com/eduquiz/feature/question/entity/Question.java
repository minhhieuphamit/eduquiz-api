package com.eduquiz.feature.question.entity;

/**
 * @Entity questions
 * Fields: id, chapter(@ManyToOne), content(TEXT - có thể chứa LaTeX),
 * optionA, optionB, optionC, optionD (TEXT - có thể chứa LaTeX),
 * correctAnswer(CHAR: A/B/C/D), difficulty(enum: EASY/MEDIUM/HARD),
 * explanation(TEXT - có thể chứa LaTeX)
 * <p>
 * Lưu ý: LaTeX được lưu dạng plain text ($x^2$, $$\int_0^1$$)
 * Frontend (KaTeX) sẽ render thành công thức đẹp
 * TODO: Implement @Entity @Data
 */
public class Question {
}
