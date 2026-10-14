package icbm.classic.content.entity;

import icbm.classic.content.reg.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Used as a placeholder to position riding entities on launchers and missiles.
 */
public class EntityPlayerSeat extends Entity
{
    private BlockPos hostPos;

    // TODO(1.12): Riding offset has known issues in 1.12, kept as is per design instructions.
    public float offsetX = 0;
    public float offsetY = 0;
    public float offsetZ = 0;

    public EntityPlayerSeat(EntityType<? extends EntityPlayerSeat> type, Level level)
    {
        super(type, level);
    }

    public EntityPlayerSeat(Level level)
    {
        this(ModEntities.MISSILE_SEAT, level);
    }

    public void setHostPos(BlockPos pos)
    {
        this.hostPos = pos;
    }

    public BlockPos getHostPos()
    {
        return hostPos;
    }
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // No synched data required for placeholder seat
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location)
    {
        if (player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }
        if (this.hasPassenger(player))
        {
            return InteractionResult.SUCCESS;
        }
        if (!this.level().isClientSide())
        {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale)
    {
        return new Vec3(offsetX, offsetY, offsetZ);
    }

    @Override
    public boolean isPickable()
    {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity)
    {
        return false;
    }

    @Override
    public void removePassenger(Entity passenger)
    {
        final double prevX = passenger.getX();
        final double prevY = passenger.getY();
        final double prevZ = passenger.getZ();

        super.removePassenger(passenger);

        // Player will sometimes warp when dismounting; clamp if moved excessively
        if (Math.abs(prevX - passenger.getX()) > 2 || Math.abs(prevY - passenger.getY()) > 2 || Math.abs(prevZ - passenger.getZ()) > 2)
        {
            passenger.setPos(prevX, prevY, prevZ);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        in.read("pos", BlockPos.CODEC).ifPresent(p -> this.hostPos = p);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        if (hostPos != null)
        {
            out.store("pos", BlockPos.CODEC, hostPos);
        }
    }
}
