package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 【Walking Skeleton】Sprint1空实现，避免发件箱阻塞；Sprint2/Sprint3替换真实业务逻辑
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 */
@Component
public class ReputationEventHandlers {

    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        // TODO(S2) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO(S2) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        // TODO(S2) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO(S2) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        // TODO(S2) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(CommentPostedEvent e) {
        // TODO(S2) see tech-debt issue #18
    }
}