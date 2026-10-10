# 开发约定

本文件适用于本仓库所有代码、资源、文档和构建脚本。用户在当前会话中的明确要求优先于本文件。

## 项目事实与决策

- 先读 [文档索引](docs/README.md)、对应架构文档和实施手册，再动手修改。
- 升级目标锁定为 **Minecraft 26.3 + Fabric**（Java 25、Fabric Loader 0.19.5、Fabric API 0.162.0+26.3、Fabric Loom 1.18.3、Gradle 9.8.1）。更换游戏版本或加载器属于架构变更，必须新增 ADR，禁止静默降级或改用 NeoForge/Quilt。
- 升级执行顺序已由用户确定：**① 26.3 + Fabric 移植（最先）→ ② FE 电力 → ③ 反弹道导弹系统（倒数第二）→ ④ 多语言（最后）**。下游阶段不得提前开工，除非实施手册总目录明确允许并行。
- `prod/1.12` 分支是 Forge 1.12.2 的线上维护分支，**不要在该分支上做移植工作**。移植在 `port/26.3-fabric` 分支进行。
- `docs/meetings/` 只记录需求和讨论经过；当前规则以 `docs/architecture/` 和 `docs/decisions/` 为准，执行顺序以 `docs/implementation/README.md` 总目录为准。
- 文档与代码不一致时，在同一个改动中修正文档，不留下互相矛盾的说明。

## 移植不变量

- 模组 ID 保持 `icbmclassic`，Java 根包保持 `icbm.classic`，资源命名空间保持 `icbmclassic`。
- 使用 Mojang 官方名称（26.1 起 Minecraft 不再混淆）；不使用 Yarn，不写任何 `remap`/`modImplementation`。
- 不迁移 1.12 存档（见 ADR 0002）。不写 DataFixer，不为旧方块/物品 ID 做别名。
- 爆炸物采用“平铺化”注册：每种爆炸物的方块、导弹、手雷、炸弹矿车各有独立 ID（见 [registry-ids.md](docs/architecture/registry-ids.md)）。
- 游戏主线程上禁止阻塞：不 `join()`/`get()` 等待线程、不做文件 I/O 之外的长耗时工作。线程化爆炸只在工作线程**计算**，方块修改必须回到服务端主线程按预算执行。
- 客户端代码（渲染、Screen、按键、客户端网络接收）只能出现在 `client` 源集包 `icbm.classic.client..` 下，并只由 `client` 入口注册；服务端运行时不得加载任何 `net.minecraft.client` 类。
- 网络数据包在服务端一律校验：玩家距离、方块是否加载、目标是否存在、数值范围。客户端发来的数字永远不可信。

## 能源

- 电力单位统一为 FE。Fabric 上通过 Team Reborn Energy API（`teamreborn:energy:5.0.0`）对外互通，换算固定 **1 FE = 1 E**（见 ADR 0003）。
- 机器内部只调用 `icbm.classic.lib.energy` 门面，禁止在机器类中直接 `import team.reborn.energy..`，以便未来增加其他能源 API 适配。
- `ConfigMain.REQUIRES_POWER = false` 时所有机器视为满电，这一行为必须保留。

## 反导系统

- 反导拦截的服务端逻辑必须确定性可测：数学部分（拦截点、弹着点预测、近炸判定）放在不依赖 Minecraft 的纯 Java 类中，并有 JUnit 测试。
- 雷达一次只为同一目标分配配置上限数量的拦截弹，禁止“所有发射台同时齐射”的旧行为回归。

## 国际化

- 语言文件为 `assets/icbmclassic/lang/<code>.json`，`en_us.json` 是唯一事实源；其他语言只能翻译已有键，不能新增键。
- 代码中所有面向玩家的文字必须使用翻译键（`Component.translatable`），禁止新增硬编码英文字符串。
- 翻译必须遵守 [术语表](docs/architecture/i18n.md#术语表)。机器翻译完成不等于验收完成，母语审校是人工门禁。

## 依赖与供应链

- 所有依赖写精确版本，不写 `+`、`latest`、范围版本。新增依赖必须先写进 `docs/implementation/references.md`。
- 运行时只允许依赖 Fabric API 和 Team Reborn Energy；其他模组（JEI/REI/EMI、Mekanism 等）的联动属于后续范围，不在本轮实施。
- 保留上游许可证（`LICENSE.md`）、资源许可证（`src/main/resources/assets/icbmclassic/LICENSE.md`）和贡献者名单。

## 代码与提交

- 一次提交只包含一个可单独审阅的变更（通常是一张步骤卡）；先检查工作区已有改动并保留他人修改。
- 提交说明使用**小写英文类型前缀 + 简洁中文说明**，例如 `feat(radar): 增加雷达航迹表`、`docs: 补充阿拉伯文兼容性边界`。类型前缀必须满足仓库钩子 `scripts/commit-msg`：`^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\([a-z \-]+\))?!?: .+$`（与 `../Minecraft-Server-Launcher` 的 `Feat: …` 写法相比，本仓库因钩子要求类型小写）。步骤卡会给出确切提交信息，照抄。
- 不自动更改作者信息，**不添加模型或 Bot 的协作者署名**（不写 `Co-Authored-By` 等行）。
- 用户没有要求提交时不自行创建提交。不 push、不 force、不 rebase、不改写历史。
- 配置文件、日志样例与文档中的令牌、密钥必须是明显虚构的值。
- 遵守 `.editorconfig`；代码风格与周边代码一致（大括号换行风格以所在文件为准）。

## 模型分工与协同验收规范

- **DeepSeek / Oh-my-pi 执行约定**（执行 Agent）：
  - 务必小心翼翼地严格按照文档设计与架构规范进行编写与执行。
  - 以完成单元测试（JUnit）与 GameTest 为目标，确保各项测试通过并达到步骤卡的完成判据。
  - 完成任务后**不应该原子化提交**，而是保留工作区变更，等待 Gemini 验收后再行提交。
  - 任务结束后，必须写一串专门的交接提示词（Handover Prompt），详述本轮修改中仓库发生了什么、改动了哪些文件与模块、实现了哪些功能以及测试现状，让 Gemini 能够准确明白上下文并接管验收测试。格式见实施手册 §8.2。
- **Gemini / Antigravity / Pi 执行约定**（验收 Agent）：
  - 接手任务时，必须严格通过 WSL 互操作完成冒烟测试（复跑步骤卡“验证”命令，必要时 `runServer`/`runGameTest`）。
  - **重要约束**：测试时遇到问题**应该停下等待人类输入**，严禁擅自忽略错误或未沟通盲目修正。
  - 在冒烟测试通过且符合预期后，统一由 Gemini 负责执行代码提交。
- 图形客户端实机、其他模组联机、母语审校属于人工门禁，任何 Agent 都不能代替人勾选。

## 环境与命令执行安全

- **禁止全盘与高层级扫描**：严禁使用 `find` 或递归搜索工具扫描根级别文件夹、宿主挂载盘及超大型目录（例如 `/mnt/c`、`/mnt/d` 或其下的超大型文件夹）。查找文件必须限定在具体的项目子目录或相对路径中，并尽可能限制检索深度，防止 WSL 跨系统 I/O 卡死与资源耗尽。`~/.gradle/caches` 下查找时必须带具体子路径（见实施手册 §4.3）。
- 运行 Gradle 前必须 `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。
- 长时间运行的 `runServer`/`runClient`/`runGameTest` 进程在验证结束后必须关闭。

## 文档与 TODO

- 架构行为写在 `docs/architecture/`；决策变更追加 `docs/decisions/` ADR，不改写旧 ADR 的结论。
- TODO 状态只有 `TODO`、`DOING`、`BLOCKED`、`DONE`；`DONE` 必须链接证据。
- 用“已决定”“待验证”“后续范围”区分产品决策、技术验证和未排期功能。没有实机证据不得写“已支持”“已兼容”“已验证”。
