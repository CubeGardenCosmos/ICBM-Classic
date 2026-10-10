package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.api.EnumTier;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.explosion.IBlastFactory;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.reg.events.ExplosiveContentRegistryEvent;
import icbm.classic.api.reg.events.ExplosiveRegistryEvent;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.lib.explosive.reg.ExBlockContentReg;
import icbm.classic.lib.explosive.reg.ExGrenadeContentReg;
import icbm.classic.lib.explosive.reg.ExMinecartContentReg;
import icbm.classic.lib.explosive.reg.ExMissileContentReg;
import icbm.classic.lib.explosive.reg.ExplosiveRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Registers every explosive of the mod (registry-ids.md §1) together with the content types that can
 * carry them, then locks the registry.
 * <p>
 * 1.12 registered numeric ids here ({@code forceID}) and stored the id -> name map in a config file.
 * ADR 0002 drops numeric ids and legacy save migration, so this step only registers the
 * {@link Identifier} key, the tier, the blast factory and the fuse callbacks.
 * <p>
 * The 1.12 {@code newEx} calls also built the blast instances (BlastTNT, BlastNuclear, ...). Those
 * classes are ported by S3.02–S3.09, so the factories are placeholders that throw until then; the
 * original 1.12 expression is kept in a comment above each call so the blast steps can restore it
 * verbatim. Tiers, ids, fuse times and content enablement are already final.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public final class ExplosiveInit
{
    private ExplosiveInit() {}

    public static void init()
    {
        final ExplosiveRegistry registry = new ExplosiveRegistry();
        ICBMClassicAPI.EXPLOSIVE_REGISTRY = registry;

        ICBMClassicAPI.EX_BLOCK_REGISTRY = new ExBlockContentReg();
        ICBMClassicAPI.EX_GRENADE_REGISTRY = new ExGrenadeContentReg();
        ICBMClassicAPI.EX_MINECART_REGISTRY = new ExMinecartContentReg();
        ICBMClassicAPI.EX_MISSILE_REGISTRY = new ExMissileContentReg();

        // Register default content types
        registry.registerContentRegistry(ICBMClassicAPI.EX_BLOCK_REGISTRY);
        registry.registerContentRegistry(ICBMClassicAPI.EX_GRENADE_REGISTRY);
        registry.registerContentRegistry(ICBMClassicAPI.EX_MISSILE_REGISTRY);
        registry.registerContentRegistry(ICBMClassicAPI.EX_MINECART_REGISTRY);

        // Fire registry event for content types
        ExplosiveContentRegistryEvent.EVENT.invoker().onRegisterContent(registry);

        // Lock content types, done to prevent errors with adding content
        registry.lockNewContentTypes();

        //=================== Tier 1
        // 1.12: new BlastTNT().setDamageToEntities(ConfigBlast.condensed.damage).setBlastSize(ConfigBlast.condensed.energyScale)
        ICBMExplosives.CONDENSED = newEx(EnumTier.ONE, "condensed");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.CONDENSED.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.CONDENSED);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.CONDENSED.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.CONVENTIONAL);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.CONDENSED.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONDENSED);


        // 1.12: new BlastShrapnel().setProjectile(fragments: critical, fire 100, ConfigBlast.shrapnel.damage).setBlastSize(ConfigBlast.shrapnel.fragments)
        ICBMExplosives.SHRAPNEL = newEx(EnumTier.ONE, "shrapnel");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.SHRAPNEL.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.SHRAPNEL);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.SHRAPNEL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.SHRAPNEL);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.SHRAPNEL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.SHRAPNEL);


        // 1.12: new BlastFire().setBlastSize(ConfigBlast.incendiary.scale)
        ICBMExplosives.INCENDIARY = newEx(EnumTier.ONE, "incendiary");
        // 1.12: world.spawnParticle(EnumParticleTypes.LAVA, x, y + 0.5D, z, 0.0D, 0.0D, 0.0D)
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseTickListener(ICBMExplosives.INCENDIARY.getRegistryKey(), ExplosiveInit::spawnFuseLavaParticle);
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.INCENDIARY.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.INCENDIARY);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.INCENDIARY.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.INCENDIARY);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.INCENDIARY.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.INCENDIARY);


        // 1.12: new BlastDebilitation().setDuration(ConfigBlast.debilitation.duration).setBlastSize(ConfigBlast.debilitation.size)
        ICBMExplosives.DEBILITATION = newEx(EnumTier.ONE, "debilitation");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.DEBILITATION.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.DEBILITATION);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.DEBILITATION.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.DEBILITATION);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.DEBILITATION.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.DEBILITATION);


        // 1.12: new BlastChemical().setToxicityBuildup(...).setToxicityScale(...).setToxicityMinDamage(...).setDuration(...).setBlastSize(ConfigBlast.chemical.size)
        ICBMExplosives.CHEMICAL = newEx(EnumTier.ONE, "chemical");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.CHEMICAL.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.CHEMICAL);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.CHEMICAL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.CHEMICAL);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.CHEMICAL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.CHEMICAL);


        // 1.12: new BlastShrapnel().setProjectile(fragments: anvil, ConfigBlast.anvil.damage).setBlastSize(ConfigBlast.anvil.fragments)
        ICBMExplosives.ANVIL = newEx(EnumTier.ONE, "anvil");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ANVIL.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ANVIL);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.ANVIL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.ANVIL);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ANVIL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ANVIL);


        // 1.12: new BlastTNT().setDestroyItems().setPushType(PushType.REPEL).setBlastSize(ConfigBlast.repulsive.scale)
        ICBMExplosives.REPULSIVE = newEx(EnumTier.ONE, "repulsive");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.REPULSIVE.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.REPULSIVE);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.REPULSIVE.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.REPULSIVE);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.REPULSIVE.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.REPULSIVE);


        // 1.12: new BlastTNT().setDestroyItems().setPushType(PushType.ATTRACT).setBlastSize(ConfigBlast.attractive.scale)
        ICBMExplosives.ATTRACTIVE = newEx(EnumTier.ONE, "attractive");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ATTRACTIVE.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ATTRACTIVE);
        ICBMClassicAPI.EX_GRENADE_REGISTRY.setFuseSupplier(ICBMExplosives.ATTRACTIVE.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.GRENADES.ATTRACTIVE);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ATTRACTIVE.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ATTRACTIVE);


        //=================== Tier 2
        // 1.12: new BlastShrapnel().setProjectile(fragments: explosive, ConfigBlast.fragmentation.damage, explosionSize).setBlastSize(ConfigBlast.fragmentation.fragments)
        ICBMExplosives.FRAGMENTATION = newEx(EnumTier.TWO, "fragmentation");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.FRAGMENTATION.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.FRAGMENTATION);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.FRAGMENTATION.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.FRAGMENTATION);


        // 1.12: new BlastContagious().setToxicityScale(...).setToxicityBuildup(...).setToxicityMinDamage(...).setDuration(...).setBlastSize(ConfigBlast.contagious.size)
        ICBMExplosives.CONTAGIOUS = newEx(EnumTier.TWO, "contagious");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.CONTAGIOUS.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.CONTAGIOUS);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.CONTAGIOUS.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONTAGIOUS);


        // 1.12: new BlastSonic().setBlastSize(ConfigBlast.sonic.scale)
        ICBMExplosives.SONIC = newEx(EnumTier.TWO, "sonic");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.SONIC.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.SONIC);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.SONIC.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.SONIC);


        // 1.12: new BlastBreach().setDepth(...).setWidth(...).setEnergy(...).setEnergyDistanceScale(...).setEnergyCostDistance(...).setDamageToEntities(ConfigBlast.breaching.damage)
        ICBMExplosives.BREACHING = newEx(EnumTier.TWO, "breaching");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.BREACHING.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.BREACHING);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.BREACHING.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.BREACHING);


        // 12 -> Regen

        // 1.12: new BlastThermobaric().setEnergy(ConfigBlast.thermobaric.energy).setBlastSize(ConfigBlast.thermobaric.scale)
        ICBMExplosives.THERMOBARIC = newEx(EnumTier.TWO, "thermobaric");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.THERMOBARIC.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.THERMOBARIC);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.THERMOBARIC.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.THERMOBARIC);

        // 14 -> S-Mine

        //=================== Tier 3
        // 1.12: new BlastNuclear().setEnergy(ConfigBlast.nuclear.energy).setBlastSize(ConfigBlast.nuclear.scale)
        ICBMExplosives.NUCLEAR = newEx(EnumTier.THREE, "nuclear");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.NUCLEAR.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.NUCLEAR);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.NUCLEAR.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.NUCLEAR);


        // 1.12: ActionDataEmpArea.INSTANCE.create(w, x, y, z, s, null)
        ICBMExplosives.EMP = newEx(EnumTier.THREE, "emp");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.EMP.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.EMP);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.EMP.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.EMP);


        // 1.12: new BlastExothermic().setBlastSize(ConfigBlast.exothermic.scale)
        ICBMExplosives.EXOTHERMIC = newEx(EnumTier.THREE, "exothermic");
        // 1.12: world.spawnParticle(EnumParticleTypes.LAVA, x, y + 0.5D, z, 0.0D, 0.0D, 0.0D)
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseTickListener(ICBMExplosives.EXOTHERMIC.getRegistryKey(), ExplosiveInit::spawnFuseLavaParticle);
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.EXOTHERMIC.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.EXOTHERMIC);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.EXOTHERMIC.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.EXOTHERMIC);


        // 1.12: new BlastEndothermic().setBlastSize(ConfigBlast.endothermic.scale)
        ICBMExplosives.ENDOTHERMIC = newEx(EnumTier.THREE, "endothermic");
        //TODO add ice fuse animation
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ENDOTHERMIC.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ENDOTHERMIC);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ENDOTHERMIC.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ENDOTHERMIC);


        // 1.12: new BlastAntiGravitational().setBlastSize(ConfigBlast.antigravitational.scale)
        ICBMExplosives.ANTI_GRAVITATIONAL = newEx(EnumTier.THREE, "antigravitational");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ANTI_GRAVITATIONAL.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ANTI_GRAVITATIONAL);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ANTI_GRAVITATIONAL.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ANTI_GRAVITATIONAL);


        // 1.12: new BlastEnder().setBlastSize(ConfigBlast.ender.scale)
        ICBMExplosives.ENDER = newEx(EnumTier.THREE, "ender");
        ICBMClassicAPI.EX_MISSILE_REGISTRY.setInteractionListener(ICBMExplosives.ENDER.getRegistryKey(), ExplosiveInit::todoEnderMissileCoordSet);
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setActivationListener(ICBMExplosives.ENDER.getRegistryKey(), ExplosiveInit::todoEnderBlockCoordSet);
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ENDER.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ENDER);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ENDER.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ENDER);

        // Hypersonic was removed in 4.3.0; ADR 0002 drops the numeric placeholder so it is not registered at all.

        //=================== Tier 4
        // 1.12: new BlastAntimatter().setBlastSize(ConfigBlast.antimatter.size)
        ICBMExplosives.ANTIMATTER = newEx(EnumTier.FOUR, "antimatter");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.ANTIMATTER.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.ANTIMATTER);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.ANTIMATTER.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.ANTIMATTER);

        //TODO add config (disable by default) for alarm audio

        // 1.12: new ActionSpawnRedmatter(w, new Vec3d(x, y, z), s, ICBMExplosives.REDMATTER)
        ICBMExplosives.REDMATTER = newEx(EnumTier.FOUR, "redmatter");
        ICBMClassicAPI.EX_BLOCK_REGISTRY.setFuseSupplier(ICBMExplosives.REDMATTER.getRegistryKey(), (world, x, y, z) -> ConfigBlast.FUSE_TIMES.EXPLOSIVES.REDMATTER);
        ICBMClassicAPI.EX_MINECART_REGISTRY.setFuseSupplier(ICBMExplosives.REDMATTER.getRegistryKey(), (entity) -> ConfigBlast.FUSE_TIMES.BOMB_CARTS.REDMATTER);

        //=================== No content, only blast

        // 1.12: new BlastRadioactiveBlockSwaps()
        ICBMExplosives.ROT = newEx(EnumTier.NONE, "rot"); //TODO add item version
        // 1.12: new BlastMutation()
        ICBMExplosives.MUTATION = newEx(EnumTier.NONE, "mutation"); //TODO add item version

        //=================== New Explosives not part of classic original
        // 1.12: new BlastColor().setBlastSize(ConfigBlast.colorful.scale)
        ICBMExplosives.COLOR = newEx(EnumTier.ONE, "colors");
        // 1.12: ActionSpawnEntity(w, new Vec3d(x, y, z), s, ICBMExplosives.SMOKE) with ActionFields.ENTITY_REG_NAME = ICBMEntities.SMOKE
        ICBMExplosives.SMOKE = newEx(EnumTier.ONE, "smoke");

        // Fire registry event for explosives, then lock everything (no new explosives or content types after this)
        ExplosiveRegistryEvent.EVENT.invoker().onRegisterExplosives(registry);
        registry.lock();
    }

    /**
     * Registers an explosive plus the content types its tier allows (tier != NONE -> block, missile,
     * bomb cart; tier ONE additionally -> grenade, and the bomblet follows the grenade).
     * <p>
     * 1.12 additionally reserved a numeric id here ({@code forceID}); ADR 0002 removed the numeric id.
     */
    private static IExplosiveData newEx(EnumTier tier, String name)
    {
        final Identifier regName = ICBMConstants.id(name);
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.register(regName, tier, todoBlast(name));

        //Do default content types per explosive
        if (tier != EnumTier.NONE)
        {
            ICBMClassicAPI.EX_BLOCK_REGISTRY.enableContent(regName);
            ICBMClassicAPI.EX_MISSILE_REGISTRY.enableContent(regName);
            ICBMClassicAPI.EX_MINECART_REGISTRY.enableContent(regName);
        }
        if (tier == EnumTier.ONE)
        {
            ICBMClassicAPI.EX_GRENADE_REGISTRY.enableContent(regName);
        }

        return data;
    }

    /**
     * TODO(S3.02–S3.09): the 1.12 blast factory for this explosive. Every blast class is ported by the
     * blast steps, so the factory throws until they replace this call. The 1.12 expression that has to
     * be restored sits in a comment above each {@link #newEx} call.
     */
    private static IBlastFactory todoBlast(String explosive)
    {
        return (world, x, y, z, source) -> {
            throw new UnsupportedOperationException("ICBM: blast factory for icbmclassic:" + explosive
                    + " is added with the blast content (S3), it can not create a blast yet");
        };
    }

    /** 1.12 {@code world.spawnParticle(EnumParticleTypes.LAVA, x, y + 0.5D, z, 0, 0, 0)} on the server. */
    private static void spawnFuseLavaParticle(Level world, double x, double y, double z, int tick)
    {
        if (world instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(ParticleTypes.LAVA, x, y + 0.5D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * TODO(S3.07) the ender explosive stores the teleport target on the missile through
     * {@code EnderBlastCustomization} + the {@code icbmclassic:gps_data} component. 1.12 also
     * registered this callback on the missile content registry, which was an empty stub there, so this
     * simply keeps the call site until the ender blast is ported.
     */
    private static boolean todoEnderMissileCoordSet(Entity entity, Player player, InteractionHand hand)
    {
        return false;
    }

    /** TODO(S3.09) same as {@link #todoEnderMissileCoordSet} but for the explosive block activation. */
    private static boolean todoEnderBlockCoordSet(Level world, BlockPos pos, Player player, InteractionHand hand,
                                                  Direction facing, float hitX, float hitY, float hitZ)
    {
        return false;
    }
}
