package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：投票 / 改投 / 撤销（value ∈ {-1, 0, 1}，0 表示撤销）。
 * 返回的 scoreDelta 告诉调用方（以及前端）本次操作对目标得分的净影响，
 * 事件同时携带旧值与新值，下游据此计算声誉增量。
 */
@Service
public class VoteService {

    private final VoteRepository votes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public VoteService(VoteRepository votes, QuestionRepository questions, AnswerRepository answers,
                       IntegrationEventPublisher events, Clock clock) {
        this.votes = votes;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    public record VoteResult(int myVote, int scoreDelta) {
    }

    private record Target(long authorId, long questionId) {
    }

    @Transactional
    public VoteResult cast(Actor actor, TargetType type, long targetId, int value) {
        if (value < -1 || value > 1) {
            throw new BusinessRuleException("QA-0302", "投票只允许 +1、-1 或者 0（撤销）");
        }
        Target target = resolve(type, targetId);
        Optional<Vote> existing = votes.find(actor.userId(), type, targetId);
        java.time.Instant now = clock.instant();

        // 撤销：没有票可撤时直接返回，保证 PUT 幂等
        if (value == 0) {
            if (existing.isEmpty()) {
                return new VoteResult(0, 0);
            }
            Vote old = existing.get();
            int oldValue = old.value();
            votes.delete(old);
            adjustScore(type, targetId, -oldValue);
            publish(type, targetId, target, actor.userId(), oldValue, 0, now);
            return new VoteResult(0, -oldValue);
        }

        if (existing.isPresent()) {
            Vote vote = existing.get();
            // 重复投相同的票：幂等，不产生事件、不调整得分
            if (vote.value() == value) {
                return new VoteResult(value, 0);
            }
            int oldValue = vote.change(value, now);
            votes.save(vote);
            adjustScore(type, targetId, value - oldValue);
            publish(type, targetId, target, actor.userId(), oldValue, value, now);
            return new VoteResult(value, value - oldValue);
        }

        // 首次投票：领域层校验取值，以及"不能给自己投票"
        Vote vote = Vote.cast(actor.userId(), target.authorId(), toVoteTargetType(type), targetId, value, now);
        votes.save(vote);
        adjustScore(type, targetId, value);
        publish(type, targetId, target, actor.userId(), 0, value, now);
        return new VoteResult(value, value);
    }

    private Target resolve(TargetType type, long targetId) {
        return switch (type) {
            case QUESTION -> {
                Question question = questions.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("QA-0101", "问题不存在：" + targetId));
                yield new Target(question.authorId(), question.id());
            }
            case ANSWER -> {
                Answer answer = answers.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("QA-0102", "回答不存在：" + targetId));
                yield new Target(answer.authorId(), answer.questionId());
            }
        };
    }

    private void adjustScore(TargetType type, long targetId, int delta) {
        if (delta == 0) {
            return;
        }
        switch (type) {
            case QUESTION -> questions.adjustScore(targetId, delta);
            case ANSWER -> answers.adjustScore(targetId, delta);
        }
    }

    private void publish(TargetType type, long targetId, Target target, long voterId, int oldValue, int newValue,
                         java.time.Instant now) {
        events.publish(new VoteCastEvent(EventIds.next(), now, type.name(), targetId, target.authorId(), voterId,
                target.questionId(), oldValue, newValue));
    }

    /** 领域枚举（QUESTION/ANSWER）到 Vote 聚合内部枚举的映射。 */
    private static Vote.TargetType toVoteTargetType(TargetType type) {
        return Vote.TargetType.valueOf(type.name());
    }
}
