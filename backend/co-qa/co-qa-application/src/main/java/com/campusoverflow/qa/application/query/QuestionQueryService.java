package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答查询用例（读路径）。
 * <p>直接查投影行（避开聚合装配），批量向 Identity 取作者信息避免 N+1；
 * 列表走 Cache-Aside（TTL + 抖动，写操作后由命令侧整体失效）。</p>
 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;
    private static final int MAX_TAGS = 50;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        if (cache != null) {
            Optional<PageResult<QuestionSummaryView>> hit = cache.get(courseId, page);
            if (hit.isPresent()) {
                return hit.get();
            }
        }

        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
        Map<Long, UserSummary> authors = usersOf(rows.items().stream().map(QuestionRow::authorId).toList());
        PageResult<QuestionSummaryView> result =
                rows.map(row -> summary(row, authors.get(row.authorId())));

        if (cache != null) {
            cache.put(courseId, page, result);
        }
        return result;
    }

    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow row = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-0101", "问题不存在：" + questionId));

        // 浏览量先写 Redis，定时批量落库（避免热点行写放大）；展示值仍取读模型
        if (viewCounter != null) {
            viewCounter.increment(questionId);
        }

        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        Map<String, Integer> myVotes = actorOrNull == null
                ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId,
                        answerRows.stream().map(AnswerRow::id).toList());

        // 一次性批量取全部作者（问题作者 + 回答作者 + 认证人 + 评论作者），避免 N+1
        Set<Long> authorIds = new LinkedHashSet<>();
        authorIds.add(row.authorId());
        for (AnswerRow answer : answerRows) {
            authorIds.add(answer.authorId());
            if (answer.endorsedBy() != null) {
                authorIds.add(answer.endorsedBy());
            }
        }
        for (CommentRow comment : commentRows) {
            authorIds.add(comment.authorId());
        }
        Map<Long, UserSummary> authors = usersOf(authorIds);

        boolean manager = actorOrNull != null && identity != null
                && identity.canManageCourse(actorOrNull.userId(), row.courseId());
        boolean author = actorOrNull != null && actorOrNull.userId() == row.authorId();

        // 权限视图只用于前端按钮展示，真正的校验仍在领域层
        PermissionsView permissions = actorOrNull == null
                ? PermissionsView.NONE
                : new PermissionsView(author || manager,
                        manager || (author && row.answerCount() == 0),
                        row.acceptedAnswerId() == null && (author || manager),
                        manager);

        List<CommentView> questionComments = commentViews(commentRows, "QUESTION", row.id(), authors);
        List<AnswerView> answers = answerRows.stream()
                .map(answer -> answerView(answer, row, commentRows, myVotes, authors, actorOrNull, manager))
                .toList();

        return new QuestionDetailView(row.id(), row.title(), row.body(), toSafeHtml(row.body()), row.tags(),
                courseOf(row.courseId()), AuthorView.of(row.authorId(), authors.get(row.authorId())), row.score(),
                row.answerCount(), row.viewCount(), row.acceptedAnswerId(),
                myVotes.getOrDefault("QUESTION:" + row.id(), 0), row.createdAt(), row.updatedAt(), permissions,
                questionComments, answers);
    }

    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(Math.min(Math.max(limit, 1), MAX_TAGS));
    }

    private QuestionSummaryView summary(QuestionRow row, UserSummary author) {
        return new QuestionSummaryView(row.id(), row.title(), excerpt(row.body()), row.tags(), row.courseId(),
                AuthorView.of(row.authorId(), author), row.score(), row.answerCount(), row.viewCount(),
                row.acceptedAnswerId() != null, row.createdAt());
    }

    private AnswerView answerView(AnswerRow answer, QuestionRow question, List<CommentRow> commentRows,
                                  Map<String, Integer> myVotes, Map<Long, UserSummary> authors, Actor actorOrNull,
                                  boolean manager) {
        boolean editable = actorOrNull != null && (actorOrNull.userId() == answer.authorId() || manager);
        AuthorView endorsedBy = answer.endorsedBy() == null
                ? null
                : AuthorView.of(answer.endorsedBy(), authors.get(answer.endorsedBy()));
        return new AnswerView(answer.id(), answer.body(), toSafeHtml(answer.body()),
                AuthorView.of(answer.authorId(), authors.get(answer.authorId())), answer.score(),
                Objects.equals(question.acceptedAnswerId(), answer.id()), endorsedBy,
                myVotes.getOrDefault("ANSWER:" + answer.id(), 0), editable, answer.createdAt(), answer.updatedAt(),
                commentViews(commentRows, "ANSWER", answer.id(), authors));
    }

    private List<CommentView> commentViews(List<CommentRow> rows, String targetType, long targetId,
                                           Map<Long, UserSummary> authors) {
        return rows.stream()
                .filter(comment -> targetType.equals(comment.targetType()) && comment.targetId() == targetId)
                .map(comment -> new CommentView(comment.id(),
                        AuthorView.of(comment.authorId(), authors.get(comment.authorId())), comment.parentId(),
                        comment.body(), comment.createdAt()))
                .toList();
    }

    private CourseRefView courseOf(Long courseId) {
        if (courseId == null || identity == null) {
            return null;
        }
        return identity.findCourse(courseId)
                .map(course -> new CourseRefView(course.id(), course.code(), course.name()))
                .orElse(null);
    }

    private Map<Long, UserSummary> usersOf(Collection<Long> userIds) {
        if (identity == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return identity.findUsers(userIds);
    }

    private String toSafeHtml(String markdown) {
        return renderer == null ? markdown : renderer.toSafeHtml(markdown);
    }

    private String excerpt(String markdown) {
        if (renderer != null) {
            return renderer.toPlainText(markdown, EXCERPT_LENGTH);
        }
        if (markdown == null) {
            return "";
        }
        int length = markdown.codePointCount(0, markdown.length());
        return length <= EXCERPT_LENGTH ? markdown
                : markdown.substring(0, markdown.offsetByCodePoints(0, EXCERPT_LENGTH));
    }
}
