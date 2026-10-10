# 0004：反弹道导弹系统——雷达火控 + 制导拦截

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：项目维护者、规划 Agent
- 关联需求：[需求记录](../meetings/2026-10-10-upgrade-requirements.md) 第 4 条；详细设计见 [anti-ballistic-missile.md](../architecture/anti-ballistic-missile.md)

## 背景

1.12 中雷达站发现“会命中保护区”的导弹后，每 `sam_delay`（40 tick）在无线电频道广播一次 `IncomingMissileMessage`：

- EMP 塔收到后，若导弹在 EMP 范围内就放电（这是目前唯一可靠的防御）。
- 发射屏/巡航发射器把该消息当作“设定目标 + 立即发射”，**无论装填什么导弹都会发射**，目标是来袭导弹当前位置——装了核弹也会朝自己头顶发射。
- 若装填的是 SAM（`surface_to_air_missile`），SAM 追踪目标并在距离 ≤ 飞行速度（4 格）时造成 100 点伤害。

存在的问题：所有同频发射台同时齐射；无弹着点预测（只判断“是否在靠近”）；SAM 纯追踪，高速目标易脱靶；单 tick 位置判断会被高速目标“穿过”；雷达无航迹/交战状态；GUI 无法选择防御方式。

## 决定

**用户要求**：雷达发现导弹后，不再只能用 EMP 塔，还能发射反弹道导弹直接摧毁目标。

**讨论结论 / 工程缺省**：

1. SAM 重构并更名为**反弹道导弹（ABM）**：物品与实体 ID `icbmclassic:anti_ballistic_missile`。7.0.0 不发布 SAM ID（ADR 0002 不兼容旧存档，所以可直接改名）。
2. 雷达新增**航迹表**（每个目标一个 track）与**弹着点预测**；只对预测落点在保护半径（`triggerRange`）内的目标交战。
3. 雷达新增**防御模式**：`OFF`、`EMP_ONLY`、`ABM_ONLY`、`ABM_THEN_EMP`（默认）。`ABM_THEN_EMP` 在剩余撞击时间 ≤ `emp_fallback_ticks` 且仍未拦截时，才让 EMP 塔放电。
4. **火控分配**：两段式无线电消息——`InterceptQueryMessage` 收集同频发射台报价，雷达按“发射台到预测拦截点距离”排序，再用 `InterceptOrderMessage` 指名发射台开火。每个目标同时在飞的拦截弹不超过 `max_interceptors_per_target`（默认 1），由每个世界唯一的 `EngagementRegistry` 跨雷达去重。
5. **发射台不再响应 `IncomingMissileMessage`**（删除“装什么打什么”的旧行为）。只有装填 ABM 的发射台响应 `InterceptOrderMessage`。EMP 塔继续响应 `IncomingMissileMessage`。
6. **ABM 制导**：助推段垂直爬升，之后用“前置追踪”（解析拦截点，失败退回纯追踪），带每 tick 最大转向角限制。
7. **近炸引信**：每 tick 计算两条运动线段的最近距离（防止高速穿越），≤ `proximity_radius` 即判定拦截。
8. **拦截效果**：默认命中率 1.0（可配置）；目标导弹被移除且**不触发战斗部**（可配置为触发）；在拦截点产生不破坏方块的视觉爆炸。燃料耗尽或目标丢失超时则空中自毁。
9. 拦截数学（拦截点求解、线段最近距离、弹着点外推）放在纯 Java 类，JUnit 覆盖。

## 影响

- 新增无线电消息 2 种、雷达 NBT 字段（防御模式）、雷达 GUI 控件、ABM 配置文件 `config/icbmclassic/abm.json`。
- 发射屏/巡航发射器的无线电处理改变（删除对 `IIncomingMissileMessage` 的开火处理）。这是玩法变化，需写入 CHANGE_LOG。
- 雷达渲染点数据增加状态字节（颜色）。
- 只能拦截**已加载区块中的实体导弹**；处于“区块外模拟飞行”状态的导弹不可见、不可拦截（见架构文档 §9）。

## 备选方案

- **比例导引（PN）**：更真实，但调参复杂、弱 Agent 难以调试。先用前置追踪 + 转向限制；PN 作为后续范围。
- **单条消息 + 先到先得**：接收顺序不确定，无法选最近发射台。拒绝。
- **拦截时用伤害（保留 100 点伤害）**：结果依赖导弹血量配置，玩家难以预期。拒绝，改为直接摧毁。

## 验证

未验证。S7.01（JUnit 数学）、S7.10（GameTest 拦截场景）、S7.11（人工实机演练）产生证据。
