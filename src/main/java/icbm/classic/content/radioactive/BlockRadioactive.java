package icbm.classic.content.radioactive;

import icbm.classic.config.ConfigMain;
import icbm.classic.config.blocks.ConfigBlocks;
import icbm.classic.content.gas.ProtectiveArmorHandler;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Radioactive block left behind by a nuclear blast, decays back into normal blocks and irradiates the
 * entities standing near it.
 * <p>
 * 1.12 had one block with a {@code type} property (dirt/stone) and a metadata driven item; ADR 0002
 * flattens it into {@code radioactive_dirt} and {@code radioactive_stone}, so the type is a constructor
 * argument and the registry id carries it (docs/architecture/registry-ids.md §2).
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>the 1.12 {@code randomTick} only called {@code updateTick}, which was the scheduled tick handler:
 *     26.3 keeps both halves, {@link #randomTick} (driven by {@code Properties#randomTicks()}) and
 *     {@link #tick} (driven by {@code Level#scheduleTick}), delegating to the same decay pass.</li>
 *     <li>1.12 {@code tickRate(World)} was only read by this block's own rescheduling ({@link #tickRate()});
 *     26.3 {@code Block} has no tick rate hook, the random tick speed of the level covers that role.</li>
 *     <li>the 1.12 {@code damageSource} was a {@code new DamageSource("icbmclassic:radioactive_block")};
 *     26.3 builds the source from the data pack entry registered by S3.02,
 *     {@link ModDamageTypes#RADIOACTIVE_BLOCK}.</li>
 *     <li>the per type hardness and explosion resistance (1.12 {@code getBlockHardness} /
 *     {@code getExplosionResistance} answered with the vanilla dirt/stone values) moved into the block
 *     properties, see {@link #properties(Kind)}.</li>
 * </ul>
 */
public class BlockRadioactive extends Block
{
    /** 1.12 {@code BlockRadioactive.EnumType}, now one block instance per value (ADR 0002). */
    public enum Kind
    {
        /** 1.12 {@code EnumType.DIRT} (meta 0), decays into dirt. */
        DIRT(0.5F, 0.5F, MapColor.DIRT, Blocks.DIRT),
        /** 1.12 {@code EnumType.STONE} (meta 1, the default), decays into stone. */
        STONE(1.5F, 6F, MapColor.STONE, Blocks.STONE);

        /** 1.12 {@code Blocks.DIRT.getBlockHardness(...)} / {@code Blocks.STONE.getBlockHardness(...)}. */
        private final float destroyTime;
        /** 1.12 {@code Blocks.DIRT.getExplosionResistance(...)} / {@code Blocks.STONE...}. */
        private final float explosionResistance;
        private final MapColor mapColor;
        /** 1.12 {@code doDecaySelf} placed this block when the decay roll hit. */
        private final Block decayedBlock;

        Kind(float destroyTime, float explosionResistance, MapColor mapColor, Block decayedBlock)
        {
            this.destroyTime = destroyTime;
            this.explosionResistance = explosionResistance;
            this.mapColor = mapColor;
            this.decayedBlock = decayedBlock;
        }

        /** 1.12 {@code doDecaySelf} switched on the type property and placed the matching vanilla block. */
        public BlockState decayedState()
        {
            return decayedBlock.defaultBlockState();
        }
    }

    private final Kind kind;

    public BlockRadioactive(Kind kind, BlockBehaviour.Properties properties)
    {
        super(properties);
        this.kind = kind;
    }

    /**
     * Block properties of one radioactive type, used by {@code ModBlocks} when it registers the two
     * instances.
     * <p>
     * 1.12: {@code super(Material.ROCK)}, {@code setHardness(0.5f)} and a per type override of
     * {@code getBlockHardness}/{@code getExplosionResistance} returning the vanilla dirt/stone values.
     * {@code Material.ROCK} becomes {@code requiresCorrectToolForDrops} plus the pickaxe tag, the same
     * mapping the S2.03 concrete uses (porting-guide.md §2).
     * The 1.12 constructor also enabled random ticking when {@code decay_delay} was above zero
     * ({@code if (ConfigBlocks.radioactive.decayDelay > 0) this.setTickRandomly(true);}), the 26.3
     * equivalent is {@link BlockBehaviour.Properties#randomTicks()}.
     *
     * @param kind radioactive type of the block being registered
     * @return properties for that type
     */
    public static BlockBehaviour.Properties properties(Kind kind)
    {
        final BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(kind.mapColor)
                .strength(kind.destroyTime, kind.explosionResistance)
                .requiresCorrectToolForDrops();
        return ConfigBlocks.radioactive.decayDelay > 0 ? properties.randomTicks() : properties;
    }

    public Kind getKind()
    {
        return kind;
    }

    /** 1.12 {@code damageSource = new DamageSource("icbmclassic:radioactive_block")}. */
    public static DamageSource damageSource(Level level)
    {
        return ModDamageTypes.source(level, ModDamageTypes.RADIOACTIVE_BLOCK);
    }

    /** 1.12 {@code tickRate(World)}: the delay range used when the decay pass reschedules itself. */
    public int tickRate()
    {
        return ConfigBlocks.radioactive.decayDelay;
    }

    /**
     * 1.12 {@code randomTick} only forwarded to {@code updateTick}. 26.3 random ticks the block when the
     * properties enable it, the scheduled tick below covers the rest of the 1.12 loop.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        updateTick(level, pos, state, random);
    }

    /** 1.12 {@code updateTick}, the scheduled tick handler of the block. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        updateTick(level, pos, state, random);
    }

    private void updateTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random)
    {
        final boolean stop = this.tryDecaySelf(level, pos, random);
        this.tryDecayEntities(level, pos, random);

        if (!stop && !ConfigBlocks.radioactive.randomTickOnly)
        {
            level.scheduleTick(pos, this, random.nextInt(tickRate()) + 1);
        }
    }

    private boolean tryDecaySelf(Level level, BlockPos pos, RandomSource random)
    {
        if (ConfigBlocks.radioactive.decayBlockChance <= 0.000000001)
        {
            return false;
        }
        // Block death chance
        if (random.nextFloat() < ConfigBlocks.radioactive.decayBlockChance)
        {
            // TODO fire action event
            this.doDecaySelf(level, pos);
            return true;
        }
        return false;
    }

    private void doDecaySelf(Level level, BlockPos pos)
    {
        // 1.12: worldIn.setBlockState(pos, Blocks.DIRT/STONE.getDefaultState())
        level.setBlock(pos, kind.decayedState(), Block.UPDATE_ALL);
    }

    private void tryDecayEntities(ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (ConfigBlocks.radioactive.decayEffectRange <= 0 || ConfigBlocks.radioactive.decayEffectChance <= 0)
        {
            return;
        }

        // 1.12: new AxisAlignedBB(pos).grow(range, range, range) around the block
        final double range = ConfigBlocks.radioactive.decayEffectRange;
        final AABB bounds = new AABB(pos).inflate(range);
        final List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, bounds);
        for (LivingEntity entity : entities)
        {
            if (random.nextFloat() < ConfigBlocks.radioactive.decayEffectChance)
            {
                float protection = ProtectiveArmorHandler.getProtectionRating(entity);
                if (protection < ConfigMain.protectiveArmor.minProtectionRadiation || protection < random.nextFloat())
                {
                    //TODO randomize damage
                    //TODO scale damage by protection percentage
                    //TODO scale damage by range

                    entity.hurtServer(level, damageSource(level), ConfigBlocks.radioactive.decayEffectDamage);
                    if (ConfigBlocks.radioactive.decayWitherDuration > 0)
                    {
                        // 1.12 used a hardcoded 20 here, the config only guards the effect
                        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 20));
                    }
                }
            }
        }
    }

    /**
     * 1.12 {@code randomDisplayTick}, client side only: an occasional decay click and the TODO for the
     * area of effect particles.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        if (random.nextInt(12) == 0)
        {
            // 1.12: ICBMSounds.RADIOACTIVE_TICK.play(worldIn, x, y, z, volume, pitch, true),
            // SoundCategory.BLOCKS with the distance delay of the client side world.playSound
            level.playLocalSound(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                    ModSounds.RADIOACTIVE_TICK, SoundSource.BLOCKS,
                    1.0F + random.nextFloat() * 0.5F, 1 + random.nextFloat() * 0.2F,
                    true);
        }

        //TODO spawn particles showing AOE
    }
}
