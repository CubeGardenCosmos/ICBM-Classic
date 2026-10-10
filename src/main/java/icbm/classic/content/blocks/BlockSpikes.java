package icbm.classic.content.blocks;

import icbm.classic.config.blocks.ConfigBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Spikes. 1.12 had one block with three metadata types; ADR 0002 flattens them into one class and three
 * instances ({@code spikes}, {@code poison_spikes}, {@code fire_spikes}).
 * <p>
 * 1.12 collision was disabled ({@code getCollisionBoundingBox} returned null) so entities walk through
 * the block and take damage every collision tick; 26.3 uses {@code noCollission} on the properties.
 * <p>
 * 1.12 used a custom damage source ({@code new DamageSource("icbmclassic:spikes")}). That damage type is
 * not in registry-ids.md §7, so the 26.3 port uses the vanilla cactus source that has the same behaviour
 * (point damage, no special death message handling needed).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class BlockSpikes extends Block
{
    public enum SpikeType
    {
        NORMAL,
        POISON,
        FIRE
    }

    private final SpikeType type;

    public BlockSpikes(SpikeType type, BlockBehaviour.Properties properties)
    {
        super(properties);
        this.type = type;
    }

    public SpikeType getSpikeType()
    {
        return type;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                               InsideBlockEffectApplier applier, boolean precise)
    {
        // 1.12: onEntityCollidedWithBlock only reacted to living entities
        if (entity instanceof LivingEntity living && level instanceof ServerLevel serverLevel)
        {
            final DamageSource source = level.damageSources().cactus();
            switch (type)
            {
                case POISON ->
                {
                    living.hurtServer(serverLevel, source, ConfigBlocks.spikes.poisonDamage);
                    // 1.12: new PotionEffect(MobEffects.POISON, 7 * 20, 0)
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 7 * 20, 0));
                }
                case FIRE ->
                {
                    living.hurtServer(serverLevel, source, ConfigBlocks.spikes.fireDamage);
                    // 1.12: entity.setFire(7)
                    living.igniteForSeconds(7);
                }
                default -> living.hurtServer(serverLevel, source, ConfigBlocks.spikes.normalDamage);
            }
        }
    }
}
