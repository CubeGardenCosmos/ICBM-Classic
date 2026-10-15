# 1.12 → 26.3 移植账本

由 `python3 -I scripts/port_ledger.py generate` 生成；覆盖 `legacy-1.12/src/main/java` 下全部 660 个 .java 文件。
状态：`TODO`、`PORTED`、`MERGED → <新文件>`、`DROPPED（原因）`；`DROPPED` 必须有原因。
「步骤」列按 [porting-guide.md §3](../architecture/porting-guide.md#3-包级移植映射) 预填；`—` 表示 §3 未列出，
由执行该文件移植的步骤补写。改动本文件即改动进度，`check` 要求所有行不再是 `TODO`。

| 状态 | 旧文件（相对 legacy-1.12/src/main/java） | 目标 / 说明 | 步骤 |
| --- | --- | --- | --- |
| PORTED | com/builtbroken/jlib/data/network/IByteBufReader.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/network/IByteBufWriter.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/IPos2D.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/IPos3D.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/ITransform.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/Pos2D.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/Pos2DBean.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/data/vector/Pos3D.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| PORTED | com/builtbroken/jlib/lang/StringHelpers.java | jlib 纯 Java，原样移植（data/network 保留 io.netty.buffer.ByteBuf） | S1.01 |
| DROPPED（无 proxy） | icbm/classic/CommonProxy.java | Fabric 无 proxy，逻辑并入入口类 | — |
| PORTED | icbm/classic/ICBMClassic.java | Forge 入口 → Fabric ModInitializer；每次阶段性追加注册调用（S3.02 加 WorkTickingActionHandler 事件接线、S3.05 加 ModEffects.register、S4.01 加 setupMissileRegistries()：TARGET_DATA/FLIGHT_LOGIC 建表+注册事件+lock） | S0.03、S3.02、S3.05、S4.01 |
| PORTED | icbm/classic/ICBMConstants.java | Existing Fabric-side constants retain the legacy fields and add `id(String)` for 26.3 Identifiers | S0.03 |
| DROPPED（1.12 的开发工具，把爆炸物注册表写成 JSON 配方） | icbm/classic/RecipeGeneration.java | 配方改由 datagen ICBMRecipeProvider 生成 | S2.08 |
| PORTED | icbm/classic/api/EnumTier.java | StringRepresentable + ChatFormatting；getLocalizedName 改 Component.translatable（LanguageUtility 未移植，getName→getSerializedName）；S2.10 键改 icbmclassic.tier.* | S1.10、S2.10 |
| PORTED | icbm/classic/api/ICBMClassicAPI.java | 动作字段（S1.08）+ 本步补齐 MISSILE_TARGET_DATA/FLIGHT_LOGIC/PROJECTILE_DATA/EXPLOSIVE/EX_*/EXPLOSIVE_CUSTOMIZATION 注册表与 EX_* Identifier 常量；删全部 @CapabilityInject，改指向 IcbmLookups | S1.08、S1.10 |
| PORTED | icbm/classic/api/ICBMClassicHelpers.java | 删 hasCapability/getCapability：getMissile 走 instanceof+IcbmLookups.MISSILE；isExplosive/getExplosive(Entity) 走 instanceof；getLauncher 走 IcbmLookups；S3.09 关闭 getExplosive(ItemStack) 的 TODO(S2.05)（改走 CapabilityExplosiveStack.fromStack）；S5.08 的 getGPSData 以 GpsData/GlobalPos 兼容视图读取、写入 gps_data 组件 | S1.10、S3.09、S5.08 |
| PORTED | icbm/classic/api/actions/IAction.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/IActionData.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/IActionProvider.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/IPotentialAction.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/cause/IActionCause.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/cause/IActionSource.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/cause/ICausedByAction.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/cause/ICausedByBlock.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/cause/ICausedByEntity.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/conditions/ICondition.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/conditions/IConditionCause.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/conditions/IConditionLayer.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/ActionField.java | 动作字段系统重设计：save/load 直接操作 ValueInput/ValueOutput | S1.08 |
| PORTED | icbm/classic/api/actions/data/ActionFields.java | 动作字段系统重设计：save/load 直接操作 ValueInput/ValueOutput | S1.08 |
| PORTED | icbm/classic/api/actions/data/ActionTypes.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/BlockActionTypes.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/EntityActionTypes.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/IActionFieldProvider.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/IActionFieldReceiver.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/data/WorldActionTypes.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/listener/IActionListener.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/listener/IActionListenerHandler.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/status/ActionStatusTypes.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/actions/status/IActionStatus.java | 动作系统 | S1.08 |
| PORTED | icbm/classic/api/caps/IEMPReceiver.java | World→Level；javadoc 指向 IcbmLookups.EMP_RECEIVER_BLOCK/ENTITY/ITEM | S1.10 |
| PORTED | icbm/classic/api/caps/IExplosive.java | jspecify 注解；实体/方块实体直接实现，物品走平铺 id + 数据组件（§2.6） | S1.10 |
| PORTED | icbm/classic/api/caps/IGPSData.java | Vec3d→Vec3、维度改 Identifier（无 DimensionManager，删 getWorld()）；数据组件 icbmclassic:gps_data（S2.04/S5.08） | S1.10 |
| PORTED | icbm/classic/api/caps/IMissileHolder.java | hasMissile 改走 ICBMClassicHelpers.isMissile(ItemStack)（IcbmLookups.MISSILE_STACK） | S1.10 |
| PORTED | icbm/classic/api/data/BlockActivateFunction.java | World→Level、EntityPlayer→Player、EnumHand→InteractionHand、EnumFacing→Direction、BlockPos | S1.10 |
| PORTED | icbm/classic/api/data/D3Consumer.java | 纯 Java，无改动 | S1.10 |
| PORTED | icbm/classic/api/data/EntityInteractionFunction.java | EntityPlayer→Player、EnumHand→InteractionHand | S1.10 |
| PORTED | icbm/classic/api/data/EntityTickFunction.java | 类型不变（net.minecraft.world.entity.Entity） | S1.10 |
| PORTED | icbm/classic/api/data/IBoundBox.java | POS 泛型边界盒接口，纯 Java | S1.05（S1.10 复用） |
| PORTED | icbm/classic/api/data/IWorldPosition.java | Vec3d→Vec3、World→Level、isRemote→isClientSide()、getVec3d→getVec3；S1.01 建立 | S1.01（S1.03/S1.10 复用） |
| PORTED | icbm/classic/api/data/Int3Consumer.java | 纯 Java，无改动 | S1.10 |
| PORTED | icbm/classic/api/data/Int3Looper.java | 纯 Java，无改动 | S1.10 |
| PORTED | icbm/classic/api/data/WorldEntityIntSupplier.java | 类型不变（Entity） | S1.10 |
| PORTED | icbm/classic/api/data/WorldPosIntSupplier.java | World→Level | S1.10 |
| PORTED | icbm/classic/api/data/WorldTickFunction.java | World→Level | S1.10 |
| PORTED | icbm/classic/api/data/meta/ITypeTaggable.java | 纯 Java，动作/状态元标签基础 | S1.08（S1.10 复用） |
| PORTED | icbm/classic/api/data/meta/MetaTag.java | 纯 Java（NonNullList 换 net.minecraft.core），动作/状态元标签基础 | S1.08（S1.10 复用） |
| DROPPED（ADR 0003） | icbm/classic/api/energy/IEnergyBuffer.java | DROPPED（ADR 0003） | S1.07 |
| DROPPED（ADR 0003） | icbm/classic/api/energy/IEnergyBufferProvider.java | DROPPED（ADR 0003） | S1.07 |
| PORTED | icbm/classic/api/events/BlastBuildEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/BlastEvent.java | 仅作数据载体（不再继承 Forge Event）；World→Level、Entity | S1.10 |
| PORTED | icbm/classic/api/events/EmpEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/ExplosiveDefuseEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/LaserRemoteTriggerEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/LauncherEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/LauncherSetTargetEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/MissileChunkEvent.java | 切断 content 依赖：删 LoadedChunkPair/ForgeChunkManager.Ticket，改带 Identifier 维度 + ChunkPos + timeLeft（S4.03 自行管理区块票据） | S1.10 |
| PORTED | icbm/classic/api/events/MissileEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载）；RayTraceResult→HitResult | S1.10 |
| PORTED | icbm/classic/api/events/MissileRideEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/RadarGunTraceEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/events/RemoteTriggerEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlast.java | World→Level、Vec3d→Vec3、Entity；实现 IWorldPosition+IAction | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastFactory.java | World→Level、jspecify | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastIgnore.java | 类型不变 | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastInit.java | World→Level、NBTTagCompound→CompoundTag、Entity；javadoc 指向 IActionData#create(Level,...) | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastMovable.java | 类型不变 | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastRestore.java | save/load 改 ValueOutput/ValueInput（§4） | S1.10 |
| PORTED | icbm/classic/api/explosion/IBlastTickable.java | 类型不变 | S1.10 |
| PORTED | icbm/classic/api/explosion/redmatter/IBlastVelocity.java | Entity + jspecify | S1.10 |
| PORTED | icbm/classic/api/launcher/ILauncherSolution.java | 类型不变（IMissileLauncher/IMissileTarget 已换签名） | S1.10 |
| PORTED | icbm/classic/api/launcher/IMissileLauncher.java | Vec3d→Vec3、jspecify；配置引用 ConfigMissile 保持；capability 由 IcbmLookups.MISSILE_LAUNCHER 替代 | S1.10 |
| PORTED | icbm/classic/api/missiles/ICapabilityMissileBuilder.java | World→Level（String getMissileId 保留） | S1.10 |
| PORTED | icbm/classic/api/missiles/ICapabilityMissileStack.java | ItemStack 不变；由 IcbmLookups.MISSILE_STACK 查询 | S1.10 |
| PORTED | icbm/classic/api/missiles/IMissile.java | Entity 不变；继承 IWorldPosition（Vec3/Level） | S1.10 |
| PORTED | icbm/classic/api/missiles/IMissileAiming.java | posX/Y/Z→getX/getY/getZ、rotationYaw/Pitch→getYRot/getXRot | S1.10 |
| PORTED | icbm/classic/api/missiles/parts/IMissileFlightLogic.java | World→Level、Entity；嵌套函数接口保留 | S1.10 |
| PORTED | icbm/classic/api/missiles/parts/IMissileFlightLogicStep.java | 无类型改动 | S1.10 |
| PORTED | icbm/classic/api/missiles/parts/IMissileTarget.java | Vec3d→Vec3 | S1.10 |
| PORTED | icbm/classic/api/missiles/parts/IMissileTargetDelayed.java | 无类型改动 | S1.10 |
| PORTED | icbm/classic/api/missiles/projectile/IProjectileData.java | World→Level、EnumHand→InteractionHand、jspecify | S1.10 |
| PORTED | icbm/classic/api/missiles/projectile/IProjectileDataRegistry.java | World→Level、ResourceLocation→Identifier、Entity | S1.10 |
| PORTED | icbm/classic/api/missiles/projectile/IProjectileStack.java | Entity/ItemStack 不变 | S1.10 |
| PORTED | icbm/classic/api/missiles/projectile/IProjectileThrowable.java | jspecify 注解 | S1.10 |
| PORTED | icbm/classic/api/missiles/projectile/ProjectileTypes.java | MetaTag 不变（icbm.classic.api.data.meta） | S1.10 |
| PORTED | icbm/classic/api/radio/IRadio.java | World→Level，IBoundBox<BlockPos> | S1.05 |
| PORTED | icbm/classic/api/radio/IRadioChannelAccess.java | @Nonnull→jspecify NonNull | S1.05 |
| PORTED | icbm/classic/api/radio/IRadioMessage.java | 无类型改动 | S1.05 |
| PORTED | icbm/classic/api/radio/IRadioReceiver.java | 无类型改动 | S1.05 |
| PORTED | icbm/classic/api/radio/IRadioSender.java | 无类型改动 | S1.05 |
| PORTED | icbm/classic/api/radio/messages/IIncomingMissileMessage.java | IMissile 已可移植（Entity）；触发回调沿用 ITriggerActionMessage#onTriggerCallback(IRadioTargetAssignable) | S1.10（S4.04 复用） |
| PORTED | icbm/classic/api/radio/messages/ITargetMessage.java | Vec3d→Vec3，@Nullable→jspecify | S1.05 |
| PORTED | icbm/classic/api/radio/messages/ITextMessage.java | @Nullable→jspecify | S1.05 |
| PORTED | icbm/classic/api/radio/messages/ITriggerActionMessage.java | onTriggerCallback 参数 IActionStatus→IRadioTargetAssignable | S1.05 |
| PORTED | icbm/classic/api/refs/ICBMEntities.java | 常量换 registry-ids.md §4 新 ID（Identifier，ICBMConstants.id） | S1.10 |
| PORTED | icbm/classic/api/refs/ICBMExplosives.java | 字段保留（值 null + TODO(S2.02) 由 ExplosiveRegistry 赋值） | S1.10 |
| PORTED | icbm/classic/api/reg/IExplosiveCustomization.java | Identifier/ItemStack 不变 | S1.10 |
| PORTED | icbm/classic/api/reg/IExplosiveData.java | EnumTier 保留；继承 IActionData | S1.10 |
| PORTED | icbm/classic/api/reg/IExplosiveRegistry.java | ResourceLocation→Identifier | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExBlockRegistry.java | Identifier；引用 api/data/BlockActivateFunction；S3.09 新增 @Nullable getActivationListener(Identifier)（1.12 只写 map 不读，BlockActivateFunction#onActivate 无调用方，该 getter 随 26.3 的 ExplosiveBlock#useWithoutItem 调用而新增） | S1.10、S3.09 |
| PORTED | icbm/classic/api/reg/content/IExFuseBlockRegistry.java | World→Level、Identifier | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExFuseEntityRegistry.java | Entity、Identifier | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExGrenadeRegistry.java | 无改动 | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExMinecartRegistry.java | 无改动 | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExMissileRegistry.java | EntityPlayer→Player、EnumHand→InteractionHand、Identifier | S1.10 |
| PORTED | icbm/classic/api/reg/content/IExplosiveContentRegistry.java | Identifier、ItemStack | S1.10 |
| PORTED | icbm/classic/api/reg/events/ActionCauseRegistryEvent.java | 改为 Fabric Event（EventFactory.createArrayBacked），回调参数为对应 IBuilderRegistry | S1.08 |
| PORTED | icbm/classic/api/reg/events/ActionPotentialRegistryEvent.java | 改为 Fabric Event（EventFactory.createArrayBacked），回调参数为对应 IBuilderRegistry | S1.08 |
| PORTED | icbm/classic/api/reg/events/ActionRegistryEvent.java | 改为 Fabric Event（EventFactory.createArrayBacked），回调参数为对应 IBuilderRegistry | S1.08 |
| PORTED | icbm/classic/api/reg/events/ActionStatusRegistryEvent.java | 改为 Fabric Event（EventFactory.createArrayBacked），回调参数为对应 IBuilderRegistry | S1.08 |
| PORTED | icbm/classic/api/reg/events/ConditionalRegistryEvent.java | 改为 Fabric Event（EventFactory.createArrayBacked），回调参数为对应 IBuilderRegistry | S1.08 |
| PORTED | icbm/classic/api/reg/events/ExplosiveContentRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载）；删 register(id, registry) 糖方法（监听器直接调 registry.registerContentRegistry） | S1.10 |
| PORTED | icbm/classic/api/reg/events/ExplosiveCustomizationRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/reg/events/ExplosiveRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载）；删 Forge 时代糖方法 register/enableContent/done（监听器直接调 registry） | S1.10 |
| PORTED | icbm/classic/api/reg/events/MissileFlightLogicRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/reg/events/MissileTargetRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/reg/events/ProjectileDataRegistryEvent.java | Fabric Event + Callback（EventFactory.createArrayBacked；可取消事件回调返回 boolean，数据由事件对象承载） | S1.10 |
| PORTED | icbm/classic/api/reg/obj/IBuildableObject.java | ResourceLocation→Identifier、INBTSerializable→ISavable、ITextComponent→Component；删 @SideOnly | S1.03（S1.10 复用） |
| PORTED | icbm/classic/api/reg/obj/IBuilderRegistry.java | save/load 改走 ValueOutput/ValueInput（键 id + data 子节点）；删 1.12 的 save(NBTTagList) 默认方法 | S1.03（S1.10 复用） |
| PORTED | icbm/classic/api/tile/IRotatable.java | EnumFacing→Direction | S1.10 |
| PORTED | icbm/classic/api/tile/IRotation.java | EnumFacing→Direction | S1.10 |
| MERGED → icbm/classic/client/particle/ModParticlesClient.java | icbm/classic/client/ClientProxy.java | spawnSmoke/spawnAirParticle/spawnExplosionParticles 落地为 ParticleIcbmSmoke + 原版 POOF/SMOKE；spawnMissileSmoke/spawnPadSmoke（1.12 客户端实体更新驱动）改为 ClientTickEvents.END_CLIENT_TICK 遍历导弹实体，按是否在移动选择引擎/发射台烟雾（ConfigClient 开关保留）；隧道/渲染注册部分见 IcbmEntityRenderers | S4.07 |
| MERGED → icbm/classic/client/render/IcbmEntityRenderers.java | icbm/classic/client/ClientReg.java | RenderingRegistry.registerEntityRenderingHandler → Fabric EntityRendererRegistry#register（该 API 在 Fabric API 27 标记 deprecated，但仍是 mod 初始化期注册渲染器的可用入口），由 ICBMClassicClient#onInitializeClient 调用；S4.07 注册全部 16 个 icbmclassic 实体类型（RenderExBlock/RenderFragments/RenderEntityBlock/RenderLightBeam/RenderRedmatter/RenderMissile/RenderAsItem/RenderParachute/RenderBombDroplet，NoopRenderer 用于 EntityExplosion/EntitySmoke/EntityPlayerSeat，原版 TntMinecartRenderer 用于 bomb_cart），并注册太空鸡模型层与鸡渲染器（IcbmChickenRenderer）替换原版；ClientLifecycleEvents.CLIENT_STARTED 自检遍历 icbmclassic 实体类型报告缺失渲染器 | S4.07 |
| MERGED → icbm/classic/content/reg/ModCreativeTabs.java | icbm/classic/client/ICBMCreativeTab.java | 1.12 客户端排序表改为 main 源集 displayItems；内容按 ID 查表，未注册的跳过；S5.08 注册后工具与 battery ID 自动出现在工具顺序 | S2.05、S5.08 |
| MERGED → icbm/classic/content/reg/ModSounds.java | icbm/classic/client/ICBMSounds.java | 16 个 SoundEvent 注册 + assets/icbmclassic/sounds.json（字幕键 subtitles.icbmclassic.*）；1.12 SoundCategory 改由播放点传 SoundSource | S2.09 |
| DROPPED（平铺化的 normal/poison/fire spikes 各有独立注册物品模型；客户端 item mesh 与 metadata 选择已由 datagen 生成的 item/block models 取代，ported code 无引用） | icbm/classic/client/SpikeMeshDefinition.java | 1.12 metadata-based client mesh selector is obsolete under flattened spike IDs and datagen models | S2.03、S2.06 |
| DROPPED（1.12 无调用者） | icbm/classic/client/fx/FXAntimatterParticle.java | grep 全 1.12 源码仅本文件自身出现，无任何 spawn 调用；26.3 不移植（delete dead weight） | S4.07 |
| MERGED → icbm/classic/client/particle/ParticleIcbmSmoke.java | icbm/classic/client/fx/ParticleAirICBM.java | ParticleAirICBM 与 ParticleSmokeICBM 的唯一差异是 1.12 原版烟的自升力，26.3 原版 SmokeParticle（BaseAshSmokeParticle#tick 只做 sprite 动画）没有自升力，故两者合并为一个类；setAge/setColor（随机色变体）照抄 | S4.07 |
| DROPPED（只服务于合并掉的 ParticleLauncherSmoke） | icbm/classic/client/fx/ParticleCollisionLogic.java | 仅被 ParticleLauncherSmoke#move 使用，该豁免只针对 S5 才引入的发射台方块，26.3 由原版 Particle#move 碰撞处理 | S4.07 |
| MERGED → icbm/classic/client/particle/ParticleIcbmSmoke.java | icbm/classic/client/fx/ParticleLauncherSmoke.java | 1.12 相比 ParticleAirICBM 只多了“忽略发射台方块”的碰撞豁免与自带 setAge/setColor；发射台方块在 S5 才注册，S4.07 用同一粒子类承接（S5 需要时再补豁免） | S4.07 |
| MERGED → icbm/classic/client/particle/ParticleIcbmSmoke.java | icbm/classic/client/fx/ParticleSmokeICBM.java | 1.12 无调用者（ClientProxy#spawnSmoke 无人调用），行为并入同一粒子类，供数据包/指令/后续内容使用 | S4.07 |
| DROPPED（平铺化后由 datagen 模型取代） | icbm/classic/client/mapper/BlockModelMapperExplosive.java | 1.12 运行时拼装爆炸物方块模型；26.3 每个爆炸物方块有独立 json（ICBMModelProvider 生成），无需映射器 | S4.07 |
| DROPPED（平铺化后由 datagen 模型取代） | icbm/classic/client/mapper/ItemModelMapperExplosive.java | 同上，物品模型由 ICBMModelProvider/datagen 生成 | S4.07 |
| PORTED → icbm/classic/client/model/CruiseLauncherTopModel.java | icbm/classic/client/models/CruiseLauncherTopModel.java | Blockbench yaw/pitch hierarchy converted to a LayerDefinition/ModelPart model; texture and cube geometry retained | S5.07 |
| PORTED | icbm/classic/client/models/ModelChickenHelmet.java | ModelBase + ModelRenderer → Model<ChickenRenderState> + LayerDefinition（CubeListBuilder.texOffs(40,12).addBox(-3,-6.5,-4.5,6,7,6)，PartPose.offset(0,15,-4)，64x32）；rotateAngleX/Y 改在 setupAnim 里由 ChickenRenderState.xRot/yRot 设置；isChild 的 translate(0,5*scale,2*scale) 改为 bone 的 5/2 模型单位偏移 | S4.07 |
| PORTED → icbm/classic/client/model/ModelICBM.java | icbm/classic/client/models/ModelICBM.java | Legacy ModelBase model base converted to the client Model base; used only by CruiseLauncherTopModel | S5.07 |
| MERGED → icbm/classic/client/render/entity/item/RenderItemImp.java | icbm/classic/client/render/entity/RenderEntityItem2.java | 1.12 已标 @Deprecated，只被 RenderBombDroplet 用来画一个假 EntityItem；26.3 直接提交物品模型 | S4.07 |
| PORTED → icbm/classic/client/render/entity/RenderExBlock.java + state/ExBlockRenderState.java | icbm/classic/client/render/entity/RenderExBlock.java | 方块外观：1.12 blockExplosive + ROTATION_PROP=UP + EX_PROP=爆炸物数据 → 26.3 按 id 查 ModBlocks.EXPLOSIVE_BLOCKS 取平铺方块 + FACING=UP（硬编码，1.12 的 TODO get direction from rotation 原样保留）；renderBlockBrightness → BlockModelResolver#update + BlockModelRenderState#submit；引信膨胀与白闪照 TntRenderer 范式（膨胀保留 1.12 的 f^3，不用原版 f^4；白闪用 overlay 而非第二遍几何）；rot/mutation 无平铺方块形态时不渲染并告警 | S3.09、S4.07（拆分：S3 实体） |
| MERGED → 原版 NoopRenderer | icbm/classic/client/render/entity/RenderExplosion.java | 1.12 的 doRender 是空实现（getEntityTexture 返回 models/grey.png 但从不使用）→ 26.3 直接注册原版 NoopRenderer，语义等价；注意 1.12 未注册渲染器只是隐形，26.3 会让渲染线程 NPE，所以这个映射是必需的 | S4.07（拆分：S3 实体） |
| PORTED → icbm/classic/client/render/entity/RenderFragments.java + state/FragmentsRenderState.java | icbm/classic/client/render/entity/RenderFragments.java | 两条分支照抄：铁砧分支用 MovingBlockRenderState + submitMovingBlock 提交 Blocks.ANVIL/CHIPPED_ANVIL（1.12 的 BlockAnvil.DAMAGE 数值属性在 26.3 是方块变体，随机水平 FACING 与 1.12 的 TODO 保留）；普通碎片分支把 1.12 的 2 个尾羽面 + 4 次 90 度旋转面用 submitCustomGeometry + entityCutout(fragment.png) 逐面提交（顶点/UV 逐字照抄，全亮光照替代 1.12 的 disableLighting）；arrowShake 在 1.12 就不同步，客户端恒 0，行为保持一致 | S4.07（拆分：S3 实体） |
| PORTED → icbm/classic/client/render/entity/RenderLightBeam.java + state/LightBeamRenderState.java | icbm/classic/client/render/entity/RenderLightBeam.java | 轴对齐光柱（内层 beamSize、外层 beamGlowSize）照抄 1.12 的四角/四边/UV 布局，用 submitCustomGeometry + RenderTypes.beaconBeam(...,false/true) 提交；高度 255 - (int)y 与 y-5 偏移这两个 1.12 hack 保留；动画 UV frac(t*0.2 - floor(t*0.1)) 与外层静态 UV 照抄；1.12 的 alphaFunc/disableFog/贴图 clamp 由 RenderType 决定（差异见证据）；shouldRender 恒 true 对应 1.12 的无限渲染包围盒 | S4.07（拆分：S3 实体） |
| PORTED | icbm/classic/client/render/entity/RenderMissile.java | RenderManager/RenderItem → EntityRenderer + ItemModelResolver + ItemStackRenderState（客户端源集 client/render/entity/）；toStack→getRenderStack（SynchedEntityData）、heldItemMissile 的 index 1 物品→getHeldItem；偏移/旋转照抄（translate(0,0.2,0)、yaw-180、pitch-90、rotate 后 translate(0,-0.8,0)/(0,0.27,-0.12)、index1 额外 pitch+10）；isFull3D 不存在，index1 固定 scale 0.5；getRenderBoundingBox 的 expand(5) → getBoundingBoxForCulling；调试包围盒/GL 立即模式删除 | S4.07 |
| MERGED → 原版 NoopRenderer | icbm/classic/client/render/entity/RenderSeat.java | 1.12 doRender 只调 super（真正的绘制被注释掉）、贴图 null、shadowSize 0；26.3 用原版 NoopRenderer（与 marker/interaction 同款） | S4.07 |
| MERGED → 原版 NoopRenderer | icbm/classic/client/render/entity/RenderSmoke.java | 1.12 doRender 只调 super、getEntityTexture 返回 null、shadowSize 0，实际视觉来自实体每 tick 生成的原版烟粒子 → 26.3 注册 NoopRenderer；粒子部分属于 S4.07 正式步骤 | S4.07（拆分：S3 实体） |
| PORTED | icbm/classic/client/render/entity/item/RenderAsItem.java | 泛型 ItemStack 访问器 + RenderItemImp；1.12 默认 translate 的 GROUND 模型偏移在 26.3（ItemDisplayContext.NONE）不存在，已删 | S4.07 |
| PORTED | icbm/classic/client/render/entity/item/RenderItemImp.java | 立即模式 RenderItem → ItemStackRenderState 提取/提交；translate/rotate/scale 钩子保持 1.12 形状（每个 index 一次）；billboard 用 CameraRenderState.yRot；空栈回退物 1.12 Blocks.FIRE → 26.3 Items.BLAZE_POWDER（火方块无物品形态） | S4.07 |
| PORTED | icbm/classic/client/render/entity/layer/LayerChickenHelmet.java | LayerRenderer → RenderLayer<ChickenRenderState, ChickenModel>；1.12 在层里直接读实体能力，26.3 层只拿到 render state，故新增 IcbmChickenRenderer + IcbmChickenRenderState（createRenderState/extractRenderState 拷贝 space_chicken 附件）+ 同步该附件；贴图 textures/entity/space_chicken.png；renderColoredCutoutModel 提交 | S4.07 |
| PORTED | icbm/classic/command/CommandUtils.java | 只保留 isICBMEntity/isMissile/getEntities（loadedEntityList → ServerLevel#getAllEntities、getEntitiesWithinAABB → Level#getEntitiesOfClass）；removeFront/parseRadius/getNumber/getWorld 由 Brigadier 参数类型取代（Vec3Argument.vec3(false)、DimensionArgument、IntegerArgumentType）；isConsoleSender 替代 1.12 的 `sender instanceof MinecraftServer`（26.3 无法读取 CommandSourceStack 的 CommandSource）；EntityBombDroplet/EntityGrenade 待 S4/S5 补回，1.12 TODO 原样保留 | S3.10 |
| PORTED | icbm/classic/command/ICBMCommands.java | 命令树改 Brigadier（literal/then/executes）；根节点 requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) 保 1.12 权限等级 2；组节点空参数执行显示该组 help；注册经 Fabric CommandRegistrationCallback（ICBMClassic#onInitialize），输出用 S2.10 迁移的 icbmclassic.command.* 键 | S3.10 |
| PORTED | icbm/classic/command/sub/CommandHelp.java | 保留 1.12 displayHelp + collectHelpForAll/WorldOnly/ServerOnly 三段逻辑（HelpLine 记录 usage 与三组帮助串，按有无实体决定 world-only 行、按 isConsoleSender 决定 server-only 行）；help 输出仍是无翻译键字面量（1.12 TextComponentString） | S3.10 |
| PORTED | icbm/classic/command/sub/CommandLag.java | `[radius]` 默认 1000；ICBM 实体过滤 + WorkTickingActionHandler.removeNear 清理；消息 icbmclassic.command.lag.remove（1.12 把 double range 塞进 %3$d 槽，26.3 只支持 %s，值已改 %s 见 S3.10 证据「差异」） | S3.10 |
| PORTED | icbm/classic/command/sub/CommandRemove.java | 两种形态拆成 Brigadier 分支：`<type> [radius]`（仅世界中发送者，控制台报 1.12 的未知命令消息）与 `<type> <dim> <x> <y> <z> [radius]`；buildSelector、all/missile(s)/explosion(s)/ex 别名、未知类型不删任何实体、tab 建议 all/missile/explosion 照抄；setDead → Entity#discard | S3.10 |
| PORTED | icbm/classic/command/sub/blast/CommandBlastList.java | 按 id 排序列出 EXPLOSIVE_REGISTRY 全部爆炸物；输出仍是 1.12 的字面量 "Explosive Types: "（1.12 的 translate TODO 保留） | S3.10 |
| PORTED | icbm/classic/command/sub/blast/CommandBlastSpread.java | 单一 8 参数形态 `<count> <distance> <id> <dim> <x> <y> <z> <scale>`；count/distance 保 1.12 CommandBase.parseInt(min 1)；告示串 icbmclassic.command.spread.started 参数顺序与 1.12 相同（维度 id/世界类型 → 维度标识/维度类型注册名），网格坐标计算与逐条 doAction 照抄 | S3.10 |
| PORTED | icbm/classic/command/sub/blast/CommandBlastTrigger.java | 短式 `<id> <size>`（仅世界中发送者）与长式 `<id> <dim> <x> <y> <z> <size>`，保留 1.12 的 trigger 节点名；scale<=0 报 icbmclassic.command.blast.error.scale.zero；id 建议来自 EXPLOSIVE_REGISTRY；getExplosive 改 allowNull=true 查询（1.12 经 helper 查询永远返回 condensed，错误分支不可达）；1.12 未使用的 TRANSLATION_TRIGGERED/THREADING 等常量不保留 | S3.10 |
| MERGED → icbm/classic/command/ICBMCommands.java | icbm/classic/command/system/CommandEntryPoint.java | Forge ICommand 入口（权限等级 2、原始 args 转发、tab 补全）→ 根 literal 节点 + requires(Commands.LEVEL_GAMEMASTERS)，1.12 的权限 2 不变 | S3.10 |
| MERGED → icbm/classic/command/ICBMCommands.java | icbm/classic/command/system/CommandGroup.java | 父子关系 → `literal(...).then(...)`；MapWithDefault 的「未知子命令走 help」改为 Brigadier 自身未知命令错误，空参数仍显示该组 help | S3.10 |
| MERGED → icbm/classic/command/ICBMCommands.java | icbm/classic/command/system/ICommandGroup.java | 组接口（getSubCommands/registerCommand/getUsage）由树结构与 CommandHelp.HelpLine 列表取代 | S3.10 |
| MERGED → icbm/classic/command/sub/CommandHelp.java + 各命令节点的 executes | icbm/classic/command/system/ISubCommand.java | handleCommand → executes(...)；displayHelp/getUsage/collectHelp* → HelpLine + CommandHelp.displayHelp；getTabSuggestions → SuggestionProvider | S3.10 |
| MERGED → icbm/classic/command/sub/CommandHelp.java（HelpLine） | icbm/classic/command/system/SubCommand.java | 抽象基类（name/parent/usage/默认 help 三组）→ 各命令类的 NAME/HELP_LINE 常量与 executes 分支 | S3.10 |
| PORTED | icbm/classic/config/ConfigBattery.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/ConfigClient.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/ConfigDebug.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/ConfigFlyingBlocks.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/ConfigItems.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| DROPPED（ADR 0006：JEI 联动不在本轮范围） | icbm/classic/config/ConfigJEI.java | 删除，无 JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/ConfigMain.java | 改写为 @IcbmConfig JSON 配置；删除 Forge onConfigChangedEvent | S1.02 |
| PORTED | icbm/classic/config/ConfigThread.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/ConfigBlast.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/ConfigFuseTimes.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/ConfigThermobaric.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigAntimatter.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigAnvil.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigBreaching.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigChemical.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigCondensed.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigContagious.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigDebilitation.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigEmp.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigExBasics.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigFragment.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigNuclear.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigRedmatter.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blast/types/ConfigShrapnel.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blocks/ConfigBlocks.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blocks/ConfigRadioactive.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/blocks/ConfigSpikes.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/machines/ConfigEmpTower.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/machines/ConfigLauncher.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/machines/ConfigRadar.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigBomblet.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigCargoItem.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigClusterMissile.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigHeldItemMissile.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigMissile.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/missile/ConfigSAMMissile.java | 改写为 @IcbmConfig JSON 配置 | S1.02 |
| PORTED | icbm/classic/config/util/BlockReplacementData.java | BlockState+CompoundTag，经 TagValueOutput/Input 应用方块实体数据 | S1.02 |
| PORTED | icbm/classic/config/util/BlockStateConfigList.java | IBlockState→BlockState、IProperty→Property；元数据条目退回默认状态 | S1.02 |
| PORTED | icbm/classic/config/util/BlockStateConfigListOld.java | ForgeRegistries→RegistryLookup；元数据条目退回默认状态 | S1.02 |
| PORTED | icbm/classic/config/util/ItemStackConfigList.java | 键改为 namespace:path 字符串；元数据条目退回物品键 | S1.02 |
| PORTED | icbm/classic/config/util/ResourceConfigEntry.java | ResourceLocation→namespace:path 字符串键 | S1.02 |
| PORTED | icbm/classic/config/util/ResourceConfigList.java | ResourceLocation→namespace:path 字符串键；Loader→FabricLoader | S1.02 |
| PORTED | icbm/classic/content/actions/ActionProvider.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/ActionUpdateBlockState.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/causeby/CauseByTimer.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/conditionals/ConditionAnd.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/conditionals/ConditionOR.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/conditionals/ConditionTargetDistance.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/conditionals/ConditionalImpact.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/content/actions/emp/ActionDataEmpArea.java | 爆炸 id 的 IExplosiveData 由工厂持有，create 时用注册表查 ICBMClassicAPI.EXPLOSIVE_REGISTRY（1.12 走 ExplosiveRegistry.get）；SaveHandler→ValueInput/Output | S3.07 |
| PORTED | icbm/classic/content/actions/emp/ActionEmpArea.java | 立方体三趟扫描（方块/方块实体/实体）照抄；Forge 事件 → EmpEvent.BlockPre/BlockPost（Fabric Event）；CapabilityEMP → IcbmLookups.EMP_RECEIVER_BLOCK/ENTITY/ITEM；能量抽空留 S6.05 TODO | S3.07 |
| PORTED | icbm/classic/content/actions/emp/EmpHandler.java | BlockChanceOut 表照 1.12；配置源改 icbmclassic/blast.json#emp.replacements.list（Forge 配置路径不复存在）；loadFromConfig/setup 照抄 | S3.07 |
| PORTED | icbm/classic/content/actions/entity/ActionSpawnEntity.java | 生成实体动作；实体存档走 TagValueOutput/TagValueInput（1.12 writeToNBT+merge+readFromNBT）；注册到 ACTION_REGISTRY 的 icbmclassic:entity.spawn（ActionSystem.setup） | S3.05 |
| PORTED | icbm/classic/content/blast/Blast.java | 26.3 的 Explosion 是接口 → 改为 implements（不再 extends 构造器）；删 Forge onExplosionStart 钩子与 doExplosionA/B；线程字段（thread/threadResults/ThreadExplosion）随 S3.06 重建一并删除；保留 1.12 缺陷 getBlastSource 自递归；S3.04 补回 1.12 从 Explosion 继承的 affectedBlockPositions/getAffectedBlockPositions | S3.02、S3.04 |
| PORTED | icbm/classic/content/blast/BlastAntiGravitational.java | 去掉旧 ThreadSmallExplosion 字段，只用 doRun + getThreadResults()；飞行方块生成/实体抬升/RUNNING_TICKS 照抄 | S3.04 |
| PORTED | icbm/classic/content/blast/BlastBeam.java | 两次 worker 遍历跑在 S1.09 框架（每次 capture 独立快照、结果回主线程）；光束/飞行方块/换块/计时顺序与 1.12 的 8 个 has* 标志一致；createExplosion → level.explode | S3.04 |
| MERGED → icbm/classic/content/blast/threaded/BlastThreaded.java（新增：1.12 的 Blast 线程字段 + ThreadExplosion 外壳在 26.3 的归处） | icbm/classic/content/blast/threaded/BlastThreaded.java | 无 1.12 对应文件：把 Blast 里的 thread/threadResults/createAndStartThread/markThreadCompleted/isThreadCompleted 收敛到线程基类，跑在 S1.09 的 LevelSnapshot/BlockEditQueue 上；S3.06 加 protected snapshot 供子类在工作线程读取 | S3.04、S3.06 |
| PORTED | icbm/classic/content/blast/BlastBreach.java | 继承 BlastTNT；朝向枚举改 Direction、getFrontOffset* → getStep*、getAdjustedHorizontalFacing → getDirection；HOST_DIRECTION 字段保留 | S3.04 |
| PORTED | icbm/classic/content/blast/BlastEndothermic.java | 制冷表照抄（水→冰、熔岩→黑曜石、流动熔岩→雪层、岩浆块→石头等）与药水效果；setWorldTime 改 clockManager | S3.04 |
| PORTED | icbm/classic/content/blast/BlastExothermic.java | 换热表照抄（水/冰/植物/岩/沙/土 → 熔岩/岩浆块/下界岩/火）；改为经 mutateBlocks(worker 结果) 生效；setWorldTime 改 clockManager | S3.04 |
| PORTED | icbm/classic/content/blast/BlastFire.java | 放火用 BaseFireBlock.getState + canSurvive；其余照抄 | S3.04 |
| PORTED | icbm/classic/content/blast/BlastHelpers.java | Int3Consumer/Int3Looper 改自 api.data，循环与停止语义不变 | S3.02 |
| PORTED | icbm/classic/content/blast/BlastMutation.java | 手工生成转换实体 → Mob#convertTo(EntityType, ConversionParams, AfterConversion)（新实体生成、旧实体 discard）；不改声音禁用的行为 | S3.04 |
| PORTED | icbm/classic/content/blast/BlastRadioactiveBlockSwaps.java | 继承 BlastThreaded + IBlastTickable；替换表改用 RadioactiveHandler.radioactiveBlockSwaps（1.12 表重建，S3.08）；原先按 id 解析 radioactive_dirt/stone 的占位代码与其 warn 已删除 | S3.04、S3.08 |
| PORTED | icbm/classic/content/blast/BlastShrapnel.java | setLocationAndAngles → setPos+setYRot/setXRot（含 yRotO/xRotO）；motion 写改 setDeltaMovement；六面环生成逻辑与 1.12 一致 | S3.04 |
| PORTED | icbm/classic/content/blast/BlastSonic.java | 由 Blast 改继承 BlastThreaded，ThreadLargeExplosion 的射线收集并入 doRun(worker+快照)；每 4 tick 消费结果、区块半径增长、实体击退照 1.12；S3.09 补回 1.12 的爆炸物方块连锁触发（block == BlockReg.blockExplosive → instanceof ExplosiveBlock，getTileEntity → getBlockEntity instanceof ExplosiveBlockEntity → trigger(false)） | S3.04、S3.09 |
| PORTED | icbm/classic/content/blast/BlastStatus.java | new ResourceLocation(DOMAIN,…) → ICBMConstants.id(…)，ImmutableStatus 工厂不变 | S3.02 |
| PORTED | icbm/classic/content/blast/BlastTNT.java | 射线/能量/推拉数学照抄（PushType 保留）；onBlockExploded+掉落 → BlockState#onExplosionHit + Block.popResource；爆炸物方块连锁留 TODO(S3.09)；粒子改 ServerLevel#sendParticles | S3.04 |
| DROPPED（26.3 无 Capability 系统；1.12 中该类只是空实现且无调用方） | icbm/classic/content/blast/caps/CapabilityBlast.java | IBlast 查询方法全部返回固定值，唯一作用是 CapabilityManager.INSTANCE.register | S3.02 |
| DROPPED（26.3 无 Capability 系统；1.12 中该类只是空实现且无调用方） | icbm/classic/content/blast/caps/CapabilityBlastVelocity.java | onBlastApplyMotion 恒返回 false，唯一作用是 CapabilityManager.INSTANCE.register | S3.02 |
| PORTED | icbm/classic/content/blast/ender/BlastEnder.java | 传送逻辑照抄（搜寻落点、传送实体与自身方块）；Forge 事件无对应物故直接执行；目标点来自 ExplosiveSettings#enderTarget（S3.07 新增 GlobalPos 字段） | S3.07 |
| PORTED | icbm/classic/content/blast/ender/EnderBlastCustomization.java | 爆炸自定义（末影目标坐标）照抄，标签读写走 ValueInput/Output，配置版本号与 1.12 一致 | S3.07 |
| PORTED | icbm/classic/content/blast/gas/BlastChemical.java | 伤害源改 ModDamageTypes.CHEMICAL；毒性累积/最小伤害照抄；毒作用改施加注册的 TOXIN 效果 | S3.05 |
| PORTED | icbm/classic/content/blast/gas/BlastColor.java | 颜色爆炸照抄（依赖 lib/colors 两个纯数学类） | S3.05 |
| PORTED | icbm/classic/content/blast/gas/BlastContagious.java | 伤害源改 ModDamageTypes.CONTAGIOUS；传染分支照抄；作用改注册的 VIRUS 效果 | S3.05 |
| PORTED | icbm/classic/content/blast/gas/BlastDebilitation.java | 效果集合与持续时间照抄，改用 addEffect | S3.05 |
| PORTED | icbm/classic/content/blast/gas/BlastGasBase.java | 气体云逐 tick 逻辑（扩云/路径/粒子/实体扫描）照抄；粒子改 ServerLevel#sendParticles 的 ColorParticleOption；防护门槛用 ProtectiveArmorHandler | S3.05 |
| PORTED | icbm/classic/content/blast/helpers/BlastBlockHelpers.java | Forge IFluidBlock 删除 → LiquidBlock（原版+模组流体方块都继承它）；BlockLiquid→LiquidBlock、LEVEL 属性同名 | S3.02 |
| PORTED | icbm/classic/content/blast/imp/BlastBase.java | 无 Explosion 父类；保留 1.12 缺陷 Math.min(blastSize,1) 与 locked/buildBlast 语义 | S3.02 |
| PORTED | icbm/classic/content/blast/redmatter/ActionSpawnRedmatter.java | 移植；生成 redmatter 实体动作，注册名与 1.12 一致（随 S3.06 的 ACTION_REGISTRY 接线） | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/DamageSourceRedmatter.java | 1.12 的 DamageSource 子类（bypassesArmor/absolute）→ 数据包 redmatter 伤害类型 + 标签（S3.02 已建）；本类保留为薄包装，供调用点取传 | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/EntityRedmatter.java | IEntityAdditionalSpawnData → SynchedEntityData（尺寸/最大尺寸）；存档键不变；capability 覆写删除；渲染字段 → S4.07 | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/caps/BlastRedmatterWrapper.java | IBlast 包装器照抄（无 capability 系统，仍是普通类） | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/caps/CapRedmatterPull.java | 拉拽逻辑照抄；由 RedmatterLogic 直接读字段，不再经 capability | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/logic/RedmatterBlockCollector.java | 静态方块收集器照抄（纯整数数学 + Direction 顺序不变）；1.12 本就在主线程路径调用，未引入快照 | S3.06 |
| PORTED | icbm/classic/content/blast/redmatter/logic/RedmatterLogic.java | 主逻辑照抄（移动/合并/方块销毁/实体伤害）；1.12 的合并-移动 TODO 原样保留；每 tick 预算常量不变 | S3.06 |
| PORTED → icbm/classic/content/blast/redmatter/EntityRedmatter.java（clientVisualSize + lerpClientVisualSize） | icbm/classic/content/blast/redmatter/render/RedmatterClientLogic.java | 1.12 挂在实体上的客户端尺寸插值处理器：visualSize 字段 + lerpSize(deltaTick) 一次性方法照抄进实体（不引用任何客户端类，服务端只是存在不用）；渲染器每帧先拷走上一帧的值再 lerp，等价于 1.12 的“先绘制后 lerp” | S4.07（拆分：S3 实体） |
| PORTED → icbm/classic/client/render/entity/RenderRedmatter.java + state/RedmatterRenderState.java | icbm/classic/content/blast/redmatter/render/RenderRedmatter.java | 三部分全部改成提交式渲染：黑盘（blackhole.png 双面纹理四边形，按 -ticksExisted 绕 Y 旋转）、黑球与脉冲外壳（1.12 的即时模式球体 → 32x32 球面网格三角形，颜色/脉冲周期/半径公式照抄）、光束（1.12 的末影龙死亡射线副本 → RenderTypes.dragonRays + 同样的 seed 432 随机序列、6 次随机旋转、beamCount/timeScale/rotationScale 公式与颜色缓存 randomColorsForBeams 照抄）；渲染器不再自行平移，位置由 26.3 分发器提供 | S4.07（拆分：S3 实体） |
| PORTED | icbm/classic/content/blast/thread/BasicResistanceCallBack.java | 默认抗性回调；1.12 的 Blast 字段与 IFluidBlock 检查删除（26.3 `Block#getExplosionResistance()` 无参），可安全用于工作线程 | S1.09 |
| PORTED | icbm/classic/content/blast/thread/IThreadCallBack.java | World→Level、Block、BlockPos、IPos3D；工作线程调用时 level 为 null（不向工作线程暴露世界） | S1.09 |
| MERGED → S3.06 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadExplosion.java | 1.12 的 thread-per-blast 设计（Thread 子类 + doRun(World, Location)）与 26.3 快照/预算队列架构不兼容；S3.02 已从 Blast 删除线程字段，改写落在 S3.06 | S3.06 |
| MERGED → S3.06 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadLargeExplosion.java | 依赖 ThreadExplosion 与 content.blast.Blast，随线程化爆炸在 S3.06 重写 | S3.06 |
| MERGED → S3.06 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadSmallExplosion.java | 依赖 ThreadExplosion 与 content.blast.Blast，随线程化爆炸在 S3.06 重写 | S3.06 |
| PORTED | icbm/classic/content/blast/threaded/BlastAntimatter.java | 反物质爆炸：收集逻辑搬进 doRun（worker 读快照），damageUnbreakable 连基岩一起清、范围内红物质清理、伤害/粒子/音效照抄 | S3.06 |
| PORTED | icbm/classic/content/blast/threaded/BlastNuclear.java | 核爆：射线步进/能量衰减照抄（读快照），阻力与可破坏判定同 1.12；tick 消费与收尾走 BlastThreaded | S3.06 |
| PORTED | icbm/classic/content/blast/threaded/BlastThermobaric.java | 温压爆炸：同核爆的射线模型，能量/规模配置照抄 | S3.06 |
| MERGED → S3.02 | icbm/classic/content/blast/threaded/BlastThreaded.java | 线程化爆炸基类，依赖 content.blast.Blast 与 S3.02 的爆炸框架重写 | S3.02 |
| MERGED → icbm/classic/content/reg/ModBlocks.java | icbm/classic/content/blocks/BlockConcrete.java | 1.12 三种 meta 改为 concrete/compact_concrete/reinforced_concrete 三个原版 Block 实例 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockGlassButton.java | BlockSetType.STONE + 20 tick，Properties 里设 glass 声音 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockGlassPressurePlate.java | BlockSetType.OAK（= 1.12 Sensitivity.EVERYTHING）+ 玻璃声音 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockReinforcedGlass.java | TransparentBlock + noOcclusion | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockSpikes.java | 三种 meta 改为 SpikeType 字段 + 三个实例；伤害源改用原版 cactus（§7 无 spikes 伤害类型） | S2.03 |
| PORTED → icbm/classic/content/blocks/emptower/BlockEmpTower.java | icbm/classic/content/blocks/emptower/BlockEmpTower.java | EMP 塔方块；EnumTowerTypes → EnumProperty，SPIN 对应 TileEmpTowerFake；空手/持物右击打开菜单 | S5.06 |
| PORTED → icbm/classic/content/blocks/emptower/ItemBlockEmpTower.java | icbm/classic/content/blocks/emptower/ItemBlockEmpTower.java | BlockItem 子类；默认/扩展状态使用旧 tooltip 翻译键与 BONUS_RADIUS | S5.06 |
| PORTED → icbm/classic/content/blocks/emptower/PropertyTowerStates.java | icbm/classic/content/blocks/emptower/PropertyTowerStates.java | EnumTowerTypes implements StringRepresentable 并映射 EnumProperty | S5.06 |
| PORTED → icbm/classic/content/blocks/emptower/RadioEmpTower.java | icbm/classic/content/blocks/emptower/RadioEmpTower.java | 响应来袭导弹与遥控触发；保留成功回执 | S5.06 |
| PORTED → icbm/classic/client/render/tile/emptower/EmpTowerRenderer.java + EmpTowerRenderState.java | icbm/classic/content/blocks/emptower/TESREmpTower.java | Rotating coil/electric block models, interpolated rotation and fake multiblock host lookup | S5.07 |
| PORTED → icbm/classic/content/blocks/emptower/TileEMPTower.java | icbm/classic/content/blocks/emptower/TileEMPTower.java | 塔结构、充能/冷却、EMP 动作、Radio、GUI Codex 与存档 | S5.06 |
| PORTED → icbm/classic/content/blocks/emptower/TileEmpTowerFake.java | icbm/classic/content/blocks/emptower/TileEmpTowerFake.java | 假方块实体持有主塔 BlockPos；未合并独立的 TileMulti（无 1.12 调用者） | S5.06 |
| PORTED → icbm/classic/content/blocks/emptower/gui/ContainerEMPTower.java | icbm/classic/content/blocks/emptower/gui/ContainerEMPTower.java | 电池槽过滤保留 icbmclassic:battery 预留 ID | S5.06 |
| PORTED → icbm/classic/client/content/blocks/emptower/gui/GuiEMPTower.java | icbm/classic/content/blocks/emptower/gui/GuiEMPTower.java | EMP 塔客户端 GUI；源集移动 | S5.06 |
| MERGED → icbm/classic/content/blocks/explosive/ExplosiveBlock.java | icbm/classic/content/blocks/explosive/BlockExplosive.java | ROTATION_PROP（PropertyDirection，6 向，默认 NORTH）→ FACING=BlockStateProperties.FACING，放置取 getClickedFace；setPlacedBy 保留红石通电检查 + 水平四向的火焰/熔岩扫描（FLOWING_LAVA 并入 LAVA）与 1.12 的 TODO 日志；neighborChanged → hasNeighborSignal；onBlockExploded → BlockBehaviour#onExplosionHit（在移除方块前调用，时机同 1.12；wasExploded 在 26.3 无调用方且在移除后）；onBlockActivated → useItemOn（hurtAndBreak/consume 保 1.12 创造豁免）+ useWithoutItem（调用注册的激活监听器，新增）；onEntityCollidedWithBlock(EntityArrow) → onProjectileHit + isOnFire；createNewTileEntity → newBlockEntity；静态 triggerExplosive 与 !isRemote 守卫照抄；rotate/mirror 与 onBlockAdded 按 1.12 未覆写保持不变 | S3.09 |
| DROPPED（平铺化） | icbm/classic/content/blocks/explosive/ItemBlockExplosive.java | 平铺方块物品由 ModItems（RegHelper.blockItem）注册，无元数据/子类型（ADR 0002）；1.12 getSubItems 的循环 → ModCreativeTabs（23 项）；getDetailedInfo/嘲讽文本属工具提示管线（ExplosiveSettings，S4.06） | S3.09 |
| DROPPED（平铺化） | icbm/classic/content/blocks/explosive/PropertyExplosive.java | 元数据驱动的爆炸物属性被「每个爆炸物一个平铺方块」取代（23 个，ADR 0002，registry-ids.md §1/§2），属性类及其 parse/getName 映射不再存在 | S3.09 |
| MERGED → icbm/classic/content/blocks/explosive/ExplosiveBlockEntity.java | icbm/classic/content/blocks/explosive/TileEntityExplosive.java | capabilityExplosive → icbmclassic:explosive_settings 组件（applyImplicitComponents/collectImplicitComponents），并按 1.12 键名 explosive_stack 显式存档（值改为设置复合；1.12 的 toStack 带 TODO 会丢自定义数据，本步按步骤卡要求修好）；trigger(boolean) 1:1 照抄含 breaching 特例（HOST_DIRECTION=getDirection().getOpposite()）与两处 1.12 TODO；setFire(100) → igniteForSeconds(100)、spawnEntity → addFreshEntity、setBlockToAir → removeBlock(pos,false)；IRotatable 走方块状态，setDirection 用 Block.UPDATE_CLIENTS；hasBeenTriggered 保持公开、不复位、不存档（同 1.12）；删 getUpdatePacket/getUpdateTag/onDataPacket（26.3 无客户端可见状态）；无 getTicker（1.12 该 tile 不 tick） | S3.09 |
| PORTED | icbm/classic/content/blocks/launcher/FiringPackage.java | Firing countdown and save/load state; adapted to ValueInput/ValueOutput | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/LauncherBaseCapability.java | Saves launcher UUID through SaveHandler | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/LauncherLangs.java | Launcher language keys | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/LauncherSolution.java | Target and firing-group selection | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/base/BlockLauncherBase.java | Base registration, placement networking, inventory interaction and ticker | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/base/LauncherCapability.java | Missile validation, launch and flight setup using 26.3 APIs | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/base/TileLauncherBase.java | Base inventory, energy, save/load, seat and network lifecycle | S5.02 |
| PORTED → icbm/classic/client/render/tile/launcher/LauncherBaseRenderer.java + LauncherBaseRenderState.java | icbm/classic/content/blocks/launcher/base/TESRLauncherBase.java | Loaded missile item model offset along launch direction with the 1.12 yaw/pitch | S5.07 |
| PORTED | icbm/classic/content/blocks/launcher/base/gui/ContainerLaunchBase.java | Launcher base menu with missile/energy slots and player inventory | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/base/gui/GuiLauncherBase.java | Client launcher base screen with status, direction, and energy display | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/connector/BlockLaunchConnector.java | Connector registration, network interaction and debug text | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/connector/TileLauncherConnector.java | Connector network node and save/load | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/BlockCruiseLauncher.java | Cruise launcher block, GUI interaction, ticker and drops | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/CLauncherCapability.java | Direct flight launch, aiming status and anti-ballistic missile velocity selection | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/RadioCruise.java | Cruise radio target and trigger message behavior (no legacy incoming-missile handler) | S5.04 |
| PORTED → icbm/classic/client/render/tile/cruise/CruiseLauncherRenderer.java + CruiseLauncherRenderState.java | icbm/classic/content/blocks/launcher/cruise/TESRCruiseLauncher.java | Layered yaw/pitch turret model and held missile item model; debug-only GL sphere grid omitted | S5.07 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/TileCruiseLauncher.java | Aiming, inventory/energy, redstone, radio, persistence, menu and codexes | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/gui/ContainerCruiseLauncher.java | Cruise launcher menu with missile/battery slots | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/gui/GuiCruiseLauncher.java | Client target/channel/firing screen | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/cruise/gui/LaunchButton.java | Pre-check enabled cruise launch button | S5.04 |
| PORTED | icbm/classic/content/blocks/launcher/frame/BlockLaunchFrame.java | Frame block state, registration and network interaction | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/frame/EnumFrameState.java | Frame state enum implements StringRepresentable | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/frame/PropertyFrameState.java | EnumProperty<EnumFrameState> | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/frame/TileLauncherFrame.java | Frame network node and save/load | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/ILauncherComponent.java | Launcher component interface | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/LauncherEntry.java | Launcher network entry state | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/LauncherNetwork.java | Network membership, inventory and firing dispatch | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/LauncherNode.java | Network discovery and invalidation | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/NetworkEnergyStorage.java | Internal IcbmEnergyBuffer allocator; no external energy API | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/network/NetworkInventory.java | Flattened inventory view for launcher network | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/screen/BlockLaunchScreen.java | Launcher screen block and menu interaction | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/BlockScreenCause.java | Action cause for launch-screen initiated launches | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/LauncherPair.java | Launcher/screen pairing value | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/RadioScreen.java | Radio target/trigger endpoint for launcher screen | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/TileLauncherScreen.java | Launcher screen block entity, network, state, persistence and codexes | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/gui/ContainerLaunchScreen.java | Launcher screen menu and inventory | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/screen/gui/GuiLauncherScreen.java | Client launcher screen with target, radio, launch and status controls | S5.03 |
| PORTED | icbm/classic/content/blocks/launcher/status/FiringWithDelay.java | Delayed launch status and translatable message | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/status/LaunchedWithMissile.java | Launched status retaining missile reference | S5.02 |
| PORTED | icbm/classic/content/blocks/launcher/status/LauncherStatus.java | Launcher action statuses | S5.02 |
| PORTED | icbm/classic/content/blocks/multiblock/TileMulti.java | BlockEntity + ValueInput/ValueOutput；主方块位置存 NBTConstants.HOST_POS（int[3]→BlockPos），onLoad 的 TODO 无常驻对象；hostPosition 加 getter/setter 供假方块使用 | S5.01 |
| PORTED → icbm/classic/content/blocks/radarstation/BlockRadarStation.java | icbm/classic/content/blocks/radarstation/BlockRadarStation.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/EnumRadarState.java | icbm/classic/content/blocks/radarstation/EnumRadarState.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/PropertyRadarState.java | icbm/classic/content/blocks/radarstation/PropertyRadarState.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/RadarObjectType.java | icbm/classic/content/blocks/radarstation/RadarObjectType.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/RadioRadar.java | icbm/classic/content/blocks/radarstation/RadioRadar.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/TileRadarStation.java | icbm/classic/content/blocks/radarstation/TileRadarStation.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/data/RadarDotType.java | icbm/classic/content/blocks/radarstation/data/RadarDotType.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/data/RadarRenderData.java | icbm/classic/content/blocks/radarstation/data/RadarRenderData.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/data/RadarRenderDot.java | icbm/classic/content/blocks/radarstation/data/RadarRenderDot.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/content/blocks/radarstation/gui/ContainerRadarStation.java | icbm/classic/content/blocks/radarstation/gui/ContainerRadarStation.java | 雷达站 | S5.05 |
| PORTED → icbm/classic/client/content/blocks/radarstation/gui/GuiRadarStation.java | icbm/classic/content/blocks/radarstation/gui/GuiRadarStation.java | 雷达站；GUI 移至客户端源集 | S5.05 |
| PORTED → icbm/classic/client/content/blocks/radarstation/gui/RadarComponent.java | icbm/classic/content/blocks/radarstation/gui/RadarComponent.java | 雷达站；GUI 组件移至客户端源集 | S5.05 |
| PORTED | icbm/classic/content/cargo/CargoHolderHandler.java | 允许物品判定（ConfigCargoItem） | S4.06 |
| PORTED | icbm/classic/content/cargo/CargoProjectileData.java | 继承 BuildableObject，实现 IProjectileData/ISavable；乘客生成支持 PROJECTILE/ENTITY/BLOCK/ITEM 模式；SaveHandler 序列化 stack 与 mode | S4.06 |
| PORTED | icbm/classic/content/cargo/ItemThrowableProjectile.java | 继承 Item，使用 BOW 动画与 3 秒使用时长，释放时调用 throwProjectile 生成降落伞/气球投射物 | S4.06 |
| PORTED | icbm/classic/content/cargo/ProjectileCargoMode.java | 枚举保留 ITEM, BLOCK, ENTITY, PROJECTILE | S4.06 |
| PORTED | icbm/classic/content/cargo/RecipeCargoData.java | 26.3 CustomRecipe 子类，装载货物到降落伞/气球物品组件 HELD_ITEM | S4.06 |
| PORTED | icbm/classic/content/cargo/RecipeCargoDataDecraft.java | 26.3 CustomRecipe 子类，从降落伞/气球拆解取出货物，getRemainingItems 保留空降落伞/气球 | S4.06 |
| PORTED | icbm/classic/content/cargo/balloon/BalloonProjectileData.java | 货物气球投射物数据，newEntity 生成 EntityBalloon | S4.06 |
| PORTED | icbm/classic/content/cargo/balloon/EntityBalloon.java | 气球挂载实体，初期浮力上升（FLOATING_GRAVITY = -0.02），升力耗尽后慢降或 5% 概率破损落地卸载乘客 | S4.06 |
| PORTED | icbm/classic/content/cargo/parachute/EntityParachute.java | 降落伞挂载实体，低重力（0.01）与高空气阻力（0.95）缓慢下落，落地释放乘客并掉落降落伞物品 | S4.06 |
| PORTED | icbm/classic/content/cargo/parachute/ParachuteProjectileData.java | 降落伞投射物数据，newEntity 生成 EntityParachute | S4.06 |
| PORTED | icbm/classic/content/cargo/parachute/RenderParachute.java | 客户端源集 client/render/entity/RenderParachute.java；始终 billboard（180-camera.yRot），scale 取 EntityParachute#getRenderScale（新增同步读写器） | S4.07 |
| DROPPED（1.12 未注册的半成品） | icbm/classic/content/cargo/placer/BlockCargoHook.java | 1.12 未在 BlockReg 注册，无实际引用 | S4.06 |
| DROPPED（1.12 未注册的半成品） | icbm/classic/content/cargo/placer/TileEntityCargoHook.java | 1.12 未在 BlockReg 注册，无实际引用 | S4.06 |
| DROPPED（1.12 未注册的半成品） | icbm/classic/content/cargo/placer/TileEntityCargoHookTicking.java | 1.12 未在 BlockReg 注册，无实际引用 | S4.06 |
| PORTED | icbm/classic/content/cluster/action/ActionCluster.java | 集束子弹药分层抛射算法，支持旋转矩阵、偏角与发射圆盘生成，逐个生成投射物或掉落物 | S4.06 |
| PORTED | icbm/classic/content/cluster/action/ActionDataCluster.java | 实现 IActionData 与 ISavable，序列化 clusterSpawnEntries 物品列表，在 ActionSystem 注册 | S4.06 |
| PORTED | icbm/classic/content/cluster/bomblet/BombletProjectileData.java | 子弹药投射物数据，newEntity 生成 EntityBombDroplet 并写入爆炸物物品栈 | S4.06 |
| PORTED | icbm/classic/content/cluster/bomblet/BombletProjectileStack.java | 投射物栈包装器 | S4.06 |
| PORTED | icbm/classic/content/cluster/bomblet/EntityBombDroplet.java | 继承 EntityProjectile，持 CapabilityExplosiveEntity，落地或受爆炸火焰伤害触发 ICBM 爆炸物 | S4.06 |
| PORTED | icbm/classic/content/cluster/bomblet/ItemBombDroplet.java | 继承 ExplosiveFormItem，最大堆叠 16，显示爆炸物等阶 tooltip | S4.06 |
| PORTED | icbm/classic/content/cluster/bomblet/RenderBombDroplet.java | 客户端源集 client/render/entity/RenderBombDroplet.java；直接提交 toStack() 的物品模型，translate(0,0.15,0)、yaw-180、pitch+90、旋转后 translate(0,0.2,0)；1.12 绑定的 fragment 贴图由物品模型自带贴图取代 | S4.07 |
| PORTED | icbm/classic/content/cluster/missile/CapabilityClusterMissileStack.java | 实现 ICapabilityMissileStack，构造 EntityMissileActionable，读取 ExplosiveSettings 配置子弹药装载并在接近目标 3 格触发 ActionDataCluster | S4.06 |
| PORTED | icbm/classic/content/cluster/missile/ClusterMissileHandler.java | 集束导弹子弹药容量与允许物品判定 | S4.06 |
| PORTED | icbm/classic/content/cluster/missile/ItemClusterMissile.java | 集束导弹物品，显示子弹药配置数量与类型 | S4.06 |
| PORTED | icbm/classic/content/cluster/missile/RecipeCluster.java | 26.3 CustomRecipe 子类，集束导弹与子弹药合成，结果写入 ExplosiveSettings 的 cluster 配置 | S4.06 |
| PORTED | icbm/classic/content/entity/EntityBombCart.java | 继承原版 MinecartTNT（26.3 包 vehicle.minecart）；IEntityAdditionalSpawnData → SynchedEntityData（爆炸物物品栈）；explodeCart → explode(DamageSource,double)、killMinecart → destroy(ServerLevel,DamageSource)、ignite → primeFuse（引信经 Mixin Accessor 覆盖）、getDefaultDisplayTile → getDefaultDisplayBlockState（按爆炸 id 取平铺方块 + FACING=UP）；爆炸走 CapabilityExplosiveEntity → 爆炸物工厂；注册 bomb_cart（4 区块/2 tick，0.98x0.7） | S4.05 |
| PORTED | icbm/classic/content/entity/EntityExplosion.java | IEntityAdditionalSpawnData 删除 → 爆炸 ID/偏移走 SynchedEntityData，blast 只在服务端构造；setDead→discard、readEntityFromNBT→readAdditionalSaveData(ValueInput)；注册为 explosion_holder（7 区块/5 tick） | S3.02 |
| PORTED | icbm/classic/content/entity/EntityExplosive.java | IEntityAdditionalSpawnData → SynchedEntityData（爆炸 id/引信/朝向）；IRotatable 保留、IExplosive 改由实体自身实现（IEMPReceiver 转发 CapabilityEmpKill）；引信到时仍走 EXPLOSIVE_REGISTRY→IExplosiveData#create→doAction（工厂在 S3.04 前仍为占位） | S3.03 |
| PORTED | icbm/classic/content/entity/EntityFragments.java | 三种模式（箭/铁砧/爆炸）保留；伤害源沿用 1.12 的原版 arrow 类型（1.12 TODO 不修）；mode/damage/critical 等改 SynchedEntityData，其余走 SaveHandler；已加 en_us 键 entity.icbmclassic.fragment | S3.03 |
| PORTED | icbm/classic/content/entity/EntityGrenade.java | 继承 Entity；IEntityAdditionalSpawnData → SynchedEntityData（爆炸物物品栈，客户端渲染/解析爆炸 id）；motionX/Y/Z → getDeltaMovement/setDeltaMovement、rotationYaw/Pitch → setYRot/setXRot + yRotO/xRotO、setLocationAndAngles → snapTo、ticksExisted → tickCount、canBeCollidedWith/canBePushed → isPickable/isPushable；IExplosive 由实体自身实现（转发 CapabilityExplosiveEntity）；handleWaterMovement/canTriggerWalking 无 26.3 对应删除；引信走 EX_GRENADE_REGISTRY；注册 grenade（4 区块/5 tick，0.25x0.25） | S4.05 |
| PORTED | icbm/classic/content/entity/EntityLightBeam.java | 光束颜色/尺寸/进度改 SynchedEntityData（updateDataBeforeSync + onSyncedDataUpdated 回填 1.12 公开字段）；1.12 存档方法本身为空，端口按字段表补 SaveHandler 并记录 | S3.03 |
| PORTED | icbm/classic/content/entity/EntityPlayerSeat.java | 玩家座椅实体，用于发射架/导弹骑乘，保留 1.12 骑乘偏移已知问题 | S4.06 |
| PORTED | icbm/classic/content/entity/EntitySmoke.java | 颜色/寿命改 SynchedEntityData；粒子 world.spawnParticle → ServerLevel#sendParticles（ParticleTypes.SMOKE，数量/速度换算与 1.12 等效） | S3.03 |
| PORTED | icbm/classic/content/entity/flyingblock/BlockCaptureData.java | INBTSerializable<NBTTagCompound> → ISavable(ValueInput/ValueOutput)；键名原样（block_state/source_stack/block_entity） | S3.03 |
| PORTED | icbm/classic/content/entity/flyingblock/EntityFlyingBlock.java | 继承移植后的 EntityProjectile；方块状态走 EntityDataSerializers.BLOCK_STATE（S4.07 渲染用）；落地放回方块/掉落逻辑照抄；gravity 钩子改名 getProjectileGravity（26.3 getGravity 为 final） | S3.03 |
| PORTED | icbm/classic/content/entity/flyingblock/FlyingBlock.java | 静态生成/白名单/配置加载照抄；spawn 回调在 addFreshEntity 前推同步数据（等价 1.12 spawn data 时机） | S3.03 |
| PORTED → icbm/classic/client/render/entity/RenderEntityBlock.java（沿用原版 FallingBlockRenderState） | icbm/classic/content/entity/flyingblock/RenderEntityBlock.java | 渲染被掀起的方块：26.3 用 FallingBlockRenderState + MovingBlockRenderState + submitMovingBlock（原版落地管线），变换等价于 1.12 的 translate(y+0.5) + rotate(-90,Y) + translate(-0.5,-0.5,0.5)（推导见证据）；shadowRadius 0.5；1.12 本来就不应用实体旋转，保持一致；1.12 的 failedBlocks + try/catch 回退无等价触发点（26.3 提交是延迟的，缺模型时由原版给占位模型），已记录为差异 | S4.07（拆分：S3 实体） |
| PORTED | icbm/classic/content/gas/ProtectiveArmorHandler.java | 配置项改 ConfigMain.protectiveArmor（item_ratings 字符串列表，ItemStackConfigList.FloatOut 已在 S1.02 移植）；护甲槽用 EquipmentSlotGroup.ARMOR 的人类槽位 | S3.05 |
| PORTED | icbm/classic/content/items/ItemAntidote.java | 食用改 food/consumable 组件；效果清除改 finishUsingItem，清单在 S3.05 补齐（4 个 ICBM 效果 + gas 用到的原版效果） | S2.04、S3.05 |
| PORTED → icbm/classic/content/items/ItemBattery.java | 本步仅注册为普通单件 Item（零/无能量条，避免伪造存量）；机器槽的 icbmclassic:battery 临时过滤器现匹配真实物品；S6.04 接入 SimpleEnergyItem 并将类移至 lib/energy/item | S5.08 |
| PORTED | icbm/classic/content/items/ItemBombCart.java | 继承 ExplosiveFormItem；onItemUse → useOn(UseOnContext)，在铁轨上放 EntityBombCart（BlockRailBase.isRailBlock → BlockTags.RAILS，isAscending → RailShape#isSlope）；stack size 3、initCapabilities（Forge）删除（爆炸 id 由扁平物品自带），元数据/getSubItems 删除；显示名沿用物品的 CUSTOM_NAME 组件 | S2.05、S4.05 |
| MERGED → icbm/classic/content/reg/ModItems.java | icbm/classic/content/items/ItemCrafting.java | 元数据子类型改为 10 个平铺化普通 Item；OreDictionary → c: 标签（S2.07） | S2.04 |
| PORTED → icbm/classic/content/items/ItemDefuser.java | 作为普通 Item（ItemICBMElectrical 在 S5.01 已 DROPPED）；AttackEntityCallback 对 IExplosive 调 onDefuse、TNT 掉落 Items.TNT、炸弹矿车 destroy，移除实体；无伪电量/能量成本，电池接口留 S6 | S5.08 |
| PORTED | icbm/classic/content/items/ItemGrenade.java | 继承 ExplosiveFormItem；蓄力投掷照抄：getItemUseAction → getUseAnimation(BOW)、getMaxItemUseDuration → getUseDuration(ItemStack,LivingEntity)、onItemRightClick → use（startUsingItem + CONSUME）、onPlayerStoppedUsing → releaseUsing（生成 EntityGrenade，按蓄力算投掷能量，音效 ENTITY_TNT_PRIMED → TNT_PRIMED）；stack size 16；initCapabilities（Forge）删除（爆炸 id 由扁平物品自带），元数据/getSubItems 删除；tier 提示沿用翻译键 | S2.05、S4.05 |
| PORTED → icbm/classic/content/items/ItemLaserDetonator.java | 继承 ItemRadio；Entity#pick(200,1,false) 取目标，gps_data/radio_channel 组件、LaserRemoteTriggerEvent 与 TriggerActionTargetMessage；Fabric UseItemCallback 代替 Forge 包；消息改 translatable | S5.08 |
| PORTED | icbm/classic/content/items/ItemMissile.java | 继承 ExplosiveFormItem；appendHoverText 显示等级（appendExplosiveTier）与 collectInformation；ModItems 注册 23 个平铺物品并注册 IcbmLookups.MISSILE_STACK | S2.05、S4.02 |
| PORTED → icbm/classic/content/items/ItemRadarGun.java | use/useOn 取射线或点击方块坐标；GpsData(Optional<GlobalPos>) 写入 gps_data，Shift 清除；RadarGunTraceEvent 保留 | S5.08 |
| PORTED → icbm/classic/content/items/ItemRemoteDetonator.java | 继承 ItemRadio；UseItemCallback 发 RemoteTriggerEvent 与 TriggerActionMessage，FakeRadioSender 使用 ServerPlayer，频道来自 radio_channel 组件 | S5.08 |
| PORTED → icbm/classic/content/items/ItemRocketLauncher.java | ItemRocketLauncher(false/true) 共用于 rocket_launcher/ballistic_launcher；use/releaseUsing 蓄力、库存导弹 IcbmLookups.MISSILE_STACK、创建/发射 missile entity，直接火箭设置 DeadFlightLogic(HANDHELD_FUEL)；Energy 成本因 ItemICBMElectrical 已 DROPPED 而留待 S6 | S5.08 |
| PORTED | icbm/classic/content/items/behavior/BombCartDispenseBehavior.java | 继承 DefaultDispenseItemBehavior；用 BlockSource/BlockTags.RAILS/RailShape 复现 1.12 前方或下方铁路判定、坡道高度、1.125 方块生成偏移与自定义名称；无铁路时使用父类普通物品喷出行为 | S5.09 |
| PORTED | icbm/classic/content/items/behavior/GrenadeDispenseBehavior.java | 继承 DefaultDispenseItemBehavior；从 BlockSource.center() 生成 EntityGrenade，设置物品栈并使用 1.12 朝向、0.1 垂直分量、0.5 速度与 1.0 不精确度参数；默认方块音效/动画由父类执行 | S5.09 |
| DROPPED（Forge 事件在 26.3 无对应） | icbm/classic/content/missile/MissileEventHandler.java | 两条订阅事件均已并入内容类：EntityMountEvent 的下马判定改为 `EntityMissile#removePassenger` 触发 `MissileRideEvent.Stop`；ChunkEvent.Unload 里的 BallisticFlightLogicOld 模拟随该类删除（S4.03 用 SavedData 追踪取代） | S4.01 |
| PORTED | icbm/classic/content/missile/entity/CapabilityEmpMissile.java | 实现 IEMPReceiver（Level 版签名）；setDead→discard、createExplosion→level.explode(…, ExplosionInteraction.NONE)、rand→level.getRandom；S4.02 回填 EntityExplosiveMissile/EntityMissileActionable 分支，SAM 分支改判 IRadioTargetAssignable | S4.01、S4.02 |
| PORTED | icbm/classic/content/missile/entity/CapabilityMissile.java | 去 Forge Capability 外壳（删 register/CapabilityManager），保留类名；implements IMissile, ISavable；triggerFlightLogic/launch 改用 missile.level()/getX/getY/getZ、ModSounds.MISSILE_LAUNCH + Level#playSound、RadarRegistry.add；SAVE_LOGIC 用 SaveHandler（nodeBuildableObject target/flight、nodeSavable source） | S4.01 |
| PORTED | icbm/classic/content/missile/entity/EntityMissile.java | abstract EntityMissile<E> extends EntityProjectile<E> implements IMissile，委托 missileCapability；删 IEntityAdditionalSpawnData/IPacketIDReceiver/PACKET_DESC，改 SynchedEntityData（DATA_RENDER_STACK/DATA_SMOKE_COLOR）；processInitialInteract→interact(Player,InteractionHand,Vec3)；getMountedYOffset→getPassengerAttachmentPoint；setDead→remove(RemovalReason)；hurtServer 实现 hasHealth+TIER 生命值；鸡骑乘彩蛋接 ModSounds.MEEP 与 CapSpaceChicken 附件；存档用 nodeSavable(missile) | S4.01 |
| PORTED | icbm/classic/content/missile/entity/anti/EntitySurfaceToAirMissile.java | EntityMissile<EntitySurfaceToAirMissile>，实现 IRadioTargetAssignable（assignTarget 调 scanLogic.setCurrentTarget）；initHealth(TIER_2_HEALTH)；tick 执行 scanLogic 与近炸判定（距离<=FLIGHT_SPEED 造成 MISSILE_IMPACT 伤害后 discard）；toStack 返回 ANTI_BALLISTIC_MISSILE | S4.04 |
| PORTED | icbm/classic/content/missile/entity/anti/SAMTargetData.java | BuildableObject<SAMTargetData, IBuilderRegistry<IMissileTarget>> 实现 IMissileTarget，REG_NAME 为 anti.missile；seekByAABB（getEntitiesOfClass）与 seekByRadar（RadarRegistry）；队列限制 5 个目标 | S4.04 |
| PORTED | icbm/classic/content/missile/entity/anti/item/CapabilitySAMStack.java | 实现 ICapabilityMissileStack，getMissileId 返回 icbmclassic:missile.sam；newMissile 实例化 EntitySurfaceToAirMissile(ModEntities.MISSILE_SAM, world) | S4.04 |
| PORTED | icbm/classic/content/missile/entity/anti/item/ItemSurfaceToAirMissile.java | 26.3 普通 Item，stacksTo(1)；注册 IcbmLookups.MISSILE_STACK | S4.04 |
| PORTED | icbm/classic/content/missile/entity/explosive/EntityExplosiveMissile.java | EntityMissile<EntityExplosiveMissile>，实现 IExplosive；持有 TargetRangeDet 与 CapabilityExplosiveEntity；根据 Tier 设置生命值；onDestroyedBy 爆炸/火焰时触发爆炸；tick 更新 TargetRangeDet；interact 触发 EX_MISSILE_REGISTRY 回调；actionOnImpact 调用 explosive.doExplosion；toStack 返回爆炸物 stack（空则回退平铺物品）；SaveHandler 存取 explosive | S4.02 |
| PORTED | icbm/classic/content/missile/entity/explosive/EntityMissileActionable.java | EntityMissile<EntityMissileActionable>；PotentialAction + ActionFieldProvider（IMPACTED/HOST_ENTITY/HOST_POSITION/TARGET_POSITION）；onDestroyedBy 爆炸/火焰时触发 mainAction；tick 更新 mainAction；toStack 返回 originalStack；SaveHandler 存取 potential_action 与 original_stack | S4.02 |
| PORTED | icbm/classic/content/missile/entity/itemstack/EntityHeldItemMissile.java | 继承 EntityMissile，命中后掉落装载物品或执行交互；toStack 返回携带 HELD_ITEM 组件的导弹 | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/HeldActionMode.java | PRIMARY, SECONDARY, PRIMARY_FIRST, SECONDARY_FIRST | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/item/CapabilityHeldItemMissile.java | 实现 ICapabilityMissileStack，生成携带物品导弹实体并传递 HELD_ITEM 组件 | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/item/HeldItemMissileHandler.java | 携带物品导弹允许装载物品判定 | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/item/ItemHeldItemMissile.java | 携带物品导弹物品，显示装载物品 tooltip | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/item/RecipeHeldItemMissile.java | 26.3 CustomRecipe 子类，导弹装载货物到 HELD_ITEM 组件 | S4.06 |
| PORTED | icbm/classic/content/missile/entity/itemstack/item/RecipeHeldItemMissileDecraft.java | 26.3 CustomRecipe 子类，从导弹拆解取出货物，getRemainingItems 保留空导弹 | S4.06 |
| PORTED | icbm/classic/content/missile/logic/TargetRangeDet.java | 近炸触发器：world.isRemote→level().isClientSide()；读 BallisticTargetingData.impactHeightOffset 或 cluster 偏移；inRange 接近目标距离时触发 explosive.doExplosion 并 discard() | S4.02 |
| PORTED | icbm/classic/content/missile/logic/flight/ArcFlightLogic.java | motionX/Y/Z→setDeltaMovement、rotation→setYRot/setXRot（同步 yRotO/xRotO）；弧线参数抽成静态纯函数 calculateArcParameters（JUnit 覆盖）；shouldSimulate 用 ServerLevel#hasChunk；S4.03 接通 MissileTrackerHandler.simulateMissile 模拟移交 | S4.01、S4.03 |
| DROPPED（ADR 0002：不迁移 1.12 存档） | icbm/classic/content/missile/logic/flight/BallisticFlightLogicOld.java | 未在 26.3 注册（注册表也不注册其 ballistic 名）；1.12 只在注册、chunk unload 模拟、getMountedYOffset 与 EntityMissileDataFixer 里出现，后两处均为旧存档兼容路径 | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/DeadFlightLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/DirectFlightLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/FollowTargetLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/WarmupFlightLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/move/MoveByFacingLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/move/MoveByVec3Logic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/move/MoveForTicksLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/prefab/AccelerateByFacingLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/prefab/AccelerateByVec3Logic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/flight/prefab/FlightLogic.java | 类型/API 对照表替换（INBTSerializable→ISavable/SaveHandler、EnumFacing→Direction、Vec3d→Vec3、motion→getDeltaMovement/setDeltaMovement、rotation→setYRot/setXRot 并同步 yRotO/xRotO） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/source/ActionSource.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| PORTED | icbm/classic/content/missile/logic/source/cause/ActionCause.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| PORTED | icbm/classic/content/missile/logic/source/cause/CausedByBlock.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| PORTED | icbm/classic/content/missile/logic/source/cause/EntityCause.java | getName→getName().getString()、getUniqueID→getUUID、EntityPlayer→Player；新增 getEntity(Level) 用 Level#getEntity(UUID) 延迟解析（1.12 只有运行缓存） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/source/cause/RedstoneCause.java | EnumFacing→Direction、nodeFacing→nodeDirection；构造改为 (Level, BlockPos, BlockState, Direction) | S4.01 |
| PORTED | icbm/classic/content/missile/logic/targeting/BallisticTargetingData.java | Vec3d→Vec3；INBTSerializable→ISavable（ValueInput/ValueOutput，键 x/y/z/firingDelay/impact_height 不变） | S4.01 |
| PORTED | icbm/classic/content/missile/logic/targeting/BasicTargetData.java | ResourceLocation→Identifier、Vec3d→Vec3、INBTSerializable→ISavable；getRegistry() 返回 ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY | S4.01 |
| DROPPED（MissileChunkEvent 与 TrackedChunk 直接承载 ChunkPos + timeLeft，无外部引用） | icbm/classic/content/missile/tracker/LoadedChunkPair.java | 1.12 临时数据类，26.3 统一由 MissileChunkEvent 与内部 TrackedChunk 承载 | S4.03 |
| PORTED | icbm/classic/content/missile/tracker/MissileTrackerData.java | RecordCodecBuilder 编解码（ticks/target/data/preLoadChunkTimer 键语义），支持 Vec3 map/list 双向解码；实体数据通过 TagValueOutput/saveWithoutId 序列化 | S4.03 |
| PORTED | icbm/classic/content/missile/tracker/MissileTrackerHandler.java | 静态门禁与生命周期调度（simulateMissile 校验/EnteringSimQueue 事件/discard）；ServerLevelEvents 与 ServerTickEvents 驱动 | S4.03 |
| PORTED | icbm/classic/content/missile/tracker/MissileTrackerWorld.java | extends SavedData，持有 missileList/missileSpawnList/trackedChunks；每 20 tick 驱动模拟；到达目标后强加载中心+4邻区块（MissileChunkEvent.Load/Unload 事件与原版 setChunkForced API）；到期后于目标上空重构并生成实体 | S4.03 |
| PORTED | icbm/classic/content/potion/ContagiousPoison.java | 不再继承 Potion：改普通应用器，效果集合与时长/倍率照抄（chemical/contagious 两套），CustomPotionEffect 仍承载无 curative 列表的语义 | S3.05 |
| DROPPED（1.12 里没有具体子类实例化，仅抽象壳；效果注册改由 content/reg/ModEffects 完成） | icbm/classic/content/potion/CustomPotion.java | 自注册进 Potion 注册表的构造逻辑由 ModEffects.register() 取代 | S3.05 |
| PORTED | icbm/classic/content/potion/CustomPotionEffect.java | 26.3 MobEffectInstance 无 curative 列表，保留同名包装类以承载效果实例并记录差异 | S3.05 |
| PORTED | icbm/classic/content/potion/Poison.java | name/id 映射表与 protected/creative 判定照抄；两个被注释掉的护甲/方块防护分支保持注释状态（1.12 未启用） | S3.05 |
| PORTED | icbm/classic/content/radioactive/BlockRadioactive.java | EnumType metadata → 两个实例 radioactive_dirt/radioactive_stone（ADR 0002，构造参数 Kind）；randomTick 与 scheduled tick 共用一个 decay 流程，randomTicks 由 decay_delay 门控；1.12 getBlockHardness/getExplosionResistance 的按类型数值进 Properties（Material.ROCK → requiresCorrectToolForDrops + 镐标签）；伤害源改 ModDamageTypes.RADIOACTIVE_BLOCK；randomDisplayTick → animateTick（playLocalSound）；1.12 的 TODO 与硬编码 20 tick wither 原样保留 | S2.03、S3.08 |
| PORTED | icbm/classic/content/radioactive/RadioactiveHandler.java | BlockChanceOut 表照 1.12：stone→radioactive_stone、dirt/farmland/dirt_path/grass_block/mycelium→radioactive_dirt（chance 0.2）、Material.PLANTS/LEAVES/VINE 的方块→空气（复用 BlastBeam#isVegetation 的 26.3 标签表，须在数据包标签加载后 setup()）；配置源 icbmclassic/blast.json#nuclear.radioactive_replacements.list | S2.03、S3.08 |
| MERGED → icbm/classic/content/reg/ModBlocks.java | icbm/classic/content/reg/BlockReg.java | 注册事件改成 onInitialize 调用；方块与方块物品；S3.09 在既有 RegHelper 循环里保留 1.12 blockExplosive 的属性（map color FIRE、strength(2,0)、SoundType.CLOTH→WOOL），FACING 状态与共享方块实体在 ExplosiveBlock 内，属性本身未变 | S2.01、S3.09 |
| MERGED → icbm/classic/content/reg/ModBlockEntities.java | icbm/classic/content/reg/BlockReg.java（registerTileEntity 部分） | GameRegistry.registerTileEntity(TileEntityExplosive) → Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, icbmclassic:explosive, FabricBlockEntityTypeBuilder.create(ExplosiveBlockEntity::new, <23 个平铺爆炸物方块>).build())，23 个方块共享一个类型（registry-ids.md §2）；S5.02–S5.06 的机器类型仍 TODO | S3.09 |
| MERGED → icbm/classic/content/reg/ModEntities.java | icbm/classic/content/reg/EntityReg.java | 1.12 名称映射（missile→explosive_missile）与旧 ID 一并删除（ADR 0002）；tracking 由方块改区块；S3.07 在实体类型建好后注册 EMP 接收器（IcbmLookups.init 早于本类）；S4.05 注册 grenade/bomb_cart | S2.01、S3.02、S3.03、S3.06、S3.07、S4.05 |
| PORTED | icbm/classic/content/reg/ExplosiveInit.java | 注册 23 个平铺化爆炸物与引信/内容规则；blast 工厂逐个换成真实实现（S3.04–S3.08 全部完成，无 todoBlast 占位）；S3.09 末影激活监听器落地（todoEnderBlockCoordSet → 读 icbmclassic:gps_data 写 ExplosiveSettings#enderTarget 并提示 icbmclassic.message.launcher.toolTargetSet/noTargetInTool）；导弹侧 todoEnderMissileCoordSet 仍为 TODO(S3.07) | S2.02、S3.04、S3.05、S3.06、S3.07、S3.08、S3.09 |
| MERGED → icbm/classic/content/reg/ModItems.java | icbm/classic/content/reg/ItemReg.java | 平铺化物品注册；OreDictionary → c: 标签（S2.04/S2.07）；S4.05 registerFormItems 按形态选真实类（GRENADE→ItemGrenade、BOMB_CART→ItemBombCart，其余仍 ExplosiveFormItem）；S5.08 注册六类工具（rocket_launcher/ballistic_launcher 共用 ItemRocketLauncher）与电池 | S2.01、S4.05、S5.08 |
| DROPPED（ADR 0002） | icbm/classic/datafix/DataFixerHelpers.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/EntityBombCartDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/EntityExplosiveDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/EntityGrenadeDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/EntityMissileDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/ItemStackDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/TileExplosivesDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| DROPPED（ADR 0002） | icbm/classic/datafix/TileRadarStationDataFixer.java | 不迁移旧存档（ADR 0002） | — |
| PORTED | icbm/classic/lib/CalculationHelpers.java | 纯 Java，随机数参数保留 java.util.Random | S1.01（S1.03/S1.10 复用） |
| DROPPED | icbm/classic/lib/CapabilityPrefab.java | DROPPED（无 Capability 系统） | S1.10 |
| DROPPED（26.3 使用 Registries/BuiltInRegistries，不存在 Forge IForgeRegistry；ported tree 无 ForgeRegistryHelpers 调用，注册访问由新 registry APIs 完成） | icbm/classic/lib/ForgeRegistryHelpers.java | Forge registry fuzzy lookup helper has no ported caller or Forge registry counterpart | S5.11 |
| PORTED | icbm/classic/lib/InventoryUtility.java | World→Level、Vec3d→Vec3、IItemHandler 能力→Container、EntityItem→ItemEntity（setPickupDelay→setPickUpDelay）；stacksMatch* 改 ItemStack.isSameItemSameComponents，doTagsMatch 保留原始 CompoundTag | S5.01 |
| DROPPED（ported tree 无 LanguageUtility 调用；翻译由 Component.translatable 和语言资源处理，TextInput 的坐标格式化有独立实现） | icbm/classic/lib/LanguageUtility.java | Forge-era localization/tooltip helper has no ported caller; current translation and vector formatting paths replace its needed behavior | S2.04、S2.10 |
| DROPPED（ported tree 无 MapWithDefault 调用；1.12 命令未知子命令回退帮助行为由 Brigadier 分支处理，且 map 默认值未被移植逻辑依赖） | icbm/classic/lib/MapWithDefault.java | HashMap default-value helper has no ported caller; command routing is now Brigadier | S3.10 |
| PORTED | icbm/classic/lib/NBTConstants.java | 纯字符串常量；log4j 用 26.3 自带的 org.apache.logging.log4j | S1.01（S1.03/S1.10 复用） |
| PORTED | icbm/classic/lib/actions/ActionBase.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionDataBase.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionDataGeneric.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionSystem.java | 动作系统（ISavable + SaveHandler）；S3.05 注册 icbmclassic:entity.spawn、S3.07 注册 icbmclassic:emp.area、S4.01 注册 entity/block/block.redstone 三个导弹 cause | S1.08、S3.05、S3.07、S4.01 |
| PORTED | icbm/classic/lib/actions/PotentialAction.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/PotentialActionImp.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/PotentialActionKnown.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/WorkTickingActionHandler.java | Forge @Mod.EventBusSubscriber + WorldEvent.Unload → ServerLevelEvents.UNLOAD/SERVER_STOPPED 接线；ArrayList→CopyOnWriteArrayList，add/remove 收 IBlast（步骤卡要求），新增 clearAll | S3.02 |
| PORTED | icbm/classic/lib/actions/conditionals/Condition.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/conditionals/timer/TimerCondition.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/conditionals/timer/TimerTickingStatus.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/fields/ActionFieldProvider.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/listners/ActionListenerHandler.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/listners/ActionListenerLayer.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/status/ActionResponses.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/status/ActionStatus.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/status/ImmutableStatus.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/status/MissingFieldStatus.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/buildable/BuildableObject.java | 实现新接口 ISavable；save/load(ValueOutput/ValueInput) 经 SaveHandler<T>；Lombok @Getter 改显式 getter | S1.03 |
| PORTED | icbm/classic/lib/buildable/BuildableObjectRegistry.java | 键 ResourceLocation→Identifier；FMLCommonHandler mod name 改为模组 id；Lombok @Getter 改显式 isLocked() | S1.03 |
| MERGED | icbm/classic/lib/capability/chicken/CapSpaceChicken.java | MERGED → 数据附件 ModAttachments.SPACE_CHICKEN（保留 isSpace/setSpace 静态工具，删除 DataManager/Capability 外壳） | S1.10 |
| MERGED | icbm/classic/lib/capability/emp/CapabilityEMP.java | MERGED → IcbmLookups#init() 注册原版 EMP 接收器（EntityTypes.ITEM/CREEPER，替代 AttachCapabilitiesEvent）；模组自身实体类型（primed_explosive）由 ModEntities 在类型建好后注册（IcbmLookups.init 更早） | S1.10、S3.07 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpChecker.java | 普通类，仅实现 IEMPReceiver（含新增 API Lookup 之外无 capability 代码） | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpCreeper.java | World→Level；EntityLightningBolt→EntityTypes.LIGHTNING_BOLT + Creeper#thunderHit；删 ICapabilityProvider | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpEntityItem.java | EntityItem→ItemEntity；stacksMatchExact→ItemStack.matches；删 ICapabilityProvider | S1.10 |
| MERGED | icbm/classic/lib/capability/emp/CapabilityEmpInventory.java | MERGED → vanilla Container 版（IItemHandler → Container，getCapability() → getContainer()；EntityInv 的 Player→getInventory()）；item 内层库存分支保持 1.12 行为（1.12 会在 EntityInv/TileInv 之间无终止递归，故留 TODO(S3.07) 与注释，不复活该缺陷）、能量抽空待 S6.05 | S1.10、S3.07 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpKill.java | setDead→discard | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosive.java | 删 ICapabilitySerializable，改 ISavable（EXPLOSIVE_ID 走 ValueOutput/ValueInput） | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveEntity.java | ISavable；ItemEntity 掉落、ItemStack.isSameItem、level.isClientSide() | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveStack.java | 构造签名 (ItemStack, Identifier)；自定义项存档走 IBuilderRegistry#save/load；S3.09 新增静态 fromStack(ItemStack)（BlockItem#getBlock instanceof ExplosiveBlock → 爆炸 id，替代 1.12 的 ItemStack#getCapability(EXPLOSIVE_CAPABILITY)）；S4.05 fromStack 再识别扁平形态物品（ExplosiveFormItem#getExplosiveId，手雷/炸弹矿车）；构造器里「从组件读自定义项」的 TODO(S2.05) 保留给 S4.06 设置管线 | S1.10、S3.09、S4.05 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveStatic.java | INBTSerializable→ISavable（空实现，与 1.12 一致） | S1.10 |
| DROPPED（由 `icbmclassic:gps_data` 数据组件替代，S5.08） | icbm/classic/lib/capability/gps/CapabilityGPSData.java | 世界位置数据由 GpsData(Optional<GlobalPos>) 持久化 | S5.08 |
| DROPPED（由 `icbmclassic:gps_data` 数据组件替代，S5.08） | icbm/classic/lib/capability/gps/CapabilityGPSDataItem.java | ItemStack capability 改为 gps_data 数据组件；组件兼容视图位于 ICBMClassicHelpers | S5.08 |
| DROPPED（坐标卡工具提示与定位器转移流程不属于 S5.08 的六件工具，且依赖已删 LanguageUtility/全局维度查询；GPS 值由 gps_data 组件直接读写） | icbm/classic/lib/capability/gps/GPSDataHelpers.java | 旧版玩家定位器交互辅助不再使用 | S5.08 |
| MERGED | icbm/classic/lib/capability/launcher/CapabilityMissileHolder.java | MERGED → vanilla Container 版（IItemHandlerModifiable → Container）；consumeMissile 走 IcbmLookups.MISSILE_STACK | S1.10 |
| PORTED | icbm/classic/lib/capability/launcher/CapabilityMissileLauncher.java | 删 capability 注册；状态暂用 ActionResponses（TODO(S5.02) LauncherStatus） | S1.10 |
| PORTED | icbm/classic/lib/capability/missile/CapabilityMissileStack.java | 实现 ICapabilityMissileStack；getMissileId 生成 icbmclassic:missile[...]；newMissile 返回 EntityExplosiveMissile 的 missileCapability 并 setStack / initHealth；删 1.12 CapabilityManager | S4.02 |
| PORTED | icbm/classic/lib/colors/ColorB.java | 纯数学颜色类，无 MC 类型（S3.05 随 colors 爆炸移植） | S3.05 |
| PORTED | icbm/classic/lib/colors/ColorHelper.java | 纯数学颜色工具（S3.05 随 colors 爆炸移植） | S3.05 |
| PORTED | icbm/classic/lib/data/BoundBlockPos.java | BlockPos/Vec3 边界盒实现 | S1.05（S1.10 复用） |
| PORTED | icbm/classic/lib/data/FaceRotations.java | EnumFacing→Direction；CW_FACE 由序数索引，26.3 枚举序（DOWN,UP,NORTH,SOUTH,WEST,EAST）下把每张轴表放回所属面（1.12 字面量按 UP 开头的假设书写，见实现注释） | S5.01 |
| PORTED | icbm/classic/lib/data/IMachineInfo.java | 原样移植（纯接口） | S5.01 |
| PORTED | icbm/classic/lib/data/LazyBuilder.java | 纯 Java 惰性构建器，动作与内容复用 | S1.08（S2.02/S4 复用） |
| DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | icbm/classic/lib/energy/storage/EnergyActionType.java | DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | S1.07 |
| MERGED → icbm/classic/lib/energy/IcbmEnergyBuffer.java | icbm/classic/lib/energy/storage/EnergyBuffer.java | S1.07 建内部 IcbmEnergyBuffer，S6 完成门面；S6 不改接口 | S1.07 |
| DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | icbm/classic/lib/energy/storage/EnergyChangeCallback.java | DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystem.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystemFE.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystemNull.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/IEnergySystem.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| PORTED | icbm/classic/lib/explosive/ThreadWorkBlast.java | 通用收集型工作，`doRun(int, LevelSnapshot)`/`onStarted(LevelSnapshot)`；被线程化爆炸（S3.02/S3.06）与 ExplosiveRegistry（S2.02）使用 | S1.09、S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExBlockContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存；S3.09 getDeviceStack 返回平铺 <ex>_explosive 方块物品（rot/mutation 无方块形态时返回 EMPTY），并实现 getActivationListener | S2.02、S3.09 |
| PORTED | icbm/classic/lib/explosive/reg/ExGrenadeContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExMinecartContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExMissileContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；getDeviceStack 返回 ModItems.EXPLOSIVE_MISSILES；实现 setLaunchListener/setMissileUpdateListener/setInteractionListener 回调管理并在 triggerLaunch/triggerFlightUpdate/onInteraction 触发 | S2.02、S4.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExplosiveContentRegistry.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExplosiveData.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExplosiveRegistry.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/IPacket.java | CustomPacketPayload + StreamCodec 取代 IPacket，见 src/main/java/icbm/classic/network/CodexPayload.java | S1.04 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/IPacketIDReceiver.java | 自定义载荷的 ID 分发由 Fabric 处理，不需要接收器接口 | S1.04 |
| DROPPED（Fabric 载荷替代） | icbm/classic/lib/network/PacketEvents.java | 旧实现基于 lib/tracker；错误分支改为 IcbmNetwork/IcbmClientNetwork 内 logDebug/logError | S1.04 |
| PORTED | icbm/classic/lib/network/ex/PacketTileReadException.java | 保留类与构造签名（Location/BlockEntity/BlockState） | S1.04 |
| PORTED | icbm/classic/lib/network/lambda/GeneralCodexs.java | radioChannel/radioToggleDisable 两个工厂，Identifier + BlockEntity + setChanged | S1.05 |
| PORTED | icbm/classic/lib/network/lambda/PacketCodex.java | 构建器 API 与语义不变；编解码改 RegistryFriendlyByteBuf，发送走 CodexPayload + IcbmNetwork，新增 codexId/encodeBody/decodeAsSetters/locate | S1.04 |
| PORTED | icbm/classic/lib/network/lambda/PacketCodexEntry.java | encoder/decoder 由 ByteBuf 改 RegistryFriendlyByteBuf | S1.04 |
| PORTED | icbm/classic/lib/network/lambda/PacketCodexReg.java | key 改为 Identifier codexId；重复注册抛 RuntimeException | S1.04 |
| DROPPED（Fabric 载荷替代） | icbm/classic/lib/network/lambda/PacketEventUtils.java | 仅被 PacketEvents/PacketLambda* 使用，随二者一起删除 | S1.04 |
| PORTED | icbm/classic/lib/network/lambda/entity/PacketCodexEntity.java | 定位前缀 varInt(entity id)；sendToAllAround 改 sendToTracking（PlayerLookup.tracking(Entity)） | S1.04 |
| MERGED → src/main/java/icbm/classic/network/CodexPayload.java | icbm/classic/lib/network/lambda/entity/PacketLambdaEntity.java | 包体编码并入 PacketCodex.encodeBody + createPayload；接收校验/应用并入 IcbmNetwork.handleServer 与 PacketCodex.handleFrom* | S1.04 |
| PORTED | icbm/classic/lib/network/lambda/tile/PacketCodexTile.java | 定位前缀 BlockPos；构造收 Class<TARGET>；isValid=非 null 且未移除；sendToAllAround 改 sendToTracking（PlayerLookup.tracking(BlockEntity)） | S1.04 |
| MERGED → src/main/java/icbm/classic/network/CodexPayload.java | icbm/classic/lib/network/lambda/tile/PacketLambdaTile.java | 包体编码并入 PacketCodex.encodeBody + createPayload；接收校验/应用并入 IcbmNetwork.handleServer 与 PacketCodex.handleFrom* | S1.04 |
| DROPPED（Fabric 载荷替代） | icbm/classic/lib/network/lambda/tile/PacketTileEvents.java | 旧实现基于 lib/tracker；接收端错误分支改为 PacketCodex.logDebug/logError | S1.04 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/netty/PacketEncoderDecoderHandler.java | 载荷编解码由 Fabric PayloadTypeRegistry 与自定义 StreamCodec 负责 | S1.04 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/netty/PacketInboundHandler.java | 载荷分发由 ServerPlayNetworking/ClientPlayNetworking 负责 | S1.04 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/netty/PacketManager.java | 改为 IcbmNetwork.registerPayloads + CodexPayload 两个 Type | S1.04 |
| DROPPED（Fabric 网络替代） | icbm/classic/lib/network/packet/PacketBase.java | IPacket 基类随之删除 | S1.04 |
| DROPPED（26.3 原版同步实体位置；原调用方 MekProxy 已 DROPPED） | icbm/classic/lib/network/packet/PacketEntityPos.java | 26.3 用原版实体位置同步 | S1.04 |
| DROPPED（26.3 原版同步手持物；调用方 ItemRadarGun/ItemLaserDetonator 在 S5.08 处理） | icbm/classic/lib/network/packet/PacketPlayerItem.java | 26.3 用原版手持物品同步 | S1.04 |
| MERGED → src/main/java/icbm/classic/network/AirParticlePayload.java | icbm/classic/lib/network/packet/PacketSpawnAirParticle.java | 字段不变（维度改 Identifier）；粒子类型未移植，接收端先 debug 日志，S4.07 补完 | S1.04 |
| MERGED → src/main/java/icbm/classic/network/BlockExplosionFxPayload.java | icbm/classic/lib/network/packet/PacketSpawnBlockExplosion.java | 字段不变（维度改 Identifier）；粒子类型未移植，接收端先 debug 日志，S4.07 补完 | S1.04 |
| PORTED | icbm/classic/lib/projectile/CachedProjectileData.java | 类型替换；apply 的 `instance != null` 条件按 1.12 原样保留（缓存不生效是 1.12 既有行为） | S3.01 |
| DROPPED（26.3 无 Capability 系统，见 porting-guide §2.6；物品直接暴露投射物数据） | icbm/classic/lib/projectile/CapabilityProjectileStack.java | 物品/栈直接提供 IProjectileStack，S4.06 接入货柜与集束内容 | S3.01 |
| PORTED | icbm/classic/lib/projectile/EntityProjectile.java | 直系继承 Entity（EntityICBM 属 S5.01，hasHealth/canBeDestroyed 暂内联）；onUpdate→tick、rayTraceBlocks→ClipContext、实体射线→ProjectileUtil、IEntityAdditionalSpawnData→SynchedEntityData(freezeMotion)；排除 EntityPlayerSeat 碰撞（S4.06 已完成） | S3.01、S4.06 |
| PORTED | icbm/classic/lib/projectile/InGroundData.java | INBTSerializable→ISavable；getMaterial() 删除（26.3 无 Material，改判 state.isAir()） | S3.01 |
| PORTED | icbm/classic/lib/projectile/ProjectileDataRegistry.java | ResourceLocation→Identifier、ForgeRegistries→BuiltInRegistries、ItemMonsterPlacer→SpawnEggItem；1.12 的投射物数据/转换逻辑由 ProjectileDataRegistryEvent 在首次服务器启动时注册（S4.06 完成）；接线在 ICBMClassic#onInitialize | S3.01、S4.06 |
| PORTED | icbm/classic/lib/projectile/ProjectileItemConversion.java | 类型替换，行为不变 | S3.01 |
| PORTED | icbm/classic/lib/projectile/ProjectileStack.java | INBTSerializable→ISavable、nodeBuildableObject 保留；类型参数由 EntityParachute（S4.06）暂放为 Entity | S3.01 |
| PORTED | icbm/classic/lib/projectile/vanilla/ArrowProjectileData.java | EntityTippedArrow→Arrow(EntityTypes.ARROW)、pickupStatus→AbstractArrow.pickup | S3.01 |
| PORTED | icbm/classic/lib/projectile/vanilla/EntitySpawnProjectileData.java | ForgeRegistries→BuiltInRegistries（DefaultedRegistry 先 containsKey）、newInstance→EntityType.create、NBT 读取改 TagValueInput+Entity#load 并还原 UUID | S3.01 |
| PORTED | icbm/classic/lib/projectile/vanilla/ItemProjectileData.java | EntityItem→ItemEntity（位置构造器）、setInfinitePickupDelay→setNeverPickUp | S3.01 |
| PORTED | icbm/classic/lib/projectile/vanilla/SpectralArrowProjectileData.java | 1.12 误用 EntityTippedArrow；26.3 按语义改用 SpectralArrow(EntityTypes.SPECTRAL_ARROW) | S3.01 |
| PORTED | icbm/classic/lib/projectile/vanilla/TippedArrowProjectileData.java | setPotionEffect→Arrow 以拾取物品栈构造（药水来自 DataComponents.POTION_CONTENTS） | S3.01 |
| PORTED | icbm/classic/lib/radar/RadarEntity.java | world→Level，字段直读 getX/getY/getZ，`getEntityId`→`getId`，`ChunkPos` 用 `net.minecraft.world.level` record | S1.06 |
| PORTED | icbm/classic/lib/radar/RadarMap.java | `dimID`→`ResourceKey<Level> dimension`，`remove(Chunk)`→`remove(ChunkPos)`，其余行为照 1.12 | S1.06 |
| PORTED | icbm/classic/lib/radar/RadarRegistry.java | 按 `ResourceKey<Level>` 分表；Forge 订阅者改为 `onChunkUnload`/`onLevelTick`/`clear`/`clearAll` | S1.06 |
| DROPPED（26.3 无 Capability 系统，见 porting-guide §2.6；无线电由类直接实现） | icbm/classic/lib/radio/CapabilityRadio.java | 无线电由类直接实现 IRadio | S1.05 |
| PORTED | icbm/classic/lib/radio/RadioMap.java | 维度键 ResourceKey<Level>，行为照 1.12 | S1.05 |
| PORTED | icbm/classic/lib/radio/RadioRegistry.java | Map<ResourceKey<Level>, RadioMap>，提供 clear/clearAll/onLevelUnload，删除 Forge 事件 | S1.05 |
| PORTED | icbm/classic/lib/radio/imp/Radio.java | INBTSerializable→ISavable，键 channel/disabled 不变 | S1.05 |
| PORTED | icbm/classic/lib/radio/imp/RadioTile.java | T extends BlockEntity，新增 ensureRegistered/unregister | S1.05 |
| PORTED | icbm/classic/lib/radio/messages/IncomingMissileMessage.java | onTriggerCallback 改调 IRadioTargetAssignable#assignTarget(missile.getMissileEntity())，删 LaunchedWithMissile/EntitySurfaceToAirMissile；Vec3d→Vec3、motionX→getDeltaMovement | S1.10（S4.04 复用） |
| PORTED | icbm/classic/lib/radio/messages/RadioTranslations.java | 保留旧键字符串 | S1.05 |
| PORTED | icbm/classic/lib/radio/messages/TargetMessage.java | Vec3d→Vec3 | S1.05 |
| PORTED | icbm/classic/lib/radio/messages/TextMessage.java | 无类型改动 | S1.05 |
| PORTED | icbm/classic/lib/radio/messages/TriggerActionMessage.java | 无类型改动 | S1.05 |
| PORTED | icbm/classic/lib/radio/messages/TriggerActionTargetMessage.java | Vec3d→Vec3 | S1.05 |
| DROPPED（1.12 OreDictionary 条件已改为 c: 标签/26.3 datagen 配方；ported tree 无该 Forge 条件或工厂调用） | icbm/classic/lib/recipe/ConditionOreValue.java | Forge OreDictionary recipe condition has no 26.3 caller; recipe ingredients use convention tags and datagen | S2.07、S2.08 |
| DROPPED（1.12 自定义爆炸物 ingredient factory 仅解析 Forge JSON 工厂且处理 metadata；平铺化物品由 datagen 使用具体注册物品，ported tree 无调用） | icbm/classic/lib/recipe/ExIngredientFactory.java | Forge JSON ingredient factory is replaced by flattened item references in datagen recipes | S2.08 |
| DROPPED（1.12 自定义 shaped recipe factory 仅供 Forge recipe factory JSON 使用；26.3 recipes 由 ICBMRecipeProvider datagen 生成，ported tree 无调用） | icbm/classic/lib/recipe/ExRecipeFactory.java | Forge shaped recipe factory has no runtime caller; datagen emits standard 26.3 recipes | S2.08 |
| MERGED → icbm/classic/lib/saving/ISaveNode.java | icbm/classic/lib/saving/INbtSaveNode.java | 改名 ISaveNode<E>；save(E, ValueOutput)/load(E, ValueInput)，不再有 NBT 数据类型参数 | S1.03 |
| MERGED → icbm/classic/lib/saving/SaveHandler.java | icbm/classic/lib/saving/NbtSaveHandler.java | 改名 SaveHandler；save/load 收 ValueOutput/ValueInput，新增 saveToTag/loadFromTag（TagValueOutput/TagValueInput） | S1.03 |
| MERGED → icbm/classic/lib/saving/SaveNode.java | icbm/classic/lib/saving/NbtSaveNode.java | 改名 SaveNode<E>；泛型 NBT 数据参数改为 ValueOutput/ValueInput 两个抽象方法 | S1.03 |
| MERGED → icbm/classic/lib/saving/SaveRoot.java | icbm/classic/lib/saving/NbtSaveRoot.java | 改名 SaveRoot；out.child/in.child 嵌套，mainRoot 直写当前层；nodeVec3/nodeIdentifier/nodeDirection/nodeSavable；nodeWorldDim 改存 Identifier | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveBuildableObject.java | IBuilderRegistry#save/load 改走 ValueOutput/ValueInput，键沿用 1.12 的 id + data 子节点 | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveBuildableObjectList.java | out.childrenList(name) 写多个元素，读回用 in.childrenList(name) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeBlockPos.java | out.store(name, BlockPos.CODEC, pos) / in.read(name, BlockPos.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeBlockState.java | out.store(name, BlockState.CODEC, state) / in.read(name, BlockState.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeBoolean.java | out.putBoolean(name, v) / in.getBooleanOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeByte.java | out.putByte(name, v) / in.getByteOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeCompoundTag.java | out.store(name, CompoundTag.CODEC, tag) / in.read(name, CompoundTag.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeDouble.java | out.putDouble(name, v) / in.getDoubleOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeEnum.java | 写 name() 字符串；按 accessor 读回，失败保留默认并 ICBMClassic.logger().warn | S1.03 |
| MERGED → icbm/classic/lib/saving/nodes/SaveNodeDirection.java | icbm/classic/lib/saving/nodes/SaveNodeFacing.java | 改名 SaveNodeDirection；out.store(name, Direction.CODEC, dir) / in.read(name, Direction.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeFloat.java | out.putFloat(name, v) / in.getFloatOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeInteger.java | out.putInt(name, v) / in.getIntOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeItemStack.java | out.store(name, ItemStack.OPTIONAL_CODEC, stack) / in.read(name, ItemStack.OPTIONAL_CODEC)；仍跳过空栈 | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodePos.java | Pos#save(ValueOutput)（S1.01 已就绪）写入子 child，读回 new Pos(in.child(key)) | S1.03 |
| MERGED → icbm/classic/lib/saving/nodes/SaveNodeIdentifier.java | icbm/classic/lib/saving/nodes/SaveNodeResourceLocation.java | 改名 SaveNodeIdentifier；out.store(name, Identifier.CODEC, key) / in.read(name, Identifier.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeString.java | out.putString(name, v) / in.getStringOr(name, 默认取 getter 当前值) | S1.03 |
| PORTED | icbm/classic/lib/saving/nodes/SaveNodeUUID.java | out.store(name, UUIDUtil.CODEC, uuid) / in.read(name, UUIDUtil.CODEC) | S1.03 |
| MERGED → icbm/classic/lib/saving/nodes/SaveNodeVec3.java | icbm/classic/lib/saving/nodes/SaveNodeVec3d.java | 改名 SaveNodeVec3；out.store(name, Vec3.CODEC, vec) / in.read(name, Vec3.CODEC) | S1.03 |
| PORTED | icbm/classic/lib/thread/IThreadWork.java | 增加 LevelSnapshot 参数（doRun/onStarted）；onCompleted 在工作线程执行，方块修改交给 BlockEditQueue | S1.09 |
| PORTED | icbm/classic/lib/thread/WorkerThread.java | 守护线程 + ConcurrentLinkedQueue（任务+快照）；禁用 Thread.stop → volatile doRun + interrupt + join(5000) | S1.09 |
| PORTED | icbm/classic/lib/thread/WorkerThreadManager.java | INSTANCE/线程池/killThreads（5 秒总等待 + 丢弃告警）；runningAsDev → FabricLoader#isDevelopmentEnvironment | S1.09 |
| PORTED | icbm/classic/lib/tile/ITick.java | 纯接口，动作与方块实体 tick 复用 | S1.08（S2.02/S4 复用） |
| PORTED | icbm/classic/lib/tile/TickAction.java | 原样移植（仅依赖 ITick 与 java.util.function） | S5.01 |
| PORTED | icbm/classic/lib/tile/TickDoOnce.java | 原样移植（仅依赖 ITick） | S5.01 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTracker.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerData.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerEntry.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerField.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerHelpers.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerNode.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/EventTrackerType.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/IEventTrackerListener.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| DROPPED（消费者 S1.04 已删，26.3 无引用） | icbm/classic/lib/tracker/ListenerEntry.java | 1.12 仅被 lib/network/PacketEvents 与 lambda/tile 使用，26.3 已转 Fabric 载荷，grep src/ 0 引用 | S4.03 |
| MERGED → 由 S1.09 的 lib/thread/BlockEditQueue.java 取代 | icbm/classic/lib/transform/BlockEditHandler.java | 1.12 用 addScheduledTask 排队方块修改；本步不移植，S1.09 以 BlockEditQueue 在主线程按预算应用 | S1.01 |
| PORTED | icbm/classic/lib/transform/PosDistanceSorter.java | Vec3d→Vec3、EnumFacing→Direction（javap 核对） | S1.01 |
| PORTED | icbm/classic/lib/transform/RotationHelper.java | Vec3d→Vec3、MathHelper→Mth（wrapDegrees 返回 double） | S1.01 |
| PORTED | icbm/classic/lib/transform/region/Cube.java | Vec3d→Vec3、AxisAlignedBB→AABB；存档改 save(ValueOutput)/构造收 ValueInput（toNBT() 删除）；getChunks/getEntities/getTilesInArea 改用 Level API | S1.01 |
| PORTED | icbm/classic/lib/transform/region/Rectangle.java | 纯数学，原样移植（Point/Shape2D 已换 26.3 依赖） | S1.01 |
| PORTED | icbm/classic/lib/transform/region/Shape2D.java | Vec3d→Vec3 | S1.01 |
| PORTED | icbm/classic/lib/transform/region/Shape3D.java | NBTTagCompound 构造改为 ValueInput（键名 CENTER/YAW/PITCH/ROLL 保留） | S1.01 |
| PORTED | icbm/classic/lib/transform/region/Triangle.java | 纯数学，原样移植 | S1.01 |
| PORTED | icbm/classic/lib/transform/rotation/EulerAngle.java | EnumFacing→Direction；NBT 构造/写改为 ValueInput/ValueOutput（toNBT() 删除） | S1.01 |
| PORTED | icbm/classic/lib/transform/rotation/IRotation.java | 纯数学，原样移植 | S1.01 |
| PORTED | icbm/classic/lib/transform/rotation/Quaternion.java | 纯数学，原样移植（transform 返回 Pos） | S1.01 |
| PORTED | icbm/classic/lib/transform/vector/AbstractLocation.java | World→Level；维度 int→Identifier；DimensionManager 与需要世界上下文的 (ValueInput/ByteBuf) 构造删除；ILocation(发射器) 接口删除 | S1.01 |
| PORTED | icbm/classic/lib/transform/vector/AbstractPos.java | Vec3d→Vec3、EnumFacing→Direction；IPosition→net.minecraft.core.Position；存档改 save(ValueOutput)；rayTrace 改 ClipContext/HitResult | S1.01 |
| PORTED | icbm/classic/lib/transform/vector/Location.java | World→Level；IB/ValueInput 世界解析构造删除；BlockFaceShape(枚举)→isFaceSturdy；playSound 用 playLocalSound 的 distanceDelay | S1.01 |
| PORTED | icbm/classic/lib/transform/vector/Point.java | NBTTagCompound→ValueInput/ValueOutput（键名 X/Y 保留） | S1.01 |
| PORTED | icbm/classic/lib/transform/vector/Pos.java | Vec3d→Vec3、EnumFacing→Direction；Entity 旋转字段改 getYRot/getXRot 与 yRotO/xRotO | S1.01 |
| PORTED | icbm/classic/lib/world/IProjectileBlockInteraction.java | World→Level、Vec3d→Vec3、EnumFacing→Direction、IBlockState→BlockState；EnumHitReactions 不变 | S3.01 |
| PORTED | icbm/classic/lib/world/ProjectileBlockInteraction.java | Material 表→方块标签表（LEAVES/SNOW/IMPERMEABLE）、Blocks.PORTAL→NETHER_PORTAL、末地传送门委托 BlockState#entityInside、breakBlock 去掉 Forge 的 canEntityDestroy 守卫；register() 在 ICBMClassic#onInitialize 调用 | S3.01 |
| DROPPED（ADR 0006） | icbm/classic/mods/ModInteraction.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/ModProxy.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/CargoItemWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/ClusterItemAllWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/ClusterItemWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/DecraftCargoItemWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/DecraftHeldItemMissileWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/HeldItemMissileWrapper.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/jei/JEIProxy.java | 联动模组代码删除（ADR 0006） | — |
| DROPPED（ADR 0006） | icbm/classic/mods/mekanism/MekProxy.java | 联动模组代码删除（ADR 0006） | — |
| PORTED | icbm/classic/prefab/FakeRadioSender.java | EntityPlayer→ServerPlayer；sendStatusMessage(comp,true)→sendOverlayMessage(Component)，TextComponentTranslation→Component.translatable | S5.01 |
| PORTED | icbm/classic/prefab/entity/EntityICBM.java | Entity + SynchedEntityData(HEALTH/MAX_HEALTH)、attackEntityFrom→hurtServer、readEntityFromNBT→ValueInput/SaveHandler、setDead→discard、isEntityInvulnerable→isInvulnerableToBase；getYOffset 无常驻变量故 alignToBounds 按 box.minY-height。注：EntityProjectile 仍保留自身内联的 hasHealth/IWorldPosition 副本，改继承 EntityICBM 属后续步骤 | S5.01 |
| PORTED | icbm/classic/prefab/gui/ContainerBase.java | AbstractContainerMenu；构造器补 MenuType/containerId，addPlayerInventory 用 addStandardInventorySlots(8,84)、新增通用 quickMoveStack（1.12 无 transferStackInSlot）；canInteractWith→stillValid（容器原样 / 方块实体 4 格） | S5.01 |
| PORTED | icbm/classic/prefab/gui/GuiContainerBase.java | GUI 类移入 client 源集，包名 icbm.classic.client.prefab.gui；drawGuiContainerBackgroundLayer→extractBackground（先 super 画变暗层再画面板）、foreground→extractLabels、drawScreen→extractRenderState、tooltip→setTooltipForNextFrame；绘制全部走 GuiGraphicsExtractor（无 OpenGL） | S5.01 |
| PORTED | icbm/classic/prefab/gui/GuiFormatHelpers.java | Vec3d→Vec3；错误键改为 S2.10 迁移后的 icbmclassic.gui.error.* | S5.01 |
| PORTED | icbm/classic/prefab/gui/IGuiComponent.java | 接口移入 client 源集；draw* 改 GuiGraphicsExtractor，onKeyTyped(char,int)→onKeyTyped(KeyEvent)+onCharTyped(CharacterEvent)，onMouseClick→MouseButtonEvent | S5.01 |
| PORTED | icbm/classic/prefab/gui/IPlayerUsing.java | getPlayersUsing 改 List<ServerPlayer>；addPlayerToUseList 只接受 ServerPlayer（客户端玩家不登记） | S5.01 |
| PORTED | icbm/classic/prefab/gui/NumericField.java | 移至 client 源集；GuiTextField 基类→原版 EditBox，writeText→insertText（数字过滤）、getText/setText→getValue/setValue | S5.01 |
| PORTED | icbm/classic/prefab/gui/TextInput.java | 移至 client 源集；基类 GuiTextFieldBase→EditBox 子类，drawTextBox→extractWidgetRenderState、drawHorizontalLine→GuiGraphicsExtractor#horizontalLine；焦点处理保留但改为与调用前状态比较（1.12 的守卫恒真，见实现注释），焦点变更仍由 EditBox#setFocused 通知 26.3 SDL 文本输入 | S5.01 |
| PORTED | icbm/classic/prefab/gui/button/DisableButton.java | 移至 client 源集；GuiButton→AbstractButton/Button（无 buttonId），drawButton→extractContents + blit，mousePressed 的 Alt 判定→isValidClickButton；Alt/Shift 状态取 Minecraft#hasAltDown/hasShiftDown | S5.01 |
| PORTED | icbm/classic/prefab/gui/button/FaceRotationButton.java | 移至 client 源集；同上映射，displayString→setMessage，EnumFacing→Direction，标签仍取方向名首字母（单字符，S8 审计项） | S5.01 |
| PORTED | icbm/classic/prefab/gui/button/GuiButtonBase.java | 移至 client 源集；GuiButton→Button，actionPerformed 分发→onPress(InputWithModifiers)，extractContents 画原版精灵+标签；按钮 id 删除（26.3 用实例标识） | S5.01 |
| PORTED | icbm/classic/prefab/gui/button/RedstoneButton.java | 移至 client 源集；drawButton→extractContents + blit，enabled/hovered→active/isMouseOver | S5.01 |
| PORTED | icbm/classic/prefab/gui/components/SlotEnergyBar.java | 移至 client 源集；bindTexture+drawTexturedModalRect→blit(container.getBackground())，GuiScreen.isShiftKeyDown→Minecraft#hasShiftDown，TextComponentTranslation→Component.translatable，键改 S2.10 的 icbmclassic.gui.energy* | S5.01 |
| DROPPED（1.12 全仓无调用；能量槽过滤由机器的 InventorySlot 校验器承担，原版 Slot#mayPlace 已委托 Container#canPlaceItem；物品能量在 S6.04 接 Team Reborn） | icbm/classic/prefab/gui/slot/SlotEnergyItem.java | 1.12 isItemValid 走 EnergySystem；该类在 1.12 全仓无调用者 | S5.01 |
| PORTED | icbm/classic/prefab/gui/textbox/GuiTextFieldBase.java | 移至 client 源集；GuiTextField→原版 EditBox（26.3 用 EditBox 即自动处理 SDL 文本输入与焦点通知），只保留 host 引用；drawTextBox/onKeyTyped/onMouseClick 由原版 widget 分发承担 | S5.01 |
| PORTED | icbm/classic/prefab/gui/tooltip/IToolTip.java | 移至 client 源集；ITextComponent→Component | S5.01 |
| PORTED | icbm/classic/prefab/gui/tooltip/Tooltip.java | 移至 client 源集；ITextComponent→Component | S5.01 |
| PORTED | icbm/classic/prefab/gui/tooltip/TooltipBase.java | 移至 client 源集；ITextComponent→Component，hover 延迟/命中判定原样；自绘提示框改由 GuiGraphicsExtractor#setTooltipForNextFrame 承担 | S5.01 |
| PORTED | icbm/classic/prefab/gui/tooltip/TooltipTranslations.java | 移至 client 源集；GuiScreen.isShiftKeyDown→Minecraft#hasShiftDown，ITextComponent→Component | S5.01 |
| PORTED | icbm/classic/prefab/inventory/InventorySlot.java | 原样移植（校验器与 tick/变更回调不变） | S5.01 |
| PORTED | icbm/classic/prefab/inventory/InventoryWithSlots.java | ItemStackHandler→实现原版 Container（getContainerSize/getItem/setItem/removeItem/removeItemNoUpdate/clearContent/canPlaceItem/stillValid）；存档改 ISavable+ContainerHelper.saveAllItems/loadAllItems(ValueOutput/ValueInput)；getSlot(int) 改名 getSlotHandler（26.3 Container#getSlot 同名不同返回类型）；setSize/Size 删除（26.3 容器定长） | S5.01 |
| DROPPED（Forge 工具提示框架） | icbm/classic/prefab/item/ItemBase.java | 只做 setName 与 .info 提示；26.3 物品为普通 Item，提示文本未迁移（见 S2.04 证据） | S2.04 |
| DROPPED（26.3 BlockItem#useOn/#place 已包含 1.12 onItemUse 的放置行为，放置检查钩子是 protected BlockItem#canPlace(BlockPlaceContext, BlockState)（S5.06 的 EMP 塔物品直接继承 BlockItem）；工具提示框架随 ItemBase 在 S2.04 一并 DROPPED） | icbm/classic/prefab/item/ItemBlockAbstract.java | 1.12 的放置钩子由 BlockItem 的 protected canPlace/placeBlock 承担；工具提示框架随 ItemBase 一起 DROPPED | S5.01 |
| DROPPED（ADR 0002 平铺化，26.3 无 metadata/子类型） | icbm/classic/prefab/item/ItemBlockSubTypes.java | 1.12 的 setHasSubtypes/getMetadata/getUnlocalizedName(meta) 无对应，物品与方块平铺 | S5.01 |
| DROPPED（Forge 专用） | icbm/classic/prefab/item/ItemICBMBase.java | 只做 setCreativeTab/setName，由 Item.Properties 与 ModItems 承担 | S2.04 |
| DROPPED（1.12 的 int 能量物品基类：getEnergy 恒返回 MAX_VALUE、discharge 为空操作；26.3 物品能量由 Team Reborn Energy 的 EnergyStorage.ITEM+DataComponent 承担，S6.04 接入） | icbm/classic/prefab/item/ItemICBMElectrical.java | 1.12 也只有 getEnergy=MAX_VALUE / discharge 空实现 | S5.01 |
| PORTED | icbm/classic/prefab/item/ItemRadio.java | ItemBase 已 DROPPED 故直接继承 Item；频道改 icbmclassic:radio_channel 数据组件（NBTConstants.HZ 浮点回退不迁移）；1.12 onItemUseFirst 在 26.3 无钩子，改为 Item#useOn（方块不消耗交互时才触发），用 IRadio/IRadioChannelAccess 取代 RADIO_CAPABILITY | S5.01 |
| DROPPED（Forge 能力容器；26.3 用 DataComponent（gps_data/held_item/radio_channel）+ ItemApiLookup 取代） | icbm/classic/prefab/item/ItemStackCapProvider.java | ItemRadarGun/ItemMissile/ItemBattery 的能力挂载改数据组件 | S5.01 |
| DROPPED（26.3 战利品表由 datagen 生成（S2.07），物品堆用原版 entry + set_components 表达） | icbm/classic/prefab/item/LootEntryItemStack.java | 1.12 只在 ICBMClassic 的代码化战利品池里使用 | S5.01 |
| PORTED | icbm/classic/prefab/tile/BlockICBM.java | BlockContainer→BaseEntityBlock（newBlockEntity 留给子类）；ROTATION_PROP→BlockStateProperties.FACING（与 S3.09 的爆炸物一致），放置朝向取玩家水平朝向；硬度/抗性 10；dropInventory+breakBlock 掉落改由 BlockEntity#preRemoveSideEffects 承担（方块层在移除后已拿不到方块实体） | S5.01 |
| PORTED | icbm/classic/prefab/tile/IGuiTile.java | 1.12 的 getServerGuiElement/getClientGuiElement 改为 ExtendedMenuProvider<BlockPos>（getScreenOpeningData 返回 getBlockPos()）；客户端 Screen 由 MenuScreens 在 client 源集注册，main 不得引用 net.minecraft.client | S5.01 |
| PORTED | icbm/classic/prefab/tile/TileMachine.java | TileEntity→BlockEntity；1.12 update() 拆成 serverTick(ServerLevel)/clientTick() 驱动同一 tickActions 列表；markDirty→setChanged 覆写（服务端额外 sendBlockUpdated 同步描述包）+ getUpdatePacket/getUpdateTag；shouldRefresh 删除；world/pos→getLevel()/getBlockPos() | S5.01 |
