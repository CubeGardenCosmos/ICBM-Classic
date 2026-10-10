# FE 电力系统

依据 [ADR 0003](../decisions/0003-fe-energy-via-team-reborn-energy.md)。实施阶段：S1.07（内部缓冲）、S5（机器使用内部缓冲）、S6（对外互通、电池、EMP、GUI）。

## 1. 术语

- **FE**：玩家可见的能量单位。所有配置、提示、GUI 一律写 “FE”。
- **E**：Team Reborn Energy 的单位，与 FE 1:1。代码与文档中出现 E 的地方都等价于 FE。
- **缓冲（buffer）**：机器内部存储的能量。
- **对外存储（exposed storage）**：通过 `EnergyStorage.SIDED`/`EnergyStorage.ITEM` 暴露给其他模组的视图。

## 2. 1.12 现状（对照）

| 机器 / 物品 | 1.12 类 | 缓冲容量配置 | 消耗配置 |
| --- | --- | --- | --- |
| 发射台基座 | `TileLauncherBase`（`EnergyBuffer`） | `ConfigLauncher.POWER_CAPACITY = 1200` | `ConfigLauncher.POWER_COST = 1200`（每次发射） |
| 巡航发射器 | `TileCruiseLauncher` | 同上 | 同上 |
| 发射台网络 | `NetworkEnergyStorage`（把输入平均分配给网络中所有发射台基座，只收不放） | — | — |
| 发射屏 | `TileLauncherScreen` | 见旧代码 | 见旧代码 |
| 雷达站 | `TileRadarStation` | `ConfigRadar.POWER_CAPACITY = 20000` | `ConfigRadar.POWER_COST = 1000`（每 tick） |
| EMP 塔 | `TileEMPTower` | 按范围计算 | `ConfigEmpTower.ENERGY_COST_AREA = 100`（每格范围）、`ENERGY_COST_TICKING = 10`（维持/tick）、`ENERGY_COST_TICKING_CAP = 100`、`ENERGY_INPUT = 10000`（每 tick 最大接收） |
| 电池 | `ItemBattery`（`EnergyBuffer`） | `ConfigBattery.BATTERY_CAPACITY = 100000` | 输入/输出上限 `BATTERY_INPUT_LIMIT`/`BATTERY_OUTPUT_LIMIT` |
| 全局开关 | `ConfigMain.REQUIRES_POWER = true` | false 时机器视为满电、不消耗 | |
| EMP 爆炸 | `ActionEmpArea`：无 `IEMPReceiver` 的方块实体/实体，若 `ConfigBlast.emp.DRAIN_ENERGY_TILES`/`DRAIN_ENERGY_ENTITY` 开启则把能量直接设为 0（反射改 Forge `EnergyStorage#energy`） | | |

所有默认数值在 26.3 中**原样保留**，单位 FE。

## 3. 包结构（S6 完成后）

```text
icbm.classic.lib.energy
├── IcbmEnergyBuffer.java        # 机器内部缓冲（S1.07 创建，S6 不改接口）
├── EnergyApi.java               # 唯一 import team.reborn.energy 的类（S6.01）
├── ExposedEnergyStorage.java    # 把 IcbmEnergyBuffer 适配为 TR EnergyStorage（S6.02，SnapshotParticipant）
├── LauncherNetworkEnergy.java   # 发射台网络的对外存储（S6.03，替代 NetworkEnergyStorage）
├── EnergyItemSlotHelper.java    # 电池槽放电（S6.04）
└── EnergyFormat.java            # “12.5 kFE” 等格式化（S6.06）
```

删除：`IEnergySystem`、`EnergySystem`、`EnergySystemFE`、`EnergySystemNull`、`EnergyActionType`、`EnergyChangeCallback`（如仍需回调则并入 `IcbmEnergyBuffer`）、`api/energy/IEnergyBuffer`、`api/energy/IEnergyBufferProvider`。

## 4. `IcbmEnergyBuffer`（S1.07 创建，纯 Java，不依赖 TR）

```java
package icbm.classic.lib.energy;

import java.util.function.LongSupplier;

/** 机器内部能量缓冲。单位 FE。主线程使用，非线程安全。 */
public final class IcbmEnergyBuffer {
    private final LongSupplier capacity;     // 读配置，允许运行时变化
    private final LongSupplier maxReceive;   // 每次 receive 的上限（每 tick 由调用方保证只调一次或累计）
    private long stored;
    private Runnable onChange = () -> {};

    public IcbmEnergyBuffer(LongSupplier capacity, LongSupplier maxReceive) { ... }
    public IcbmEnergyBuffer onChange(Runnable r) { this.onChange = r; return this; }

    public long getStored()   { return EnergyRules.ignoresPower() ? getCapacity() : stored; }
    public long getCapacity() { return capacity.getAsLong(); }

    /** 尝试接收，返回实际接收量；simulate=true 不修改。 */
    public long receive(long amount, boolean simulate) { ... clamp 到 [0, min(maxReceive, capacity - stored)] ... }

    /** 有足够能量则扣除并返回 true；REQUIRES_POWER=false 时总返回 true 且不扣。 */
    public boolean consume(long amount) { ... }

    /** 只检查不扣除。 */
    public boolean has(long amount) { ... }

    /** 直接设置（存档加载、EMP 清零用），夹到 [0, capacity]。 */
    public void setStored(long value) { ... onChange.run(); }
}
```

- `EnergyRules.ignoresPower()` 返回 `!ConfigMain.REQUIRES_POWER`。
- 存档：`SaveHandler` 节点 `nodeLong("energy", ...)`（1.12 键名 `"energy"` 保留，类型由 int 变 long）。
- JUnit：接收夹取、消耗不足返回 false 且不扣、`REQUIRES_POWER=false` 行为、容量变小时 `getStored` 不超过容量（`setStored` 夹取；`getStored` 返回 `min(stored, capacity)`）。

## 5. 对外互通（S6）

### 5.1 依赖

```groovy
dependencies {
    implementation "teamreborn:energy:5.0.0"
    include "teamreborn:energy:5.0.0"
}
```

`fabric.mod.json` 的 `depends` **不**写 `team_reborn_energy`（已 Jar-in-Jar 内嵌）。构件 sha256：`889afc438d3e4add5cfdac76517da7987a2c495e4731690a56f2c5dee775db59`（2026-10-10 下载计算）；其 `fabric.mod.json` 要求 `java>=25`、`minecraft>=26.1-`、`fabric-transfer-api-v1>=5.1.0`。

### 5.2 已核实的 TR Energy 5.0.0 API（javap）

- `team.reborn.energy.api.EnergyStorage`：`SIDED`（`BlockApiLookup<EnergyStorage, Direction>`）、`ITEM`（`ItemApiLookup<EnergyStorage, ContainerItemContext>`）、`EMPTY`、`ENERGY_COMPONENT`（`DataComponentType<Long>`）；方法 `insert(long, TransactionContext)`、`extract(long, TransactionContext)`、`getAmount()`、`getCapacity()`、`supportsInsertion()`、`supportsExtraction()`。
- `team.reborn.energy.api.base.SimpleEnergyStorage(long capacity, long maxInsert, long maxExtract)`，公开字段 `amount`。
- `team.reborn.energy.api.base.SimpleEnergyItem`：`createStorage(ContainerItemContext, capacity, maxIn, maxOut)`、`getStoredEnergyUnchecked(ItemStack)`、`setStoredEnergyUnchecked(ItemStack, long)`。
- `team.reborn.energy.api.EnergyStorageUtil.move(from, to, max, TransactionContext)`。

### 5.3 机器对外存储：`ExposedEnergyStorage`

```java
/** 把 IcbmEnergyBuffer 暴露为只收不放的 TR EnergyStorage。事务回滚安全。 */
final class ExposedEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    private final IcbmEnergyBuffer buffer;
    private final Runnable onCommit;            // 通常是 blockEntity::setChanged

    @Override public boolean supportsExtraction() { return false; }
    @Override public long extract(long max, TransactionContext tx) { return 0; }
    @Override public long insert(long max, TransactionContext tx) {
        StoragePreconditions.notNegative(max);
        long accepted = buffer.receive(max, true);
        if (accepted > 0) {
            updateSnapshots(tx);                // 必须在修改前调用
            buffer.receive(accepted, false);
        }
        return accepted;
    }
    @Override public long getAmount()   { return buffer.getStored(); }
    @Override public long getCapacity() { return buffer.getCapacity(); }
    @Override protected Long createSnapshot()       { return buffer.getStored(); }
    @Override protected void readSnapshot(Long v)   { buffer.setStored(v); }
    @Override protected void onFinalCommit()         { onCommit.run(); }
}
```

- 机器**不提供 extract**：ICBM 机器只是耗电方。
- 每 tick 接收上限：`IcbmEnergyBuffer.maxReceive`。EMP 塔沿用 `ENERGY_INPUT = 10000`；其他机器用容量本身（即不限）。
- 注册（`EnergyApi.register()`）：

```java
EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.getExposedEnergy(), ModBlockEntities.RADAR_STATION);
// launcher_base、cruise_launcher、emp_tower、launcher_screen 同理；
// launcher_frame、launcher_connector 返回 LauncherNetworkEnergy（见 5.4）
// emp_tower_fake 返回主方块的存储（主方块未加载时返回 null）
```

- `REQUIRES_POWER=false` 时注册仍进行，但 `insert` 返回 0（缓冲视为满）。

### 5.4 发射台网络：`LauncherNetworkEnergy`

替代 1.12 `NetworkEnergyStorage`，行为相同：把一次 `insert(max)` 平均分给网络中所有发射台基座（含巡航发射器）的 `ExposedEnergyStorage`，余数逐个补齐；每个子 `insert` 用**同一个** `TransactionContext`（嵌套参与者自动随外层提交/回滚）。只收不放。

### 5.5 电池（`icbmclassic:battery`）

- 物品类实现 `SimpleEnergyItem`：容量 `ConfigBattery.BATTERY_CAPACITY`，输入/输出上限对应配置。
- 注册：`EnergyStorage.ITEM.registerForItems((stack, ctx) -> SimpleEnergyItem.createStorage(ctx, cap, in, out), ModItems.BATTERY)`。
- 存量保存在 TR 的 `EnergyStorage.ENERGY_COMPONENT`，**不**自建组件。
- 耐久条：覆盖 `isBarVisible`/`getBarWidth`/`getBarColor`（26.3 签名用 javap 核对）显示电量。
- 提示文本：`icbmclassic.energy.stored` = `"%s / %s FE"`。

### 5.6 机器的电池槽（放电）

1.12 机器有一个“能量物品槽”，每 tick 从物品向缓冲放电（`EnergyBuffer#dischargeItem`）。26.3：

```java
// 每 tick，在机器 tick 中（服务端）
try (Transaction tx = Transaction.openOuter()) {
    EnergyStorage item = EnergyStorage.ITEM.find(slotStack, ContainerItemContext.ofSingleSlot(slotStorage));
    if (item != null) {
        long moved = EnergyStorageUtil.move(item, exposedEnergyIgnoringRequiresPower, buffer.getCapacity() - buffer.getStored(), tx);
        if (moved > 0) tx.commit();
    }
}
```

`slotStorage` 来自 `InventoryStorage.of(container, null).getSlot(slotIndex)`。槽位过滤器：`EnergyStorageUtil.isEnergyStorage(stack)`（替代 1.12 `EnergySystem::isEnergyItem`）。这段逻辑封装在 `EnergyItemSlotHelper.dischargeInto(...)`，是机器代码里唯二可以接触 TR 类型的地方之一（另一个是 `EnergyApi`）——为遵守 AGENTS.md，`EnergyItemSlotHelper` 也放在 `lib/energy` 包内。

### 5.7 EMP 对能量的影响

替代 1.12 `ActionEmpArea` 中的反射清零：

- 方块：对半径内每个方块实体，若没有 `IEMPReceiver` 且 `ConfigBlast.emp.DRAIN_ENERGY_TILES`：对 6 个方向及 `null` 方向 `EnergyStorage.SIDED.find(level, pos, dir)`，对找到的每个**不同**实例在一个事务中 `extract(Long.MAX_VALUE, tx)` 并提交。不支持 extract 的存储（例如 ICBM 自己的机器）跳过；ICBM 机器通过自身 `IEMPReceiver` 实现清空 `IcbmEnergyBuffer`。
- 实体：若 `DRAIN_ENERGY_ENTITY`，对玩家/生物背包中的每个物品用 `EnergyStorage.ITEM` 抽空（配合 `InventoryStorage`/`PlayerInventoryStorage`）。
- 1.12 中“无法直接设值则跳过”的 TODO 不实现。

### 5.8 GUI 与提示

- 所有机器 GUI 显示能量条，悬停文本 `icbmclassic.energy.stored`。数值通过 `ContainerData` 同步：`long` 拆成两个 int（高 32 位、低 32 位），客户端拼回。
- `EnergyFormat.format(long)`：`< 1000` 显示整数；`< 1e6` 显示 `x.y k`；`< 1e9` 显示 `x.y M`；否则 `x.y G`；单位后缀统一翻译键 `icbmclassic.energy.unit` = `"FE"`。
- 机器提示（`IMachineInfo`）保留 1.12 的 `NEEDS_POWER`、`ENERGY_COST_TICK` 信息项。

## 6. 测试要求

| 测试 | 类型 | 步骤 |
| --- | --- | --- |
| `IcbmEnergyBuffer` 规则 | JUnit | S1.07 |
| 事务回滚后缓冲不变 | GameTest：开事务 insert → 不提交 → 断言 amount 不变 | S6.02 |
| 网络平均分配 | GameTest：3 个基座 + 1 个连接器，从连接器 insert 3000 → 每个基座约 1000 | S6.03 |
| 电池放电到雷达 | GameTest：电池放槽，tick 若干次，雷达缓冲增加、电池减少 | S6.04 |
| 外部发电方块推送 | GameTest：测试模组中的“无限能源方块”每 tick 用 `EnergyStorageUtil.move` 推给相邻雷达 → 雷达工作 | S6.07 |
| EMP 抽空外部存储 | GameTest：测试模组方块暴露 `SimpleEnergyStorage(10000, 0, 10000)`，EMP 后为 0 | S6.05 |
| Tech Reborn 实机 | 人工门禁 | S6.08 |
