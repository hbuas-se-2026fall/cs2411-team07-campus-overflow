\# 领域事件目录



| 事件名称 | 发布上下文 | 触发用例 | 订阅者与处理 |

|---|---|---|---|

| UserRegisteredEvent | Identity | 用户注册/创建教职工 | Reputation：开立声誉账户，发放初始积分 |

| QuestionPostedEvent | Q\&A | 发布问题 | Discovery：插入搜索投影 |

| QuestionEditedEvent | Q\&A | 编辑问题 | Discovery：更新搜索投影文本与标签 |

| AnswerSubmittedEvent | Q\&A | 提交回答 | Reputation：授初来乍到徽章；Discovery：回答计数+1，通知提问者 |

| AnswerAcceptedEvent | Q\&A | 采纳最佳答案 | Reputation：声誉+15、发徽章、结算悬赏；Discovery：标记已采纳，通知回答者 |

| AnswerEndorsedEvent | Q\&A | 教师认证回答 | Reputation：授名师认可徽章；Discovery：通知回答者 |

| VoteCastEvent | Q\&A | 投票/改投/撤销 | Reputation：计算声誉增量（受每日上限）；Discovery：同步得分 |

| CommentPostedEvent | Q\&A | 发表评论 | Discovery：解析@提及，发送通知 |

| ContentDeletedEvent | Q\&A | 软删除问答 | Discovery：投影标记删除，更新统计 |

| ReputationChangedEvent | Reputation | 声誉记账 | Reputation：更新排行榜Redis ZSET |

| BadgeAwardedEvent | Reputation | 授予徽章 | Discovery：推送徽章通知 |

| BountyOpened / Awarded / ClosedEvent | Reputation | 悬赏相关操作 | Discovery：维护悬赏标记，发送悬赏通知 |



