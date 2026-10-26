\# 架构理解笔记

\## 一、模块化单体与六边形分层

本项目采用模块化单体架构，内部划分4个限界上下文：

\- Identity（身份与课程，参考实现样例）

\- Q\&A（问答业务）

\- Reputation（声誉、积分激励）

\- Discovery（内容检索发现）



每个上下文严格分为五层：api、domain、application、infrastructure、interfaces

1\. api层：对外契约，存放查询DTO、集成事件记录，仅依赖JDK和共享内核，不包含业务逻辑。

2\. domain领域层：聚合根、实体、值对象、领域策略、仓储接口；禁止引入Spring、JPA等框架，仅依赖共享内核，所有核心业务规则写在此层。

3\. application应用层：用例编排，负责加载聚合、调用领域方法、持久化、发布领域事件、审计日志、缓存失效，定义事务边界。

4\. infrastructure基础设施层：仓储适配器、JPA数据库实体、Redis实现，实现domain层定义的仓储接口。

5\. interfaces接口层：REST Controller，接收前端请求，封装操作者信息，调用应用层服务。



\## 二、8条ADR决策摘要

\### ADR‑001 模块化单体 + 六边形分层

决策：整体为模块化单体，内部按限界上下文拆分，遵循六边形架构分层；跨上下文通信只能依赖对方api包或者监听集成事件。

约束：禁止直接导入其他上下文domain、application包下代码。



\### ADR‑002 事务性发件箱模式

在同一个本地事务内完成聚合保存 + 写入outbox\_event事件；后台程序中继消费事件；投递保证至少一次，消费端必须做幂等，防止重复事件造成数据错误。



\### ADR‑003 Redis会话存储替代JWT认证

使用Cookie+CSRF双提交方案；封禁账号时直接删除Redis内会话，账号可以立刻下线。



\### ADR‑004 声誉使用追加式账本 + 定期对账

Reputation模块所有分数变动全部写入流水账本LedgerEntry；用户当前声誉值为全部流水之和；定时任务对账，发现数据不一致告警。



\### ADR‑005 MySQL ngram全文索引

用于中文内容检索，最小分词长度为2，单个汉字无法检索。



\### ADR‑006 用户内容软删除

所有业务内容使用deletedAt字段标记删除，不会物理删除数据库行；已删除内容不在列表、搜索结果展示。



\### ADR‑007 OpenAPI契约驱动前后端协作

接口契约统一维护在Swagger OpenAPI，分页返回格式统一，时间字段统一使用UTC ISO‑8601格式。



\### ADR‑008 领域对象和JPA实体分离

domain聚合对象不等于数据库JPA实体；计数器（回答数量、分数等）不通过聚合set方法修改，由仓储执行原子SQL更新。



\## 三、Identity上下文学习总结

Identity是完整参考样例，示范DDD五层标准写法：

1\. domain层：User、Course、CourseMember聚合，PasswordPolicy值对象，仓储接口；业务校验写在领域工厂方法内。

2\. application层：UserRegistrationService等应用服务，标准流程：参数校验→工厂创建聚合→保存聚合→发布领域事件→审计记录。

3\. infrastructure层：JPA映射实体、BCrypt密码加密、Redis限流实现。

4\. interfaces层：AuthController、CourseController，对外提供REST接口。



\## 四、架构测试FF约束规则

1\. FF‑1 domain包不能依赖Spring、JPA等外部框架

2\. FF‑2 上下文之间只能依赖对方\*-api包

3\. FF‑4 依赖方向只能由外向内，领域层不能调用应用层

4\. FF‑6 JPA实体只允许放在infrastructure包，数据库表带上下文前缀：id\_ / qa\_ / rep\_ / dis\_

