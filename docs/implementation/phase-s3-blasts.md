# S3 爆炸系统与爆炸相关实体

## 阶段说明

1.12 的爆炸实现依赖若干实体：`Blast` 基类使用 `EntityExplosion`（爆炸持有实体），破片类爆炸生成 `EntityFragments`，反重力生成 `EntityFlyingBlock`（继承 `EntityProjectile`），光束类使用 `EntityLightBeam`，烟雾爆炸生成 `EntitySmoke`，反物质与红物质使用 `EntityRedmatter`。因此本阶段**先**移植投射物基类和这些实体（不含渲染器，渲染器在 S4.07），**再**移植爆炸。

爆炸物 → 实现类对照（来自 `legacy-1.12/.../content/reg/ExplosiveInit.java`）：

| 爆炸物 | 实现 | 本阶段步骤 |
| --- | --- | --- |
| condensed、repulsive、attractive | `BlastTNT`（参数不同） | S3.04 |
| incendiary | `BlastFire` | S3.04 |
| shrapnel、anvil、fragmentation | `BlastShrapnel` + `EntityFragments` | S3.04 |
| sonic | `BlastSonic` | S3.04 |
| breaching | `BlastBreach` | S3.04 |
| exothermic、endothermic | `BlastExothermic`/`BlastEndothermic`（继承 `BlastBeam`） | S3.04 |
| antigravitational | `BlastAntiGravitational` + `EntityFlyingBlock` | S3.04 |
| rot、mutation | `BlastRadioactiveBlockSwaps`、`BlastMutation` | S3.04 |
| debilitation、chemical、contagious、colors | `content/blast/gas/*` | S3.05 |
| smoke | `ActionSpawnEntity` + `EntitySmoke` | S3.05 |
| nuclear、thermobaric、antimatter | `content/blast/threaded/*` | S3.06 |
| redmatter | `ActionSpawnRedmatter` + `EntityRedmatter` | S3.06 |
| emp | `ActionDataEmpArea`/`ActionEmpArea`（`content/actions/emp`） | S3.07 |
| ender | `BlastEnder` | S3.07 |

**每个爆炸的最低测试**（GameTest）：在测试结构中放置一圈石头/玻璃/泥土 + 一只僵尸，直接调用该爆炸物的 `IBlastFactory` 在中心触发，等待完成（线程化爆炸用 `succeedWhen`），断言“至少一个方块被改变或僵尸受到伤害/获得效果”（按爆炸类型选择合理断言，写在步骤卡中）。规模统一用配置的最小可用值或测试中覆盖的 `setBlastSize(3)`，避免测试太慢。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S3.01 投射物基类

- 前置：S2.01、S1.04
- 必读：porting-guide.md §2.4；`legacy-1.12/.../lib/projectile/**`（12）、`lib/world/**`（2，`IProjectileBlockInteraction`）
- 改动文件：对应新文件；账本
- 目标：`EntityProjectile<E>` 基类可被导弹、飞行方块、子弹药、降落伞、气球继承。

**操作**

1. `EntityProjectile` 继承 `net.minecraft.world.entity.Entity`（**不要**改为继承原版 `Projectile`，以保持 1.12 行为与字段；若确需原版 `Projectile` 的命中逻辑，BLOCKED 询问）。
2. 1.12 `onUpdate` 中的移动、碰撞检测（射线 `world.rayTraceBlocks`）→ `level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))`；实体碰撞 → `ProjectileUtil.getEntityHitResult(...)`（javap 核对参数）。
3. `IEntityAdditionalSpawnData`：基类需要同步的字段（如 `sourceOfProjectile`、`ticksInAir`）改 `SynchedEntityData`；只在生成时需要的值（例如初始方向）同步 `setDeltaMovement` 即可（原版添加实体包已包含速度）。
4. `lib/projectile/vanilla/**`（原版投射物的数据注册，如箭）：照抄，用于 `PROJECTILE_DATA_REGISTRY`。
5. `defineSynchedData(SynchedEntityData.Builder)` 中定义所有字段默认值；`readAdditionalSaveData`/`addAdditionalSaveData` 走 `SaveHandler`。
6. GameTest：gametest 源集定义一个最小子类 `TestProjectile`（注册到测试模组的实体类型——测试模组可以自己 `Registry.register`，ID 命名空间 `icbmclassic-test`），以固定速度朝一面石墙生成 → 若干 tick 后断言触发了 `onImpact`（子类中设标志位）。

**提交信息**：`refactor(projectile): 移植投射物基类`

---

### S3.02 爆炸框架与伤害类型

- 前置：S3.01、S2.02、S1.08、S1.09
- 必读：`legacy-1.12/.../content/blast/{Blast,BlastStatus,BlastHelpers}.java`、`content/blast/{imp,helpers,caps}/**`、`content/entity/EntityExplosion.java`、`lib/explosive/**` 余下部分；registry-ids.md §7 伤害类型
- 改动文件：对应新文件；`ModEntities`（注册 `explosion_holder`）；新建 `src/main/resources/data/icbmclassic/damage_type/*.json`、`content/reg/ModDamageTypes.java`；GameTest；账本
- 目标：`Blast` 生命周期（创建→预检→执行→完成）与爆炸持有实体可用；伤害类型齐全。

**操作**

1. `Blast` 继承原版 `Explosion` 吗？1.12 中 `Blast extends Explosion`。26.3 的 `Explosion` 是接口还是类、构造参数为何，先 javap `net.minecraft.world.level.Explosion` 与 `ServerExplosion`。**决定规则**：若 `Explosion` 是接口 → `Blast` 实现它（只实现查询方法）；若是不可继承的类 → `Blast` 不再继承，原有 `world.newExplosion` 风格调用改为 `level.explode(...)`，Forge 的 `ExplosionEvent` 钩子删除。在证据中写明结论。
2. `BlastStatus`、`BlastHelpers`、`BlastBase`、`BlastBlockHelpers` 照抄类型替换。
3. 1.12 `MinecraftForge.EVENT_BUS.post(new BlastEvent...)` → `api/events` 中的 Fabric `Event`（S1.10 已建则直接用）。
4. `EntityExplosion`（`explosion_holder`）：注册实体类型（`MobCategory.MISC`，尺寸照抄 `setSize`，tracking 按 registry-ids.md §4）。
5. 伤害类型：`grep -rn 'DamageSource("' legacy-1.12/src/main/java` 与 `new DamageSource\|EntityDamageSource` 列出全部；每个建一个 `data/icbmclassic/damage_type/<id>.json`：

   ```json
   { "message_id": "icbmclassic.<id>", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player" }
   ```

   `ModDamageTypes` 持有 `ResourceKey<DamageType>` 常量与工具 `source(Level, key)` / `source(Level, key, Entity direct, Entity causing)`（`level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key)` 一类写法，javap 核对）。死亡消息键 `death.attack.icbmclassic.<id>`（及 `.player`）加入 `en_us.json`，英文照抄 1.12 `death.attack.*` 文本。爆炸类伤害类型加入标签 `minecraft:is_explosion`（`data/minecraft/tags/damage_type/is_explosion.json`，`"replace": false`）。
6. GameTest：用一个最小测试爆炸（gametest 中继承 `Blast`，`doExplode` 内把中心方块设为空气）验证生命周期状态依次变化并最终 `COMPLETED`（以 `BlastStatus` 枚举实际值为准）。

**提交信息**：`feat(blast): 移植爆炸框架与伤害类型`

---

### S3.03 爆炸相关实体

- 前置：S3.02
- 必读：`legacy-1.12/.../content/entity/{EntityFragments,EntityLightBeam,EntitySmoke,EntityExplosive}.java`、`content/entity/flyingblock/**`（不含 `RenderEntityBlock`）
- 改动文件：对应新文件；`ModEntities`；GameTest；账本（渲染类标 `→ S4.07`）
- 目标：碎片、光束、烟雾、点燃的炸药（`primed_explosive`）、飞行方块实体可生成、可存档、行为与 1.12 一致。

**操作**

1. 注册实体类型（ID 与追踪参数见 registry-ids.md §4）。
2. `EntityFragments`：箭矢/铁砧/爆炸碎片三种模式字段保留；命中伤害用 `ModDamageTypes` 中的 `shrapnel`（或 1.12 实际使用的伤害源）。
3. `EntityFlyingBlock`：持有 `BlockState`，`SynchedEntityData` 同步（`EntityDataSerializers.BLOCK_STATE`）；落地放回方块的逻辑照抄。
4. `EntityExplosive`（点燃的炸药方块实体）：持有 `explosiveId`（`Identifier`，同步为字符串或用 `EntityDataSerializers` 中合适类型）与引信计时；引信到时调用 `ExplosiveRegistry` 中该爆炸物工厂。
5. `EntitySmoke`、`EntityLightBeam`：照抄。
6. GameTest：每种实体生成 → 存档往返（`saveWithoutId` + 新实体 `load`，javap 核对 26.3 实体存取方法；或用 `ValueOutput` 工具）字段相等；飞行方块落地后放回方块；`primed_explosive`（`condensed`，引信设 1 tick）爆炸后附近石头消失。

**提交信息**：`feat(entity): 移植碎片飞行方块光束烟雾与点燃炸药实体`

---

### S3.04 普通爆炸

- 前置：S3.03
- 必读：本阶段说明对照表；对应 `legacy-1.12/.../content/blast/*.java`；`config/blast/**` 中相关配置
- 改动文件：`BlastTNT`、`BlastFire`、`BlastShrapnel`、`BlastSonic`、`BlastBreach`、`BlastBeam`、`BlastExothermic`、`BlastEndothermic`、`BlastAntiGravitational`、`BlastRadioactiveBlockSwaps`、`BlastMutation`；`ExplosiveInit` 中对应工厂；GameTest `BasicBlastsGameTest`；账本
- 目标：上表 S3.04 行列出的 13 种爆炸物可触发且效果与 1.12 一致。

**操作**：逐个类按通用做法移植。要点：

- `BlastTNT`：1.12 中可能直接调用原版爆炸并带“推/拉”模式（repulsive/attractive）。原版 `level.explode` 在 26.3 无“推拉”参数：推拉效果按 1.12 代码手动对实体施加速度。
- `BlastFire`：放火方块用 `BaseFireBlock.getState(level, pos)`（javap 核对）。
- `BlastRadioactiveBlockSwaps`（rot）依赖放射性方块——方块在 S3.08 注册。本步先实现，方块引用通过 `BuiltInRegistries.BLOCK.getOptional(ICBMConstants.id("radioactive_dirt"))` 获取，不存在则跳过替换并 warn；GameTest 中对 rot 的断言放到 S3.08。
- `BlastMutation`：1.12 把生物转换（村民→僵尸村民等）；26.3 用 `Mob#convertTo(EntityType, ConversionParams, ...)`（javap 核对签名）。
- 破片：fragmentation 的碎片会爆炸，测试时降低数量。

**GameTest 断言**：condensed/breaching/sonic → 至少 1 个石头变空气；incendiary → 至少 1 个火方块出现；shrapnel/anvil/fragmentation → 生成 ≥1 个 `fragment` 实体或僵尸受伤；repulsive/attractive → 僵尸位置在 5 tick 后远离/靠近中心；exothermic/endothermic → 1.12 对应方块转换（如水→冰/岩浆）至少一处；antigravitational → 生成 ≥1 个 `flying_block`；mutation → 放置的村民变为僵尸村民（或 1.12 中对应转换）。

**提交信息**：`feat(blast): 移植常规爆炸`

---

### S3.05 气体爆炸、药水效果与防护装备

- 前置：S3.03
- 必读：`legacy-1.12/.../content/blast/gas/**`（5）、`content/potion/**`（4）、`content/gas/ProtectiveArmorHandler.java`、`ConfigMain.protectiveArmor`、`content/actions/entity/**`（`ActionSpawnEntity` 等，S1.08 暂缓的部分）
- 改动文件：对应新文件；新建 `content/reg/ModEffects.java`；`ItemAntidote`（补 S2.04 的 TODO）；GameTest；账本
- 目标：debilitation、chemical、contagious、colors、smoke 爆炸可用；自定义效果（中毒、传染等）注册；防护装备判定可用。

**操作**

1. `CustomPotion`/`Poison`/`ContagiousPoison` → 继承 `MobEffect`，注册到 `BuiltInRegistries.MOB_EFFECT`（ID 用 1.12 的 potion 名转 snake_case，补充到 registry-ids.md §7，同一提交）。施加效果 `entity.addEffect(new MobEffectInstance(holder, duration, amplifier))`，`holder` = `BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)`（javap 核对）。
2. 气体云：照抄 1.12 逐 tick 在区域内对实体施加效果的逻辑。
3. `ProtectiveArmorHandler`：配置项 `item_ratings`（`"minecraft:iron_helmet=0.02"` 格式）解析 → 延迟解析为 `Map<Item, Float>`。
4. `ActionSpawnEntity`（smoke 使用）移植。
5. `ItemAntidote` 清除本步注册的效果。
6. GameTest：chemical 触发后僵尸获得对应效果；穿戴配置中满防护的盔甲架/僵尸不获得效果（以 1.12 逻辑能支持的实体为准）；smoke 生成 `smoke` 实体；解毒剂清除效果（模拟玩家 `makeMockServerPlayerInLevel()` 使用物品，方法名见 references.md §5）。

**提交信息**：`feat(blast): 移植气体爆炸药水效果与防护装备`

---

### S3.06 线程化爆炸与红物质

- 前置：S3.03
- 必读：`legacy-1.12/.../content/blast/threaded/**`（4）、`content/blast/thread/**` 余下部分、`content/blast/redmatter/**`（含 `caps`、`logic`；`render` 标 → S4.07）
- 改动文件：对应新文件；`ModEntities`（`redmatter`）；GameTest；账本
- 目标：nuclear、thermobaric、antimatter 用 S1.09 框架运行；redmatter 实体（吸积、移动、合并逻辑照 1.12）可用。

**操作**

1. `BlastThreaded`：主线程 `LevelSnapshot.capture(level, center, radius)` → 提交工作 → `BlockEditQueue.submit(...)`；`destroyBlock` 在主线程执行，1.12 `block.onBlockExploded` → 26.3 `state.onExplosionHit(...)` 或直接 `level.removeBlock`/`destroyBlock`（javap `BlockBehaviour` 中与爆炸相关的方法；无合适方法则 `level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)`）。
2. `BlastNuclear`：1.12 先计算再生成冲击波实体/辐射，照抄顺序。
3. `BlastAntimatter`：配置 `damageUnbreakable` 为 true 时连基岩都清除（与 1.12 一致）；杀死范围内红物质。
4. `EntityRedmatter`：1.12 有 TODO 说合并/移动逻辑不工作，**保持原样**，不修。
5. GameTest：nuclear（`setBlastSize(4)`、能量调低）在 200 tick 内使中心 3 格内的石头消失；antimatter（size 3）清除基岩（配置开启时）；redmatter 实体生成后 40 tick 内吸走附近至少 1 个方块；服务器停止时无残留工作线程（检查 `WorkerThreadManager` 状态方法）。

**提交信息**：`feat(blast): 移植线程化爆炸与红物质`

---

### S3.07 EMP 与末影爆炸

- 前置：S3.03
- 必读：`legacy-1.12/.../content/actions/emp/**`、`content/blast/ender/**`、`config/blast/types/ConfigEmp.java`、energy-fe.md §5.7
- 改动文件：对应新文件；GameTest；账本
- 目标：EMP 对方块实体、实体、物品、导弹生效（能量清空部分见下）；ender 爆炸传送。

**操作**

1. `ActionEmpArea`：方块遍历 → `IcbmLookups.EMP_RECEIVER_BLOCK.find(level, pos, null)`；实体 → `EMP_RECEIVER_ENTITY.find(entity, null)`；库存 → 对 `Container` 方块实体与玩家背包中的物品 `EMP_RECEIVER_ITEM.find(stack, null)`。
2. **能量抽空**（`DRAIN_ENERGY_TILES`/`DRAIN_ENERGY_ENTITY`）：本步只处理 ICBM 自身机器（它们实现 `IEMPReceiver`，S5 中清空 `IcbmEnergyBuffer`）；对外部模组存储的抽空在 S6.05 完成。代码中留 `// S6.05: drain external EnergyStorage` 注释位置。
3. 方块替换（`BlockReplacementData`，配置驱动）照抄。
4. Forge `EmpEvent` → `api/events` Fabric 事件。
5. `BlastEnder`：传送目标坐标来自物品/方块上设置的坐标（`EnderBlastCustomization`）；坐标存于 DataComponent `explosive_settings` 的可选字段 `ender_target`（`Optional<GlobalPos>`）——在本步给 `ExplosiveSettings` 增加该字段（Codec 用 `optionalFieldOf`）。
6. GameTest：EMP 使苦力怕失去爆炸能力（1.12 `CapabilityEmpCreeper` 的效果，按实际实现断言）；掉落物中的“可被 EMP 的物品”被处理（按 1.12 逻辑）；ender 爆炸把附近僵尸传送到设定坐标附近。

**提交信息**：`feat(blast): 移植 EMP 与末影爆炸`

---

### S3.08 放射性方块

- 前置：S3.04、S2.03
- 必读：`legacy-1.12/.../content/radioactive/**`
- 改动文件：`BlockRadioactive`（两个实例 `radioactive_dirt`、`radioactive_stone`）、`RadioactiveHandler`；`ModBlocks`/`ModItems`；datagen 补模型、掉落、标签；GameTest；账本
- 目标：放射性方块注册、随机刻扩散/伤害行为与 1.12 一致；rot 爆炸可用。

**操作**

1. `EnumType` 去掉，两个实例分别构造（`BlockRadioactive(Kind kind, Properties)`）。`randomTick` 照抄（需 `.randomTicks()`）。
2. 伤害源 `radioactive_block`（S3.02 已注册）。
3. 重新运行 `./gradlew runDatagen` 并提交生成物。
4. GameTest：rot 爆炸后出现放射性方块；站在放射性方块上的僵尸受伤（若 1.12 是随机刻触发，测试用多次手动调用 `randomTick` 或 `entityInside`，按实际实现）。

**提交信息**：`feat(blocks): 移植放射性方块`

---

### S3.09 爆炸物方块与引信

- 前置：S3.04、S3.05、S3.06、S3.07、S3.08
- 必读：`legacy-1.12/.../content/blocks/explosive/**`（4）、`IExBlockRegistry`、`ExplosiveInit` 中 `setFuseSupplier/setFuseTickListener/setActivationListener`
- 改动文件：`content/blocks/explosive/ExplosiveBlock.java`（替换 S2.05 占位）、新建 `ExplosiveBlockEntity.java`；`ModBlockEntities`（`explosive`）；GameTest；账本（`BlockExplosive`→`MERGED → ExplosiveBlock`，`TileEntityExplosive`→`MERGED → ExplosiveBlockEntity`，`ItemBlockExplosive`/`PropertyExplosive`→`DROPPED（平铺化）`）
- 目标：23 种爆炸物方块：红石激活、火焰/燃烧箭点燃、被爆炸连锁、末影定位交互，与 1.12 一致。

**操作**

1. `ExplosiveBlock(Identifier explosiveId, Properties)`；方块状态属性：朝向 `FACING`（1.12 `ROTATION_PROP`，用 `BlockStateProperties.FACING`）。
2. 方块实体 `ExplosiveBlockEntity` 只用于保存 1.12 `capabilityExplosive` 中的定制数据（`ExplosiveSettings`）；放置时从物品组件复制（`applyImplicitComponents`/`collectImplicitComponents`，javap 已核实存在于 `BlockEntity`）。
3. 激活：`neighborChanged` 收到红石 → 点燃；`onProjectileHit` 着火箭 → 点燃；`wasExploded`（26.3 名称 javap 核对）→ 以短引信点燃；`useItemOn` 打火石/火焰弹 → 点燃；ender 的 `setActivationListener` 在 `useWithoutItem` 中调用。点燃 = 移除方块并生成 `primed_explosive` 实体，引信时间来自 `EX_BLOCK_REGISTRY.getFuseTime(...)`。
4. 发射器（Dispenser）放置/点燃行为：S5.09。
5. 更新 datagen 方块状态为带 `facing` 的 `cube_bottom_top` 旋转变体，重新生成。
6. GameTest：对 `condensed_explosive` 通红石 → 引信后爆炸；对 `nuclear_explosive` 仅断言生成了 `primed_explosive`（不等爆炸，测试结束前 `discard`）；方块掉落自身（挖掘）。

**提交信息**：`feat(blocks): 实现平铺化爆炸物方块与引信`

---

### S3.10 `/icbm` 命令

- 前置：S3.09
- 必读：`legacy-1.12/.../command/**`（13）；references.md §2“命令”
- 改动文件：`src/main/java/icbm/classic/command/**`；`ICBMClassic.java`；GameTest；账本（自研 `CommandGroup`/`SubCommand` 体系 → `MERGED → Brigadier 树`）
- 目标：`/icbm help`、`/icbm blast <explosive> <pos> [scale]`、`/icbm blast spread ...`、`/icbm remove ...`、`/icbm lag` 与 1.12 参数/输出一致。

**操作**

1. 用 `CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ...)` 注册 `Commands.literal("icbm")` 树；权限等级照 1.12（`requires(src -> src.hasPermission(2))`，javap 核对 26.3 权限 API——可能已改为 `PermissionLevel`/`permissions()`）。
2. 参数：爆炸物 ID 用 `IdentifierArgument.id()`（javap 核对类名）+ 自定义建议（`SuggestionProvider` 列出 `ExplosiveRegistry` 全部 ID）；坐标 `Vec3Argument.vec3()`；维度 `DimensionArgument.dimension()`。
3. 输出消息用翻译键（S2.10 迁移后的 `icbmclassic.command.*`），参数顺序与 1.12 相同。
4. GameTest：通过 `server.getCommands().performPrefixedCommand(source, "icbm blast icbmclassic:condensed ~ ~ ~ 2")`（方法名 javap 核对）执行，断言石头消失；未知爆炸物 ID 返回失败且不抛异常。

**提交信息**：`feat(command): icbm 命令改用 Brigadier`
