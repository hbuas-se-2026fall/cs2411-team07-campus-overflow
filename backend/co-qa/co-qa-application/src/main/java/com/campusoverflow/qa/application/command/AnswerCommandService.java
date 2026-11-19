package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：提交 / 编辑 / 删除回答，采纳最佳答案，教师认证。 */
@Service
public class AnswerCommandService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public AnswerCommandService(QuestionRepository questions, AnswerRepository answers, IdentityApi identity,
                                IntegrationEventPublisher events, QuestionListCache listCache, AuditTrail audit,
                                Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.events = events;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long submit(Actor actor, long questionId, String body) {
        Question question = loadQuestion(questionId);
        // 领域规则：已删除的问题不能回答（QA-0201），已有采纳答案的问题不再收新回答（QA-2006）
        Answer answer = Answer.submit(question, actor.userId(), MarkdownBody.ofAnswer(body), clock.instant());
        question.ensureOpenForAnswers();

        Answer saved = answers.save(answer);
        // 回答数是反规范化计数器，由仓储原子自增，避免并发丢失更新
        questions.adjustAnswerCount(questionId, 1);
        invalidateListCache();

        events.publish(new AnswerSubmittedEvent(EventIds.next(), clock.instant(), saved.id(), questionId,
                saved.authorId(), question.authorId(), question.title().value()));
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long answerId, String body) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());

        // 权限判定：作者本人，或该课程的管理者（教师/助教/管理员）
        answer.edit(actor.userId(), managesCourse(actor, question.courseId()), MarkdownBody.ofAnswer(body),
                clock.instant());
        answers.save(answer);
        invalidateListCache();
    }

    @Transactional
    public void delete(Actor actor, long answerId) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean accepted = question.isAccepted(answerId);

        // 域规则：已采纳回答不允许作者本人删除（防"先骗采纳再删除"，QA-0206）
        answer.softDelete(actor.userId(), managesCourse(actor, question.courseId()), accepted, clock.instant());
        answers.save(answer);
        questions.adjustAnswerCount(question.id(), -1);
        invalidateListCache();

        events.publish(new ContentDeletedEvent(EventIds.next(), clock.instant(), "ANSWER", answerId,
                question.id()));
        audit.record("ANSWER_DELETED", actor.userId(), "ANSWER", String.valueOf(answerId),
                "questionId=" + question.id());
    }

    /** 场景 R-2：采纳最佳答案。状态变更与 Outbox 事件在同一事务提交；声誉结算异步进行。 */
    @Transactional
    public void accept(Actor actor, long questionId, long answerId) {
        Question question = loadQuestion(questionId);
        Answer answer = loadAnswer(answerId);

        // 领域规则：只能是提问者或课程管理者；不能重复采纳；答案必须属于该问题
        question.accept(answer, actor, managesCourse(actor, question.courseId()));
        questions.save(question);
        invalidateListCache();

        events.publish(new AnswerAcceptedEvent(EventIds.next(), clock.instant(), answer.id(), question.id(),
                answer.authorId(), question.authorId(), actor.userId(), question.title().value()));
        audit.record("ANSWER_ACCEPTED", actor.userId(), "ANSWER", String.valueOf(answerId),
                "questionId=" + question.id());
    }

    @Transactional
    public void endorse(Actor actor, long answerId) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());

        // 领域规则：只有课程管理者可以认证，且同一回答只能认证一次
        answer.endorse(actor.userId(), managesCourse(actor, question.courseId()));
        answers.save(answer);

        events.publish(new AnswerEndorsedEvent(EventIds.next(), clock.instant(), answer.id(), question.id(),
                answer.authorId(), actor.userId(), question.title().value()));
        audit.record("ANSWER_ENDORSED", actor.userId(), "ANSWER", String.valueOf(answerId),
                "questionId=" + question.id());
    }

    private Question loadQuestion(long id) {
        return questions.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-0101", "问题不存在：" + id));
    }

    private Answer loadAnswer(long id) {
        return answers.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-0102", "回答不存在：" + id));
    }

    /**
     * 是否为该课程的教师/助教/管理员。Identity 通过开放主机服务查询，Q&A 不直接访问其数据。
     * 单元测试中 identity 允许为 null（Sprint1 行走骨架阶段只验证编排逻辑）。
     */
    private boolean managesCourse(Actor actor, Long courseId) {
        return identity != null && identity.canManageCourse(actor.userId(), courseId);
    }

    private void invalidateListCache() {
        if (listCache != null) {
            listCache.invalidateAll();
        }
    }
}
