# S4 导弹与投掷物

## 阶段说明

移植导弹实体、飞行逻辑、导弹物品、区块外追踪、SAM（以新 ID `anti_ballistic_missile` 原样移植）、手雷、炸弹矿车、集束导弹、货物投放（降落伞/气球）、携带物品导弹、座椅，以及全部实体渲染器与粒子。

**导弹体系（1.12）**：`EntityMissile<E>`（继承 `EntityProjectile`）持有 `CapabilityMissile`（实现 `IMissile`：飞行逻辑 `IMissileFlightLogic`、目标数据 `IMissileTarget`、来源 `IActionSource`、在空 tick 数）；具体导弹：`EntityExplosiveMissile`（爆炸物）、`EntityMissileActionable`（任意动作）、`EntitySurfaceToAirMissile`、`EntityHeldItemMissile`。飞行逻辑与目标数据通过 `ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY` 等 builder 注册表按 ID 存档/重建。

**本阶段删除的旧行为**：无。行为保持与 1.12 一致；ABM 的玩法改造在 S7。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S4.01 导弹实体与飞行逻辑

- 前置：S3.01
- 必读：`legacy-1.12/.../content/missile/entity/{EntityMissile,CapabilityMissile,CapabilityEmpMissile}.java`、`content/missile/logic/**`（全部飞行逻辑、目标数据、来源/原因）、`content/missile/MissileEventHandler.java`、`config/missile/ConfigMissile.java`
- 改动文件：对应新文件；`ICBMClassic.java`（向 builder 注册表登记飞行逻辑/目标/原因类型——照抄 1.12 `ICBMClassic` 中 `MISSILE_FLIGHT_LOGIC_REGISTRY.register(...)` 等调用）；GameTest；账本
- 目标：可以在代码中生成一枚导弹实体，按 `ArcFlightLogic` 飞向目标坐标并在命中时回调。

**操作**

1. `EntityMissile`：
   - 1.12 实现了 `IEntityAdditionalSpawnData`（写入飞行逻辑、目标等）。26.3：客户端只需要渲染所需信息（导弹物品外观、是否有玩家骑乘、尾焰颜色），改为 `SynchedEntityData`：`DATA_RENDER_STACK`（`EntityDataSerializers.ITEM_STACK`，由服务端 `toStack()` 设置）、`DATA_SMOKE_COLOR`（`INT`，RGB 打包）。飞行逻辑只在服务端运行，客户端靠原版位置/速度插值。
   - `PACKET_DESC`（1.12 用 codex 同步描述）→ 若只用于上述字段则删除，改用 `SynchedEntityData`；在账本说明。
   - 生命值（`hasHealth`、`ConfigMissile.TIER_*_HEALTH`）：覆写 `hurtServer`。
   - `processInitialInteract`（玩家骑乘导弹）→ `interact(Player, InteractionHand)`。
   - `getRenderBoundingBox`、`isInRangeToRenderDist`：客户端渲染相关，改在渲染器 `shouldRender` 中处理（S4.07），实体中删除。
   - 实体类型注册在 S4.02（具体子类），本步基类为 abstract。
2. `CapabilityMissile` → 改名为 `MissileState`？**不改名**（保留类名以便对照），去掉 Capability 外壳，作为 `EntityMissile` 的字段，`EntityMissile implements IMissile` 时委托给它。
3. 飞行逻辑（`ArcFlightLogic`、`DirectFlightLogic`、`DeadFlightLogic`、`FollowTargetLogic`、`WarmupFlightLogic`、`prefab/**`、`move/**`；`BallisticFlightLogicOld` 仅在 1.12 存档兼容中使用 → `DROPPED（ADR 0002）`，用 `grep -rn BallisticFlightLogicOld legacy-1.12/src` 确认只在注册/兼容处出现，否则 BLOCKED）：类型替换。`entity.motionX` 等 → `getDeltaMovement()/setDeltaMovement(...)`；`entity.rotationYaw` → `setYRot`，同时设置 `yRotO`（上一帧）避免插值抖动。
4. 目标数据（`BasicTargetData`、`BallisticTargetingData`、`TargetRangeDet`）、来源与原因（`ActionSource`、`EntityCause`、`CausedByBlock`、`RedstoneCause`、`ActionCause`）：存档用 `SaveHandler`；`EntityCause` 存实体 UUID（加载后用 `ServerLevel#getEntity(UUID)` 延迟解析）。
5. `MissileEventHandler`（1.12 订阅 Forge 事件）：逐个事件替换为 Fabric 事件（例如实体加载/卸载 → `ServerEntityEvents`），无对应事件的 BLOCKED。
6. `CapabilityEmpMissile`：实现 `IEMPReceiver`，由 `EntityMissile` 直接实现或在 `IcbmLookups.EMP_RECEIVER_ENTITY.registerForTypes(...)` 中为全部导弹类型注册（在 S4.02 类型存在后完成注册；本步写好类）。其中对 SAM 的分支改为 `instanceof IRadioTargetAssignable` 或在 S4.04 回填。
7. GameTest（与 S4.02 合并执行亦可，本步至少编译通过 + 纯逻辑测试）：JUnit 测 `ArcFlightLogic` 的弧线参数计算（1.12 的 `calculateTrajectory` 类方法若为纯数学，抽出测试；否则在 S4.02 用 GameTest）。

**提交信息**：`refactor(missile): 移植导弹实体基类与飞行逻辑`

---

### S4.02 爆炸导弹、动作导弹与导弹物品

- 前置：S4.01、S3.09
- 必读：`legacy-1.12/.../content/missile/entity/explosive/**`、`content/items/ItemMissile.java`、`IExMissileRegistry`、`ExplosiveInit` 中 `EX_MISSILE_REGISTRY.setInteractionListener`
- 改动文件：对应新文件；`ModEntities`（`explosive_missile`、`generic_missile`）；`content/items/ExplosiveFormItem.java`（MISSILE 形态改为 `ItemMissile`，保持构造签名）；`IcbmLookups`（导弹 EMP 注册）；GameTest；账本
- 目标：23 种导弹物品可由代码生成导弹实体并飞行、命中后触发对应爆炸。

**操作**

1. `EntityExplosiveMissile`：持有爆炸物（`CapabilityExplosiveEntity`），命中 → 调用爆炸物工厂；存档保存 `explosiveId` 与 `ExplosiveSettings`。`toStack()` 返回对应平铺化导弹物品。
2. `EntityMissileActionable`：照抄。
3. `ItemMissile(Identifier explosiveId, Properties)`：1.12 中导弹物品本身不能直接使用（由发射台发射），提示文本（等级、爆炸物名称）照抄到 `appendHoverText`，文本用翻译键。
4. GameTest `MissileFlightGameTest`：在测试区域生成 `condensed` 导弹实体，设置 `BasicTargetData(目标坐标=测试区地面某点)` 与 `ArcFlightLogic`，`succeedWhen` 断言 400 tick 内目标点附近石头被破坏（测试结构需要足够高的空间；结构尺寸在 `@GameTest` 注解参数中设置，javap 核对 Fabric `GameTest` 注解字段，如 `structure`/`maxTicks`）。

**提交信息**：`feat(missile): 移植爆炸导弹动作导弹与导弹物品`

---

### S4.03 区块外导弹追踪

- 前置：S4.02
- 必读：overview.md §7；`legacy-1.12/.../content/missile/tracker/**`（4）、`lib/tracker/**`（9）
- 改动文件：对应新文件（`MissileTrackerData extends SavedData`）；`ICBMClassic.java`（事件）；GameTest；账本
- 目标：导弹飞出已加载区块时转为数据模拟，接近目标时在目标区块重新生成实体；数据随世界存档。

**操作**

1. `SavedDataType<MissileTrackerData>`：`new SavedDataType<>(ICBMConstants.id("missile_tracker"), MissileTrackerData::new, MissileTrackerData.CODEC, null)`（第 4 个参数 `DataFixTypes` 传 null 是否允许，javap/源码核对；不允许则用 `DataFixTypes.SAVED_DATA_COMMAND_STORAGE` 一类无害值并记录）。获取：`level.getDataStorage().computeIfAbsent(TYPE)`（方法名 javap 核对 `SavedDataStorage`）。
2. Codec：每条记录（导弹实体完整存档 `CompoundTag` + 模拟位置 + 剩余 tick）用 `RecordCodecBuilder`；导弹实体存档用 `entity.saveWithoutId(...)` 或 26.3 的 `TagValueOutput`（S1.03 工具）。
3. 卸载检测：1.12 在导弹 `onUpdate` 中检查下一位置的区块是否加载；26.3 用 `level.hasChunk(x >> 4, z >> 4)`（javap 核对）或 `getChunkSource().getChunkNow(...) != null`。
4. 重新生成：目标区块加载时（`ServerChunkEvents.CHUNK_LOAD`）或每 tick 检查模拟到期的记录，`EntityType.loadEntityRecursive`（javap 核对）恢复实体并 `addFreshEntity`。
5. `lib/tracker/**`（1.12 的通用事件/日志追踪器，`entity logs switched over to event system` 等 TODO 相关）：照抄或按账本标注。
6. GameTest：生成导弹 → 手动调用 tracker 的“转为模拟”方法（不依赖真实区块卸载）→ 实体被移除且 SavedData 中有 1 条记录 → 推进模拟到期 → 实体在目标附近重新出现。另测 SavedData 编解码往返。

**提交信息**：`feat(missile): 区块外导弹追踪改用 SavedData`

---

### S4.04 SAM 原样移植为 `anti_ballistic_missile`

- 前置：S4.02、S1.06
- 必读：ADR 0004 背景；`legacy-1.12/.../content/missile/entity/anti/**`（含 `item/`）、`config/missile/ConfigSAMMissile.java`、`lib/radio/messages/IncomingMissileMessage.java`
- 改动文件：`EntitySurfaceToAirMissile`、`SAMTargetData`、`ItemSurfaceToAirMissile`、`CapabilitySAMStack`（若只是物品能力外壳则 `DROPPED`）；`ModEntities`、`ModItems`（ID `anti_ballistic_missile`）；`IncomingMissileMessage`（回填 S1.05 的 `IRadioTargetAssignable`）；GameTest；账本
- 目标：1.12 SAM 行为在 26.3 原样可用（追踪、距离≤速度时造成伤害），以新 ID 注册。**类名本步不改**（S7.03 改）。

**操作**

1. 实体/物品注册使用 `anti_ballistic_missile`。`ICBMEntities.MISSILE_SAM` 常量指向新 ID。
2. `EntitySurfaceToAirMissile implements IRadioTargetAssignable`：`assignTarget(Entity)` → `scanLogic.setCurrentTarget(entity)`。
3. 伤害：`currentTarget.hurtServer(serverLevel, ModDamageTypes.source(level, MISSILE_IMPACT, this, owner), ConfigMissile.SAM_MISSILE.ATTACK_DAMAGE)`。
4. 1.12 发射台中 `getItem() == ItemReg.itemSAM` 的判断（`LauncherCapability`、`CLauncherCapability`）在 S5 移植发射台时改为 `== ModItems.ANTI_BALLISTIC_MISSILE`。
5. GameTest：生成一枚目标导弹（`condensed`，朝某方向匀速 `DirectFlightLogic`）与一枚 SAM（`assignTarget`）→ 200 tick 内目标导弹死亡。

**提交信息**：`feat(missile): 以新标识移植地对空导弹原型`

---

### S4.05 手雷与炸弹矿车

- 前置：S3.09、S3.01
- 必读：`legacy-1.12/.../content/entity/{EntityGrenade,EntityBombCart}.java`、`content/items/{ItemGrenade,ItemBombCart}.java`、`IExGrenadeRegistry`、`IExMinecartRegistry`
- 改动文件：对应新文件；`ModEntities`（`grenade`、`bomb_cart`）；`ExplosiveFormItem` 的 GRENADE/BOMB_CART 形态改为 `ItemGrenade`/`ItemBombCart`；GameTest；账本
- 目标：10 种手雷可投掷、引信后爆炸；23 种炸弹矿车可放到铁轨上、被激活铁轨/碰撞触发爆炸。

**操作**

1. `ItemGrenade.use`：1.12 有蓄力投掷（`onPlayerStoppedUsing`）→ 26.3 `releaseUsing(ItemStack, Level, LivingEntity, int)` + `getUseDuration`（javap 核对签名）。
2. `EntityBombCart` 继承原版 TNT 矿车：26.3 类名 `MinecartTNT`（javap 核对，构造器/引爆方法可能是 `primeFuse`、`explode`）。1.12 通过 AT 访问 `minecartTNTFuse`；26.3 先找公开方法，没有则用 Mixin `@Accessor`（加入 `icbmclassic.mixins.json`）。爆炸改为调用 ICBM 爆炸物工厂。
3. `ItemBombCart.useOn` 放置到铁轨上：参照原版 `MinecartItem` 写法（javap / 源码）。
4. 发射器行为在 S5.09。
5. GameTest：手雷实体（`condensed`，引信 1 tick）生成后石头被破坏；炸弹矿车放到激活铁轨上爆炸。

**提交信息**：`feat(entity): 移植手雷与炸弹矿车`

---

### S4.06 集束导弹、子弹药、货物投放、携带物品导弹与座椅

- 前置：S4.02、S4.05
- 必读：`legacy-1.12/.../content/cluster/**`、`content/cargo/**`、`content/missile/entity/itemstack/**`、`content/entity/EntityPlayerSeat.java`；S2.08 证据中标“→ S4.06”的特殊配方
- 改动文件：对应新文件；`ModEntities`（`bomblet`、`parachute`、`balloon`、`held_item_missile`、`missile_seat`）；`ModItems`（`cluster_missile`、`held_item_missile`、`parachute`、`balloon`、BOMBLET 形态）；`ModDataComponents`（扩展 `ExplosiveSettings`、新增 `held_item`）；新建自定义配方序列化器；datagen 配方；GameTest；账本
- 目标：集束导弹在空中释放子弹药；降落伞/气球可投掷并挂载物品/实体；携带物品导弹可装载物品并在目标处投放；导弹座椅可骑乘。

**操作**

1. 集束：`ItemClusterMissile` 的子弹药配置（数量、类型）存 `ExplosiveSettings` 新字段 `cluster`（`Optional<ClusterSettings>`，`record ClusterSettings(Identifier bombletExplosive, int count)`），默认值照抄 `ClusterMissileHandler`。`ActionCluster` 照抄。
2. 子弹药 `EntityBombDroplet`、`ItemBombDroplet`（BOMBLET 形态 10 个）照抄。
3. 货物：`ItemThrowableProjectile`（降落伞、气球物品）、`EntityParachute`、`EntityBalloon`、`CargoProjectileData`、`CargoHolderHandler`。`content/cargo/placer/**`（`BlockCargoHook`、`TileEntityCargoHook*`）在 1.12 **未注册**（`BlockReg` 中无引用）→ `DROPPED（1.12 未注册的半成品）`，用 grep 结果作为证据。
4. 携带物品导弹：装载物品存 DataComponent `held_item`。26.3 中带物品的组件类型：javap 查 `ItemStackTemplate`（26.1 公告提到）是否有 `CODEC`/`STREAM_CODEC`；有则用它，否则用 `ItemStack.CODEC` 并在证据记录。
5. 特殊配方（`RecipeCluster`、`RecipeHeldItemMissile`、`RecipeHeldItemMissileDecraft`、`RecipeCargoData`、`RecipeCargoDataDecraft`）：26.3 自定义配方：`CustomRecipe` 子类 + `RecipeSerializer`（`MapCodec` + `StreamCodec`）注册到 `BuiltInRegistries.RECIPE_SERIALIZER`；配方 JSON 用 datagen 或手写 `data/icbmclassic/recipe/*.json`。javap 核对 `CustomRecipe` 构造参数与 `matches/assemble` 签名（26.x 用 `CraftingInput`）。若 API 差异过大无法照抄 → 该配方 BLOCKED，其余继续。
6. 座椅 `EntityPlayerSeat`：照抄；骑乘位置偏移照抄（1.12 TODO 说偏移有问题，不修）。
7. GameTest：集束导弹在空中触发后生成 N 个 `bomblet` 实体；降落伞实体缓慢下落（速度上限断言）；携带物品导弹命中后在目标处掉落装载物品；配方：`CraftingInput` 构造输入后 `matches` 为真（若可在 GameTest 中访问 `RecipeManager`）。

**提交信息**：`feat(entity): 移植集束导弹货物投放与携带物品导弹`

---

### S4.07 实体渲染器与粒子

- 前置：S4.04、S4.06
- 必读：porting-guide.md §2.4 渲染行、§8；`legacy-1.12/.../client/**`（26）、各 `Render*.java`（`client/render/entity/**`、`content/**/Render*.java`、`content/blast/redmatter/render/**`、`content/entity/flyingblock/RenderEntityBlock.java`）、`client/fx/**`、`client/models/**`
- 改动文件：`src/client/java/icbm/classic/client/**`；`ICBMClassicClient.java`；`ModParticles`（main，注册粒子类型）；registry-ids.md §7 补粒子 ID；账本
- 目标：每种实体在客户端有渲染器（不再隐形或崩溃）；粒子可用。客户端外观正确与否由人工门禁 S5.12 判断。

**操作**

1. 每个 1.12 `Render<T>` → `EntityRenderer<T, S>`，`S` 为自定义 `XxxRenderState extends EntityRenderState`，字段只放渲染需要的值（在 `extractRenderState` 中从实体复制）。
2. `RenderMissile`：在 `submit` 中提交 `DATA_RENDER_STACK` 物品的模型，按 `yRot`/`xRot` 旋转（1.12 中的旋转与平移顺序照抄）。提交物品模型的 API：先 javap `net.minecraft.client.renderer.entity.ItemEntityRenderer`、`net.minecraft.client.renderer.item.ItemStackRenderState`、`SubmitNodeCollector` 看原版如何提交物品；照原版 `ItemEntityRenderer` 的模式写。**禁止**直接 OpenGL。
3. `RenderEntityBlock`（飞行方块）：参照原版 `FallingBlockRenderer` 的 26.3 写法。
4. `ModelICBM`、`ModelChickenHelmet`、`CruiseLauncherTopModel`（1.12 代码模型）：`ModelChickenHelmet` 用于太空鸡彩蛋——改为 26.3 `Model`/`ModelPart`（`LayerDefinition`）写法，层注册用 `EntityModelLayerRegistry`（Fabric，javap 核对）；`CruiseLauncherTopModel` 属于方块实体渲染 → S5.07。
5. `client/mapper/**`（1.12 模型映射器）→ `DROPPED（平铺化后由 datagen 模型取代）`。
6. 粒子：`ParticleSmokeICBM`、`ParticleAirICBM`、`ParticleLauncherSmoke`、`FXAntimatterParticle` → `SimpleParticleType` 注册（main）+ `ParticleProviderRegistry`/`ParticleFactoryRegistry`（Fabric，名称 javap 核对）注册 provider（client）+ `assets/icbmclassic/particles/<id>.json`。补完 S1.04 中特效载荷的客户端处理。
7. `ClientReg`/`ClientProxy` → `ICBMClassicClient.onInitializeClient` 中逐项注册（账本 `MERGED → ICBMClassicClient`）。

**验证**：`./gradlew build`；`./gradlew runClient` **不在 Agent 能力范围内**（无图形环境）——只验证编译与“每个实体类型都有注册的渲染器”：在 client 源集写一个启动自检（`ClientLifecycleEvents.CLIENT_STARTED` 中遍历 `icbmclassic` 实体类型，检查 `EntityRenderers` 是否有 provider，缺失则 `LOGGER.error`），并在证据中说明“由 S5.12 人工观察日志”。

**提交信息**：`feat(client): 移植实体渲染器与粒子`
