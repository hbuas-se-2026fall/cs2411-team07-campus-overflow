package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：发布 / 编辑 / 删除问题。
 * <p>编排顺序（arc42 应用层模板）：校验并构造值对象 → 加载聚合 → 授权 → 调用领域方法 → 保存 →
 * 维护标签计数等反规范化数据 → 发布集成事件 → 缓存失效 → 审计。</p>
 */
@Service
public class QuestionCommandService {

    static final int PLAIN_TEXT_FOR_INDEX = 2000;

    private final QuestionRepository questions;
    private final TagRepository tags;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public QuestionCommandService(QuestionRepository questions, TagRepository tags, IdentityApi identity,
                                  IntegrationEventPublisher events, ContentRenderer renderer,
                                  QuestionListCache listCache, AuditTrail audit, Clock clock) {
        this.questions = questions;
        this.tags = tags;
        this.identity = identity;
        this.events = events;
        this.renderer = renderer;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        // 值对象构造即校验：标题 10–150 字、正文非空、标签 1–5 个（QA-1001/QA-1002/QA-1004/QA-1005）
        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tagValue = Tags.of(cmd.tags());
        requireExistingCourse(cmd.courseId());

        Question question = Question.post(actor.userId(), cmd.courseId(), title, body, tagValue, clock.instant());
        Question saved = questions.save(question);

        // 标签使用计数是反规范化数据，由仓储原子累加
        adjustTagUsage(tagValue.names(), 1);
        invalidateListCache();

        events.publish(new QuestionPostedEvent(EventIds.next(), clock.instant(), saved.id(), actor.userId(),
                saved.courseId(), saved.title().value(), plainText(saved.body().raw()), saved.tags().names()));
        audit.record("QUESTION_POSTED", actor.userId(), "QUESTION", String.valueOf(saved.id()),
                "courseId=" + saved.courseId());
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Question question = load(questionId);
        boolean actorManagesCourse = managesCourse(actor, question.courseId());

        Title newTitle = new Title(cmd.title());
        MarkdownBody newBody = MarkdownBody.ofQuestion(cmd.body());
        Tags newTags = Tags.of(cmd.tags());
        List<String> oldTagNames = question.tags().names();

        // 领域层负责“已删除不可编辑（QA-2007）”与“仅作者或课程管理者可编辑（QA-2001）”
        question.edit(actor, actorManagesCourse, newTitle, newBody, newTags, clock.instant());
        questions.save(question);

        // 只对发生变化的标签调整用法计数（新增 +1，移除 -1），避免全量重算
        Set<String> removed = new HashSet<>(oldTagNames);
        removed.removeAll(newTags.names());
        Set<String> added = new HashSet<>(newTags.names());
        added.removeAll(oldTagNames);
        adjustTagUsage(removed, -1);
        adjustTagUsage(added, 1);
        invalidateListCache();

        events.publish(new QuestionEditedEvent(EventIds.next(), clock.instant(), question.id(),
                question.title().value(), plainText(question.body().raw()), question.tags().names()));
        audit.record("QUESTION_EDITED", actor.userId(), "QUESTION", String.valueOf(questionId),
                "courseId=" + question.courseId());
    }

    @Transactional
    public void delete(Actor actor, long questionId) {
        Question question = load(questionId);

        // 领域规则：提问者只能删无回答的问题（QA-2005），课程管理者可删任意问题
        question.softDelete(actor, managesCourse(actor, question.courseId()), clock.instant());
        questions.save(question);

        adjustTagUsage(question.tags().names(), -1);
        invalidateListCache();

        events.publish(new ContentDeletedEvent(EventIds.next(), clock.instant(), "QUESTION", questionId, questionId));
        audit.record("QUESTION_DELETED", actor.userId(), "QUESTION", String.valueOf(questionId),
                "courseId=" + question.courseId());
    }

    /** 按 id 加载聚合，不存在抛 404（QA-0101）。包级可见：便于应用层单元测试直接驱动。 */
    Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA-0101", "问题不存在：" + questionId));
    }

    /** 关联了课程时校验课程存在；未关联课程（courseId 为 null）允许发布。 */
    private void requireExistingCourse(Long courseId) {
        if (courseId == null || identity == null) {
            return;
        }
        identity.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("QA-0104", "课程不存在：" + courseId));
    }

    /** 是否为该课程的教师/助教/管理员。单元测试中 identity 允许为 null（只验证编排逻辑）。 */
    private boolean managesCourse(Actor actor, Long courseId) {
        return identity != null && identity.canManageCourse(actor.userId(), courseId);
    }

    private void adjustTagUsage(Collection<String> tagNames, int delta) {
        if (tags == null || tagNames == null || tagNames.isEmpty()) {
            return;
        }
        tags.adjustUsage(tagNames, delta);
    }

    private void invalidateListCache() {
        if (listCache != null) {
            listCache.invalidateAll();
        }
    }

    /** 事件里携带纯文本用于检索索引；渲染器不可用时退化为截断原文。 */
    private String plainText(String markdown) {
        if (renderer != null) {
            return renderer.toPlainText(markdown, PLAIN_TEXT_FOR_INDEX);
        }
        if (markdown == null) {
            return "";
        }
        int length = markdown.codePointCount(0, markdown.length());
        return length <= PLAIN_TEXT_FOR_INDEX ? markdown
                : markdown.substring(0, markdown.offsetByCodePoints(0, PLAIN_TEXT_FOR_INDEX));
    }
}
