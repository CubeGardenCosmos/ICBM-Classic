# 0003：FE 电力经 Team Reborn Energy 互通

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：项目维护者、规划 Agent
- 关联需求：[需求记录](../meetings/2026-10-10-upgrade-requirements.md) 第 3 条

## 背景

“FE”（Forge Energy）是 Forge/NeoForge 的 `IEnergyStorage` 能量单位与接口。Fabric 没有内置能量 API；Fabric 生态事实标准是 **Team Reborn Energy**（Maven `teamreborn:energy`，基于 Fabric Transfer API 的 `Transaction` 与 API Lookup），Tech Reborn、Modern Industrialization 等主流科技模组都使用它，并普遍按 1:1 与 FE 对应。1.12 版 ICBM 已全面使用 FE（`CapabilityEnergy.ENERGY`），配置中的数值（如雷达每 tick 1000）均以 FE 计。

## 决定

**用户要求**：电力使用 FE。

**工程缺省**（实现方式）：

1. 玩家可见的单位、配置项、提示文本一律写 **FE**。所有 1.12 配置默认数值原样保留（单位不变）。
2. 对外互通使用 **Team Reborn Energy `5.0.0`**（依据：README 写明 5.x 用于 MC 26.1 及以后；Tech Reborn 26.3 分支使用 5.0.0）。换算固定 **1 FE = 1 E**，不提供可配置倍率。
3. 用 `include(implementation("teamreborn:energy:5.0.0"))` 把 API 打进 jar（Jar-in-Jar），玩家无需单独安装。
4. 机器代码只依赖内部门面 `icbm.classic.lib.energy`：
   - `IcbmEnergyBuffer`：机器内部缓冲（`long` 存量，接口方法用 `long`）。
   - `EnergyApi`：唯一允许 `import team.reborn.energy` 的类，负责 `EnergyStorage.SIDED`、`EnergyStorage.ITEM` 注册与查找。
5. 机器是**接收方**：不主动从邻居抽电（遵守 Team Reborn Energy 的 push 约定）。电池槽中的物品由机器主动放电到自己的缓冲（这是物品→机器，不违反约定）。
6. 1.12 的 `IEnergySystem` 多系统抽象、`EnergySystemFE` 反射改值、`api.energy.IEnergyBuffer`（已 `@Deprecated`）全部删除。
7. Mekanism 联动、IC2 配置项删除。

## 影响

- 机器（发射台基座、巡航发射器、雷达、EMP 塔、发射屏）在 S5 移植时先使用 `IcbmEnergyBuffer`（不对外暴露），S6 再接入 `EnergyApi` 对外暴露。这样 S5 不依赖 S6。
- 电池物品 `icbmclassic:battery` 通过 `EnergyStorage.ITEM` 暴露，存量保存在 DataComponent `icbmclassic:energy`（`long`）。
- 将来若要做 NeoForge 版本，只需新增一个 `EnergyApi` 实现。

## 备选方案

- **自定义能量 API（无互通）**：玩家无法用其他模组发电。拒绝。
- **“FE API for Fabric”（Modrinth `fe-api`）**：生态覆盖小，26.3 版本未核实。拒绝。
- **同时支持多个 Fabric 能量 API**：复杂度高，主流模组已统一到 Team Reborn Energy。拒绝。

## 验证

- 上游事实：TechReborn `26.3` 分支 `gradle.properties` 含 `energy_version=5.0.0`、`minecraft_version=26.3`（2026-10-10 读取）。
- 本项目互通：未验证。S6.07（GameTest，模拟发电方块推送）与 S6.08（人工门禁，与 Tech Reborn 6.2.0 实机）产生证据。
