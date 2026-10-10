# S1 基础库

## 阶段说明

把不直接产生游戏内容的底层库先移植，后续阶段在其上搭内容。每步都遵循 [README §5.4 移植通用做法](README.md#54-移植类步骤的通用做法s1s5-大多数步骤)。

**范围约定**：本阶段只移植步骤卡列出的旧包。若某个类依赖尚未移植的内容类（如 `content.blocks.*`），用以下方式切断依赖，**不要**提前移植那个内容类：

- 依赖用于“判断是否某个方块/实体”→ 先用接口（已在 `api` 包里的）代替具体类；
- 无法切断 → BLOCKED，在证据中列出依赖链。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。每步完成后 `scripts/check.sh` 必须通过。

---

### S1.01 jlib 与 `lib/transform`

- 前置：S0.05、S0.06
- 必读：porting-guide.md §2.1；`legacy-1.12/src/main/java/com/builtbroken/jlib/**`（9 个文件）；`legacy-1.12/src/main/java/icbm/classic/lib/transform/**`（16 个文件）
- 改动文件：上述旧文件对应的新文件；`src/test/java/icbm/classic/lib/transform/**`（新测试）；账本
- 目标：向量、旋转、区域、位置工具在 26.3 下可用，并有 JUnit 覆盖。

**操作**

1. 移植 `com.builtbroken.jlib` 全部文件。`data/network` 中用到 Netty `ByteBuf` 的方法保留 `io.netty.buffer.ByteBuf` 参数类型（26.3 仍自带 Netty）。
2. 移植 `lib/transform` 全部文件：
   - `Vec3d`→`Vec3`，`EnumFacing`→`Direction`，`MathHelper`→`Mth`，`World`→`Level`，`AxisAlignedBB`→`AABB`。
   - `Location`/`Pos` 类中持有 `World` 的字段改为 `Level`；`world.provider.getDimension()` 改为 `level.dimension()`（类型 `ResourceKey<Level>`）。若类把维度存成 `int` 用于网络/存档，改为存 `Identifier`（`dimension().identifier()`，方法名用 javap 核对 `ResourceKey`）。
   - `BlockEditHandler`（1.12 用 `addScheduledTask` 排队方块修改）：**本步不移植**，在账本标注“→ S1.09”。
3. 为下列行为写 JUnit（只测纯数学，不需要世界）：`Pos`/`Point` 加减乘、距离；`EulerAngle` 由 yaw/pitch 生成方向向量（与 1.12 结果相同：用旧代码在纸面推导期望值，至少 4 个角度：0°、90°、180°、-45° 俯仰）；`Cube`/区域 `isWithin`；`PosDistanceSorter` 排序。

**验证**：`./gradlew test` 通过；`scripts/check.sh` 通过。

**完成判据**：25 个旧文件在账本中都不是 `TODO`；新增测试 ≥ 10 个断言。

**失败处理**：某类依赖 `World` 的大量方法而难以移植 → 只移植纯数学部分，其余方法删除并在账本说明“删除：调用方在 Sx.yy 直接使用 Level API”。

**提交信息**：`refactor(lib): 移植 jlib 与 transform 向量库`

---

### S1.02 JSON 配置系统与全部配置类

- 前置：S0.05、S0.06
- 必读：porting-guide.md §6；ADR 0006 第 1 条；`legacy-1.12/src/main/java/icbm/classic/config/**`（42 个文件）
- 改动文件：新建 `src/main/java/icbm/classic/config/core/{IcbmConfig,ConfigEntry,ConfigRange,ConfigManager}.java`；移植全部配置类到 `src/main/java/icbm/classic/config/**`；修改 `ICBMClassic.java`；新建 `src/test/java/icbm/classic/config/core/ConfigManagerTest.java`；账本
- 目标：启动时生成/读取 `config/icbmclassic/*.json`，所有配置字段可用。

**操作**

1. 注解：

   ```java
   @Retention(RUNTIME) @Target(TYPE)  public @interface IcbmConfig { String file(); }
   @Retention(RUNTIME) @Target(FIELD) public @interface ConfigEntry { String name(); String comment() default ""; }
   @Retention(RUNTIME) @Target(FIELD) public @interface ConfigRange { double min(); double max(); }
   ```

2. `ConfigManager`：

   ```java
   public final class ConfigManager {
       /** 所有配置类，按 1.12 配置文件名排序。新增配置类必须加入这里。 */
       public static final List<Class<?>> CONFIG_CLASSES = List.of(/* TODO(agent): 全部 @IcbmConfig 类 */);

       public static void loadAll() { loadAll(FabricLoader.getInstance().getConfigDir().resolve("icbmclassic")); }

       /** 测试可传临时目录。 */
       public static void loadAll(Path dir) { for (Class<?> c : CONFIG_CLASSES) load(c, dir); }

       static void load(Class<?> cls, Path dir) {
           // 1. path = dir/<file>.json；目录不存在则创建
           // 2. 存在则用 Gson（JsonParser.parseString）读入 JsonObject；解析异常 → 把原文件改名为
           //    <file>.json.broken-<yyyyMMdd-HHmmss>，LOGGER.warn，使用空对象
           // 3. 遍历 cls 的 public static 字段（带 @ConfigEntry）：
           //    - 基本类型/String/String[]/枚举：JSON 中有且类型正确 → 赋值（数值按 @ConfigRange 夹取，越界 warn）；
           //      否则保留默认值
           //    - 其他类型（内部配置对象，如 ConfigSAMMissile 实例）：对其实例字段（带 @ConfigEntry，非 static）递归同样处理，
           //      对应 JSON 子对象
           // 4. 写回：按字段声明顺序生成新的 JsonObject，每个条目前加 "_comment_<name>": comment（comment 为空不加），
           //    子对象同理；Gson setPrettyPrinting 写回文件（UTF-8）
       }
   }
   ```

   - 字段反射用 `Class#getFields()` 会丢失声明顺序保证，改用 `getDeclaredFields()` 并过滤 `public`。
   - 枚举存 `name()`，读回 `Enum.valueOf`，未知值 warn 并保留默认。
   - 只在 `onInitialize` 第一行调用一次（overview.md §5 第 1 条）。客户端额外入口不再调用（同一进程已加载）。

3. 移植每个配置类：`@Config(modid=..., name="icbmclassic/radar")` → `@IcbmConfig(file = "radar")`（取 `name` 中 `icbmclassic/` 之后的部分；没有 `name` 的用类名去掉 `Config` 前缀转 snake_case）；`@Config.Name` → `@ConfigEntry(name)`；`@Config.Comment` 合并进 `comment`；`@Config.RangeInt/RangeDouble` → `@ConfigRange`；`@Config.LangKey`、`@Config.RequiresMcRestart`、`@Config.Ignore` 删除（`@Ignore` 字段不加 `@ConfigEntry`）。字段名、默认值、静态性**不变**。
4. 删除与 Forge 配置同步相关的类（如 `ConfigEventHandler`、`@Mod.EventBusSubscriber` 的 `onConfigChanged`），账本标 `DROPPED（ADR 0006：无游戏内配置界面）`。
5. 1.12 配置中引用 Mekanism/IC2 的条目删除，账本说明。
6. `config/util/**`（如 `BlockReplacementData`、物品 ID 解析）：把 `ResourceLocation` 解析改为 `Identifier.tryParse`；涉及注册表查找（字符串→方块/物品）的，改为**延迟解析**：只在首次使用时（注册表已冻结后）查 `BuiltInRegistries.BLOCK.getOptional(id)`。
7. `ConfigManagerTest`（JUnit，`@TempDir`）：用一个测试专用配置类覆盖：①首次生成文件且含注释键；②修改 JSON 后加载生效；③越界夹取；④损坏 JSON 被备份且不抛异常；⑤缺失字段回写默认值；⑥嵌套对象读写。

**验证**：`./gradlew test`；`timeout 240 ./gradlew runServer --args=--nogui` 后 `ls run/config/icbmclassic/`（目录以实际运行目录为准）列出与 1.12 配置类数量一致的 JSON 文件；`cat run/config/icbmclassic/radar.json` 包含 `"max_range": 500`。

**完成判据**：42 个旧文件均非 `TODO`；6 项测试通过；服务端生成配置文件。

**提交信息**：`feat(config): 用 JSON 配置系统替代 Forge 配置`

---

### S1.03 `SaveHandler`

- 前置：S1.01
- 必读：porting-guide.md §4；`legacy-1.12/src/main/java/icbm/classic/lib/saving/**`（22 个）、`lib/NBTConstants.java`、`lib/buildable/**`（2 个）
- 改动文件：`src/main/java/icbm/classic/lib/saving/**`（含新接口 `ISavable`）、`lib/NBTConstants.java`、`lib/buildable/**`；新建 GameTest `src/gametest/java/icbm/classic/test/lib/SaveHandlerGameTest.java` 与工具 `src/gametest/java/icbm/classic/test/util/SaveRoundTrip.java`；账本
- 目标：声明式存档在 26.3 上可用；提供往返测试工具。

**操作**

1. `NbtSaveHandler`→`SaveHandler`，`NbtSaveRoot`→`SaveRoot`，`NbtSaveNode`→`SaveNode`，`INbtSaveNode`→`ISaveNode`（账本标 `MERGED → 新名`）。节点类 `SaveNodeXxx` 保留名称，`SaveNodeVec3d`→`SaveNodeVec3`、`SaveNodeFacing`→`SaveNodeDirection`、`SaveNodeResourceLocation`→`SaveNodeIdentifier`、`SaveNodeCompoundTag` 保留（`out.store(key, CompoundTag.CODEC, tag)`）、`SaveNodeBlockState`（`BlockState.CODEC`）、`SaveNodeItemStack`（`ItemStack.OPTIONAL_CODEC`）。
2. 每个节点实现：

   ```java
   public interface ISaveNode<E> {
       void save(E obj, ValueOutput out);
       void load(E obj, ValueInput in);
   }
   ```

   `SaveRoot` 用 `out.child(name)` / `in.childOrEmpty(name)` 嵌套；`mainRoot()` 直接写在当前层。
3. `ISavable { void save(ValueOutput out); void load(ValueInput in); }`；`nodeINBTSerializable` 改名 `nodeSavable(String key, Function<E, ISavable> getter)`，实现为 `getter.apply(obj).save(out.child(key))`。
4. `BuildableObject`（`lib/buildable`）的序列化：1.12 写入 `NBTTagCompound`（`id` + `data`）。改为 `save(ValueOutput)`/`load(ValueInput)`：键 `"id"`（`Identifier`）、`"data"`（子节点）。`SaveBuildableObject`、`SaveBuildableObjectList` 用 `childrenList`。
5. 需要“存成 `CompoundTag`”的场景（物品组件、SavedData、网络）另提供工具：`TagValueOutput`/`TagValueInput`（26.3 原版在 `net.minecraft.world.level.storage` 中的实现类，javap 查 `TagValueOutput.createWithContext(ProblemReporter, HolderLookup.Provider)` 之类的工厂方法），封装为 `SaveHandler#saveToTag(E, HolderLookup.Provider)` 与 `loadFromTag(E, CompoundTag, HolderLookup.Provider)`。工厂方法名以 javap 为准。
6. `SaveRoundTrip.assertRoundTrip(GameTestHelper helper, BlockEntity be, Consumer<BlockEntity> mutate, BiPredicate<BlockEntity, BlockEntity> equals)`：mutate → 保存为 tag → 新建同类型方块实体 → 加载 → equals 断言。
7. `SaveHandlerGameTest`：建一个测试用 POJO（在 gametest 源集），声明 int/bool/string/enum/BlockPos/Vec3/UUID/Identifier/嵌套 root/ISavable 节点，`saveToTag` → `loadFromTag` 到新对象 → 逐字段相等。

**验证**：`./gradlew build`（GameTest 包含新测试）。

**完成判据**：账本 25 个文件非 `TODO`；GameTest 通过；证据含 `TagValueOutput` 工厂方法 javap 输出。

**提交信息**：`refactor(saving): 存档处理迁移到 ValueInput 与 ValueOutput`

---

### S1.04 网络 Codex 传输层

- 前置：S1.01
- 必读：porting-guide.md §5；`legacy-1.12/src/main/java/icbm/classic/lib/network/**`（22 个）
- 改动文件：`src/main/java/icbm/classic/lib/network/**`、新建 `src/main/java/icbm/classic/network/{CodexPayload,IcbmNetwork,AirParticlePayload,BlockExplosionFxPayload}.java`、`src/client/java/icbm/classic/client/network/IcbmClientNetwork.java`；修改两个入口类；GameTest；账本
- 目标：`PacketCodex` 构建器 API 不变，底层走 Fabric 载荷，服务端校验。

**操作**

1. 删除 `lib/network/netty/**`、`IPacket`、`PacketBase`、`PacketManager`、`IPacketIDReceiver`（账本 `DROPPED（Fabric 网络替代）`）。
2. `PacketCodex<RAW, TARGET>`：
   - 新增字段 `Identifier codexId`（构造时由 `parentRegistryName` + `"/"` + `name` 得到：`Identifier.fromNamespaceAndPath(parent.getNamespace(), parent.getPath() + "/" + name)`）。
   - 节点 `encoder/decoder` 的参数类型由 `ByteBuf` 改为 `FriendlyByteBuf`；`nodeVec3d`→`nodeVec3`、`nodeFacing`→`nodeDirection`、`nodeItemStack` 改用 `ItemStack.OPTIONAL_STREAM_CODEC`（需要 `RegistryFriendlyByteBuf`：因此 codex 的 body 编码统一用 `RegistryFriendlyByteBuf`，由发送端的 `level.registryAccess()` 包装：`new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess)`）、`nodeNbtCompound` 用 `ByteBufCodecs.COMPOUND_TAG`。
   - 发送方法：`sendToServer(RAW)`（client 源集的 `IcbmClientNetwork.send`，main 源集中用一个静态 `Consumer<CodexPayload>` 钩子，client 入口启动时注入——避免 main 引用客户端类）、`sendToPlayer(RAW, ServerPlayer)`、`sendToTracking(RAW)`、`sendToGuiUsers(RAW, Collection<ServerPlayer>)`。
3. `PacketCodexReg`：`Map<Identifier, PacketCodex>`；重复注册抛异常（与 1.12 一致）。
4. `PacketCodexTile`：body 前缀 `BlockPos`；接收时：`level.isLoaded(pos)` → `getBlockEntity(pos)` → 类型检查（构造时传入 `Class<TARGET>`）。`PacketCodexEntity`：前缀 `varInt entityId`。
5. `CodexPayload` 与两个 `Type`、`StreamCodec`（`Identifier.STREAM_CODEC` + `ByteBufCodecs.BYTE_ARRAY`；若 26.3 没有 `BYTE_ARRAY` 常量则用 `ByteBufCodecs.byteArray(32767)`，javap 核对）。
6. `IcbmNetwork.registerPayloads()`（main 入口调用）：注册两种 Codex 载荷与两种特效载荷类型；`ServerPlayNetworking.registerGlobalReceiver(CodexPayload.C2S, IcbmNetwork::handleServer)`。
7. `handleServer(CodexPayload p, ServerPlayNetworking.Context ctx)`：查 codex → 必须 `isFromClient()` → 解码前缀 → porting-guide §5 的全部校验 → 在主线程应用 setters（Fabric 26.3 接收回调线程用 Javadoc/javap 确认；若在网络线程，用 `ctx.server().execute(...)`）。任何异常 catch 后 `codex.logError(...)` 丢弃。
8. 客户端 `IcbmClientNetwork.register()`：S2C codex 接收 → `Minecraft` 主线程应用；特效载荷接收 → 生成粒子（粒子类型尚未移植时先记录 debug 日志，S4.07 补完，账本注明）。
9. `GeneralCodexs`（无线电频道等通用 codex）一并移植。
10. GameTest `CodexGameTest`：构造一个测试 codex（`nodeInt`、`nodeString`、`nodeBoolean`）→ 用内部方法 `encodeBody(target)` 得到 bytes → `decodeAndApply(bytes, otherTarget)` → 字段相等。另测：`fromServer()` codex 被服务端接收函数拒绝。

**验证**：`./gradlew build`；`scripts/check.sh`。

**完成判据**：账本 22 个文件非 `TODO`；GameTest 通过；main 源集无客户端导入。

**提交信息**：`refactor(network): PacketCodex 改用 Fabric 自定义载荷传输`

---

### S1.05 无线电系统

- 前置：S1.03、S1.04
- 必读：overview.md §7；`legacy-1.12/.../lib/radio/**`（11）、`api/radio/**`（9）
- 改动文件：对应新文件；`ICBMClassic.java`（事件挂接）；GameTest；账本
- 目标：无线电注册表按 `ResourceKey<Level>` 分表，消息同步分发行为与 1.12 一致。

**操作**

1. `RadioRegistry`：`HashMap<Integer, RadioMap>` → `Map<ResourceKey<Level>, RadioMap>`；`popMessage(Level, IRadioSender, IRadioMessage)` 客户端调用直接 `LOGGER.error` 返回（与 1.12 一致）。
2. `ServerLevelEvents.UNLOAD.register((server, level) -> RadioRegistry.clear(level.dimension()))`；`ServerLifecycleEvents.SERVER_STOPPED` 清空全部。
3. 接收器登记：1.12 由方块实体 `onLoad`/`validate` 调用 `RadioRegistry.add`、`invalidate` 调用 `remove`。26.3 中方块实体没有 `onLoad`：在方块实体 `setLevel` 覆写或首次 tick 时登记（推荐：`RadioTile` 提供 `ensureRegistered()`，由宿主方块实体 tick 第一行调用），`setRemoved()` 覆写中移除。本步只实现 `RadioTile` 的这两个方法，宿主在 S5 接入。
4. 消息类（`lib/radio/messages/**`）：`IncomingMissileMessage` 依赖 `EntitySurfaceToAirMissile`（未移植）——本步把 `onTriggerCallback` 中对 SAM 的处理改为调用接口 `IRadioTargetAssignable`（新建于 `api/radio`，方法 `void assignTarget(Entity target)`），S4.04 让 SAM 实现它。`LaunchedWithMissile` 依赖发射台状态类：若未移植，同样切断为 `api` 接口或 BLOCKED。
5. `RadioTranslations` 中的键：保持旧键字符串，S2.10 统一改名。
6. GameTest：两个测试接收器（gametest 源集里实现 `IRadioReceiver` 的简单类，范围 `INFINITE`）+ 一个发送器；同频道收到，异频道收不到，`EMPTY_HZ` 不发送；世界卸载清理用直接调用 `clear` 测试。

**完成判据**：账本 20 个文件非 `TODO`；GameTest 通过。

**提交信息**：`refactor(radio): 无线电系统按维度键分表`

---

### S1.06 雷达注册表与地图

- 前置：S1.01
- 必读：`legacy-1.12/.../lib/radar/**`（3）
- 改动文件：对应新文件；`ICBMClassic.java`；GameTest；账本
- 目标：`RadarRegistry`/`RadarMap`/`RadarEntity` 可用。

**操作**

1. 维度键同 S1.05。
2. 1.12 通过 `EntityJoinWorldEvent`/实体更新把实体加入雷达地图：26.3 用 `ServerEntityEvents.ENTITY_LOAD`/`ENTITY_UNLOAD`（`net.fabricmc.fabric.api.event.lifecycle.v1`），并在 `ServerTickEvents.END_LEVEL_TICK` 中按 1.12 的更新频率刷新位置（照抄旧代码的 tick 间隔）。只登记实现 `IMissile` 的实体与 1.12 中登记的其他类型（照旧代码条件）。
3. `getAllLivingObjectsWithin(Level, x, y, z, range)`、`getRadarObjects(x, z, range)` 保持签名（类型换新）。
4. GameTest：生成一个测试实体（gametest 中实现 `IMissile` 的最小实体——若 `IMissile` 尚未在 S1.10 改签名，则改用 1.12 登记条件里的任意原版实体类型）→ 查询范围内能找到，范围外找不到。

**提交信息**：`refactor(radar): 移植雷达注册表与雷达地图`

---

### S1.07 能力映射与 `IcbmEnergyBuffer`

- 前置：S1.02、S1.03
- 必读：porting-guide.md §2.6；energy-fe.md §4；`legacy-1.12/.../lib/capability/**`（17）、`api/caps/**`（4）、`lib/CapabilityPrefab.java`、`lib/energy/**`（7）、`api/energy/**`（2）
- 改动文件：新建 `src/main/java/icbm/classic/lib/lookup/IcbmLookups.java`、`src/main/java/icbm/classic/lib/energy/{IcbmEnergyBuffer,EnergyRules}.java`；移植 `api/caps/**`；改写 `lib/capability/**`；JUnit；账本
- 目标：所有 1.12 Capability 有 26.3 对应物；内部能量缓冲可用。

**操作**

1. `IcbmLookups`：

   ```java
   public final class IcbmLookups {
       public static final BlockApiLookup<IMissileLauncher, @Nullable Direction> MISSILE_LAUNCHER =
           BlockApiLookup.get(ICBMConstants.id("missile_launcher"), IMissileLauncher.class, Direction.class);
       public static final BlockApiLookup<IMissileHolder, @Nullable Direction> MISSILE_HOLDER = ...;
       public static final BlockApiLookup<IEMPReceiver, Void> EMP_RECEIVER_BLOCK = BlockApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);
       public static final EntityApiLookup<IEMPReceiver, Void> EMP_RECEIVER_ENTITY = EntityApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);
       public static final ItemApiLookup<IEMPReceiver, Void> EMP_RECEIVER_ITEM = ItemApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);
       public static final EntityApiLookup<IMissile, Void> MISSILE = EntityApiLookup.get(ICBMConstants.id("missile"), IMissile.class, Void.class);
       public static void init() {}   // 触发类加载；在 onInitialize 中调用
   }
   ```

   `get` 的签名用 javap 核对 `net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup`。`IMissileLauncher`、`IMissileHolder`、`IMissile` 若还在 1.12 签名，先让本步只引用其类型（编译需要的话把这几个 `api` 接口在本步一并换签名，账本记录）。

2. EMP 默认实现（`CapabilityEmpCreeper`、`CapabilityEmpEntityItem`、`CapabilityEmpInventory`、`CapabilityEmpKill`、`CapabilityEmpChecker`）：改成普通类，在 `IcbmLookups.init()` 中为原版类型注册：`EMP_RECEIVER_ENTITY.registerForTypes((e, v) -> new CapabilityEmpCreeper(e), EntityType.CREEPER)` 等，照抄 1.12 中 `AttachCapabilitiesEvent` 处理器里的条件（在 `legacy-1.12` 中 `grep -rn "AttachCapabilitiesEvent" ` 找到）。
3. `CapSpaceChicken` → 数据附件 `ModAttachments.SPACE_CHICKEN`（`AttachmentRegistry.create(ICBMConstants.id("space_chicken"), b -> b.persistent(Codec.BOOL))`，builder 方法名用 javap 核对 `AttachmentRegistry`）。
4. 爆炸物能力（`CapabilityExplosive*`）：保留类，去掉 `ICapabilitySerializable`，改为普通对象，存档走 `ISavable`；`CapabilityExplosiveStack`（物品）改为从“物品的 explosiveId + DataComponent”构造——DataComponent 在 S2 注册，本步只写构造函数签名 `CapabilityExplosiveStack(ItemStack stack, Identifier explosiveId)` 并在 `// TODO(S2.02)` 注明。
5. GPS：`CapabilityGPSData*` → 在 S2.04/S5.08 用 DataComponent；本步删除 capability 外壳，保留 `GPSDataHelpers` 的纯逻辑。
6. 发射器能力（`CapabilityMissileHolder`、`CapabilityMissileLauncher`）：改为默认实现类，供方块实体组合使用。
7. `IcbmEnergyBuffer`、`EnergyRules`：完全按 energy-fe.md §4 实现。删除 `lib/energy/system/**`、`lib/energy/storage/EnergyBuffer.java`（→ `MERGED → IcbmEnergyBuffer`）、`api/energy/**`（`DROPPED（ADR 0003）`）。
8. JUnit `IcbmEnergyBufferTest`：energy-fe.md §4 列出的 4 类断言。`EnergyRules` 读取 `ConfigMain.REQUIRES_POWER`（静态字段，测试中直接赋值并在 `@AfterEach` 还原）。

**完成判据**：账本对应文件全部非 `TODO`；JUnit 通过；`grep -rn "Capability<" src/` 无结果。

**提交信息**：`refactor(lib): Capability 改为 API Lookup 与数据附件并新增内部能量缓冲`

---

### S1.08 动作系统

- 前置：S1.07
- 必读：`legacy-1.12/.../api/actions/**`（24）、`lib/actions/**`（18）、`content/actions/**`（11）
- 改动文件：对应新文件；GameTest；账本
- 目标：动作（action）/条件/原因/状态/字段系统可用；`content/actions` 中与具体内容耦合的实现可暂缓。

**操作**

1. 移植 `api/actions/**` 与 `lib/actions/**`（纯框架）。`ActionFields` 中字段的存档用 `SaveHandler`，网络用 `FriendlyByteBuf`。
2. `content/actions/**`：`causeby/**`、`conditionals/**` 移植；`emp/**`（`ActionEmpArea` 等）**暂缓到 S3.07**（依赖爆炸配置与方块替换），`entity/**`（生成实体动作）**暂缓到 S3.05**——账本标注目标步骤。
3. 1.12 `MinecraftForge.EVENT_BUS` 事件 → porting-guide §2.1 的 Fabric `Event` 写法。
4. GameTest：构造一个条件 + 简单动作（例如“在位置放置石头”的测试动作，写在 gametest 源集）→ 执行 → 断言方块存在；动作数据保存再加载后字段相等（用 S1.03 工具）。

**提交信息**：`refactor(actions): 移植动作与条件框架`

---

### S1.09 线程化爆炸框架

- 前置：S1.02
- 必读：overview.md §6；ADR 0006 第 5 条；`legacy-1.12/.../lib/thread/**`（3）、`content/blast/thread/**`（5）、`content/blast/threaded/BlastThreaded.java`、`lib/explosive/ThreadWorkBlast.java`、`lib/transform/BlockEditHandler.java`
- 改动文件：`src/main/java/icbm/classic/lib/thread/**`、新建 `lib/thread/LevelSnapshot.java`、`lib/thread/BlockEditQueue.java`；`content/blast/thread/**`（框架部分）；`config` 中线程配置；`ICBMClassic.java`；GameTest；账本
- 目标：爆炸计算在工作线程读取**快照**，方块修改在主线程按预算执行。

**操作**

1. `LevelSnapshot`（主线程创建，工作线程只读）：

   ```java
   public final class LevelSnapshot {
       // key = SectionPos.asLong(sx, sy, sz)，value = 该区块段方块容器的拷贝
       private final Long2ObjectMap<PalettedContainer<BlockState>> sections;
       private final int minY, maxY;

       /** 主线程调用：复制以 center 为中心、半径 radius 内所有**已加载**区块的区块段。未加载区块不复制（视为空气，与 1.12 跳过未加载位置一致）。 */
       public static LevelSnapshot capture(ServerLevel level, BlockPos center, int radius) {
           // 对 chunkX, chunkZ 范围：LevelChunk c = level.getChunkSource().getChunkNow(cx, cz); null 跳过
           // 对 c.getSections() 的每个 index：section.hasOnlyAir() 跳过；否则 put(SectionPos.asLong(cx, sectionY, cz), section.getStates().copy())
           //   sectionY 由 index 换算：level.getSectionYFromSectionIndex(index)（方法名 javap 核对 LevelHeightAccessor）
       }

       /** 工作线程调用。超出范围或无数据返回 Blocks.AIR.defaultBlockState()。 */
       public BlockState getBlockState(int x, int y, int z) { ... container.get(x & 15, y & 15, z & 15) ... }
   }
   ```

   已核实（javap）：`ServerChunkCache#getChunkNow(int,int)`、`ChunkAccess#getSections()`、`LevelChunkSection#getStates()`、`LevelChunkSection#hasOnlyAir()`、`PalettedContainer#copy()`、`PalettedContainer#get(int,int,int)`。

2. 工作线程中计算抗性只用**与位置无关**的方法：`state.isAir()`、`state.getBlock().getExplosionResistance()`、`state.getBlock().defaultDestroyTime()`（`< 0` 表示不可破坏，对应 1.12 `getBlockHardness < 0`）。**禁止**在工作线程调用任何以 `Level`/`BlockGetter` 为参数的方法。
3. `IThreadWork` 增加 `LevelSnapshot` 参数；`ThreadExplosion#doRun(World, Location)` 改为 `doRun(LevelSnapshot snapshot, Vec3 center, RandomSource random)`。随机数：主线程创建 `RandomSource.create(level.getRandom().nextLong())` 传入（工作线程不得使用 `level.getRandom()`）。
4. `WorkerThreadManager`：线程数来自配置（1.12 `ConfigThread`）；守护线程；`SERVER_STOPPING` 时 `shutdownNow` 并等待最多 5 秒；未完成的任务丢弃并 warn。
5. `BlockEditQueue`（替代 `BlockEditHandler`）：

   ```java
   public final class BlockEditQueue {
       /** 工作线程完成后调用（线程安全：ConcurrentLinkedQueue）。 */
       public static void submit(ResourceKey<Level> dim, List<BlockPos> edits, Consumer<BlockPos> action, Runnable onComplete);
       /** ServerTickEvents.END_LEVEL_TICK 中调用：每 tick 至多应用 ConfigThread.BLOCKS_PER_TICK 个（新增配置项，默认 2000）。 */
       static void tick(ServerLevel level);
   }
   ```

   `onComplete` 在最后一个方块应用后、同一 tick 内调用（对应 1.12 “notify blast we have entered world again”）。
6. GameTest `ThreadedBlastFrameworkGameTest`：在测试区放一个 5×5×5 石头块 → 创建快照 → 提交一个测试工作（工作线程把快照中所有石头位置收集为 edits，动作=设为空气）→ 用 `helper.succeedWhen(...)`（javap 核对名称）等待所有石头变成空气。再测：工作线程中调用 `level.getBlockState` 的代码不存在（用 `grep` 写入证据即可，不必自动化）。

**完成判据**：账本对应文件非 `TODO`；GameTest 通过；服务端关闭时日志无线程泄漏告警。

**提交信息**：`refactor(blast): 线程化爆炸改为快照计算并在主线程按预算应用`

---

### S1.10 公共 API 包收尾

- 前置：S1.05、S1.06、S1.08、S1.09
- 必读：`legacy-1.12/.../api/**` 中尚为 `TODO` 的文件（用 `python3 -I scripts/port_ledger.py stats` 与 `grep TODO docs/implementation/port-ledger.md | grep '/api/'` 列出）
- 改动文件：`src/main/java/icbm/classic/api/**` 剩余文件；`ICBMClassicAPI.java`、`ICBMClassicHelpers.java`；账本
- 目标：`api` 包全部为 26.3 签名，可被后续内容实现。

**操作**

1. 机械替换类型（porting-guide §2）。接口方法签名中出现 `World` 的改 `Level`；`EntityPlayer`→`Player`；`ItemStack` 保持；`NBTTagCompound` 参数改 `ValueOutput`/`ValueInput`（序列化方法）或 `CompoundTag`（数据传递）。
2. `ICBMClassicAPI` 中 `@CapabilityInject` 字段删除，改为指向 `IcbmLookups`（S1.07）。`EXPLOSIVE_REGISTRY` 等注册表字段保留，由 S2.02 赋值。
3. `api/refs/ICBMEntities`：把 `ResourceLocation` 常量换成 registry-ids.md §4 的新 ID（`Identifier`）。`ICBMExplosives` 保留字段（S2.02 赋值）。
4. `api/events/**`：改为 Fabric `Event`（porting-guide §2.1）。
5. `ICBMClassicHelpers.getMissile(Entity)`：`entity instanceof IMissile m ? m : IcbmLookups.MISSILE.find(entity, null)`。
6. `api` 包内**不得**引用 `icbm.classic.content.*`（检查：`grep -rn "icbm.classic.content" src/main/java/icbm/classic/api` 必须为空）。

**验证**：`./gradlew build`；上面的 grep；`python3 -I scripts/port_ledger.py stats` 中 `api/` 前缀无 `TODO`。

**提交信息**：`refactor(api): 公共 API 迁移到 26.3 类型`
