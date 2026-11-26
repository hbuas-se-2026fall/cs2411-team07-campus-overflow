package com.campusoverflow.reputation.domain;

import java.math.BigInteger;

public class ReputationRulebook {
    // 采纳回答 +15
    public static final BigInteger ANSWER_ACCEPTED = BigInteger.valueOf(15);
    // 点赞 +10
    public static final BigInteger UP_VOTE = BigInteger.valueOf(10);
    // 点踩 -2
    public static final BigInteger DOWN_VOTE = BigInteger.valueOf(-2);
    // 赞改踩：先扣10，再扣2，合计-12
    public static final BigInteger VOTE_CHANGE_UP_TO_DOWN = BigInteger.valueOf(-12);
    // 单日正向声誉上限
    public static final BigInteger DAILY_UP_LIMIT = BigInteger.valueOf(200);
    // 教师初始悬赏积分
    public static final BigInteger TEACHER_INITIAL_BOUNTY = BigInteger.valueOf(1000);
    public static final BigInteger STUDENT_INITIAL_BOUNTY = BigInteger.ZERO;


    // ========== 下面这一批就是你测试里要调用的方法，补上之后test不红了 ==========
    public static BigInteger acceptedDelta(long authorId, long questionOwnerId) {
        // 自问自答，不加声誉
        if (authorId == questionOwnerId) {
            return BigInteger.ZERO;
        }
        return ANSWER_ACCEPTED;
    }

    public static BigInteger voteDelta(int oldValue, int newValue) {
        int delta = newValue - oldValue;
        return BigInteger.valueOf(delta);
    }

    public static BigInteger applyDailyCap(BigInteger alreadyGained, BigInteger incoming) {
        // 只对正向收益做上限控制，负数直接放行
        if(incoming.compareTo(BigInteger.ZERO) <= 0){
            return incoming;
        }
        BigInteger remaining = DAILY_UP_LIMIT.subtract(alreadyGained);
        if(remaining.compareTo(BigInteger.ZERO) <= 0){
            return BigInteger.ZERO;
        }
        return incoming.min(remaining);
    }

    public static BigInteger initialPoints(boolean isTeacher) {
        return isTeacher ? TEACHER_INITIAL_BOUNTY : STUDENT_INITIAL_BOUNTY;
    }

    // 原来的getter保留，给你的事件处理器代码继续用
    public BigInteger getScoreForAcceptAnswer() {
        return ANSWER_ACCEPTED;
    }
    public BigInteger getScoreForUpVote() {
        return UP_VOTE;
    }
    public BigInteger getScoreForDownVote() {
        return DOWN_VOTE;
    }
    public BigInteger getScoreForChangeUpToDown() {
        return VOTE_CHANGE_UP_TO_DOWN;
    }
    public BigInteger getDailyUpLimit() {
        return DAILY_UP_LIMIT;
    }
}
