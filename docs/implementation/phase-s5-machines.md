# S5 机器、工具与 P1 验收

## 阶段说明

移植全部机器（发射台网络、巡航发射器、雷达站、EMP 塔）及其菜单/屏幕/渲染器、工具物品、发射器行为，然后用回归 GameTest 和人工实机关闭 P1。

**能量**：本阶段机器只用内部 `IcbmEnergyBuffer`（S1.07），**不**注册 Team Reborn Energy（S6）。为了在 S6 之前可测试，GameTest 中设置 `ConfigMain.REQUIRES_POWER = false`（测试结束还原），或直接 `buffer.setStored(...)`。1.12 中机器的能量物品槽保留（槽位存在、过滤器暂时只接受 `ModItems.BATTERY`），放电逻辑在 S6.04 接入。

**方块实体通用模式**（每台机器都按此写）：

```java
public class TileRadarStation extends TileMachine /* S5.01 的新基类，继承 BlockEntity */ implements ExtendedMenuProvider<BlockPos>, IMachineInfo {
    public TileRadarStation(BlockPos pos, BlockState state) { super(ModBlockEntities.RADAR_STATION, pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, TileRadarStation be) {
        if (level.isClientSide()) { be.clientTick(); return; }
        be.serverTick((ServerLevel) level);     // 1.12 update() 中 isServer() 分支的内容
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); SAVE_LOGIC.save(this, out); }
    @Override protected void loadAdditional(ValueInput in)  { super.loadAdditional(in);  SAVE_LOGIC.load(this, in); }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player) { return getBlockPos(); }
    @Override public Component getDisplayName() { return Component.translatable("block.icbmclassic.radar_station"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ContainerRadarStation(id, inv, this); }
}
```

方块类：继承 `BaseEntityBlock`（26.3 中需实现 `codec()`？——26.3 公告说“Block Codecs 已移除”，javap 核对 `BaseEntityBlock` 是否还有抽象 `codec()`），`newBlockEntity` 返回方块实体，`getTicker` 用 `createTickerHelper`，`useWithoutItem` 中服务端 `player.openMenu(be)`。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S5.01 方块实体基类、库存、GUI 组件

- 前置：S2.01、S1.04、S1.07
- 必读：`legacy-1.12/.../prefab/**`（33）、`lib/tile/**`（3）、`lib/data/**`（4，`IMachineInfo` 等）、`lib/InventoryUtility.java`、`content/blocks/multiblock/TileMulti.java`
- 改动文件：`src/main/java/icbm/classic/prefab/{tile,inventory,entity,item}/**`、`lib/tile/**`、`lib/data/**`；`src/client/java/icbm/classic/client/prefab/gui/**`（GUI 组件移到 client 源集，包名 `icbm.classic.client.prefab.gui`）；账本
- 目标：机器共用的基类与组件可用。

**操作**

1. `TileMachine`：继承 `BlockEntity`；1.12 `tickActions`（`TickAction`、`TickDoOnce`）保留，在 `serverTick` 中驱动。`isServer()/isClient()` 改为基于 `level.isClientSide()`。`markDirty` 覆写 → `setChanged` 覆写（保留“服务端时安排描述包”的逻辑）。
2. `InventoryWithSlots`：实现 `Container`（`getContainerSize`、`getItem`、`removeItem`、`removeItemNoUpdate`、`setItem`、`stillValid`、`clearContent`，javap 核对）；存档实现 `ISavable`（`ContainerHelper.saveAllItems`/`loadAllItems` 的 26.3 版本——javap 核对，可能接受 `ValueOutput`）。`InventorySlot` 的过滤器与 `withTick` 保留。
3. `IGuiTile`、`IPlayerUsing`：保留接口；`playersUsing` 类型改 `List<ServerPlayer>`。
4. `BlockICBM`：通用方块基类（朝向等）。
5. GUI 组件（`prefab/gui/**`：按钮、文本框、提示、槽位、能量条等）：移到 client 源集；绘制改为 `GuiGraphicsExtractor` 的 `extract*` 方法（porting-guide §7）；文本框优先换成原版 `EditBox`；自绘文本输入必须处理 SDL 焦点通知（porting-guide §7）。`SlotEnergyItem` 等槽位类留在 main（菜单在双端都要用）。
6. `ContainerBase`（若存在）：继承 `AbstractContainerMenu`，提供 `addPlayerInventory(Inventory, x, y)`（`addStandardInventorySlots`，javap 已在 Fabric 文档示例中出现）与通用 `quickMoveStack`。
7. `TileMulti` 与“假方块”：保留，主方块位置用 `BlockPos` 存档。
8. GameTest：`InventoryWithSlots` 存档往返；`TickDoOnce` 只执行一次。

**提交信息**：`refactor(prefab): 移植机器方块实体基类库存与界面组件`

---

### S5.02 发射台网络方块与方块实体

- 前置：S5.01、S4.02
- 必读：`legacy-1.12/.../content/blocks/launcher/{base,frame,connector,network,status}/**`、`FiringPackage.java`、`LauncherBaseCapability.java`、`LauncherSolution.java`、`LauncherLangs.java`、`config/machines/ConfigLauncher.java`
- 改动文件：对应新文件（不含 `gui/`、`TESR*`）；`ModBlocks`、`ModItems`、`ModBlockEntities`（`launcher_base`、`launcher_frame`、`launcher_connector`）；`IcbmLookups` 注册 `MISSILE_LAUNCHER`/`MISSILE_HOLDER`；datagen 方块状态；GameTest；账本
- 目标：发射台基座 + 发射架 + 连接器组成网络；基座可装填导弹（代码/漏斗），由代码触发发射，导弹飞向目标。

**操作**

1. 方块状态属性：`BlockLaunchFrame` 的 `EnumFrameState`/`PropertyFrameState` → `EnumProperty<EnumFrameState>`（枚举实现 `StringRepresentable`）；基座朝向保留 1.12 实际使用的属性。
2. `LauncherNetwork`/`LauncherNode`：网络发现在方块放置/破坏时重建（1.12 逻辑照抄）；`NetworkInventory`、`NetworkEnergyStorage`：本步把 `NetworkEnergyStorage` 改为对 `IcbmEnergyBuffer` 的简单分配器（内部），S6.03 再改为对外存储。
3. `LauncherCapability`（基座发射逻辑）：
   - `getItem() == ItemReg.itemSAM` → `== ModItems.ANTI_BALLISTIC_MISSILE`。
   - 发射：创建导弹实体（`ItemMissile` 对应的实体类型）、设置目标数据/飞行逻辑/来源，`level.addFreshEntity`，播放声音。
   - 不精确度计算（`applyInaccuracy`）用 `level.getRandom()`。
4. `FiringPackage`、`status/**`（`FiringWithDelay`、`LaunchedWithMissile`、`LauncherStatus`）照抄。
5. 方块交互：空手右击打开菜单（S5.03 前先什么都不做）；手持导弹右击基座装填（照 1.12）。
6. 漏斗/自动化：基座库存通过 `ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), ModBlockEntities.LAUNCHER_BASE)`（Fabric Transfer API，javap 核对）暴露。
7. 发射架高度、连接器：照抄。
8. GameTest `LauncherGameTest`：放基座 + 2 节发射架 → 断言网络包含 3 个节点；向基座库存放入 `condensed_missile` → 设置目标（东 30 格）→ 调用发射 → 断言导弹实体生成，`succeedWhen` 目标附近方块被破坏（`REQUIRES_POWER=false`）。

**提交信息**：`feat(launcher): 移植发射台网络`

---

### S5.03 发射台菜单与屏幕

- 前置：S5.02
- 必读：porting-guide.md §7；`legacy-1.12/.../content/blocks/launcher/{base,screen}/gui/**`、`content/blocks/launcher/screen/**`（发射屏方块与方块实体、`RadioScreen`、`LauncherPair`、`BlockScreenCause`）
- 改动文件：发射屏方块/方块实体、菜单（main）、屏幕（client）；`ModMenus`（`launcher_base`、`launcher_screen`）；`ModBlocks`/`ModItems`（`launcher_screen`）；`ICBMClassicClient`（`MenuScreens.register`）；codex 定义（目标坐标、频道、发射等 `fromClient` 动作）；GameTest；账本
- 目标：发射屏可设定目标坐标、无线电频道、触发发射；基座 GUI 可装填与查看状态。

**操作**

1. `ModMenus`：`Registry.register(BuiltInRegistries.MENU, id, new ExtendedMenuType<>(ContainerLauncherScreen::new, BlockPos.STREAM_CODEC))`——`ExtendedMenuType` 构造器参数 `(ExtendedFactory<T, D>, StreamCodec<? super RegistryFriendlyByteBuf, D>)` 已核实。菜单客户端构造器签名 `(int id, Inventory inv, BlockPos pos)`，从 `inv.player.level().getBlockEntity(pos)` 取方块实体。
2. 客户端→服务端的设置（目标 X/Y/Z、频道、开火）全部走 1.12 已有的 `PacketCodexTile` `fromClient()` codex（S1.04 传输层会做距离/加载/类型校验）。**额外校验**：坐标值限制在世界边界内；频道字符串长度 ≤ 32。
3. 发射屏 `RadioScreen`：保留对遥控器 `ITargetMessage`/`ITriggerActionMessage` 的处理。对 `IIncomingMissileMessage` 的处理**原样保留**（S7.06 才删除），保持 P1 行为等价。
4. 屏幕：`AbstractContainerScreen` 子类，纹理 `icbmclassic:textures/gui/...`（S2.06 已复制），文本全部翻译键。
5. GameTest：模拟玩家（`makeMockServerPlayerInLevel`）通过直接调用 codex 的服务端处理函数设置目标 → 方块实体字段改变；距离 > 64 的模拟玩家发来的设置被拒绝。

**提交信息**：`feat(launcher): 移植发射台菜单与屏幕`

---

### S5.04 巡航发射器

- 前置：S5.03
- 必读：`legacy-1.12/.../content/blocks/launcher/cruise/**`
- 改动文件：对应新文件（不含 TESR）；`ModBlocks`/`ModItems`/`ModBlockEntities`/`ModMenus`（`cruise_launcher`）；GameTest；账本
- 目标：巡航发射器可瞄准、装填、发射（含 SAM/ABM 原型）。

**操作**：同 S5.02/S5.03 模式。`CLauncherCapability` 中 SAM 判断改为 `ModItems.ANTI_BALLISTIC_MISSILE`；`RadioCruise` 原样移植（含对 `IIncomingMissileMessage` 的处理，S7.06 才删除）。GameTest：装填 `condensed_missile`、设目标、发射、命中。

**提交信息**：`feat(launcher): 移植巡航发射器`

---

### S5.05 雷达站

- 前置：S5.01、S1.05、S1.06、S4.04
- 必读：`legacy-1.12/.../content/blocks/radarstation/**`（含 `data/`、`gui/`）、`config/machines/ConfigRadar.java`
- 改动文件：对应新文件；`ModBlocks`/`ModItems`/`ModBlockEntities`/`ModMenus`（`radar_station`）；GameTest；账本
- 目标：雷达探测导弹、计算威胁、输出红石、在频道广播 `IncomingMissileMessage`（1.12 行为原样），GUI 显示雷达点阵。

**操作**

1. `TileRadarStation`：完全照 1.12 `update()` 逻辑（扫描、`isMissileGoingToHit`、`firingCooldown`、红石方块状态属性 `REDSTONE_PROPERTY`、`RADAR_STATE` 属性）。方块状态更新与邻居通知：`level.setBlock(pos, newState, Block.UPDATE_ALL)` 已会通知邻居；1.12 额外的 `notifyNeighborsOfStateExcept` 用 `level.updateNeighborsAt(...)`（javap 核对）。
2. 红石：`BlockRadarStation#isSignalSource` 返回 true；`getSignal`/`getDirectSignal` 返回 `be.getStrongRedstonePower(dir)`。
3. 无线电：首次 tick `radio.ensureRegistered()`；`setRemoved` 移除（S1.05）。
4. `isThreat`：`entity instanceof IMissile && !(entity instanceof EntitySurfaceToAirMissile) && 有爆炸物`（1.12 用 capability 判断爆炸物 → 改为 `ICBMClassicHelpers.getExplosive(entity) != null`）。
5. `RadarRenderData` 点阵编码照抄（`FriendlyByteBuf`）。
6. GameTest：放雷达（`REQUIRES_POWER=false`）+ 旁边放红石灯 → 生成一枚朝雷达飞的 `condensed` 导弹实体 → 断言 `hasIncomingMissiles()` 为真、红石灯亮；设置频道并放一个测试无线电接收器 → 收到 `IncomingMissileMessage`。

**提交信息**：`feat(radar): 移植雷达站`

---

### S5.06 EMP 塔

- 前置：S5.01、S3.07
- 必读：`legacy-1.12/.../content/blocks/emptower/**`、`config/machines/ConfigEmpTower.java`
- 改动文件：对应新文件；`ModBlocks`/`ModItems`/`ModBlockEntities`（`emp_tower`、`emp_tower_fake`）/`ModMenus`；GameTest；账本
- 目标：EMP 塔多方块结构（基座/线圈/电极，`EnumTowerTypes` → `EnumProperty`）、充能、手动/无线电触发放电。

**操作**：照通用模式。`ItemBlockEmpTower`（放置时检查上方空间并放置多块）→ 自定义 `BlockItem` 子类覆写 `placeBlock`/`canPlace`（javap 核对）。`RadioEmpTower` 原样（响应 `IncomingMissileMessage` 与遥控器触发）。EMP 塔实现 `IEMPReceiver`（自身被 EMP 时清空缓冲，1.12 若有此逻辑则照抄）。GameTest：设置充满 → 调用 `fire(null)` → 范围内一枚导弹实体被 EMP 处理（按 `ConfigBlast.emp.missiles` 概率配置，测试中把 `missileKillChance` 设为 1 并还原）。

**提交信息**：`feat(emp): 移植 EMP 塔`

---

### S5.07 方块实体渲染器

- 前置：S5.04、S5.05、S5.06
- 必读：porting-guide.md §8；`legacy-1.12/.../**/TESR*.java`、`client/models/CruiseLauncherTopModel.java`
- 改动文件：`src/client/java/icbm/classic/client/render/tile/**`；`ICBMClassicClient`；账本
- 目标：发射台基座（显示装填的导弹）、巡航发射器（转动炮塔）、EMP 塔（旋转部件）有渲染器。

**操作**：`BlockEntityRenderer<T, S>`：`createRenderState()`、`extractRenderState(T, S, float, Vec3, CrumblingOverlay)`、`submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`（签名已核实）。导弹物品模型的提交方式与 S4.07 `RenderMissile` 相同（复用工具方法）。`BlockEntityRenderers.register(type, ctx -> new TESRLauncherBase(ctx))`（或 Fabric `BlockEntityRendererRegistry`）。1.12 TODO“导弹按发射台光照渲染偏暗”不修。

**验证**：编译 + S4.07 的启动自检扩展到方块实体渲染器。

**提交信息**：`feat(client): 移植方块实体渲染器`

---

### S5.08 工具物品

- 前置：S5.04、S5.05
- 必读：`legacy-1.12/.../content/items/{ItemRemoteDetonator,ItemLaserDetonator,ItemRadarGun,ItemRocketLauncher,ItemDefuser,ItemBattery}.java`、`prefab/FakeRadioSender.java`、`lib/capability/gps/**`
- 改动文件：对应新文件；`ModItems`；GameTest；账本
- 目标：遥控引爆器（按频道触发）、激光引爆器（指向方块设定目标并触发）、雷达枪（记录坐标到 `gps_data`）、火箭筒/弹道发射器（手持发射导弹）、拆弹器（拆除点燃的炸药/导弹）、电池（本步只注册、显示存量为 0，S6.04 实现）可用。

**操作**

1. GPS 坐标：DataComponent `gps_data`；频道：`radio_channel`。
2. 射线：`player.pick(range, partialTick, false)` 或 `level.clip(...)`（javap 核对 `Entity#pick`）。
3. 火箭筒：`use`/`releaseUsing` 照 1.12 蓄力；从背包消耗导弹物品；生成导弹并设 `HANDHELD_FUEL`。
4. 拆弹器：对 `primed_explosive`、导弹、炸弹矿车 `interactLivingEntity`/攻击实体回调（Fabric `AttackEntityCallback` 或物品 `hurtEnemy`，按 1.12 实际触发方式）。
5. `FakeRadioSender`：持有 `ServerPlayer`。
6. GameTest：雷达枪对方块使用 → 物品组件有坐标；遥控器（频道 `t1`）使用 → 同频道测试接收器收到触发消息；拆弹器对 `primed_explosive` 使用 → 实体被移除并掉落炸药方块（按 1.12）。

**提交信息**：`feat(items): 移植遥控器激光引爆器雷达枪火箭筒与拆弹器`

---

### S5.09 发射器（Dispenser）行为与红石

- 前置：S4.05、S5.05
- 必读：`legacy-1.12/.../content/items/behavior/**`；1.12 中 `BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject` 的全部调用
- 改动文件：对应新文件；`ICBMClassic.java`；GameTest；账本
- 目标：发射器可射出手雷、放置炸弹矿车（及 1.12 中注册的其他行为）。

**操作**：`DispenserBlock.registerBehavior(item, behavior)`，对每个平铺化手雷/矿车物品注册；行为类继承 `DefaultDispenseItemBehavior`，`execute(BlockSource, ItemStack)`（javap 核对）。GameTest：发射器装 `condensed_grenade`，给红石脉冲 → 生成 `grenade` 实体。

**提交信息**：`feat(items): 移植发射器投掷行为`

---

### S5.10 P1 回归 GameTest 套件

- 前置：S5.07、S5.08、S5.09、S4.07、S4.03、S3.10
- 必读：phase-1-port.md 全部条目
- 改动文件：`src/gametest/java/icbm/classic/test/regression/**`；GameTest 入口
- 目标：一个覆盖 P1 范围的回归套件。

**操作**：新增以下测试（已有的不重复写，在证据中列出“已有覆盖”的测试名）：

1. `AllBlocksPlaceable`：遍历 `icbmclassic` 命名空间全部方块，逐个在测试区放置并断言存在（多方块/需要支撑的跳过并列出原因）。
2. `AllItemsStackable`：遍历全部物品 `new ItemStack(item)` 非空、`getName` 非空，名称键在 `en_us` 中存在（读取 jar 内合并后的 `en_us.json`）。
3. `AllEntityTypesSpawnable`：遍历全部 `icbmclassic` 实体类型，`create` 后 `addFreshEntity` 不抛异常，并 `discard`。
4. `AllExplosivesTrigger`：遍历 `ExplosiveRegistry`（排除 nuclear、antimatter、redmatter 等耗时项，或把它们规模设为最小），触发后无异常。
5. `LaunchToRadarScenario`：发射台发射 → 雷达探测到 → EMP 塔（同频）放电 → 导弹被处理（概率配置设为 1）。

**验证**：`./gradlew build`；总耗时写入证据（若超过 10 分钟，把耗时项拆到单独的 `@GameTest(batch=...)`，javap 核对注解字段）。

**提交信息**：`test: 增加 26.3 移植回归测试套件`

---

### S5.11 移植账本清零与禁用名称扫描

- 前置：S5.10
- 改动文件：`docs/implementation/port-ledger.md`；（如有）遗漏文件的移植
- 目标：账本无 `TODO`；`scripts/check.sh` 通过。

**操作**：`python3 -I scripts/port_ledger.py check`。列出的 `TODO` 文件：属于已完成步骤范围但遗漏的 → 在本步移植（列入证据）；不属于任何步骤 → BLOCKED 询问。检查所有 `DROPPED` 行都有原因。

**提交信息**：`docs(impl): 移植账本清零`

---

### S5.12 人工门禁：客户端实机全流程

- 前置：S5.11
- 准备者（Agent）：构建 `./gradlew build`，记录 jar 文件名与 `sha256sum`；把下面清单写入证据；状态保持 `[H]`。
- 执行者（人）：Windows 或 Linux 桌面，Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.162.0+26.3 + 本模组 jar。

**检查清单**（每项：通过/失败 + 截图或日志位置）：

1. 启动无崩溃；日志中 S4.07 自检无“缺少渲染器”错误。
2. 创造标签页显示全部物品，图标无紫黑方块。
3. 放置每种机器方块，外观正确。
4. 搭建发射台（基座 + 发射架 + 发射屏），装填导弹，屏幕输入坐标，发射，导弹弧线飞行并命中。
5. 巡航发射器瞄准并发射。
6. 雷达站 GUI 显示来袭导弹点阵；红石输出。
7. EMP 塔充能（创造模式 `REQUIRES_POWER` 可临时关闭）并放电，范围内导弹被击落/失效。
8. 手雷投掷、炸弹矿车、火箭筒、遥控器、激光引爆器、雷达枪、拆弹器各用一次。
9. 核弹、反物质各引爆一次，服务器不卡死（记录 TPS 恢复时间）。
10. 存档退出再进入，机器设置保留。
11. 专用服务器 + 客户端联机重复第 4、6、7 项。

**提交信息**（人完成后由验收 Agent 提交证据）：`docs(impl): 记录 S5.12 客户端实机验收`
