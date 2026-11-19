package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AcceptAnswerHandler {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public AcceptAnswerHandler(QuestionRepository questionRepository,
                               AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    public void handle(AcceptAnswerCommand cmd) {
        Question question = questionRepository.findById(cmd.questionId())
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "问题不存在"));

        Answer answer = answerRepository.findById(cmd.answerId())
                .orElseThrow(() -> new NotFoundException("ANSWER_NOT_FOUND", "回答不存在"));

        question.accept(answer, cmd.operator(), cmd.operatorManagesCourse());
        questionRepository.save(question);
    }

}
