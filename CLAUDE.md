# Claude 工作说明

请先读取仓库根目录的 [AGENTS.md](AGENTS.md)，并遵守其中的版本锁定、移植不变量、能源、反导、国际化、模型分工与提交规则。

## 本仓库的工作流程

1. 阅读 `docs/README.md`、对应架构文档，以及 `docs/implementation/README.md` 中当前步骤卡与阶段说明。
2. 修改前查清相关接口：用 `javap` 核对 26.3 的真实类名与签名（命令见实施手册 §4.3）。教程、旧代码和记忆中的方法名都可能过时。
3. 1.12 旧代码在 `port/26.3-fabric` 分支的 `legacy-1.12/` 目录（S0.02 之后），只用于参考，不参与编译，**不要修改**。
4. 把改动限制在当前步骤卡范围内。不要在一个需求里顺手重构、顺手加功能、提前做后续阶段（顺序：26.3 移植 → FE → 反导 → i18n）。
5. 报告具体改动、执行过的验证和未覆盖的场景。没有在真实客户端里看过的东西，不要写“已支持”“已验证”。
6. 作为执行 Agent 时不提交，结束时输出交接提示词；作为验收 Agent 时遇到问题停下等待人类（AGENTS.md“模型分工与协同验收规范”）。

## 项目特定提醒

- 目标锁定 Minecraft 26.3 + Fabric（Java 25、Loader 0.19.5、Fabric API 0.162.0+26.3、Loom 1.18.3、Gradle 9.8.1）；改变版本属于架构变更，须新增 ADR，禁止静默降级。
- 1.12 的 `TileEntity` 对应 26.3 的 `BlockEntity`，`Capability` 对应 Fabric API Lookup / Data Attachment / DataComponent，完整对照见 `docs/architecture/porting-guide.md`。
- 爆炸物是平铺化 ID，不再有 metadata 子类型；ID 只能来自 `docs/architecture/registry-ids.md`。
- 电力单位叫 FE，代码通过 `icbm.classic.lib.energy` 门面访问，底层是 Team Reborn Energy 5.0.0。
- 雷达、EMP 塔、反弹道导弹的交互规则见 `docs/architecture/anti-ballistic-missile.md`。
- 面向玩家的文字一律 `Component.translatable`，键加入 `en_us.json`。
- 提交说明：小写类型前缀 + 中文说明（钩子要求），不加模型/Bot 协作者署名。
