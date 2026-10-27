\# 团队工作约定文档

\## DoR（就绪定义：用户故事进入Sprint待办的条件）

1\. 用户故事遵循格式：作为【角色】，我希望【功能】，以便【价值】。

2\. 附带可验证的验收标准，采用 Given/When/Then 格式编写。

3\. 完成故事点估算，识别清楚上下游依赖任务。

4\. 明确关联对应的接口、领域代码、测试范围。



\## DoD（完成定义：任务视为交付完成的标准）

1\. 代码通过Pull Request合并入develop分支，至少一名非作者评审approve。

2\. 领域层、应用层单元测试全部通过，不修改、删除原有测试断言。

3\. ArchUnit架构测试全部通过，不违反FF架构约束。

4\. CI流水线执行状态为绿色；领域模块测试覆盖率达标。

5\. 接口类任务：Swagger文档可查看，错误码符合项目约定；前端任务执行npm run build编译成功。

6\. 可在本地docker compose环境手动验证功能符合验收标准。



\## Git提交规范

提交信息格式：`type(scope): 简短描述 (#Issue编号)`

type可选值：feat / fix / test / refactor / docs / ci / chore

示例：`docs(team): add team agreement doc (#12)`



\## 分支命名约定

\- 新增功能分支：`feature/issue号‑简短描述`

\- bug修复分支：`fix/issue号‑简短描述`

禁止直接push到develop、main，所有改动必须走PR评审流程。

