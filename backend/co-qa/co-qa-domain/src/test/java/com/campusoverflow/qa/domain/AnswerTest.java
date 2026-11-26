package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Sprint1：MarkdownBody / Tags 属于 S2 待实现，按迭代规划跳过本组测试")
class AnswerTest {

    private final Actor answerer = new Actor(3, "回答者", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    @Test
    void onlyCourseManagerCanEndorseAndOnlyOnce() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        assertThatThrownBy(() -> a.endorse(answerer.userId(), false)).isInstanceOf(ForbiddenException.class);
        a.endorse(teacher.userId(), true);
        assertThat(a.endorsedBy()).isEqualTo(9L);
        assertThatThrownBy(() -> a.endorse(teacher.userId(), true)).isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptedAnswerCannotBeDeletedByItsAuthor() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        assertThatThrownBy(() -> a.softDelete(answerer.userId(), false, true, QuestionTest.NOW))
                .isInstanceOf(ForbiddenException.class);
        a.softDelete(teacher.userId(), true, true, QuestionTest.NOW);
        assertThat(a.isDeleted()).isTrue();
    }

    @Test
    void strangersCannotEditOrDelete() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        Actor stranger = new Actor(77, "路人", Role.STUDENT);
        assertThatThrownBy(() -> a.edit(stranger.userId(), false, MarkdownBody.ofAnswer("这是被路人篡改后的回答内容"), QuestionTest.NOW))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> a.softDelete(stranger.userId(), false, false, QuestionTest.NOW))
                .isInstanceOf(ForbiddenException.class);
    }
}