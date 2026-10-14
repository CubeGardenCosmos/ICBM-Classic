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
| TODO | icbm/classic/ICBMClassic.java |  | — |
| TODO | icbm/classic/ICBMConstants.java |  | — |
| DROPPED（1.12 的开发工具，把爆炸物注册表写成 JSON 配方） | icbm/classic/RecipeGeneration.java | 配方改由 datagen ICBMRecipeProvider 生成 | S2.08 |
| PORTED | icbm/classic/api/EnumTier.java | StringRepresentable + ChatFormatting；getLocalizedName 改 Component.translatable（LanguageUtility 未移植，getName→getSerializedName）；S2.10 键改 icbmclassic.tier.* | S1.10、S2.10 |
| PORTED | icbm/classic/api/ICBMClassicAPI.java | 动作字段（S1.08）+ 本步补齐 MISSILE_TARGET_DATA/FLIGHT_LOGIC/PROJECTILE_DATA/EXPLOSIVE/EX_*/EXPLOSIVE_CUSTOMIZATION 注册表与 EX_* Identifier 常量；删全部 @CapabilityInject，改指向 IcbmLookups | S1.08、S1.10 |
| PORTED | icbm/classic/api/ICBMClassicHelpers.java | 删 hasCapability/getCapability：getMissile 走 instanceof+IcbmLookups.MISSILE；isExplosive/getExplosive(Entity) 走 instanceof；getLauncher 走 IcbmLookups；getExplosive(ItemStack)/getGPSData 待数据组件（TODO S2.02/S2.04） | S1.10 |
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
| PORTED | icbm/classic/api/reg/content/IExBlockRegistry.java | Identifier；引用 api/data/BlockActivateFunction | S1.10 |
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
| TODO | icbm/classic/client/ClientProxy.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/ClientReg.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| MERGED → icbm/classic/content/reg/ModCreativeTabs.java | icbm/classic/client/ICBMCreativeTab.java | 1.12 客户端排序表改为 main 源集的 displayItems；内容按 ID 查表，未注册的跳过 | S2.05 |
| MERGED → icbm/classic/content/reg/ModSounds.java | icbm/classic/client/ICBMSounds.java | 16 个 SoundEvent 注册 + assets/icbmclassic/sounds.json（字幕键 subtitles.icbmclassic.*）；1.12 SoundCategory 改由播放点传 SoundSource | S2.09 |
| TODO | icbm/classic/client/SpikeMeshDefinition.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/fx/FXAntimatterParticle.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/fx/ParticleAirICBM.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/fx/ParticleCollisionLogic.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/fx/ParticleLauncherSmoke.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/fx/ParticleSmokeICBM.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/mapper/BlockModelMapperExplosive.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/mapper/ItemModelMapperExplosive.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/models/CruiseLauncherTopModel.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/models/ModelChickenHelmet.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/models/ModelICBM.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderEntityItem2.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderExBlock.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderExplosion.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderFragments.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderLightBeam.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderMissile.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderSeat.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/RenderSmoke.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/item/RenderAsItem.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/item/RenderItemImp.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/client/render/entity/layer/LayerChickenHelmet.java | 客户端类移入 client 源集 | S2.06、S4.07、S5.07 |
| TODO | icbm/classic/command/CommandUtils.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/ICBMCommands.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/CommandHelp.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/CommandLag.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/CommandRemove.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/blast/CommandBlastList.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/blast/CommandBlastSpread.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/sub/blast/CommandBlastTrigger.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/system/CommandEntryPoint.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/system/CommandGroup.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/system/ICommandGroup.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/system/ISubCommand.java | Brigadier 命令 | S3.10 |
| TODO | icbm/classic/command/system/SubCommand.java | Brigadier 命令 | S3.10 |
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
| MERGED → S3.07 | icbm/classic/content/actions/emp/ActionDataEmpArea.java | 依赖爆炸配置与方块替换，随 EMP 爆炸在 S3.07 移植 | S3.07 |
| MERGED → S3.07 | icbm/classic/content/actions/emp/ActionEmpArea.java | 依赖爆炸配置与方块替换，随 EMP 爆炸在 S3.07 移植 | S3.07 |
| MERGED → S3.07 | icbm/classic/content/actions/emp/EmpHandler.java | 依赖爆炸配置与方块替换，随 EMP 爆炸在 S3.07 移植 | S3.07 |
| MERGED → S3.05 | icbm/classic/content/actions/entity/ActionSpawnEntity.java | 生成实体动作，随内容实体在 S3.05 移植 | S3.05 |
| TODO | icbm/classic/content/blast/Blast.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastAntiGravitational.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastBeam.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastBreach.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastEndothermic.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastExothermic.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastFire.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastHelpers.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastMutation.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastRadioactiveBlockSwaps.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastShrapnel.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastSonic.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastStatus.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/BlastTNT.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/caps/CapabilityBlast.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/caps/CapabilityBlastVelocity.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/ender/BlastEnder.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/ender/EnderBlastCustomization.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/gas/BlastChemical.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/gas/BlastColor.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/gas/BlastContagious.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/gas/BlastDebilitation.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/gas/BlastGasBase.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/helpers/BlastBlockHelpers.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/imp/BlastBase.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/ActionSpawnRedmatter.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/DamageSourceRedmatter.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/EntityRedmatter.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/caps/BlastRedmatterWrapper.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/caps/CapRedmatterPull.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/logic/RedmatterBlockCollector.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/logic/RedmatterLogic.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/render/RedmatterClientLogic.java | 各爆炸实现 | S3.02–S3.07 |
| TODO | icbm/classic/content/blast/redmatter/render/RenderRedmatter.java | 各爆炸实现 | S3.02–S3.07 |
| PORTED | icbm/classic/content/blast/thread/BasicResistanceCallBack.java | 默认抗性回调；1.12 的 Blast 字段与 IFluidBlock 检查删除（26.3 `Block#getExplosionResistance()` 无参），可安全用于工作线程 | S1.09 |
| PORTED | icbm/classic/content/blast/thread/IThreadCallBack.java | World→Level、Block、BlockPos、IPos3D；工作线程调用时 level 为 null（不向工作线程暴露世界） | S1.09 |
| MERGED → S3.02 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadExplosion.java | 1.12 的 thread-per-blast 设计（Thread 子类 + doRun(World, Location)）与 26.3 快照/预算队列架构不兼容，随爆炸框架在 S3.02 重写 | S3.02 |
| MERGED → S3.02 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadLargeExplosion.java | 依赖 ThreadExplosion 与 content.blast.Blast，随爆炸框架在 S3.02 重写 | S3.02 |
| MERGED → S3.02 在 LevelSnapshot/BlockEditQueue 上重写（依赖 content.blast.Blast） | icbm/classic/content/blast/thread/ThreadSmallExplosion.java | 依赖 ThreadExplosion 与 content.blast.Blast，随爆炸框架在 S3.02 重写 | S3.02 |
| MERGED → S3.06 | icbm/classic/content/blast/threaded/BlastAntimatter.java | 反物质爆炸（线程化爆炸与红物质），依赖 BlastThreaded 与爆炸内容 | S3.06 |
| MERGED → S3.06 | icbm/classic/content/blast/threaded/BlastNuclear.java | 核爆（线程化爆炸与红物质），依赖 BlastThreaded 与爆炸内容 | S3.06 |
| MERGED → S3.06 | icbm/classic/content/blast/threaded/BlastThermobaric.java | 温压爆炸（线程化爆炸与红物质），依赖 BlastThreaded 与爆炸内容 | S3.06 |
| MERGED → S3.02 | icbm/classic/content/blast/threaded/BlastThreaded.java | 线程化爆炸基类，依赖 content.blast.Blast 与 S3.02 的爆炸框架重写 | S3.02 |
| MERGED → icbm/classic/content/reg/ModBlocks.java | icbm/classic/content/blocks/BlockConcrete.java | 1.12 三种 meta 改为 concrete/compact_concrete/reinforced_concrete 三个原版 Block 实例 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockGlassButton.java | BlockSetType.STONE + 20 tick，Properties 里设 glass 声音 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockGlassPressurePlate.java | BlockSetType.OAK（= 1.12 Sensitivity.EVERYTHING）+ 玻璃声音 | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockReinforcedGlass.java | TransparentBlock + noOcclusion | S2.03 |
| PORTED | icbm/classic/content/blocks/BlockSpikes.java | 三种 meta 改为 SpikeType 字段 + 三个实例；伤害源改用原版 cactus（§7 无 spikes 伤害类型） | S2.03 |
| TODO | icbm/classic/content/blocks/emptower/BlockEmpTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/ItemBlockEmpTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/PropertyTowerStates.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/RadioEmpTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/TESREmpTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/TileEMPTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/TileEmpTowerFake.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/gui/ContainerEMPTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/emptower/gui/GuiEMPTower.java | EMP 塔 | S5 |
| TODO | icbm/classic/content/blocks/explosive/BlockExplosive.java | 每个爆炸物一个 ExplosiveBlock + 共享 BlockEntity | S3.09 |
| TODO | icbm/classic/content/blocks/explosive/ItemBlockExplosive.java | 每个爆炸物一个 ExplosiveBlock + 共享 BlockEntity | S3.09 |
| TODO | icbm/classic/content/blocks/explosive/PropertyExplosive.java | 每个爆炸物一个 ExplosiveBlock + 共享 BlockEntity | S3.09 |
| TODO | icbm/classic/content/blocks/explosive/TileEntityExplosive.java | 每个爆炸物一个 ExplosiveBlock + 共享 BlockEntity | S3.09 |
| TODO | icbm/classic/content/blocks/launcher/FiringPackage.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/LauncherBaseCapability.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/LauncherLangs.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/LauncherSolution.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/BlockLauncherBase.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/LauncherCapability.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/TESRLauncherBase.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/TileLauncherBase.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/gui/ContainerLaunchBase.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/base/gui/GuiLauncherBase.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/connector/BlockLaunchConnector.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/connector/TileLauncherConnector.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/BlockCruiseLauncher.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/CLauncherCapability.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/RadioCruise.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/TESRCruiseLauncher.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/TileCruiseLauncher.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/gui/ContainerCruiseLauncher.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/gui/GuiCruiseLauncher.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/cruise/gui/LaunchButton.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/frame/BlockLaunchFrame.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/frame/EnumFrameState.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/frame/PropertyFrameState.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/frame/TileLauncherFrame.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/ILauncherComponent.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/LauncherEntry.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/LauncherNetwork.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/LauncherNode.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/NetworkEnergyStorage.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/network/NetworkInventory.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/BlockLaunchScreen.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/BlockScreenCause.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/LauncherPair.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/RadioScreen.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/TileLauncherScreen.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/gui/ContainerLaunchScreen.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/screen/gui/GuiLauncherScreen.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/status/FiringWithDelay.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/status/LaunchedWithMissile.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/launcher/status/LauncherStatus.java | 发射台网络与巡航发射器 | S5 |
| TODO | icbm/classic/content/blocks/multiblock/TileMulti.java |  | — |
| TODO | icbm/classic/content/blocks/radarstation/BlockRadarStation.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/EnumRadarState.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/PropertyRadarState.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/RadarObjectType.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/RadioRadar.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/TileRadarStation.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/data/RadarDotType.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/data/RadarRenderData.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/data/RadarRenderDot.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/gui/ContainerRadarStation.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/gui/GuiRadarStation.java | 雷达站 | S5 |
| TODO | icbm/classic/content/blocks/radarstation/gui/RadarComponent.java | 雷达站 | S5 |
| TODO | icbm/classic/content/cargo/CargoHolderHandler.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/CargoProjectileData.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/ItemThrowableProjectile.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/ProjectileCargoMode.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/RecipeCargoData.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/RecipeCargoDataDecraft.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/balloon/BalloonProjectileData.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/balloon/EntityBalloon.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/parachute/EntityParachute.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/parachute/ParachuteProjectileData.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/parachute/RenderParachute.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/placer/BlockCargoHook.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/placer/TileEntityCargoHook.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cargo/placer/TileEntityCargoHookTicking.java | 货物投放（降落伞/气球） | S4 |
| TODO | icbm/classic/content/cluster/action/ActionCluster.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/action/ActionDataCluster.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/bomblet/BombletProjectileData.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/bomblet/BombletProjectileStack.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/bomblet/EntityBombDroplet.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/bomblet/ItemBombDroplet.java | S2.05 注册 ExplosiveFormItem 占位（10 个平铺化子弹药）；S4.06 换成真实物品 | S2.05、S4.06 |
| TODO | icbm/classic/content/cluster/bomblet/RenderBombDroplet.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/missile/CapabilityClusterMissileStack.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/missile/ClusterMissileHandler.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/missile/ItemClusterMissile.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/cluster/missile/RecipeCluster.java | 集束导弹与子弹药 | S4 |
| TODO | icbm/classic/content/entity/EntityBombCart.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityExplosion.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityExplosive.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityFragments.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityGrenade.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityLightBeam.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntityPlayerSeat.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/EntitySmoke.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/flyingblock/BlockCaptureData.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/flyingblock/EntityFlyingBlock.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/flyingblock/FlyingBlock.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/entity/flyingblock/RenderEntityBlock.java | 爆炸相关实体（碎片、飞行方块等） | S3.01、S3.03 |
| TODO | icbm/classic/content/gas/ProtectiveArmorHandler.java | 气体爆炸 | S3.05 |
| PORTED | icbm/classic/content/items/ItemAntidote.java | 食用改 food/consumable 组件；效果清除改 finishUsingItem（显式清单，S3.05 补 ICBM 效果） | S2.04 |
| TODO | icbm/classic/content/items/ItemBattery.java |  | — |
| TODO | icbm/classic/content/items/ItemBombCart.java | S2.05 注册 ExplosiveFormItem 占位（23 个平铺化炸弹矿车）；S4.05 换成真实物品 | S2.05、S4.05 |
| MERGED → icbm/classic/content/reg/ModItems.java | icbm/classic/content/items/ItemCrafting.java | 元数据子类型改为 10 个平铺化普通 Item；OreDictionary → c: 标签（S2.07） | S2.04 |
| TODO | icbm/classic/content/items/ItemDefuser.java |  | — |
| TODO | icbm/classic/content/items/ItemGrenade.java | S2.05 注册 ExplosiveFormItem 占位（10 个平铺化手雷）；S4.05 换成真实手雷物品 | S2.05、S4.05 |
| TODO | icbm/classic/content/items/ItemLaserDetonator.java |  | — |
| TODO | icbm/classic/content/items/ItemMissile.java | S2.05 注册 ExplosiveFormItem 占位（23 个平铺化导弹）；S4.02 换成真实导弹物品 | S2.05、S4.02 |
| TODO | icbm/classic/content/items/ItemRadarGun.java |  | — |
| TODO | icbm/classic/content/items/ItemRemoteDetonator.java |  | — |
| TODO | icbm/classic/content/items/ItemRocketLauncher.java |  | — |
| TODO | icbm/classic/content/items/behavior/BombCartDispenseBehavior.java |  | — |
| TODO | icbm/classic/content/items/behavior/GrenadeDispenseBehavior.java |  | — |
| TODO | icbm/classic/content/missile/MissileEventHandler.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/CapabilityEmpMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/CapabilityMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/EntityMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/anti/EntitySurfaceToAirMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/anti/SAMTargetData.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/anti/item/CapabilitySAMStack.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/anti/item/ItemSurfaceToAirMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/explosive/EntityExplosiveMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/explosive/EntityMissileActionable.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/EntityHeldItemMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/HeldActionMode.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/item/CapabilityHeldItemMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/item/HeldItemMissileHandler.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/item/ItemHeldItemMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/item/RecipeHeldItemMissile.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/entity/itemstack/item/RecipeHeldItemMissileDecraft.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/TargetRangeDet.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/ArcFlightLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/BallisticFlightLogicOld.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/DeadFlightLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/DirectFlightLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/FollowTargetLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/WarmupFlightLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/move/MoveByFacingLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/move/MoveByVec3Logic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/move/MoveForTicksLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/prefab/AccelerateByFacingLogic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/prefab/AccelerateByVec3Logic.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/flight/prefab/FlightLogic.java | 导弹实体与飞行逻辑 | S4 |
| PORTED | icbm/classic/content/missile/logic/source/ActionSource.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| PORTED | icbm/classic/content/missile/logic/source/cause/ActionCause.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| PORTED | icbm/classic/content/missile/logic/source/cause/CausedByBlock.java | 动作框架数据层（IActionSource/IActionCause 实现），S4 直接复用 | S1.08（S4 复用） |
| TODO | icbm/classic/content/missile/logic/source/cause/EntityCause.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/source/cause/RedstoneCause.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/targeting/BallisticTargetingData.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/logic/targeting/BasicTargetData.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/tracker/LoadedChunkPair.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/tracker/MissileTrackerData.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/tracker/MissileTrackerHandler.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/missile/tracker/MissileTrackerWorld.java | 导弹实体与飞行逻辑 | S4 |
| TODO | icbm/classic/content/potion/ContagiousPoison.java | 药水效果 | S3.05 |
| TODO | icbm/classic/content/potion/CustomPotion.java | 药水效果 | S3.05 |
| TODO | icbm/classic/content/potion/CustomPotionEffect.java | 药水效果 | S3.05 |
| TODO | icbm/classic/content/potion/Poison.java | 药水效果 | S3.05 |
| TODO | icbm/classic/content/radioactive/BlockRadioactive.java | 平铺化方块 | S2.03、S3.08 |
| TODO | icbm/classic/content/radioactive/RadioactiveHandler.java | 平铺化方块 | S2.03、S3.08 |
| MERGED → icbm/classic/content/reg/ModBlocks.java | icbm/classic/content/reg/BlockReg.java | 注册事件改成 onInitialize 调用；方块与方块物品 | S2.01 |
| MERGED → icbm/classic/content/reg/ModEntities.java | icbm/classic/content/reg/EntityReg.java | 1.12 名称映射（missile→explosive_missile）与旧 ID 一并删除（ADR 0002） | S2.01 |
| PORTED | icbm/classic/content/reg/ExplosiveInit.java | 注册 23 个平铺化爆炸物与引信/内容规则；blast 工厂为 S3 占位（throw） | S2.02 |
| MERGED → icbm/classic/content/reg/ModItems.java | icbm/classic/content/reg/ItemReg.java | 平铺化物品注册；OreDictionary → c: 标签（S2.04/S2.07） | S2.01 |
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
| TODO | icbm/classic/lib/ForgeRegistryHelpers.java |  | — |
| TODO | icbm/classic/lib/InventoryUtility.java |  | — |
| TODO | icbm/classic/lib/LanguageUtility.java |  | — |
| TODO | icbm/classic/lib/MapWithDefault.java |  | — |
| PORTED | icbm/classic/lib/NBTConstants.java | 纯字符串常量；log4j 用 26.3 自带的 org.apache.logging.log4j | S1.01（S1.03/S1.10 复用） |
| PORTED | icbm/classic/lib/actions/ActionBase.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionDataBase.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionDataGeneric.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/ActionSystem.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/PotentialAction.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/PotentialActionImp.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| PORTED | icbm/classic/lib/actions/PotentialActionKnown.java | 动作系统（ISavable + SaveHandler） | S1.08 |
| MERGED → S3.02 | icbm/classic/lib/actions/WorkTickingActionHandler.java | 依赖 api.explosion.IBlast + content.blast.Blast（S3.02），并入爆炸线程处理 | S3.02 |
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
| MERGED | icbm/classic/lib/capability/emp/CapabilityEMP.java | MERGED → IcbmLookups#init() 注册原版 EMP 接收器（EntityTypes.ITEM/CREEPER，替代 AttachCapabilitiesEvent） | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpChecker.java | 普通类，仅实现 IEMPReceiver（含新增 API Lookup 之外无 capability 代码） | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpCreeper.java | World→Level；EntityLightningBolt→EntityTypes.LIGHTNING_BOLT + Creeper#thunderHit；删 ICapabilityProvider | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpEntityItem.java | EntityItem→ItemEntity；stacksMatchExact→ItemStack.matches；删 ICapabilityProvider | S1.10 |
| MERGED | icbm/classic/lib/capability/emp/CapabilityEmpInventory.java | MERGED → vanilla Container 版（IItemHandler → Container，getCapability() → getContainer()）；item-in-item 库存待 S3.07、能量抽空待 S6.05 | S1.10 |
| PORTED | icbm/classic/lib/capability/emp/CapabilityEmpKill.java | setDead→discard | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosive.java | 删 ICapabilitySerializable，改 ISavable（EXPLOSIVE_ID 走 ValueOutput/ValueInput） | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveEntity.java | ISavable；ItemEntity 掉落、ItemStack.isSameItem、level.isClientSide() | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveStack.java | 构造签名 (ItemStack, Identifier)；自定义项存档走 IBuilderRegistry#save/load；TODO(S2.02) 数据组件 | S1.10 |
| PORTED | icbm/classic/lib/capability/ex/CapabilityExplosiveStatic.java | INBTSerializable→ISavable（空实现，与 1.12 一致） | S1.10 |
| DROPPED | icbm/classic/lib/capability/gps/CapabilityGPSData.java | DROPPED（改用 icbmclassic:gps_data 数据组件，S2.04/S5.08） | S1.10 |
| DROPPED | icbm/classic/lib/capability/gps/CapabilityGPSDataItem.java | DROPPED（同上；物品数据组件） | S1.10 |
| PORTED | icbm/classic/lib/capability/gps/GPSDataHelpers.java | Vec3 + Identifier 维度；TextComponent→Component.translatable、sendMessage→sendSystemMessage；S2.10 键改 icbmclassic.message.gps.*（chat.launcher.noTargetInTool → icbmclassic.message.gps.set.empty） | S1.10、S2.10 |
| MERGED | icbm/classic/lib/capability/launcher/CapabilityMissileHolder.java | MERGED → vanilla Container 版（IItemHandlerModifiable → Container）；consumeMissile 走 IcbmLookups.MISSILE_STACK | S1.10 |
| PORTED | icbm/classic/lib/capability/launcher/CapabilityMissileLauncher.java | 删 capability 注册；状态暂用 ActionResponses（TODO(S5.02) LauncherStatus） | S1.10 |
| MERGED | icbm/classic/lib/capability/missile/CapabilityMissileStack.java | MERGED → S4.02 实现（依赖 content/missile/entity/explosive/EntityExplosiveMissile） | S4.02 |
| TODO | icbm/classic/lib/colors/ColorB.java |  | — |
| TODO | icbm/classic/lib/colors/ColorHelper.java |  | — |
| PORTED | icbm/classic/lib/data/BoundBlockPos.java | BlockPos/Vec3 边界盒实现 | S1.05（S1.10 复用） |
| TODO | icbm/classic/lib/data/FaceRotations.java |  | — |
| TODO | icbm/classic/lib/data/IMachineInfo.java |  | — |
| PORTED | icbm/classic/lib/data/LazyBuilder.java | 纯 Java 惰性构建器，动作与内容复用 | S1.08（S2.02/S4 复用） |
| DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | icbm/classic/lib/energy/storage/EnergyActionType.java | DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | S1.07 |
| MERGED → icbm/classic/lib/energy/IcbmEnergyBuffer.java | icbm/classic/lib/energy/storage/EnergyBuffer.java | S1.07 建内部 IcbmEnergyBuffer，S6 完成门面；S6 不改接口 | S1.07 |
| DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | icbm/classic/lib/energy/storage/EnergyChangeCallback.java | DROPPED（ADR 0003/energy-fe §3：回调并入 IcbmEnergyBuffer#onChange） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystem.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystemFE.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/EnergySystemNull.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | icbm/classic/lib/energy/system/IEnergySystem.java | DROPPED（ADR 0003：S6 用 Team Reborn EnergyStorage 替代） | S1.07 |
| PORTED | icbm/classic/lib/explosive/ThreadWorkBlast.java | 通用收集型工作，`doRun(int, LevelSnapshot)`/`onStarted(LevelSnapshot)`；被线程化爆炸（S3.02/S3.06）与 ExplosiveRegistry（S2.02）使用 | S1.09、S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExBlockContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExGrenadeContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExMinecartContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
| PORTED | icbm/classic/lib/explosive/reg/ExMissileContentReg.java | 数值 ID 层删除（ADR 0002），改按 Identifier 键；1.12 的 IntHashMap 缓存合并为按名缓存 | S2.02 |
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
| PORTED | icbm/classic/lib/projectile/EntityProjectile.java | 直系继承 Entity（EntityICBM 属 S5.01，hasHealth/canBeDestroyed 暂内联）；onUpdate→tick、rayTraceBlocks→ClipContext、实体射线→ProjectileUtil、IEntityAdditionalSpawnData→SynchedEntityData(freezeMotion)；伤害源与 EntityPlayerSeat 排除留 S3.02/S4.06 | S3.01 |
| PORTED | icbm/classic/lib/projectile/InGroundData.java | INBTSerializable→ISavable；getMaterial() 删除（26.3 无 Material，改判 state.isAir()） | S3.01 |
| PORTED | icbm/classic/lib/projectile/ProjectileDataRegistry.java | ResourceLocation→Identifier、ForgeRegistries→BuiltInRegistries、ItemMonsterPlacer→SpawnEggItem（单一生成蛋物品拆成逐生怪蛋物品）；1.12 的 IProjectileStack capability 查询留 TODO S4.06；接线在 ICBMClassic#onInitialize | S3.01 |
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
| TODO | icbm/classic/lib/recipe/ConditionOreValue.java |  | — |
| TODO | icbm/classic/lib/recipe/ExIngredientFactory.java |  | — |
| TODO | icbm/classic/lib/recipe/ExRecipeFactory.java |  | — |
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
| TODO | icbm/classic/lib/tile/TickAction.java |  | — |
| TODO | icbm/classic/lib/tile/TickDoOnce.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTracker.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerData.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerEntry.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerField.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerHelpers.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerNode.java |  | — |
| TODO | icbm/classic/lib/tracker/EventTrackerType.java |  | — |
| TODO | icbm/classic/lib/tracker/IEventTrackerListener.java |  | — |
| TODO | icbm/classic/lib/tracker/ListenerEntry.java |  | — |
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
| TODO | icbm/classic/prefab/FakeRadioSender.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/entity/EntityICBM.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/ContainerBase.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/GuiContainerBase.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/GuiFormatHelpers.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/IGuiComponent.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/IPlayerUsing.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/NumericField.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/TextInput.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/button/DisableButton.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/button/FaceRotationButton.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/button/GuiButtonBase.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/button/RedstoneButton.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/components/SlotEnergyBar.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/slot/SlotEnergyItem.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/textbox/GuiTextFieldBase.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/tooltip/IToolTip.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/tooltip/Tooltip.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/tooltip/TooltipBase.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/gui/tooltip/TooltipTranslations.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/inventory/InventorySlot.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/inventory/InventoryWithSlots.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| DROPPED（Forge 工具提示框架） | icbm/classic/prefab/item/ItemBase.java | 只做 setName 与 .info 提示；26.3 物品为普通 Item，提示文本未迁移（见 S2.04 证据） | S2.04 |
| TODO | icbm/classic/prefab/item/ItemBlockAbstract.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/item/ItemBlockSubTypes.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| DROPPED（Forge 专用） | icbm/classic/prefab/item/ItemICBMBase.java | 只做 setCreativeTab/setName，由 Item.Properties 与 ModItems 承担 | S2.04 |
| TODO | icbm/classic/prefab/item/ItemICBMElectrical.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/item/ItemRadio.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/item/ItemStackCapProvider.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/item/LootEntryItemStack.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/tile/BlockICBM.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/tile/IGuiTile.java | 方块实体基类、库存、GUI 组件 | S5.01 |
| TODO | icbm/classic/prefab/tile/TileMachine.java | 方块实体基类、库存、GUI 组件 | S5.01 |
