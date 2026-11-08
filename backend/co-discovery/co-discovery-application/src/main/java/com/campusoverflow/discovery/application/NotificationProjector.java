package com.campusoverflow.discovery.application;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知中心：把上游事件转换为站内通知。
 * 【Walking Skeleton】Sprint1 空实现，避免发件箱阻塞；Sprint3 替换真实业务逻辑
 */
@Component
public class NotificationProjector {

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO(S3) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO(S3) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        // TODO(S3) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(CommentPostedEvent e) {
        // TODO(S3) see tech-debt issue #18
    }

    @EventListener
    @Transactional
    public void on(BountyAwardedEvent e) {
        // TODO(S3) see tech-debt issue #18
    }
}