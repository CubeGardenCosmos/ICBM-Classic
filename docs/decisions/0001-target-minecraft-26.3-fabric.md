# 0001：目标平台 Minecraft 26.3 + Fabric

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：项目维护者、规划 Agent
- 关联需求：[需求记录](../meetings/2026-10-10-upgrade-requirements.md) 第 2 条

## 背景

ICBM-Classic 6.5.1 运行在 Forge 1.12.2（Java 8）上。1.12 到 26.3 之间经历了方块扁平化（1.13）、Capability 改造、数据组件（1.20.5）、渲染状态分离、26.1 去混淆、26.2 Vulkan 后端等多次破坏性变化，源代码无法“升级依赖后修编译错误”式迁移，只能按模块重写。

## 决定

**用户要求**：升级到 Minecraft 26.3，使用 Fabric。

**工程缺省**（版本锁，均于 2026-10-10 从官方源核实）：

| 项 | 值 |
| --- | --- |
| Minecraft | 26.3 |
| Java（编译与运行） | 25（本机 `/usr/lib/jvm/java-25-openjdk`） |
| Gradle | 9.8.1（wrapper，带 `distributionSha256Sum`） |
| Fabric Loom | 1.18.3，插件 ID `net.fabricmc.fabric-loom` |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.162.0+26.3 |
| 名称 | Mojang 官方名称（26.x 不混淆，无 mappings 配置） |
| 模组 ID / 包 / 资源命名空间 | `icbmclassic` / `icbm.classic` / `icbmclassic`（不变） |
| 模组版本 | 7.0.0（主版本号 +1 表示不兼容旧存档） |
| 环境 | 同一个 jar 同时支持客户端与专用服务端（`"environment": "*"`） |

工作在新分支 `port/26.3-fabric` 上进行；`prod/1.12` 保持可维护。旧源码在移植分支中移入 `legacy-1.12/`，仅供参考，移植全部完成（S9.01）后删除。

## 影响

- 所有 Forge API（`@Mod`、`@SubscribeEvent`、`Capability`、`@Config`、`SimpleNetworkWrapper`、`IGuiHandler`、`TileEntitySpecialRenderer`、OreDictionary）全部替换，映射见 [porting-guide.md](../architecture/porting-guide.md)。
- 1.12 的 `ICBMClassic_at.cfg`（Access Transformer）作废；需要访问私有成员时优先用公开 API，其次用 Mixin `@Accessor`/`@Invoker`，最后才用 Class Tweaker（`.classtweaker`）。
- 1.12 的 MinecraftJUnit 测试库作废，测试改为 JUnit 5（纯逻辑）+ Fabric GameTest（需要世界的逻辑）。
- Mekanism、JEI、IC2 联动代码删除（见 ADR 0006）。

## 备选方案

- **NeoForge 26.3**：FE 原生可用，但用户明确要求 Fabric。拒绝。
- **Architectury 多加载器**：增加一层抽象与依赖，弱 Agent 难以维护。拒绝；只在能源层预留门面。
- **先升到 1.20.1/1.21.1 再升 26.3**：中间版本没有发布需求，两次移植成本更高。拒绝。

## 验证

- 同机项目 `../Minecraft-Server-Launcher/game-mods` 已用完全相同的工具链版本组合构建并运行服务端 GameTest（其证据 `docs/implementation/evidence/S3.01.md`）。本项目自身的构建证据由 S0.03、S0.04 产生。
- 客户端实机未验证（人工门禁 S5.12）。
