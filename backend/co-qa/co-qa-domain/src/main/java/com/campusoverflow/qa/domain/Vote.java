package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import java.time.Instant;

/** 投票聚合：同一用户对同一内容仅一票（数据库唯一约束兜底），值为 +1 或 -1；撤销即删除。 */
public class Vote {

    private Long id;
    private final long voterId;
    private final TargetType targetType;
    private final long targetId;
    private int value;
    private Instant updatedAt;

    private Vote(Long id, long voterId, TargetType targetType, long targetId, int value, Instant updatedAt) {
        this.id = id;
        this.voterId = voterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public static Vote cast(long voterId, long targetAuthorId, TargetType targetType, long targetId, int value,
                            Instant now) {
        checkValue(value);
        // 业务规则：不能给自己投票
        if(voterId == targetAuthorId){
            throw new BusinessRuleException("QA-0303", "不允许给自己的内容投票");
        }
        return new Vote(null, voterId, targetType, targetId, value, now);
    }

    public static Vote reconstitute(Long id, long voterId, TargetType targetType, long targetId, int value,
                                    Instant updatedAt) {
        return new Vote(id, voterId, targetType, targetId, value, updatedAt);
    }

    /** 改投，返回原值。 */
    public int change(int newValue, Instant now) {
        checkValue(newValue);
        if(this.value == newValue){
            throw new ConflictException("QA-0301", "不能重复投相同票数");
        }
        int oldVal = this.value;
        this.value = newValue;
        this.updatedAt = now;
        return oldVal;
    }

    private static int checkValue(int value) {
        if(value != 1 && value != -1){
            throw new BusinessRuleException("QA-0302", "投票只允许 +1 或者 -1");
        }
        return value;
    }

    public void assignId(long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public long voterId() { return voterId; }
    public TargetType targetType() { return targetType; }
    public long targetId() { return targetId; }
    public int value() { return value; }
    public Instant updatedAt() { return updatedAt; }

    public enum TargetType{
        QUESTION, ANSWER, COMMENT
    }
}