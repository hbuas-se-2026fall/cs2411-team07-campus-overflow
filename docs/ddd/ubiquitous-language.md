\# 统一语言 Ubiquitous Language



会议、编码、Issue、文档统一使用本词汇表，避免一词多义。



\## Q\&A 问答上下文



| 术语 | 代码名 | 业务规则 |

|---|---|---|

| 问题 | Question | 学生发布求助；标题10-150字符，Markdown正文，1-5个标签，可关联课程 |

| 回答 | Answer | 对问题的解答，独立聚合根，通过questionId关联问题 |

| 采纳 | accept | 提问者/课程管理者选定最佳答案；一题最多一次，不可改 |

| 教师认证 | endorse | 课程教师给回答打权威标记；每个回答只能认证一次 |

| 投票 | Vote | +1赞成 / -1反对 / 0撤销；一条内容只能投一票，可改投、撤销 |

| 评论 | Comment | 问题/回答下的评论，仅支持一层回复 |

| 软删除 | softDelete / deletedAt | 标记时间戳，不物理删除；删除后不可编辑、不可采纳 |

| 课程管理者 | actorManagesCourse | 平台管理员、课程教师、助教，由IdentityApi判定 |



\## Reputation 声誉上下文



| 术语 | 代码名 | 业务规则 |

|---|---|---|

| 声誉 | reputation | 信誉分，只能通过账本流水变动；采纳+15，被赞+10，被踩-2 |

| 账本流水 | LedgerEntry | 只追加；(eventId,userId,reason) 作为幂等唯一键 |

| 每日投票上限 | DAILY\_VOTE\_CAP | 同一UTC日，投票带来的正向声誉最多200 |

| 悬赏 | Bounty | 课程管理者发起；状态OPEN/AWARDED/EXPIRED；10-500积分，1-14天期限 |

| 徽章 | BadgeType | 初来乍到、初露锋芒、解惑达人、名师认可、小有名气、德高望重 |



\## Discovery 内容发现上下文



| 术语 | 代码名 | 业务规则 |

|---|---|---|

| 搜索投影 | SearchDocument | Discovery独立模型，用于全文检索 |

| 相关问题 | RelatedQuestionPolicy | 基于标签Jaccard相似度推荐 |

| @提及 | MentionParser | 解析评论@昵称；每条评论最多通知5人 |

| 通知 | Notification | 站内消息；(sourceEventId,recipientId)唯一，仅接收人可标记已读 |



\## 公共



| 术语 | 代码名 | 业务规则 |

|---|---|---|

| 集成事件 | IntegrationEvent | 跨上下文通信的不可变record，带全局eventId |

| 发布语言 | \*-api | 各上下文对外契约包，只放DTO、查询接口、事件 |

| 操作者 | Actor | userId、displayName、role；接口层传入，领域不依赖Spring Security |



