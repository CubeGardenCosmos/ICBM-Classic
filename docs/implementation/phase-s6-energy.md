# S6 FE 电力

## 阶段说明

把 S5 中只在内部使用的 `IcbmEnergyBuffer` 接入 Team Reborn Energy 5.0.0，使其他 Fabric 科技模组可以给 ICBM 机器供电；实现电池；EMP 抽空外部能量；GUI 能量显示。

**设计依据**：[energy-fe.md](../architecture/energy-fe.md)（本阶段每一步都以它的对应小节为“必读”）、ADR 0003。

**硬规则**：只有 `icbm.classic.lib.energy` 包内的类可以 `import team.reborn.energy.*`。S6 完成后用 `grep -rln "team.reborn.energy" src/ | grep -v "/lib/energy/"` 检查，输出必须为空（GameTest 源集除外）。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S6.01 引入 Team Reborn Energy 与 `EnergyApi`

- 前置：S5.11
- 必读：energy-fe.md §3、§5.1、§5.2；references.md §1
- 改动文件：`build.gradle`；`gradle.properties`（`tr_energy_version=5.0.0`）；新建 `src/main/java/icbm/classic/lib/energy/EnergyApi.java`；`ICBMClassic.java`（overview §5 第 8 步调用 `EnergyApi.register()`）
- 目标：依赖可用并打包进 jar。

**操作**

1. 依赖（energy-fe.md §5.1）。下载 jar 并核对 sha256：`curl -fsSL -o .dev/tmp/energy-5.0.0.jar https://maven.fabricmc.net/teamreborn/energy/5.0.0/energy-5.0.0.jar && sha256sum .dev/tmp/energy-5.0.0.jar`，必须为 `889afc438d3e4add5cfdac76517da7987a2c495e4731690a56f2c5dee775db59`，否则 BLOCKED。
2. 用 javap 核对 energy-fe.md §5.2 列出的全部 API（贴到证据）。
3. `EnergyApi.register()`：本步为空方法体 + 日志；S6.03 填充。
4. 确认 jar-in-jar：`unzip -l build/libs/*-dev.jar | grep -i energy` 应出现 `META-INF/jars/energy-5.0.0.jar`。

**验证**：`./gradlew build`；上面的 unzip。

**提交信息**：`build(energy): 引入 Team Reborn Energy 5.0.0`

---

### S6.02 `ExposedEnergyStorage`

- 前置：S6.01
- 必读：energy-fe.md §5.3
- 改动文件：新建 `lib/energy/ExposedEnergyStorage.java`；修改各机器方块实体增加 `getExposedEnergy()`（返回懒创建的单例）；GameTest `EnergyTransactionGameTest`
- 目标：机器对外存储事务安全、只收不放。

**操作**

1. 按 energy-fe.md §5.3 骨架实现。`StoragePreconditions` 在 `net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions`（javap 核对）。
2. 每台机器（`launcher_base`、`cruise_launcher`、`launcher_screen`、`radar_station`、`emp_tower`）：`private ExposedEnergyStorage exposed; public ExposedEnergyStorage getExposedEnergy() { if (exposed == null) exposed = new ExposedEnergyStorage(buffer, this::setChanged); return exposed; }`。
3. GameTest：
   - 打开外层事务 `insert(500)` → 不提交（`try` 块结束自动回滚）→ `buffer.getStored()` 不变。
   - 提交后增加 500。
   - `extract` 永远返回 0。
   - `REQUIRES_POWER=false` 时 `insert` 返回 0。

**提交信息**：`feat(energy): 实现事务安全的机器对外能量存储`

---

### S6.03 机器注册与发射台网络能量

- 前置：S6.02
- 必读：energy-fe.md §5.3 注册部分、§5.4；`legacy-1.12/.../content/blocks/launcher/network/NetworkEnergyStorage.java`
- 改动文件：`EnergyApi.java`；新建 `lib/energy/LauncherNetworkEnergy.java`；删除 S5.02 的内部 `NetworkEnergyStorage`（`MERGED → LauncherNetworkEnergy`）；GameTest `LauncherNetworkEnergyGameTest`
- 目标：外部模组可通过 `EnergyStorage.SIDED` 找到每台机器与发射台网络的存储。

**操作**

1. `EnergyApi.register()`：对 5 种机器方块实体注册 `getExposedEnergy()`；对 `launcher_frame`、`launcher_connector` 注册返回 `LauncherNetworkEnergy`（网络为空时返回 `null`）；`emp_tower_fake` 返回主方块的存储。
2. `LauncherNetworkEnergy.insert(max, tx)`：节点列表 = 网络中所有发射台基座与巡航发射器；平均分配算法照抄 1.12（含 `+1` 防零的处理），每个子 insert 用同一 `tx`。
3. GameTest：3 个基座（每个容量 1200）+ 连接器；从连接器位置 `EnergyStorage.SIDED.find(level, connectorPos, Direction.UP)` 取存储 → `insert(3000)` 并提交 → 每个基座约 1000（允许 ±1 误差）；总量受容量限制时返回实际接受量。

**提交信息**：`feat(energy): 机器与发射台网络注册对外能量存储`

---

### S6.04 电池与电池槽放电

- 前置：S6.02
- 必读：energy-fe.md §5.5、§5.6；`legacy-1.12/.../content/items/ItemBattery.java`、`config/ConfigBattery.java`
- 改动文件：`ItemBattery`（实现 `SimpleEnergyItem`，放在 `content/items`——**例外**：`SimpleEnergyItem` 是 TR 接口，因此 `ItemBattery` 改为放在 `icbm.classic.lib.energy.item.ItemBattery`，`ModItems` 引用它；账本注明）；新建 `lib/energy/EnergyItemSlotHelper.java`；各机器 tick 中调用放电；槽位过滤器；GameTest `BatteryGameTest`
- 目标：电池可存电、显示电量条；机器电池槽从电池取电。

**操作**

1. `ItemBattery`：`getEnergyCapacity` 等返回配置值；`EnergyStorage.ITEM.registerForItems(...)` 写在 `EnergyApi.register()`。电量条：`isBarVisible`/`getBarWidth`/`getBarColor`（javap 核对 26.3 `Item` 中这三个方法）。提示文本 `icbmclassic.energy.stored`。
2. `EnergyItemSlotHelper.dischargeInto(Container container, int slot, IcbmEnergyBuffer buffer)`：按 energy-fe.md §5.6；使用 `InventoryStorage.of(container, null).getSlot(slot)`。
3. 槽位过滤器：`EnergyItemSlotHelper.isEnergyItem(ItemStack)` → `EnergyStorageUtil.isEnergyStorage(stack)`；替换 S5 中“只接受 BATTERY”的临时过滤器。
4. GameTest：电池设为满（`SimpleEnergyItem.setStoredEnergyUnchecked`）放入雷达电池槽 → 10 tick 后雷达缓冲增加、电池减少，总量守恒（考虑雷达每 tick 消耗：测试时把 `ConfigRadar.POWER_COST` 设为 0 并还原）。

**提交信息**：`feat(energy): 实现电池与机器电池槽放电`

---

### S6.05 EMP 抽空能量

- 前置：S6.03
- 必读：energy-fe.md §5.7；`ActionEmpArea`（S3.07 留的注释位置）
- 改动文件：`ActionEmpArea`；新建 `lib/energy/EnergyDrain.java`（`drainBlock(ServerLevel, BlockPos)`、`drainInventory(Container)`、`drainPlayer(Player)`）；各 ICBM 机器 `IEMPReceiver` 实现清空 `IcbmEnergyBuffer`；GameTest `EmpEnergyGameTest`
- 目标：EMP 清空外部模组方块/物品中的能量，以及 ICBM 机器自身缓冲。

**操作**：按 energy-fe.md §5.7。`ActionEmpArea` 只调用 `EnergyDrain`（遵守 TR 导入规则）。GameTest：测试模组中注册一个方块实体，暴露 `SimpleEnergyStorage(10000, 0, 10000)` 且初始满 → EMP（`ConfigBlast.emp.DRAIN_ENERGY_TILES=true`）→ 0；雷达缓冲被清空；玩家背包中满电电池被清空（`DRAIN_ENERGY_ENTITY=true`）。

**提交信息**：`feat(energy): EMP 抽空外部与机器能量`

---

### S6.06 GUI 能量条、格式化、提示

- 前置：S6.03、S6.04
- 必读：energy-fe.md §5.8
- 改动文件：新建 `lib/energy/EnergyFormat.java`（main）；各机器菜单 `ContainerData`（long 拆两个 int）；client 中能量条组件；`en_us.json`（`icbmclassic.energy.stored`、`icbmclassic.energy.unit`、机器信息键）；JUnit `EnergyFormatTest`
- 目标：所有机器 GUI 显示 `已存/容量 FE`，悬停显示精确值。

**操作**：`EnergyFormat.format(long)` 规则见 energy-fe.md §5.8，JUnit 覆盖 0、999、1000、12500、1_000_000、2_500_000_000L。`ContainerData`：索引 0 = 存量低 32 位，1 = 高 32 位，2 = 容量低 32 位，3 = 高 32 位；客户端 `((long) hi << 32) | (lo & 0xFFFFFFFFL)`。

**提交信息**：`feat(energy): 机器界面显示 FE 能量`

---

### S6.07 能源互通 GameTest

- 前置：S6.05、S6.06
- 必读：energy-fe.md §6
- 改动文件：gametest 源集：测试模组注册“无限能源方块”`icbmclassic-test:creative_generator`（方块实体每 tick 对 6 个方向用 `EnergyStorageUtil.move(InfiniteEnergyStorage.INSTANCE, target, Long.MAX_VALUE, tx)` 推送；`InfiniteEnergyStorage` 是 TR 自带类，javap 核对）；`EnergyInteropGameTest`
- 目标：证明外部推送可驱动 ICBM 机器工作。

**操作与断言**：

1. `REQUIRES_POWER=true`。生成器 + 雷达相邻 → 雷达 `getRadarState() != OFF`（即有电）且持续 40 tick。
2. 生成器 + 发射台网络中的连接器 → 每个基座满电 → 可发射一次（发射后能量减少 `POWER_COST`）。
3. 生成器 + EMP 塔 → 每 tick 接收不超过 `ENERGY_INPUT`。
4. 运行 `grep -rln "team.reborn.energy" src/main src/client | grep -v "/lib/energy/"`，输出为空（贴到证据）。

**提交信息**：`test(energy): 增加能源互通测试`

---

### S6.08 人工门禁：Tech Reborn 实机

- 前置：S6.07
- 准备者（Agent）：构建 jar + sha256；从 Modrinth 查询 Tech Reborn 26.3 Fabric 最新版本的下载 URL 与 sha512（references.md §0 命令），写入证据；写清单；保持 `[H]`。
- 执行者（人）：客户端 + Tech Reborn 6.2.0（及其依赖 RebornCore、Fabric API 等，按 Tech Reborn 页面说明）。

**检查清单**：

1. 同时加载 ICBM 与 Tech Reborn 无崩溃、无重复 `team_reborn_energy` 版本冲突（Loader 选择较高版本属正常，记录选中版本）。
2. Tech Reborn 发电机 + 线缆 → 雷达站、发射台、EMP 塔充电；GUI 显示 FE 增长。
3. Tech Reborn 充电器给 ICBM 电池充电；ICBM 电池放入机器电池槽放电。
4. ICBM EMP 爆炸后 Tech Reborn 机器能量被清空（`DRAIN_ENERGY_TILES=true` 时）。
5. 将结果填入 `docs/compatibility/energy-interop.md`。

**提交信息**：`docs(compat): 记录 Tech Reborn 能源互通实机结果`
