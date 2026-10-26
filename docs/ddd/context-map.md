\# 上下文映射 Context Map



\## 共享内核（Shared Kernel）

`co-shared-kernel`：全部上下文共同依赖，存放公共值对象、通用异常、集成事件基类；修改必须全员评审，禁止某个上下文私自改动。



\## 上游 → 下游 集成关系



| 上游 → 下游 | 集成模式 | 集成手段 | 说明 |

|---|---|---|---|

| Identity → Q\&A / Reputation / Discovery | 开放主机服务 OHS | 同步 UserApi + 异步 UserRegisteredEvent | 下游只拿 UserSummary，不获取密码等隐私 |

| Q\&A → Reputation | 客户-供应商 | 异步 AnswerAccepted/VoteCast 等事件 + 同步 QaQueryApi | Reputation 记账所需字段由事件携带 |

| Q\&A → Discovery | 遵奉者 Conformist | 异步 Question/Answer/Vote/Comment/ContentDeleted 事件 | Discovery 维护自己的搜索投影 |

| Reputation → Discovery | 遵奉者 Conformist | 异步 Bounty/BadgeAwarded 事件 | 用于悬赏标记、徽章通知 |



\## 子域划分



| 子域 | 类型 | 对应限界上下文 |

|---|---|---|

| 课程问答 | 核心域 | Q\&A |

| 声誉与激励 | 核心域 | Reputation |

| 身份与课程 | 支撑子域 | Identity |

| 内容发现 | 通用支撑子域 | Discovery |



