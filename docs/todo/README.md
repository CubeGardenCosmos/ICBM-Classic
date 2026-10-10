# 分阶段 TODO

## 目标与顺序

| 阶段 | 目标 | 前置 | 对应实施阶段 |
| --- | --- | --- | --- |
| P0 | 文档与项目治理基线 | — | （本文档集） |
| P1 | 升级到 Minecraft 26.3 + Fabric，功能与 6.5.1 等价 | P0 | S0–S5 |
| P2 | FE 电力互通 | P1 | S6 |
| P3 | 反弹道导弹系统 | P2 | S7 |
| P4 | 国际化（zh_cn、fr_fr、de_de、ja_jp、ar_sa） | P3 | S8 |
| P5 | 收尾、清理、发布准备 | P4 | S9 |

顺序由用户决定（见[需求记录](../meetings/2026-10-10-upgrade-requirements.md)），不得调换。阶段内部的并行规则见[实施手册](../implementation/README.md) §9。

## 状态

- `TODO` 未开始；`DOING` 进行中；`BLOCKED` 阻塞（写明外部依赖与解除条件）；`DONE` 完成（链接证据）。
- 自动化验证与人工实机验收分别记录。没有实机证据不写“已支持”。

## 条目格式与勾选规则

每个条目以 `- [ ] (n)` 开头，`n` 是小节内编号。实施手册总目录的“TODO 映射”引用这些编号，写法 `P1 基础库 3` = `phase-1-port.md` 中“基础库”小节第 (3) 条。

步骤完成后，在对应条目把 `[ ]` 改为 `[x]`，行尾追加 `（S<x.yy>，证据：../implementation/evidence/S<x.yy>.md）`。一个条目映射多个步骤时，最后一个步骤完成才勾选，之前只追加 `（部分：S<x.yy>）`。人工门禁 `[H]` 映射的条目只能由人勾选。

## 阶段文件

- [P0 文档与治理](phase-0-documents.md)
- [P1 26.3 + Fabric 移植](phase-1-port.md)
- [P2 FE 电力](phase-2-energy.md)
- [P3 反弹道导弹](phase-3-abm.md)
- [P4 国际化](phase-4-i18n.md)
- [P5 收尾与发布](phase-5-release.md)
