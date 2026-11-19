package com.campusoverflow.qa.application;

import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QaQueryApiImpl implements QaQueryApi {

    private final QuestionRepository questions;

    public QaQueryApiImpl(QuestionRepository questions) {
        this.questions = questions;
    }

    @Override
    public Optional<QuestionRef> findQuestion(long questionId) {
        // 只暴露发布语言（QuestionRef），不把 Question 聚合泄露给其他上下文
        return questions.findById(questionId).map(QaQueryApiImpl::toRef);
    }

    private static QuestionRef toRef(Question question) {
        return new QuestionRef(question.id(), question.authorId(), question.courseId(), question.title().value(),
                question.acceptedAnswerId(), question.isDeleted());
    }
}
