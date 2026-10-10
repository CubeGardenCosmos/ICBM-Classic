# 移植指南：Forge 1.12.2 → Fabric 26.3

本文件给执行 Agent 查表用。**表中的 26.3 名称均于 2026-10-10 用 `javap` 对 `minecraft-merged-deobf-26.3.jar` 或 Fabric API 0.162.0+26.3 模块核实**；遇到表中没有的类，按 [实施手册 §4.3](../implementation/README.md#43-用-javap-查-263-真实签名) 自己用 `javap` 查，**不要凭记忆或网上旧教程写方法名**（1.20/1.21 教程中的大量方法在 26.x 已改名或改签名）。

## 1. 总原则

1. **逐文件移植，保持类名与包名**：`legacy-1.12/src/main/java/icbm/classic/a/b/Foo.java` → `src/main/java/icbm/classic/a/b/Foo.java`（客户端类 → `src/client/java/...`）。只有本文件 §8 和 [registry-ids.md](registry-ids.md) 明确要求改名的类才改名。
2. **行为等价优先**：先让 26.3 版本的行为与 1.12 一致，再做步骤卡要求的改进。不要在移植步骤里修 `TODO` 注释中提到的旧问题，除非步骤卡要求。
3. **保留注释与作者署名**（`Created by Dark(DarkGuardsman, Robin) on ...`）。
4. **类型替换要彻底**：移植后的文件中不得再出现 `net.minecraftforge`、`net.minecraft.util.math.Vec3d`、`EnumFacing`、`NBTTagCompound`、`TileEntity`、`EntityPlayer`、`World`（作为类型）等 1.12 名称。S0.05 的 `scripts/check.sh` 会 grep 检查。
5. 每个移植步骤更新 [移植账本](../implementation/port-ledger.md)（S0.06 生成），把对应旧文件状态改为 `PORTED`/`MERGED`/`DROPPED`。

## 2. 类型与 API 对照表

### 2.1 基础类型

| 1.12（Forge/MCP） | 26.3（Mojang 名称） | 备注 |
| --- | --- | --- |
| `net.minecraft.util.ResourceLocation` | `net.minecraft.resources.Identifier` | 构造：`Identifier.fromNamespaceAndPath(ns, path)`；解析：`Identifier.parse(s)` / `tryParse(s)`。无公开构造函数 |
| `ICBMConstants.PREFIX + "x"`（`"icbmclassic:x"`） | `ICBMConstants.id("x")` 返回 `Identifier` | 新增工具方法 |
| `World` | `Level`（通用）/ `ServerLevel`（服务端） | 判断：`level.isClientSide()`（方法，不是字段） |
| `world.isRemote` | `level.isClientSide()` | |
| `world.provider.getDimension()`（int） | `level.dimension()` → `ResourceKey<Level>` | 维度不再是整数 |
| `BlockPos`（`util.math`） | `net.minecraft.core.BlockPos` | |
| `EnumFacing` | `net.minecraft.core.Direction` | `EnumFacing.VALUES` → `Direction.values()` |
| `Vec3d` | `net.minecraft.world.phys.Vec3` | `addVector(x,y,z)` → `add(x,y,z)`；字段 `x,y,z` 仍可直接读 |
| `AxisAlignedBB` | `net.minecraft.world.phys.AABB` | `grow` → `inflate` |
| `MathHelper` | `net.minecraft.util.Mth` | `sqrt` 返回 float |
| `NBTTagCompound` | `net.minecraft.nbt.CompoundTag` | 方块实体/实体存档改用 `ValueInput`/`ValueOutput`（§4），只有物品组件、网络、SavedData 才直接用 `CompoundTag` |
| `ITextComponent` / `TextComponentTranslation` / `TextComponentString` | `Component` / `Component.translatable(key, args...)` / `Component.literal(s)` | `literal` 只允许用于数字、坐标、玩家名，见 i18n.md |
| `EntityPlayer` / `EntityPlayerMP` | `Player` / `ServerPlayer` | |
| `EnumHand` | `InteractionHand` | |
| `EnumActionResult` / `ActionResult<ItemStack>` | `InteractionResult`（`SUCCESS`/`CONSUME`/`PASS`/`FAIL`） | `Item#use` 返回 `InteractionResult` |
| `IBlockState` | `BlockState` | |
| `Material.ROCK` 等 | 删除；用 `BlockBehaviour.Properties.of().strength(h, r).requiresCorrectToolForDrops()` + 方块标签 `minecraft:mineable/pickaxe` | |
| `SoundEvent` 注册 | `SoundEvent.createVariableRangeEvent(Identifier)` + `Registry.register(BuiltInRegistries.SOUND_EVENT, id, ev)` | |
| `DamageSource("x")` | `level.damageSources().generic()` / `.explosion(entity, attacker)`；自定义伤害类型用数据包 `data/icbmclassic/damage_type/*.json` + `ResourceKey<DamageType>` | 见 Fabric 文档 entities/damage-types |
| `Potion`（效果） | `MobEffect`，注册到 `BuiltInRegistries.MOB_EFFECT`，引用用 `Holder<MobEffect>` | |
| `@SideOnly(Side.CLIENT)` | 删除；客户端代码放 `src/client` 源集 | |
| `MinecraftForge.EVENT_BUS.post(new XxxEvent(...))`（ICBM 自己的事件，如 `EmpEvent`、`ExplosiveEvent`、`LauncherEvent`） | Fabric 事件：`public static final Event<Callback> X = EventFactory.createArrayBacked(Callback.class, listeners -> (...) -> {...})`，放在原事件类的同名类中；可取消事件用返回 `boolean`（任一监听器返回 false 即取消） | 原版/Forge 事件（tick、世界加载等）改用 Fabric API 生命周期事件，见 overview.md §5 |
| `FMLCommonHandler...getMinecraftServerInstance()` | 由事件参数或 `level.getServer()` 取得；禁止静态持有 `MinecraftServer` 跨世界卸载 | |

### 2.2 注册

| 1.12 | 26.3 |
| --- | --- |
| `RegistryEvent.Register<Block>` + `event.getRegistry().register(block)` | `Registry.register(BuiltInRegistries.BLOCK, key, block)`，在 `onInitialize` 中直接调用 |
| `block.setRegistryName(...)`、`setUnlocalizedName(...)` | 删除。**必须**在构造前给 Properties 设置 ID：`BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))`；物品同理 `new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))`。不设置会在启动时报 `Block id not set` |
| `ItemBlock` | `new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())` | `useBlockDescriptionPrefix()` 让物品使用 `block.icbmclassic.*` 翻译键 |
| `@ObjectHolder` | 删除；用 `public static final` 字段保存注册结果 |
| `GameRegistry.registerTileEntity(Class, id)` | `Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.create(FooBlockEntity::new, ModBlocks.FOO).build())` | `FabricBlockEntityTypeBuilder` 在 `net.fabricmc.fabric.api.object.builder.v1.block.entity` |
| `EntityEntryBuilder` | `EntityType.Builder.<T>of(Foo::new, MobCategory.MISC).sized(w, h).clientTrackingRange(chunks).updateInterval(ticks).build(ResourceKey.create(Registries.ENTITY_TYPE, id))` | 1.12 的 `trackingRange` 单位是方块，26.3 `clientTrackingRange` 单位是**区块**：`ceil(blocks / 16)` |
| `CreativeTabs` | `FabricCreativeModeTab.builder()`（`net.fabricmc.fabric.api.creativetab.v1`）注册到 `BuiltInRegistries.CREATIVE_MODE_TAB` | 见 S2.05 |
| `GameRegistry.addSmelting`、代码注册配方、OreDictionary | 数据包 JSON 配方（datagen `FabricRecipeProvider`）+ 物品标签（`c:` 公约标签） | 见 §9 |
| `IGuiHandler` | `MenuType` / `ExtendedMenuType<T, D>`（`net.fabricmc.fabric.api.menu.v1`）+ `MenuScreens.register` | 见 §7 |

### 2.3 方块与方块实体

| 1.12 | 26.3 |
| --- | --- |
| `TileEntity` / `ITickable#update()` | `BlockEntity`；tick 用 `EntityBlock#getTicker` 返回 `BaseEntityBlock.createTickerHelper(type, MY_TYPE, FooBlockEntity::tick)`，静态 `tick(Level, BlockPos, BlockState, FooBlockEntity)` |
| `readFromNBT` / `writeToNBT` | `loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)`（§4） |
| `getUpdatePacket` / `getUpdateTag` / `onDataPacket` | `getUpdatePacket()` 返回 `ClientboundBlockEntityDataPacket.create(this)`；`getUpdateTag(HolderLookup.Provider)` 返回 `saveWithoutMetadata(provider)`；客户端自动调用 `loadAdditional` |
| `markDirty()` | `setChanged()`；需要同步到客户端时再 `level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)` |
| `Block#onBlockActivated` | `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)`（空手）与 `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)`（持物） |
| `Block#breakBlock` | `affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)`；方块实体被移除前的副作用写在 `BlockEntity#preRemoveSideEffects(BlockPos, BlockState)`（掉落库存在这里做） |
| `neighborChanged(IBlockState, World, BlockPos, Block, BlockPos)` | `neighborChanged(BlockState, Level, BlockPos, Block, Orientation, boolean)` |
| `getStrongPower`/`getWeakPower` | `getDirectSignal` / `getSignal(BlockState, BlockGetter, BlockPos, Direction)`；还要覆盖 `isSignalSource` 返回 true |
| `getActualState`、`PropertyXxx`（自定义属性类） | `BlockStateProperties` 或 `EnumProperty.create("name", EnumClass.class)`（枚举实现 `StringRepresentable`）；`createBlockStateDefinition(StateDefinition.Builder)` 中 `builder.add(...)` |
| `EnumBlockRenderType.ENTITYBLOCK_ANIMATED` | `getRenderShape` 返回 `RenderShape.INVISIBLE`（完全由方块实体渲染器绘制时） |
| `shouldRefresh`（1.12 方块状态变化不重建 TE） | 不需要；26.3 方块状态变化（同一方块）不会替换方块实体 |
| 多方块“假方块” `TileMulti`、`TileEmpTowerFake` | 保留设计：假方块持有主方块 `BlockPos`，交互转发给主方块实体 |

### 2.4 实体

| 1.12 | 26.3 |
| --- | --- |
| `onUpdate()` | `tick()`（先调 `super.tick()`） |
| `readEntityFromNBT` / `writeEntityToNBT` | `readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)` |
| `entityInit()` + `DataParameter` + `dataManager` | `defineSynchedData(SynchedEntityData.Builder builder)`；`EntityDataAccessor<T> X = SynchedEntityData.defineId(Foo.class, EntityDataSerializers.FLOAT)`；`builder.define(X, 0f)`；`entityData.get(X)` / `entityData.set(X, v)` |
| `IEntityAdditionalSpawnData` | 删除。需要在客户端生成时就知道的数据放进 `SynchedEntityData`；大块或不常变的数据在 `EntityTrackingEvents.START_TRACKING` 时发自定义载荷 |
| `setDead()` | `discard()`；被杀死 `kill(ServerLevel)`；带原因 `remove(RemovalReason)` |
| `attackEntityFrom(source, amount)` | 服务端：`hurtServer(ServerLevel, DamageSource, float)`（覆盖它处理伤害）；调用方用 `entity.hurtServer(serverLevel, source, amount)` |
| `posX/posY/posZ`、`motionX/Y/Z` | `getX()/getY()/getZ()`、`position()`；`getDeltaMovement()` / `setDeltaMovement(Vec3)` |
| `rotationYaw/Pitch` | `getYRot()/setYRot()`、`getXRot()/setXRot()`；`yRotO`/`xRotO` 为上一 tick 值 |
| `world.getEntitiesWithinAABB(Class, aabb, predicate)` | `level.getEntitiesOfClass(Class, AABB, Predicate)` |
| `world.spawnEntity(e)` | `level.addFreshEntity(e)` |
| `getDistance(entity)` / `getDistanceSq` | `distanceTo(entity)` / `distanceToSqr(entity or Vec3)` |
| `processInitialInteract` | `interact(Player, InteractionHand)`（实体类方法，签名用 javap 核对） |
| `world.createExplosion(entity, x, y, z, power, damagesTerrain)` | `level.explode(entity, x, y, z, power, Level.ExplosionInteraction.TNT 或 NONE)` |
| 渲染：`Render<T>`、`RenderingRegistry.registerEntityRenderingHandler` | `EntityRenderer<T, S extends EntityRenderState>`：`createRenderState()`、`extractRenderState(T, S, float partialTick)`、`submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`；注册 `EntityRenderers.register(type, Ctor::new)` 或 Fabric `EntityRendererRegistry` |

### 2.5 物品

| 1.12 | 26.3 |
| --- | --- |
| `onItemRightClick(World, EntityPlayer, EnumHand)` | `use(Level, Player, InteractionHand)` 返回 `InteractionResult` |
| `onItemUse(...)` | `useOn(UseOnContext)` |
| `addInformation(stack, world, tooltip, flag)` | `appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` |
| `onUpdate(stack, world, entity, slot, selected)` | `inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)`（仅服务端调用；`EquipmentSlot` 可能为 null 表示普通背包格，用 javap/源码核对） |
| `stack.getTagCompound()` 存自定义数据 | **DataComponent**：在 `ModDataComponents` 注册 `DataComponentType<T>`（`DataComponentType.<T>builder().persistent(codec).networkSynchronized(streamCodec).build()`），读写 `stack.get(TYPE)` / `stack.set(TYPE, value)` |
| `getMetadata`、`getHasSubtypes`、`getSubItems` | 删除（平铺化，ADR 0002） |
| `setMaxStackSize(n)` | `Item.Properties#stacksTo(n)` |
| `setMaxDamage(n)` | `Item.Properties#durability(n)` |
| `IBehaviorDispenseItem` | `DispenserBlock.registerBehavior(item, behavior)`，行为类实现 `DispenseItemBehavior`（或继承 `DefaultDispenseItemBehavior`） |
| 物品模型 `models/item/*.json` + `ModelLoader.setCustomModelResourceLocation` | 物品模型定义 `assets/icbmclassic/items/<id>.json`（1.21.4 起必需）+ `models/item/<id>.json`；由 datagen `ItemModelGenerators` 生成 |

### 2.6 Capability → Fabric

| 1.12 Capability | 26.3 方案 |
| --- | --- |
| `CapabilityEnergy.ENERGY`（机器、电池） | S5 内部 `IcbmEnergyBuffer`；S6 起 `EnergyApi` → Team Reborn `EnergyStorage.SIDED` / `EnergyStorage.ITEM`（ADR 0003） |
| `ICBMClassicAPI.MISSILE_CAPABILITY`（实体上的 `IMissile`） | 导弹实体类直接实现 `IMissile`；查询工具 `ICBMClassicHelpers.getMissile(Entity)` 改为 `entity instanceof IMissile m ? m : null`。若需第三方实体也能成为导弹，用 `EntityApiLookup<IMissile, Void>`（`icbmclassic:missile`） |
| `EXPLOSIVE_CAPABILITY`（实体/物品/方块实体上的 `IExplosive`） | 实体与方块实体：类实现 `IExplosiveProvider`；物品：由物品类的 `explosiveId` 字段（平铺化）+ 可选 DataComponent `icbmclassic:explosive_settings` 构造 `IExplosive`。工具 `ICBMClassicHelpers.getExplosive(Object)` 统一入口 |
| `IMissileHolder` / `IMissileLauncher`（方块实体） | `BlockApiLookup<IMissileLauncher, @Nullable Direction>`（`icbmclassic:missile_launcher`）与 `BlockApiLookup<IMissileHolder, @Nullable Direction>`（`icbmclassic:missile_holder`） |
| `IEMPReceiver`（实体/方块实体/物品） | `BlockApiLookup<IEMPReceiver, Void>`、`EntityApiLookup<IEMPReceiver, Void>`、`ItemApiLookup<IEMPReceiver, Void>`（各 `icbmclassic:emp_receiver`）；原版实体（苦力怕、掉落物）的默认实现用 `registerForTypes` 注册 |
| `IGPSData`（物品，坐标卡/遥控器） | DataComponent `icbmclassic:gps_data`（`record GpsData(Optional<GlobalPos> pos)`） |
| `CapSpaceChicken`（彩蛋：被反重力影响的鸡） | 数据附件 `AttachmentType<Boolean>`（`AttachmentRegistry.create(id, b -> b.persistent(Codec.BOOL))`）挂在实体上 |
| `IItemHandler`（库存） | 方块实体实现 `Container`（继承 `BaseContainerBlockEntity` 或自管 `NonNullList<ItemStack>`）；对外的物品自动化用 Fabric Transfer API `ItemStorage.SIDED` + `ContainerStorage.of(container, side)` |

## 3. 包级移植映射

| 1.12 包 / 文件 | 目标 | 步骤 |
| --- | --- | --- |
| `com.builtbroken.jlib.*` | 保留（纯 Java）。`data/network` 中 ByteBuf 相关改为 `FriendlyByteBuf` | S1.01 |
| `lib/transform/**`（向量、旋转、区域） | 保留类名；内部 `Vec3d` → `Vec3`，`EnumFacing` → `Direction` | S1.01 |
| `config/**` | 改写为 §6 配置系统；字段名、默认值、注释原样保留 | S1.02 |
| `lib/saving/**` | 改为基于 `ValueInput`/`ValueOutput` 的 `SaveHandler`（§4） | S1.03 |
| `lib/network/**` | `PacketCodex` 保留 API，传输改为 §5；`netty/` 与 `PacketManager` 删除 | S1.04 |
| `lib/radio/**`、`api/radio/**` | 移植；`RadioRegistry` 按 `ResourceKey<Level>` 分表 | S1.05 |
| `lib/radar/**` | 移植；同上 | S1.06 |
| `lib/capability/**`、`api/caps/**` | 按 §2.6 改写 | S1.07 |
| `api/actions/**`、`lib/actions/**`、`content/actions/**` | 移植 | S1.08 |
| `lib/thread/**`、`content/blast/thread*/**` | 按 overview §6 改为“工作线程计算 + 主线程按预算应用” | S1.09 |
| `api/**`（其余） | 签名换为 26.3 类型 | S1.10 |
| `lib/energy/**`、`api/energy/**` | S1.07 建 `IcbmEnergyBuffer`（内部）；S6 完成门面 | S1.07、S6 |
| `content/reg/**` | `ModBlocks`/`ModItems`/`ModEntities`/`ModBlockEntities`/`ModMenus`/`ModSounds`/`ModDataComponents`/`ModCreativeTabs`；`BlockReg`→`ModBlocks` 等改名 | S2 |
| `lib/explosive/**`、`api/reg/**` | `ExplosiveRegistry` + 平铺化内容生成 | S2.02 |
| `content/blocks/{BlockConcrete,BlockReinforcedGlass,BlockSpikes,BlockGlassButton,BlockGlassPressurePlate}`、`content/radioactive/**` | 平铺化方块 | S2.03、S3.08 |
| `content/items/ItemCrafting`、`ItemBase` 物品 | 平铺化材料物品 | S2.04 |
| `content/blast/**` | 移植 | S3.02–S3.07 |
| `content/gas/**`、`content/potion/**` | 移植 | S3.05 |
| `content/blocks/explosive/**` | 每个爆炸物一个 `ExplosiveBlock` 实例 + 共享 `ExplosiveBlockEntity` | S3.09 |
| `command/**` | Brigadier | S3.10 |
| `lib/projectile/**`、`content/entity/**`（爆炸相关） | 移植 | S3.01、S3.03 |
| `content/missile/**`、`content/cluster/**`、`content/cargo/**`、手雷/矿车/座椅实体 | 移植 | S4 |
| `prefab/**` | 方块实体基类、库存、GUI 组件 | S5.01 |
| `content/blocks/launcher/**`、`radarstation/**`、`emptower/**` | 移植 | S5 |
| `client/**` | `src/client/java/icbm/classic/client/**` | S2.06、S4.07、S5.07 |
| `mods/**` | **DROPPED**（ADR 0006） | — |
| `datafix/**` | **DROPPED**（ADR 0002） | — |
| `RecipeGeneration.java` | 改为 datagen `ICBMRecipeProvider` | S2.08 |
| `CommonProxy.java` | **DROPPED**（Fabric 无 proxy；逻辑并入入口类） | S0.03 |

## 4. 存档：`NbtSaveHandler` → `SaveHandler`

1.12 用法（保留这种声明式写法）：

```java
private static final NbtSaveHandler<TileRadarStation> SAVE_LOGIC = new NbtSaveHandler<TileRadarStation>()
    .mainRoot()
    /* */.nodeInteger("detection_range", TileRadarStation::getDetectionRange, TileRadarStation::setDetectionRange)
    .base();
```

26.3 版本：类名改为 `icbm.classic.lib.saving.SaveHandler<E>`，节点方法名保持不变（`nodeInteger`、`nodeBoolean`、`nodeString`、`nodeEnum`、`nodeBlockPos`、`nodeVec3`、`nodeUUID`、`nodeIdentifier`、`nodeItemStack`、`nodeINBTSerializable`→改名 `nodeSavable`、`nodeBuildableObject` 等），实现改为：

```java
public void save(E obj, ValueOutput out)   // 每个节点调用 out.putInt / out.store(key, CODEC, value) / out.child(key)
public void load(E obj, ValueInput in)     // 每个节点调用 in.getIntOr(key, default) / in.read(key, CODEC) / in.child(key)
```

规则：

- 26.3 `ValueInput` 已核实的方法：`getIntOr`、`getInt`（Optional）、`getLongOr`、`getFloatOr`、`getDoubleOr`、`getBooleanOr`、`getByteOr`、`getStringOr`、`getString`、`getIntArray`、`read(String, Codec)`、`read(MapCodec)`、`child`、`childOrEmpty`、`childrenList`、`list(String, Codec)`。`ValueOutput`：`putInt`、`putLong`、`putFloat`、`putDouble`、`putBoolean`、`putByte`、`putShort`、`putString`、`putIntArray`、`store(String, Codec, T)`、`storeNullable`、`child`、`childrenList`、`list`、`discard`。
- 复杂类型一律用 Codec：`BlockPos.CODEC`、`Vec3.CODEC`、`UUIDUtil.CODEC`、`Identifier.CODEC`、`ItemStack.OPTIONAL_CODEC`、`Direction.CODEC`。枚举存 `name()` 字符串，读回用 `valueOf` 并对未知值回退默认值。
- 1.12 的 `INBTSerializable<NBTTagCompound>`（如 `Radio`、`InventoryWithSlots`）改为实现新接口 `icbm.classic.lib.saving.ISavable { void save(ValueOutput); void load(ValueInput); }`。
- 保留原 NBT 键名（例如 `"safetyRadius"`），即使不兼容旧存档也不改，便于对照旧代码。
- 每个实现 `SaveHandler` 的类在 GameTest 中至少有一个“保存→加载→字段相等”往返测试（S1.03 提供测试工具 `SaveRoundTrip.assertRoundTrip(helper, blockEntity)`）。

## 5. 网络：`PacketCodex` 传输层

保留 `PacketCodex`/`PacketCodexTile`/`PacketCodexEntity` 的构建器 API（`nodeInt`、`nodeBoolean`、`fromServer()`、`fromClient()` 等），上层机器代码几乎不改。传输层改为两个 Fabric 载荷：

```java
// src/main/java/icbm/classic/network/CodexPayload.java
public record CodexPayload(Identifier codexId, byte[] body) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CodexPayload> S2C = new CustomPacketPayload.Type<>(ICBMConstants.id("codex_s2c"));
    public static final CustomPacketPayload.Type<CodexPayload> C2S = new CustomPacketPayload.Type<>(ICBMConstants.id("codex_c2s"));
    // 两个 Type 共用一个 StreamCodec：Identifier.STREAM_CODEC + ByteBufCodecs.BYTE_ARRAY（长度上限 32767）
    ...
}
```

- 注册（`IcbmNetwork.registerPayloads()`，main 源集）：`PayloadTypeRegistry.clientboundPlay().register(S2C, CODEC)` 与 `PayloadTypeRegistry.serverboundPlay().register(C2S, CODEC)`。**26.3 名称是 `clientboundPlay()`/`serverboundPlay()`，不是旧教程里的 `playS2C()`/`playC2S()`。**
- 服务端接收：`ServerPlayNetworking.registerGlobalReceiver(C2S_TYPE, (payload, ctx) -> ...)`；客户端接收在 client 入口用 `ClientPlayNetworking.registerGlobalReceiver`。处理函数已在主线程执行（Fabric 默认行为，用 javap/Javadoc 核对；若不是，用 `ctx.server().execute(...)`）。
- Codex 的 ID 由“1.12 的 `REGISTRY_NAME` + `/` + 名称”构成，例如 `icbmclassic:radarstation/range.detection`。**不再使用顺序整数 ID**。
- `body` 的前缀是目标定位：方块实体 codex 写 `BlockPos`（`writeBlockPos`），实体 codex 写实体网络 id（`writeVarInt`）。
- **服务端校验（必须）**：玩家所在维度与目标一致；`player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64 * 64`；`level.isLoaded(pos)`；目标方块实体类型正确；`fromClient()` 的 codex 才允许从客户端接收。任一失败：记录 debug 日志并丢弃，不抛异常、不踢人。
- 发送工具：`sendToServer(raw)`、`sendToPlayer(ServerPlayer, raw)`、`sendToTracking(BlockEntity)`（`PlayerLookup.tracking(blockEntity)`）、`sendToGuiUsers(raw, Collection<ServerPlayer>)`。
- 1.12 的 `PacketSpawnAirParticle`、`PacketSpawnBlockExplosion` 改为独立载荷（`AirParticlePayload`、`BlockExplosionFxPayload`），用 `StreamCodec.composite` 编写。

## 6. 配置系统

1.12：

```java
@Config(modid = ICBMConstants.DOMAIN, name = "icbmclassic/radar")
public class ConfigRadar {
    @Config.Name("max_range") @Config.Comment("Max range of radar ...")
    public static int MAX_RANGE = 500;
}
```

26.3（`icbm.classic.config.core` 包，S1.02 实现）：

```java
@IcbmConfig(file = "radar")                       // → config/icbmclassic/radar.json
public final class ConfigRadar {
    @ConfigEntry(name = "max_range", comment = "Max range of radar in blocks (meters) to detect entities")
    @ConfigRange(min = 1, max = 10000)
    public static int MAX_RANGE = 500;
}
```

- 支持的字段类型：`int`、`long`、`float`、`double`、`boolean`、`String`、`String[]`、枚举，以及**内部静态类实例字段**（如 `ConfigMissile.SAM_MISSILE`，映射为 JSON 子对象，子对象字段用同样的注解）。
- 文件格式：顶层 JSON 对象，每个条目 `"name": value`，注释写在同级 `"_comment_name": "..."` 中（JSON 无注释语法）。
- 加载：文件不存在 → 写出默认值；字段缺失 → 用默认值并回写；值越界 → 夹到范围内并 `LOGGER.warn`；JSON 解析失败 → 备份为 `<file>.json.broken-<时间戳>`，使用默认值并写出新文件，**不崩溃**。
- 只在启动时加载一次（服务端与客户端各自加载自己的 config 目录）；重载命令属于后续范围。
- 需要客户端与服务端一致的数值（例如 ABM 速度影响客户端预测）一律只在服务端使用，客户端靠实体同步数据，不读配置。
- 配置类清单：照搬 `legacy-1.12/src/main/java/icbm/classic/config/**` 中所有 `@Config` 类；Forge 的 `@Config.LangKey` 删除（不做游戏内配置界面）。

## 7. GUI：Container/GuiContainer → Menu/Screen

| 1.12 | 26.3 |
| --- | --- |
| `Container` | `AbstractContainerMenu`；构造函数 `(int containerId, Inventory inv, ...)`；`quickMoveStack(Player, int)`、`stillValid(Player)` |
| `GuiContainer` | `AbstractContainerScreen<M>`（client 源集） |
| `drawGuiContainerBackgroundLayer` | 覆盖 `extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta)`（26.x 渲染改名；以 javap `AbstractContainerScreen` 结果为准） |
| `drawGuiContainerForegroundLayer` | `extractLabels(GuiGraphicsExtractor, int, int)` |
| `drawTexturedModalRect` | `g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH)` |
| `GuiButton` / `actionPerformed` | `Button.builder(Component, btn -> {...}).bounds(x, y, w, h).build()`，在 `init()` 中 `addRenderableWidget` |
| `GuiTextField` | `EditBox`。**26.3 改用 SDL 输入：自定义文本控件必须在获得/失去焦点时调用 `Minecraft.getInstance().onTextInputFocusChange(this, focused)`，否则无法输入字符**（Fabric 26.3 公告）。使用原版 `EditBox` 则无需处理 |
| `IGuiHandler#getServerGuiElement` + `player.openGui` | 方块实体实现 `ExtendedMenuProvider<BlockPos>`（`getScreenOpeningData(ServerPlayer)` 返回 `getBlockPos()`），`player.openMenu(be)`；`MenuType` 用 `new ExtendedMenuType<>(FooMenu::new, BlockPos.STREAM_CODEC)` 注册到 `BuiltInRegistries.MENU` |
| 同步数值（能量、进度） | `ContainerData`（int 值，`addDataSlots`）；字符串/列表等用 `PacketCodex` 的 `fromServer()` codex（雷达点阵沿用 1.12 做法） |
| `Minecraft.getMinecraft().displayGuiScreen(x)` | `Minecraft.getInstance().gui.setScreen(x)`（26.2 起 setScreen 移到 `Gui`；用 javap 核对） |

## 8. 渲染

- 方块实体渲染器（1.12 `TESR*`）→ `BlockEntityRenderer<T, S extends BlockEntityRenderState>`：`createRenderState()`、`extractRenderState(T, S, float, Vec3, ModelFeatureRenderer.CrumblingOverlay)`、`submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`。注册：`BlockEntityRenderers.register(type, ctx -> new FooRenderer(ctx))`（client 入口）。
- 导弹等 3D 模型：1.12 用 JSON 方块模型 + 物品模型旋转绘制。26.3 用 `submit` 中提交物品模型（`ItemStackRenderState` + `SubmitNodeCollector`）——具体 API 以 S4.07 javap 结果为准，先实现“提交导弹物品模型并按 yaw/pitch 旋转”，与 1.12 外观一致即可。
- **禁止**直接调用 OpenGL（`GL11`、`GlStateManager`）。26.2 起后端可切换为 Vulkan，所有绘制必须通过 Blaze3D / `SubmitNodeCollector` / `RenderPipelines`。
- 粒子（`client/fx`）：注册 `SimpleParticleType` 到 `BuiltInRegistries.PARTICLE_TYPE`，客户端 `ParticleProviderRegistry`（Fabric）注册 provider。

## 9. 配方与标签

- 1.12 配方 JSON（`assets/icbmclassic/recipes/**`、`_factories.json`）全部作废。新配方由 datagen `ICBMRecipeProvider extends FabricRecipeProvider` 生成到 `data/icbmclassic/recipe/`（26.x 目录名为单数 `recipe`）。
- OreDictionary → 物品标签：钢锭 `c:ingots/steel`、铜锭 `c:ingots/copper`（原版已有铜锭，模组铜锭保留且加入该标签）、铁板 `c:plates/iron`、钢板 `c:plates/steel`、硫 `c:dusts/sulfur`、硝石 `c:dusts/saltpeter`。配方输入一律用标签，不写死物品。
- 1.12 自定义配方工厂（`_factories.json`）中的特殊配方（如导弹拆解 `RecipeHeldItemMissileDecraft`）改为自定义 `RecipeSerializer`（26.1 起 serializer 是 `MapCodec` + `StreamCodec`）；如无法确定，BLOCKED。
- 方块挖掘：在 datagen 中给方块加 `minecraft:mineable/pickaxe` 与 `minecraft:needs_iron_tool` 等标签，代替 1.12 `setHarvestLevel`。

## 10. 常见陷阱（执行 Agent 必读）

1. **忘记 `setId`**：任何 `Block`/`Item` 的 Properties 都必须先 `setId(ResourceKey)` 再 `new`，否则启动时报错。
2. **在服务端加载客户端类**：main 源集中出现 `net.minecraft.client` 导入会编译失败（拆分源集），这是好事，不要通过反射绕过。
3. **在工作线程改世界**：任何 `level.setBlock`、`addFreshEntity`、`getBlockEntity` 都只能在主线程。
4. **`clientTrackingRange` 单位是区块**。导弹 1.12 用 500 方块 → 26.3 填 `32`（并检查服务器 view-distance 配置的影响，在证据中记录）。
5. **`ItemStack` 不能在世界加载前创建**（26.1 起）：静态字段里不要 `new ItemStack(...)`；创造标签、配方、默认值改为延迟创建。
6. **教程过时**：Fabric 文档页面标注的版本可能是 26.2；以 javap 结果为准。
7. **不要用 `Component.literal` 写英文**：S8 会逐个清理，现在就用 `Component.translatable`，并把键加入 `en_us.json`。
8. **随机数**：`world.rand` → `level.getRandom()`（`RandomSource`），`nextFloat()` 用法相同。
