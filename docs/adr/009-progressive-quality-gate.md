\# ADR 009 渐进式CI质量门禁

\## 状态

Accepted

\## 背景

项目采用模块化单体架构，Sprint0为准备阶段，不需要全量模块测试，避免前期CI频繁失败阻塞团队工作。随着迭代推进，逐步扩大自动化测试门禁范围。

\## 决策

采用渐进式质量门禁策略：

1\. Sprint0阶段：CI仅运行 co-shared-kernel、co-identity/co-identity-domain 的单元测试；Q\&A、Reputation、Discovery模块单元测试跳过；集成测试设置continue-on-error，允许失败，不阻断合并。

2\. Sprint1阶段：新增开启Q\&A上下文单元测试门禁，SPRINT\_MODULES修改为`co-shared-kernel,co-identity/co-identity-domain,co-qa/co-qa-domain`。

3\. Sprint2阶段：新增开启Reputation上下文单元测试门禁，SPRINT\_MODULES修改为`co-shared-kernel,co-identity/co-identity-domain,co-qa/co-qa-domain,co-reputation/co-reputation-domain`。

4\. Sprint3阶段：新增开启Discovery上下文单元测试门禁。

5\. Sprint4阶段：全模块门禁开启，所有单元、架构、集成测试必须全部通过，CI绿灯才允许合并PR；同时删除集成测试`continue-on-error: true`，集成测试纳入硬性门禁。

\## 后果

优势：Sprint前期降低环境、未完成代码带来的CI阻塞，保障Sprint0准备工作顺利推进；迭代后期收紧质量门槛，保障上线质量。

风险：前期部分模块缺少自动化测试校验，需要依赖人工测试补足。



