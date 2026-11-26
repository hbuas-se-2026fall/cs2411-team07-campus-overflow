package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 应用服务用例测试：评论编排（归属问题解析、单层回复校验、事件发布）。 */
class CommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryComments comments = new InMemoryComments();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private CommentService commentService;
    private long questionId;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(comments, questions, answers, events, clock);
        Question q = questions.save(Question.post(ming.userId(), 100L, new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        questionId = q.id();
    }

    @Test
    @DisplayName("在问题上发表评论：解析出所属问题并发布 CommentPosted 事件")
    void postCommentOnQuestion() {
        long commentId = commentService.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, null,
                "补充一个反例：插入 4 的递增序列。"));

        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).commentId()).isEqualTo(commentId);
        assertThat(posted.get(0).questionId()).isEqualTo(questionId);
        assertThat(posted.get(0).authorId()).isEqualTo(ming.userId());
    }

    @Test
    @DisplayName("回答下的评论归属到回答所在的问题")
    void commentOnAnswerResolvesQuestionId() {
        long answerId = answers.save(com.campusoverflow.qa.domain.Answer.submit(
                questions.findById(questionId).orElseThrow(), hong.userId(),
                MarkdownBody.ofAnswer("关键在于黑高这个不变量。"), NOW)).id();

        long commentId = commentService.post(ming, new PostCommentCommand(TargetType.ANSWER, answerId, null,
                "这个解释很清楚。"));
        assertThat(comments.findById(commentId)).isPresent();
    }

    @Test
    @DisplayName("二级回复允许，三级回复被拒绝")
    void onlyOneLevelOfReply() {
        long topId = commentService.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, null,
                "请问第二步为什么要变色？"));

        long replyId = commentService.post(hong, new PostCommentCommand(TargetType.QUESTION, questionId, topId,
                "因为要保持黑高一致。"));
        assertThat(comments.findById(replyId).orElseThrow().parentId()).isEqualTo(topId);

        assertThatThrownBy(() -> commentService.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId,
                replyId, "再回复一层")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("仅支持一层评论回复");
    }

    @Test
    @DisplayName("空评论被领域层拒绝")
    void blankCommentRejected() {
        assertThatThrownBy(() -> commentService.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId,
                null, "   ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("评论内容不能为空");
    }

    /** 评论仓储替身：应用服务只依赖端口接口。 */
    static class InMemoryComments implements CommentRepository {
        private final Map<Long, Comment> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Comment> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Comment save(Comment comment) {
            if (comment.id() == null) {
                comment.assignId(sequence.incrementAndGet());
            }
            store.put(comment.id(), comment);
            return comment;
        }
    }
}
