package com.campusoverflow.qa.application.command;

import com.campusoverflow.shared.security.Actor;

public record AcceptAnswerCommand(
        Long questionId,
        Long answerId,
        Actor operator,
        boolean operatorManagesCourse
) {
}
