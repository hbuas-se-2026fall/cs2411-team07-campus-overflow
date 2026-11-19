package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptAnswerHandlerTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private AcceptAnswerHandler handler;

    private final Actor ownerActor = new Actor(1L, "提问人", Role.STUDENT);
    private final Actor strangerActor = new Actor(999L, "路人用户", Role.STUDENT);
    private final Actor courseManager = new Actor(888L, "助教", Role.TA);

    @Test
    void acceptSuccessByAuthor() {
        Question q = mock(Question.class);
        Answer a = mock(Answer.class);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(answerRepository.findById(10L)).thenReturn(Optional.of(a));

        AcceptAnswerCommand cmd = new AcceptAnswerCommand(1L, 10L, ownerActor, false);
        handler.handle(cmd);

        verify(q).accept(a, ownerActor, false);
        verify(questionRepository).save(q);
    }

    @Test
    void acceptSuccessByCourseManager() {
        Question q = mock(Question.class);
        Answer a = mock(Answer.class);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(answerRepository.findById(10L)).thenReturn(Optional.of(a));

        AcceptAnswerCommand cmd = new AcceptAnswerCommand(1L, 10L, courseManager, true);
        handler.handle(cmd);

        verify(q).accept(a, courseManager, true);
        verify(questionRepository).save(q);
    }

    @Test
    void questionNotFound() {
        when(questionRepository.findById(1L)).thenReturn(Optional.empty());
        AcceptAnswerCommand cmd = new AcceptAnswerCommand(1L, 10L, ownerActor, false);
        assertThrows(NotFoundException.class, () -> handler.handle(cmd));
    }

    @Test
    void answerNotFound() {
        Question q = mock(Question.class);
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(answerRepository.findById(10L)).thenReturn(Optional.empty());

        AcceptAnswerCommand cmd = new AcceptAnswerCommand(1L, 10L, ownerActor, false);
        assertThrows(NotFoundException.class, () -> handler.handle(cmd));
    }

    @Test
    void forbiddenWhenNotAuthorAndNotManager() {
        Question q = mock(Question.class);
        Answer a = mock(Answer.class);
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        when(answerRepository.findById(10L)).thenReturn(Optional.of(a));
        doThrow(new ForbiddenException("NO_PERMISSION_ACCEPT", "无权采纳"))
                .when(q).accept(a, strangerActor, false);

        AcceptAnswerCommand cmd = new AcceptAnswerCommand(1L, 10L, strangerActor, false);
        assertThrows(ForbiddenException.class, () -> handler.handle(cmd));
    }
}
