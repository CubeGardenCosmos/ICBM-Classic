# 实施手册（给执行 Agent）

> 本手册写给**逐步执行、无人实时监督**的实施 Agent。你不需要也不允许自己做架构决定：每一步做什么、改哪些文件、怎么验证、失败怎么办，都写在步骤卡里。照做即可。步骤卡没写到、或与仓库现状对不上，按 [§6 BLOCKED 规程](#6-blocked-规程) 处理，**不要猜**。

编写日期：2026-10-10。依据：[文档索引](../README.md)、ADR 0001–0006、`prod/1.12` 提交 `e862b4f15` 的代码。

---

## 目录

1. [你必须先读的东西](#1-你必须先读的东西)
2. [黄金规则](#2-黄金规则违反任意一条即停止)
3. [环境与工具位置](#3-环境与工具位置)
4. [常用命令速查](#4-常用命令速查)
5. [步骤卡格式与执行流程](#5-步骤卡格式与执行流程)
6. [BLOCKED 规程](#6-blocked-规程)
7. [角色分工、Git 与提交](#7-角色分工git-与提交)
8. [证据文件与交接提示词](#8-证据文件与交接提示词)
9. [总目录（Catalog）与并行规则](#9-总目录catalog)
10. [人工门禁汇总](#10-人工门禁汇总)

阶段文件：

| 阶段 | 文件 | 对应 TODO |
| --- | --- | --- |
| S0 基线与工具链 | [phase-s0-foundation.md](phase-s0-foundation.md) | P1 工具链 |
| S1 基础库 | [phase-s1-core-lib.md](phase-s1-core-lib.md) | P1 基础库 |
| S2 注册与资源 | [phase-s2-registry-assets.md](phase-s2-registry-assets.md) | P1 注册与资源 |
| S3 爆炸系统与爆炸相关实体 | [phase-s3-blasts.md](phase-s3-blasts.md) | P1 爆炸 |
| S4 导弹与投掷物 | [phase-s4-entities-missiles.md](phase-s4-entities-missiles.md) | P1 导弹 |
| S5 机器、工具与 P1 验收 | [phase-s5-machines.md](phase-s5-machines.md) | P1 机器与工具、验收 |
| S6 FE 电力 | [phase-s6-energy.md](phase-s6-energy.md) | P2 |
| S7 反弹道导弹 | [phase-s7-abm.md](phase-s7-abm.md) | P3 |
| S8 国际化 | [phase-s8-i18n.md](phase-s8-i18n.md) | P4 |
| S9 收尾 | [phase-s9-release.md](phase-s9-release.md) | P5 |
| 外部链接与固定版本 | [references.md](references.md) | — |
| 移植账本（S0.06 生成） | `port-ledger.md` | — |

---

## 1. 你必须先读的东西

开始**任何**步骤前，本会话内至少读过一次：

1. 仓库根目录 [AGENTS.md](../../AGENTS.md)。
2. 本文件全文。
3. 当前步骤所在阶段文件开头的“阶段说明”与当前步骤卡全文。
4. 步骤卡“必读”一栏列出的文件（架构文档对应小节、`legacy-1.12/` 下的旧代码）。
5. [references.md](references.md) 中与该步骤相关的条目。

**你不能使用网页搜索。** 需要的外部资料都在 [references.md](references.md) 给出直接 URL，用 `curl -fsSL <url>` 获取。URL 失效按 §6 处理，不要自己找替代来源。

**26.3 的类名与方法签名以 `javap` 为准**（§4.3）。架构文档里的名称已核实过一部分，但没有核实过的你必须自己核实；网上/记忆中的 1.20、1.21 写法在 26.x 很多已经改名。

---

## 2. 黄金规则（违反任意一条即停止）

1. **版本锁定**：Minecraft 26.3、Java 25、Gradle 9.8.1、Loom 1.18.3、Fabric Loader 0.19.5、Fabric API 0.162.0+26.3、Team Reborn Energy 5.0.0、Lombok 1.18.48、JUnit BOM 5.14.4。不升级、不降级、不写 `+`/`latest`/范围版本。新依赖必须先出现在 references.md。
2. **模组 ID `icbmclassic`、根包 `icbm.classic` 不变。** 注册 ID 只能来自 [registry-ids.md](../architecture/registry-ids.md)。
3. **`legacy-1.12/` 只读。** 永远不修改、不编译、不删除其中文件（S9.01 除外）。
4. **客户端代码只在 `src/client`**。main 源集不得 import `net.minecraft.client.*`、`com.mojang.blaze3d.*`、`net.fabricmc.fabric.api.client.*`。
5. **主线程不阻塞**；工作线程不改世界（overview.md §6）。
6. **服务端校验所有客户端载荷**（porting-guide.md §5）。
7. **不改测试期望来“修复”失败。** 测试失败先修代码；步骤卡明确写“修改测试 X”才可以改测试。
8. **不做步骤卡以外的事**：不顺手重构、不修旧 TODO、不加功能、不改格式化无关代码。
9. **面向玩家的文字用 `Component.translatable`**，键加入 `src/main/resources/assets/icbmclassic/lang/en_us.json`。
10. **能量只经 `icbm.classic.lib.energy`**（AGENTS.md）。
11. **没有实机证据不写“已支持/已验证”**。
12. **不 push、不 force、不 rebase、不改写历史、不删分支。**
13. **WSL 安全**：不对 `/`、`/mnt/*` 递归搜索；所有临时文件放 `$REPO/.dev/tmp/`。
14. **长进程必须关闭**：`runServer`、`runGameTest` 结束后用 `ps -ef | grep -E 'java.*(fabric|gradle)' | grep -v grep` 确认无残留（Gradle daemon 除外：`./gradlew --stop` 关闭）。

---

## 3. 环境与工具位置

执行机：Windows 上的 WSL2（Linux），用户 `klp`。

| 项目 | 位置 / 版本 | 说明 |
| --- | --- | --- |
| 仓库 | `/home/klp/ICBM-Classic`（下称 `$REPO`） | |
| 移植分支 | `port/26.3-fabric`（S0.01 创建） | 所有移植工作都在这里 |
| JDK | `/usr/lib/jvm/java-25-openjdk` | **每个运行 Gradle 的 shell 先 `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`**；不要依赖 shell 默认值 |
| javap | `$JAVA_HOME/bin/javap` | §4.3 |
| Gradle | `$REPO/gradlew`（S0.03 之后为 9.8.1） | S0.03 之前仓库里的 wrapper 是 1.12 时代的，不要用它 |
| 26.3 合并 jar（Mojang 名称） | `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar` | 本机已存在（同机项目构建产生） |
| Fabric API 模块 jar | `~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/<模块名>/<版本>/<hash>/<模块名>-<版本>.jar` | 以本项目依赖解析出的版本为准：`./gradlew dependencies --configuration compileClasspath \| grep fabric-api` |
| 参考项目 | `/home/klp/Minecraft-Server-Launcher/game-mods` | 同版本 Fabric 工具链的可用配置样例（只读） |
| Python | `python3`（标准库即可） | 脚本一律 `python3 -I` 运行 |
| 设备 | 无图形客户端测试能力 | 所有客户端实机项都是人工门禁（§10） |

临时文件：`$REPO/.dev/tmp/`（`.dev/` 在 S0.05 加入 `.gitignore`）。

---

## 4. 常用命令速查

命令从 `$REPO` 起算。

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk

# 完整构建（编译 + JUnit + GameTest）
./gradlew build

# 只跑 JUnit
./gradlew test

# 只跑 GameTest（服务端，无图形）
./gradlew runGameTest

# 数据生成（模型、方块状态、战利品表、标签、配方、英文名称）——修改注册内容后必须跑，并提交 src/main/generated
./gradlew runDatagen

# 专用服务端冒烟（首次需同意 EULA：S0.03 会写好 run/server/eula.txt）
timeout 180 ./gradlew runServer --args="--nogui" 2>&1 | tee .dev/tmp/server.log
#   看到 "Done (" 即成功；然后在另一个 shell 无法交互时，用 timeout 结束即可

# 一键检查（S0.05 之后）
scripts/check.sh

# 语言文件检查（S2.10 之后）
python3 -I scripts/check_lang.py

# 停止 Gradle 守护进程
./gradlew --stop
```

### 4.1 读旧代码

```bash
# 旧文件位置（S0.02 之后）
ls legacy-1.12/src/main/java/icbm/classic/content/blocks/radarstation/
# 在旧代码中找用法
grep -rn "IncomingMissileMessage" legacy-1.12/src/main/java | head
```

### 4.2 读 26.3 源码

```bash
./gradlew genSources            # 生成带源码的 jar（只需一次）
find ~/.gradle/caches/fabric-loom -name '*26.3*sources*.jar' | head
# 解压单个类阅读
unzip -p <sources.jar> net/minecraft/world/level/block/entity/BlockEntity.java | less
```

### 4.3 用 javap 查 26.3 真实签名

```bash
MC=~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar
$JAVA_HOME/bin/javap -cp $MC net.minecraft.world.entity.Entity | grep -E " tick\(|hurtServer"
# 内部类用 $，要加引号
$JAVA_HOME/bin/javap -cp $MC 'net.minecraft.world.item.Item$Properties' | grep setId
# 找一个类在哪个包
unzip -l $MC | grep -i 'SavedDataType'
# Fabric API 类
FAPI=$(find ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-menu-api-v1 -name '*.jar' ! -name '*sources*' | sort | tail -1)
$JAVA_HOME/bin/javap -cp $FAPI net.fabricmc.fabric.api.menu.v1.ExtendedMenuType
```

把你核对过的签名**原样**粘贴到证据文件“签名核对”一节。

---

## 5. 步骤卡格式与执行流程

### 5.1 步骤卡字段

```
### S1.03 SaveHandler 替代 NbtSaveHandler
- 前置：S1.01
- 必读：porting-guide.md §4；legacy-1.12/src/main/java/icbm/classic/lib/saving/**
- 改动文件：新建/修改/删除的完整路径
- 目标：一句话说明完成后系统多了什么行为
- 操作：编号步骤（含代码骨架）
- 验证：要运行的命令 + 期望输出
- 完成判据：全部满足才算完成
- 失败处理：常见失败与对策；超出则 BLOCKED
- 提交信息：照抄（Conventional Commits）
```

### 5.2 执行一个步骤的固定流程

1. 在 §9 总目录找到**第一个**状态为 `[ ]` 且所有“前置”均为 `[x]` 的步骤（前置含 `[!]` 的跳过）。若协调者在会话中指定了步骤，按协调者指定。
2. 把它在总目录中的状态改为 `[~]`。
3. 读步骤卡与“必读”。
4. `git status --short`：若有不属于本步骤的未提交改动，**不要动**，按 §6 BLOCKED（原因：工作区不干净）。
5. 按“操作”逐条执行。
6. 运行“验证”中的全部命令。
7. 更新移植账本（移植类步骤）。
8. 写证据文件（§8），全部完成判据满足 → 总目录状态改为 `[R]`（待验收），证据列填链接；写交接提示词（§8.2）。
9. 不满足 → 按“失败处理”修复，最多**三次不同的修复尝试**；仍失败 → §6。

**禁止**：同时做两个步骤；在本步骤里提前做下一步；跳过验证。

### 5.3 代码骨架的使用

步骤卡与架构文档里的代码块是**骨架**：类名、方法签名、字段名、常量值、文件路径、配置键名必须完全一致；方法体按注释补全。`// TODO(agent):` 是你需要补全的位置。复制时去掉列表缩进。骨架里的 26.3 方法名若与 javap 结果不同，以 javap 为准，并在证据中记录差异——**这不算 BLOCKED**；但若类根本不存在、或需要换一种实现思路，则 BLOCKED。

### 5.4 移植类步骤的通用做法（S1–S5 大多数步骤）

对步骤卡“移植文件”列出的每个旧文件：

1. 打开 `legacy-1.12/src/main/java/<路径>`，通读。
2. 在 `src/main/java/<同路径>`（客户端类在 `src/client/java/<同路径>`）新建同名文件；包名不变。
3. 按 [porting-guide.md](../architecture/porting-guide.md) §2 对照表逐个替换类型与 API。表里没有的：先 javap，再写。
4. 删除 Forge 专用部分（`@SubscribeEvent`、`@SideOnly`、`Capability` 相关覆写），按步骤卡指示接到 Fabric 事件或 API Lookup。
5. 编译：`./gradlew compileJava compileClientJava`。
6. 移植账本中把该文件标为 `PORTED`（或步骤卡指定的 `MERGED→新文件` / `DROPPED`）。

被依赖但尚未移植的类：**不要**顺手移植。若当前步骤卡没有列出它，说明总目录顺序有问题 → BLOCKED（原因：前置缺失），在证据中列出缺失类。

---

## 6. BLOCKED 规程

满足以下任一条件即 BLOCKED：

- 三次不同修复尝试后验证仍失败。
- 外部 URL 失效。
- 步骤卡与仓库现状对不上（文件不存在、前置步骤留下的接口与描述不符、需要的类不在已移植集合中）。
- 需要做步骤卡没写明的选择（两个方案都可行、需要新的注册 ID、需要新依赖）。
- 需要人类操作（图形客户端、真实其他模组联机、母语审校）。
- 工作区存在不属于你的未提交改动。

BLOCKED 时：

1. 保存现场：`git stash push -u -m "BLOCKED S<x.yy>"`。**不要** `git checkout .` 或 `git reset --hard`。
2. 写证据文件 `docs/implementation/evidence/S<x.yy>.md`：`状态：BLOCKED`、触发条件、执行的命令与**原样**输出（最后 80 行以内）、尝试过的修复（逐条）、解除条件（人需要提供什么）、stash 名称。
3. 总目录该步骤改为 `[!]`，证据列填链接。
4. 交接提示词中写明 BLOCKED。依赖它的步骤都不能开始；继续找下一个可做的步骤。

---

## 7. 角色分工、Git 与提交

### 7.1 角色

| 角色 | 做什么 | 不做什么 |
| --- | --- | --- |
| **执行 Agent**（DeepSeek / Oh-my-pi） | 按步骤卡改代码、跑验证、写证据与交接提示词；总目录标 `[R]` | **不提交**；不验收自己的步骤；不做人工门禁 |
| **验收 Agent**（Gemini / Antigravity / Pi） | 通过 WSL 互操作做冒烟测试：读交接提示词与证据；复跑“验证”命令；抽查改动是否越界（`git diff --stat` 只含步骤卡文件 + 账本 + 总目录 + 证据）；通过则提交并把 `[R]` 改 `[x]`，同时勾选 TODO | 发现问题**停下等待人类**，不擅自修代码、不忽略错误 |
| **人类维护者** | 人工门禁、BLOCKED 解除、ADR 变更、合并到主分支 | — |

### 7.2 Git

- 分支：`port/26.3-fabric`。**不要 push、不要合并到 `prod/1.12`、不要改写历史。**
- 每个步骤一个提交（BLOCKED 记录也是单独一个提交，信息 `docs(impl): 记录 S<x.yy> 阻塞原因`）。
- 提交信息格式：**小写英文类型前缀 + 简洁中文说明**（AGENTS.md），例如 `feat(radar): 增加雷达航迹表`；不加任何模型/Bot 协作者署名（不写 `Co-Authored-By`）。必须满足 `scripts/commit-msg` 钩子：`^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\([a-z \-]+\))?!?: .+$`。步骤卡给出确切文本，照抄。
- `git add` 逐个列出文件，不用 `git add -A` / `git add .`。
- 提交前 `git status --short`：出现 `.dev/`、`build/`、`run/`、`.gradle/`、`*.class` 说明 `.gitignore` 缺条目——补进 `.gitignore` 一起提交（唯一允许的额外文件）。

---

## 8. 证据文件与交接提示词

### 8.1 证据文件 `docs/implementation/evidence/S<x.yy>.md`

```markdown
# S1.03 SaveHandler 替代 NbtSaveHandler

- 状态：DONE / BLOCKED
- 日期：YYYY-MM-DD
- 执行者：（Agent 名称/模型）
- 提交：待验收提交
- 环境：WSL2 Linux x86_64；JDK（java -version 第一行）；Gradle 9.8.1

## 签名核对

（javap 原样输出，只贴与本步相关的行）

## 改动文件

（git status --short 的输出）

## 执行的验证

$ ./gradlew build
（最后 30 行原样输出）

## 结果

- 完成判据 1：满足（说明）
- 完成判据 2：满足（说明）

## 与步骤卡的差异

- 例如：骨架写 `foo()`，26.3 实际为 `bar()`，已按 javap 使用 `bar()`。

## 未覆盖 / 已知限制

- 例如：未在图形客户端验证渲染
```

规则：输出原样粘贴；不写超出证据的结论。

### 8.2 交接提示词

执行 Agent 结束时在会话最后输出一段（同时追加到证据文件末尾“交接”一节）：

```text
【交接 S<x.yy>】
分支：port/26.3-fabric，未提交。
目标：<步骤卡目标一句话>
改动：<文件列表，逐行>
实现要点：<3–6 条，包括与骨架的差异>
验证：<命令> → <结果摘要，如 BUILD SUCCESSFUL，GameTest 12/12>
未覆盖：<…>
请验收 Agent：复跑 <命令>；检查 <最容易出错的点>；通过后按提交信息 "<照抄>" 提交。
```

---

## 9. 总目录（Catalog）

状态图例：`[ ]` 未开始 · `[~]` 进行中 · `[R]` 待验收 · `[x]` 完成 · `[!]` BLOCKED · `[H]` 人工门禁（Agent 只准备材料，不能勾选）

“TODO 映射”写法：`P1 基础库 3` = `docs/todo/phase-1-port.md` “基础库”小节第 (3) 条。

**并行规则**：同一时间可以有多个执行 Agent，但**只能**并行“前置均已 `[x]`、且改动文件集合不相交”的步骤。共享文件（`ModBlocks`、`ModItems`、`en_us.json`、`fabric.mod.json`、`build.gradle`、本总目录、移植账本）的修改冲突时，后提交者 rebase 是**禁止**的——由验收 Agent 串行提交，冲突则 BLOCKED 交给人。实际操作上：S1 的 S1.02/S1.06/S1.09 可与 S1.01→S1.03 链并行；S3.04–S3.07 可并行；S8.05–S8.09 可并行。其余默认串行。

### S0 基线与工具链 → [phase-s0-foundation.md](phase-s0-foundation.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [x] | S0.01 | 建立工作分支与基线记录 | P0 文档已提交 | P1 工具链 1 | [S0.01.md](evidence/S0.01.md) |
| [x] | S0.02 | 旧代码移入 `legacy-1.12/` | S0.01 | P1 工具链 1 | [S0.02.md](evidence/S0.02.md) |
| [x] | S0.03 | Fabric 26.3 Gradle 工程骨架 | S0.02 | P1 工具链 2 | [S0.03.md](evidence/S0.03.md) |
| [x] | S0.04 | JUnit 与 GameTest 管线 | S0.03 | P1 工具链 3 | [S0.04.md](evidence/S0.04.md) |
| [x] | S0.05 | `scripts/check.sh` 与 `.gitignore` | S0.04 | P1 工具链 4 | [S0.05.md](evidence/S0.05.md) |
| [x] | S0.06 | 移植账本 | S0.02 | P1 工具链 5 | [S0.06.md](evidence/S0.06.md) |

### S1 基础库 → [phase-s1-core-lib.md](phase-s1-core-lib.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [R] | S1.01 | jlib 与 `lib/transform` | S0.05、S0.06 | P1 基础库 1 | [S1.01.md](evidence/S1.01.md) |
| [R] | S1.02 | JSON 配置系统与全部配置类 | S0.05、S0.06 | P1 基础库 2 | [S1.02.md](evidence/S1.02.md) |
| [R] | S1.03 | `SaveHandler` | S1.01 | P1 基础库 3 | [S1.03.md](evidence/S1.03.md) |
| [R] | S1.04 | 网络 Codex 传输层 | S1.01 | P1 基础库 4 | [S1.04.md](evidence/S1.04.md) |
| [R] | S1.05 | 无线电系统 | S1.03、S1.04 | P1 基础库 5 | [S1.05.md](evidence/S1.05.md) |
| [R] | S1.06 | 雷达注册表与地图 | S1.01 | P1 基础库 6 | [S1.06.md](evidence/S1.06.md) |
| [R] | S1.07 | 能力映射（API Lookup / 附件 / 组件）与 `IcbmEnergyBuffer` | S1.02、S1.03 | P1 基础库 7 | [S1.07.md](evidence/S1.07.md)（能量核心已完成；能力映射部分按依赖顺序下沉 S1.10） |
| [R] | S1.08 | 动作系统 | S1.07 | P1 基础库 8 | [S1.08.md](evidence/S1.08.md) |
| [R] | S1.09 | 线程化爆炸框架 | S1.02 | P1 基础库 9 | [S1.09.md](evidence/S1.09.md) |
| [R] | S1.10 | 公共 API 包收尾（含 S1.07 下沉的 api/caps、lib/capability、IcbmLookups 与 ICBMClassicAPI 完整字段） | S1.05、S1.06、S1.08、S1.09 | P1 基础库 10 | [S1.10.md](evidence/S1.10.md) |

### S2 注册与资源 → [phase-s2-registry-assets.md](phase-s2-registry-assets.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [x] | S2.01 | 注册骨架（Mod* 类、ID 工具） | S1.10 | P1 注册与资源 1 | [S2.01.md](evidence/S2.01.md) |
| [x] | S2.02 | `ExplosiveRegistry` 与平铺化形态 | S2.01 | P1 注册与资源 2 | [S2.02.md](evidence/S2.02.md) |
| [x] | S2.03 | 简单方块 | S2.01 | P1 注册与资源 3 | [S2.03.md](evidence/S2.03.md) |
| [x] | S2.04 | 材料与简单物品 | S2.01 | P1 注册与资源 4 | [S2.04.md](evidence/S2.04.md) |
| [x] | S2.05 | 创造模式标签页 | S2.02、S2.03、S2.04 | P1 注册与资源 5 | [S2.05.md](evidence/S2.05.md) |
| [x] | S2.06 | datagen：方块状态、模型、物品模型定义 | S2.05 | P1 注册与资源 6 | [S2.06.md](evidence/S2.06.md) |
| [x] | S2.07 | datagen：战利品表与标签 | S2.06 | P1 注册与资源 6 | [S2.07.md](evidence/S2.07.md) |
| [x] | S2.08 | 配方 | S2.07 | P1 注册与资源 7 | [S2.08.md](evidence/S2.08.md) |
| [x] | S2.09 | 声音 | S2.01 | P1 注册与资源 8 | [S2.09.md](evidence/S2.09.md) |
| [x] | S2.10 | `en_us` 迁移、`mergeLang`、`check_lang.py` 骨架 | S2.06 | P1 注册与资源 9 | [S2.10.md](evidence/S2.10.md) |

### S3 爆炸系统与爆炸相关实体 → [phase-s3-blasts.md](phase-s3-blasts.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [x] | S3.01 | 投射物基类 | S2.01、S1.04 | P1 爆炸 1 | [S3.01.md](evidence/S3.01.md) |
| [x] | S3.02 | 爆炸框架与伤害类型 | S3.01、S2.02、S1.08、S1.09 | P1 爆炸 1 | [S3.02.md](evidence/S3.02.md) |
| [x] | S3.03 | 爆炸相关实体（碎片、飞行方块、光束、烟雾、点燃炸药） | S3.02 | P1 爆炸 2 | [S3.03.md](evidence/S3.03.md) |
| [x] | S3.04 | 常规爆炸 | S3.03 | P1 爆炸 3 | [S3.04.md](evidence/S3.04.md) |
| [x] | S3.05 | 气体爆炸、药水效果、防护装备 | S3.03 | P1 爆炸 4 | [S3.05.md](evidence/S3.05.md) |
| [x] | S3.06 | 线程化爆炸与红物质 | S3.03 | P1 爆炸 5 | [S3.06.md](evidence/S3.06.md) |
| [x] | S3.07 | EMP 与末影爆炸 | S3.03 | P1 爆炸 6 | [S3.07.md](evidence/S3.07.md) |
| [x] | S3.08 | 放射性方块 | S3.04、S2.03 | P1 爆炸 7 | [S3.08.md](evidence/S3.08.md) |
| [x] | S3.09 | 爆炸物方块与引信 | S3.04、S3.05、S3.06、S3.07、S3.08 | P1 爆炸 8 | [S3.09.md](evidence/S3.09.md) |
| [x] | S3.10 | `/icbm` 命令 | S3.09 | P1 爆炸 9 | [S3.10.md](evidence/S3.10.md) |

### S4 导弹与投掷物 → [phase-s4-entities-missiles.md](phase-s4-entities-missiles.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [R] | S4.01 | 导弹实体与飞行逻辑 | S3.01 | P1 导弹 1 | [S4.01.md](evidence/S4.01.md) |
| [ ] | S4.02 | 爆炸导弹、动作导弹、导弹物品 | S4.01、S3.09 | P1 导弹 2 | |
| [ ] | S4.03 | 区块外导弹追踪（SavedData） | S4.02 | P1 导弹 3 | |
| [ ] | S4.04 | SAM 原样移植为 `anti_ballistic_missile` | S4.02、S1.06 | P1 导弹 4 | |
| [ ] | S4.05 | 手雷与炸弹矿车 | S3.09 | P1 导弹 5 | |
| [ ] | S4.06 | 集束导弹、子弹药、货物投放、携带物品导弹、座椅 | S4.02、S4.05 | P1 导弹 6 | |
| [~] | S4.07 | 实体渲染器与粒子（**拆分**：S3 已移植的 7 个实体类型渲染器已完成，导弹/手雷/座椅/降落伞渲染器与粒子仍待做） | S4.04、S4.06 | P1 导弹 7 | [S4.07-s3-entities.md](evidence/S4.07-s3-entities.md) |

### S5 机器、工具与 P1 验收 → [phase-s5-machines.md](phase-s5-machines.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S5.01 | 方块实体基类、库存、GUI 组件 | S2.01、S1.04、S1.07 | P1 机器与工具 1 | |
| [ ] | S5.02 | 发射台网络方块与方块实体 | S5.01、S4.02 | P1 机器与工具 2 | |
| [ ] | S5.03 | 发射台菜单与屏幕 | S5.02 | P1 机器与工具 3 | |
| [ ] | S5.04 | 巡航发射器 | S5.03 | P1 机器与工具 4 | |
| [ ] | S5.05 | 雷达站 | S5.01、S1.05、S1.06、S4.04 | P1 机器与工具 5 | |
| [ ] | S5.06 | EMP 塔 | S5.01、S3.07 | P1 机器与工具 6 | |
| [ ] | S5.07 | 方块实体渲染器 | S5.04、S5.05、S5.06 | P1 机器与工具 7 | |
| [ ] | S5.08 | 工具物品 | S5.04、S5.05 | P1 机器与工具 8 | |
| [ ] | S5.09 | 发射器（Dispenser）行为与红石 | S4.05、S5.05 | P1 机器与工具 9 | |
| [ ] | S5.10 | P1 回归 GameTest 套件 | S5.07、S5.08、S5.09、S4.07、S4.03、S3.10 | P1 验收 1 | |
| [ ] | S5.11 | 移植账本清零与禁用名称扫描 | S5.10 | P1 验收 1 | |
| [H] | S5.12 | 人工门禁：客户端实机全流程 | S5.11 | P1 验收 2 | |

### S6 FE 电力 → [phase-s6-energy.md](phase-s6-energy.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S6.01 | 引入 Team Reborn Energy 与 `EnergyApi` | S5.11 | P2 实现 1 | |
| [ ] | S6.02 | `ExposedEnergyStorage` | S6.01 | P2 实现 2 | |
| [ ] | S6.03 | 机器注册与发射台网络能量 | S6.02 | P2 实现 3 | |
| [ ] | S6.04 | 电池与电池槽放电 | S6.02 | P2 实现 4 | |
| [ ] | S6.05 | EMP 抽空能量 | S6.03 | P2 实现 5 | |
| [ ] | S6.06 | GUI 能量条、格式化、提示 | S6.03、S6.04 | P2 实现 6 | |
| [ ] | S6.07 | 能源互通 GameTest | S6.05、S6.06 | P2 验收 1 | |
| [H] | S6.08 | 人工门禁：Tech Reborn 实机 | S6.07 | P2 验收 2 | |

### S7 反弹道导弹 → [phase-s7-abm.md](phase-s7-abm.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S7.01 | 拦截数学与轨迹预测（JUnit） | S6.07 | P3 实现 1 | |
| [ ] | S7.02 | 雷达航迹表与弹着点预测 | S7.01 | P3 实现 2 | |
| [ ] | S7.03 | ABM 实体与制导状态机 | S7.01 | P3 实现 3 | |
| [ ] | S7.04 | 近炸引信、摧毁效果、`InterceptEvents` | S7.03 | P3 实现 4 | |
| [ ] | S7.05 | 火控与拦截消息、`EngagementRegistry` | S7.02、S7.04 | P3 实现 5 | |
| [ ] | S7.06 | 发射台响应拦截；删除来袭消息开火 | S7.05 | P3 实现 6 | |
| [ ] | S7.07 | 防御模式、EMP 兜底、GUI 与渲染点 | S7.06 | P3 实现 7 | |
| [ ] | S7.08 | 区块外模拟导弹限制测量 | S7.07 | P3 实现 8 | |
| [ ] | S7.09 | 配置、配方、资源、语言键 | S7.07 | P3 实现 9 | |
| [ ] | S7.10 | ABM GameTest 矩阵 | S7.08、S7.09 | P3 验收 1 | |
| [H] | S7.11 | 人工门禁：实机拦截演练 | S7.10 | P3 验收 2 | |

### S8 国际化 → [phase-s8-i18n.md](phase-s8-i18n.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S8.01 | 翻译键审计 | S7.10 | P4 实现 1 | |
| [ ] | S8.02 | 硬编码文本清理 | S8.01 | P4 实现 1 | |
| [ ] | S8.03 | `en_us` 补全与严格校验 | S8.02 | P4 实现 2 | |
| [ ] | S8.04 | 术语表补全 | S8.03 | P4 实现 3 | |
| [ ] | S8.05 | zh_cn | S8.04 | P4 实现 4 | |
| [ ] | S8.06 | fr_fr | S8.04 | P4 实现 5 | |
| [ ] | S8.07 | de_de | S8.04 | P4 实现 6 | |
| [ ] | S8.08 | ja_jp | S8.04 | P4 实现 7 | |
| [ ] | S8.09 | ar_sa 源文件与 `shapeArabic` | S8.04 | P4 实现 8 | |
| [ ] | S8.10 | 旧语言迁移 | S8.03 | P4 实现 9 | |
| [ ] | S8.11 | 严格校验通过 | S8.05–S8.10 | P4 验收 1 | |
| [H] | S8.12 | 人工门禁：母语审校与阿拉伯文截图 | S8.11 | P4 验收 2 | |

### S9 收尾 → [phase-s9-release.md](phase-s9-release.md)

| 状态 | 步骤 | 标题 | 前置 | TODO 映射 | 证据 |
| --- | --- | --- | --- | --- | --- |
| [ ] | S9.01 | 删除 `legacy-1.12/` | S8.11 | P5 1 | |
| [ ] | S9.02 | 版本号、CHANGE_LOG、README | S9.01 | P5 2 | |
| [ ] | S9.03 | 构件与校验和 | S9.02 | P5 3 | |
| [H] | S9.04 | 人工门禁：总验收 | 全部 | P5 4 | |

---

## 10. 人工门禁汇总

以下只能由人完成。Agent 在 `[H]` 步骤中只做：准备构建产物、写好逐条检查清单（放进证据文件）、保持 `[H]`。

| 门禁 | 需要的人/物 | 准备步骤 |
| --- | --- | --- |
| 客户端实机全流程（Windows 或 Linux 桌面，26.3 + Fabric Loader 0.19.5 + Fabric API） | 有图形环境的 PC | S5.12 |
| Tech Reborn 6.2.0 联机互通 | 同上 + Tech Reborn jar | S6.08 |
| ABM 实机拦截演练 | 同上 | S7.11 |
| 母语审校：zh_cn、fr_fr、de_de、ja_jp、ar_sa | 每种语言一名审校者 | S8.12 |
| 阿拉伯文渲染截图 | 客户端切换到 العربية | S8.12 |
| 是否公开发布 | 维护者 | S9.04 |
