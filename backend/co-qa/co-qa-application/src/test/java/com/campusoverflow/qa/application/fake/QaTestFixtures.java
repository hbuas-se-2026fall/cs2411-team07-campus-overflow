package com.campusoverflow.qa.application.fake;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 手写测试替身，Sprint1：Answer / Vote / Comment 行走骨架使用
 */
public final class QaTestFixtures {

    private QaTestFixtures() {
    }

    public static class InMemoryQuestions implements QuestionRepository {
        private final Map<Long, Question> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();
        public final Map<Long, Integer> answerCountDeltas = new HashMap<>();
        public final Map<Long, Integer> scoreDeltas = new HashMap<>();

        @Override
        public Optional<Question> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Question save(Question question) {
            if (question.id() == null) {
                question.assignId(sequence.incrementAndGet());
            }
            store.put(question.id(), question);
            return question;
        }

        @Override
        public void adjustAnswerCount(long questionId, int delta) {
            answerCountDeltas.merge(questionId, delta, Integer::sum);
        }

        @Override
        public void adjustScore(long questionId, int delta) {
            scoreDeltas.merge(questionId, delta, Integer::sum);
        }
    }

    public static class InMemoryAnswers implements AnswerRepository {
        private final Map<Long, Answer> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();
        public final Map<Long, Integer> scoreDeltas = new HashMap<>();

        @Override
        public Optional<Answer> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Answer save(Answer answer) {
            if (answer.id() == null) {
                answer.assignId(sequence.incrementAndGet());
            }
            store.put(answer.id(), answer);
            return answer;
        }

        @Override
        public void adjustScore(long answerId, int delta) {
            scoreDeltas.merge(answerId, delta, Integer::sum);
        }
    }

    public static class InMemoryVotes implements VoteRepository {
        private final Map<String, Vote> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Vote> find(long voterId, TargetType targetType, long targetId) {
            return Optional.ofNullable(store.get(key(voterId, targetType, targetId)));
        }

        @Override
        public Vote save(Vote vote) {
            if (vote.id() == null) {
                vote.assignId(sequence.incrementAndGet());
            }
            TargetType globalType = TargetType.ANSWER;
            store.put(key(vote.voterId(), globalType, vote.targetId()), vote);
            return vote;
        }

        @Override
        public void delete(Vote vote) {
            TargetType globalType = TargetType.ANSWER;
            store.remove(key(vote.voterId(), globalType, vote.targetId()));
        }

        private static String key(long voterId, TargetType type, long targetId) {
            return voterId + ":" + type + ":" + targetId;
        }
    }

    /** 记录所有已发布事件，行走骨架监听事件断言使用 */
    public static class RecordingEvents implements IntegrationEventPublisher {
        public final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }

        public <T extends IntegrationEvent> List<T> ofType(Class<T> type) {
            return published.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
        }
    }

    public static class RecordingAudit implements AuditTrail {
        public final List<String> actions = new ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            actions.add(action);
        }
    }

    public static Vote makeVote(long voterId, TargetType targetType, long targetId, int value) {
        Vote.TargetType innerType = Vote.TargetType.ANSWER;
        return Vote.reconstitute(null, voterId, innerType, targetId, value, Instant.now());
    }

    public static Vote answerVote(long voterId, long answerId, int value) {
        return makeVote(voterId, TargetType.ANSWER, answerId, value);
    }
}