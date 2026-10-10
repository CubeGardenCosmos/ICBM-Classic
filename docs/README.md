# 文档索引

本目录定义 ICBM-Classic 从 **Forge 1.12.2（6.5.1）** 升级到 **Minecraft 26.3 + Fabric（7.0.0）** 的产品与工程基线。这是一个长期目标，按阶段推进。

编写日期：2026-10-10。编写依据：`prod/1.12` 分支提交 `e862b4f15` 的代码、[需求记录](meetings/2026-10-10-upgrade-requirements.md)、[外部资料](implementation/references.md)。

## 升级目标与顺序（用户已决定）

| 顺序 | 目标 | 架构文档 | 实施阶段 |
| --- | --- | --- | --- |
| 1（首要） | 升级到 Minecraft 26.3，使用 Fabric 开发 | [overview.md](architecture/overview.md)、[porting-guide.md](architecture/porting-guide.md)、[registry-ids.md](architecture/registry-ids.md) | S0–S5 |
| 2 | 电力使用 FE | [energy-fe.md](architecture/energy-fe.md) | S6 |
| 3（倒数第二） | 反弹道导弹系统：雷达发现来袭导弹后，除 EMP 塔外还能发射反导导弹直接摧毁目标 | [anti-ballistic-missile.md](architecture/anti-ballistic-missile.md) | S7 |
| 4（最后） | i18n：中文、法文、德文、日文、阿拉伯文 | [i18n.md](architecture/i18n.md) | S8 |
| — | 收尾验收与发布准备 | — | S9 |

## 当前架构

- [系统概览](architecture/overview.md)：目标版本、模块划分、源集、入口、线程模型、目录结构。
- [移植指南](architecture/porting-guide.md)：1.12 Forge → 26.3 Fabric 的 API 对照表、包级移植映射、常见陷阱。
- [注册 ID 对照表](architecture/registry-ids.md)：平铺化后的方块、物品、实体、方块实体、菜单、声音 ID。
- [FE 电力系统](architecture/energy-fe.md)：能量门面、缓冲、侧向访问、电池、EMP、配置。
- [反弹道导弹系统](architecture/anti-ballistic-missile.md)：雷达航迹、弹着点预测、火控分配、制导、近炸引信、与 EMP 塔协同。
- [国际化](architecture/i18n.md)：语言文件格式、键命名、术语表、阿拉伯文从右到左与字形整形。

## 决策与证据

- [ADR 决策记录](decisions/README.md)
- [兼容性证据](compatibility/README.md)
- [需求与讨论记录](meetings/2026-10-10-upgrade-requirements.md)

## 计划与执行

- [分阶段 TODO](todo/README.md)：每阶段的交付物与验收条件。
- [实施手册](implementation/README.md)：给执行 Agent 的步骤卡、总目录、环境、命令、BLOCKED 规程、证据格式。
- [外部链接与固定版本](implementation/references.md)

## 历史资料

- `notes/`：1.12 时期作者笔记（TODO、未来想法）。仅作背景参考，不构成本次升级的需求。
- `CHANGE_LOG.md`：发布日志，S9 才更新。

## 文档约束

- TODO 状态只有 `TODO`、`DOING`、`BLOCKED`、`DONE`。`DONE` 要链接验证证据；自动化验证与人工实机验收分开记录。
- 兼容性表只接受可复现证据：精确版本、构件 hash、系统、步骤、结果、日志位置。
- 发现文档过时：在当前架构文档或新 ADR 中更正，不把旧讨论改写成“当时已决定”。
