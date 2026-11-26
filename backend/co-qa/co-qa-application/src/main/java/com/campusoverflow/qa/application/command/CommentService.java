package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        long questionId = resolveQuestionId(cmd.targetType(), cmd.targetId());

        Comment parent = null;
        if (cmd.parentId() != null) {
            parent = comments.findById(cmd.parentId())
                    .orElseThrow(() -> new NotFoundException("QA-0103", "被回复的评论不存在：" + cmd.parentId()));
            // 回复必须挂在同一条内容下
            if (parent.targetType() != cmd.targetType() || parent.targetId() != cmd.targetId()) {
                throw new BusinessRuleException("QA-0404", "只能回复同一内容下的评论");
            }
        }

        // 领域规则：正文非空、≤500 字、最多一层回复（QA-0401/0402/0403）
        Comment comment = Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(), parent,
                cmd.body(), clock.instant());
        Comment saved = comments.save(comment);

        events.publish(new CommentPostedEvent(EventIds.next(), clock.instant(), saved.id(), questionId,
                actor.userId(), saved.body()));
        return saved.id();
    }

    /** 评论挂在问题或回答上，都需要归到同一个问题，便于按问题聚合查询与通知。 */
    private long resolveQuestionId(TargetType type, long targetId) {
        return switch (type) {
            case QUESTION -> {
                Question question = questions.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("QA-0101", "问题不存在：" + targetId));
                yield question.id();
            }
            case ANSWER -> {
                Answer answer = answers.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("QA-0102", "回答不存在：" + targetId));
                yield answer.questionId();
            }
        };
    }
}
