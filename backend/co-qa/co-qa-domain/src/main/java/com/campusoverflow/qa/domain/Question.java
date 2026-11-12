package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import java.time.Instant;
import java.util.Objects;

/**
 * 问题聚合根——问答核心不变量的守护者。
 * <ul>
 *   <li>最多只有一个被采纳的答案，且采纳后不可更改；</li>
 *   <li>只有提问者或课程管理者（教师/助教/管理员）可以采纳；</li>
 *   <li>已删除的问题不能再被回答、编辑或采纳；</li>
 *   <li>提问者只能删除尚无回答的问题，课程管理者可删除任意问题。</li>
 * </ul>
 * score / answerCount / viewCount 为反规范化计数器，由仓储以原子 SQL 维护，不在此处修改。
 */
public class Question {

    private Long id;
    private final long authorId;
    private final Long courseId;
    private Title title;
    private MarkdownBody body;
    private Tags tags;
    private Long acceptedAnswerId;
    private final int score;
    private final int answerCount;
    private final int viewCount;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final long version;

    private Question(Long id, long authorId, Long courseId, Title title, MarkdownBody body, Tags tags,
                     Long acceptedAnswerId, int score, int answerCount, int viewCount, Instant createdAt,
                     Instant updatedAt, Instant deletedAt, long version) {
        this.id = id;
        this.authorId = authorId;
        this.courseId = courseId;
        this.title = Objects.requireNonNull(title);
        this.body = Objects.requireNonNull(body);
        this.tags = Objects.requireNonNull(tags);
        this.acceptedAnswerId = acceptedAnswerId;
        this.score = score;
        this.answerCount = answerCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Question post(long authorId, Long courseId, Title title, MarkdownBody body, Tags tags, Instant now) {
        // 新建问题：无采纳答案、未删除、计数器初始为0
        return new Question(
                null,
                authorId,
                courseId,
                title,
                body,
                tags,
                null,
                0,
                0,
                0,
                now,
                now,
                null,
                0
        );
    }

    public static Question reconstitute(Long id, long authorId, Long courseId, Title title, MarkdownBody body,
                                        Tags tags, Long acceptedAnswerId, int score, int answerCount, int viewCount,
                                        Instant createdAt, Instant updatedAt, Instant deletedAt, long version) {
        return new Question(id, authorId, courseId, title, body, tags, acceptedAnswerId, score, answerCount,
                viewCount, createdAt, updatedAt, deletedAt, version);
    }

    public void edit(Actor actor, boolean actorManagesCourse, Title newTitle, MarkdownBody newBody, Tags newTags,
                     Instant now) {
        ensureNotDeleted();
        // 校验权限：作者本人 或者 课程管理员才能编辑
        if (actor.userId() != authorId && !actorManagesCourse) {
            throw new ForbiddenException("QA-2001", "无权编辑该问题");
        }
        this.title = newTitle;
        this.body = newBody;
        this.tags = newTags;
        this.updatedAt = now;
    }

    public void accept(Answer answer, Actor operator, boolean actorManagesCourse) {
        ensureNotDeleted();

        // 1. 答案必须属于当前问题
        if (!Objects.equals(this.id(), answer.questionId())) {
            throw new BusinessRuleException("QA-2001", "该回答不属于此问题，不能采纳");
        }

        // 2. 不能重复采纳
        if (this.acceptedAnswerId() != null) {
            throw new BusinessRuleException("QA-2004", "该问题已经采纳过回答");
        }

        // 3. 权限：提问作者 或者 课程管理员
        boolean isAuthor = Objects.equals(operator.userId(), this.authorId);
        if (!isAuthor && !actorManagesCourse) {
            throw new ForbiddenException("QA-3001", "没有权限采纳该回答");
        }

        this.acceptedAnswerId = answer.id();
    }




    public void softDelete(Actor actor, boolean actorManagesCourse, Instant now) {
        ensureNotDeleted();
        // 提问者：只能删 answerCount=0（没有回答）的问题
        if (!actorManagesCourse) {
            if (actor.userId() != authorId) {
                throw new ForbiddenException("QA-2004", "非作者不能删除该问题");
            }
            if (answerCount > 0) {
                throw new BusinessRuleException("QA-2005", "已有回答的问题，提问者不能删除");
            }
        }
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void ensureOpenForAnswers() {
        ensureNotDeleted();
        // 已有采纳答案，不再开放回答
        if (acceptedAnswerId != null) {
            throw new BusinessRuleException("QA-2006", "该问题已有采纳答案，不再接受新回答");
        }
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new ConflictException("QA-2007", "问题已删除，禁止操作");
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAccepted(long answerId) {
        return acceptedAnswerId != null && acceptedAnswerId == answerId;
    }

    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("ID 已分配");
        }
        this.id = id;
    }

    public Long id() { return id; }
    public long authorId() { return authorId; }
    public Long courseId() { return courseId; }
    public Title title() { return title; }
    public MarkdownBody body() { return body; }
    public Tags tags() { return tags; }
    public Long acceptedAnswerId() { return acceptedAnswerId; }
    public int score() { return score; }
    public int answerCount() { return answerCount; }
    public int viewCount() { return viewCount; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant deletedAt() { return deletedAt; }
    public long version() { return version; }
}
