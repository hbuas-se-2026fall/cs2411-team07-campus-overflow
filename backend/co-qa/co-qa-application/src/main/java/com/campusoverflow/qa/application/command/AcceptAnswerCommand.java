package com.campusoverflow.qa.application.command;

import com.campusoverflow.shared.security.Actor;

/**
 * 采纳回答命令（场景 R-2）。
 *
 * @param questionId           被采纳回答所属的问题
 * @param answerId             被采纳的回答
 * @param operator             操作者
 * @param operatorManagesCourse 操作者是否为该课程的管理者（教师/助教/管理员），由应用层通过 Identity 开放主机服务判定
 */
public record AcceptAnswerCommand(long questionId, long answerId, Actor operator, boolean operatorManagesCourse) {
}
