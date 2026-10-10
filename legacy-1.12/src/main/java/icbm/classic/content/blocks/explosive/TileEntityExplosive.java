package icbm.classic.content.blocks.explosive;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.tile.IRotatable;
import icbm.classic.content.blast.BlastBreach;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.lib.actions.fields.ActionFieldProvider;
import icbm.classic.lib.capability.ex.CapabilityExplosiveStack;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TileEntityExplosive extends TileEntity implements IRotatable
{
    public static final String NBT_EXPLOSIVE_STACK = "explosive_stack";

    /**
     * Is the tile currently exploding
     */
    public boolean hasBeenTriggered = false;

    public CapabilityExplosiveStack capabilityExplosive = new CapabilityExplosiveStack(null);

    /**
     * Reads a tile entity from NBT.
     */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        if(nbt.hasKey(NBT_EXPLOSIVE_STACK, 10)) {
            final NBTTagCompound itemStackTag = nbt.getCompoundTag(NBT_EXPLOSIVE_STACK);
            capabilityExplosive = new CapabilityExplosiveStack(new ItemStack(itemStackTag));
        }
    }

    /**
     * Writes a tile entity to NBT.
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        if (capabilityExplosive != null && capabilityExplosive.toStack() != null)
        {
            nbt.setTag(NBT_EXPLOSIVE_STACK, capabilityExplosive.toStack().serializeNBT());
        }
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
    {
        return capability == ICBMClassicAPI.EXPLOSIVE_CAPABILITY && capabilityExplosive != null || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if (capability == ICBMClassicAPI.EXPLOSIVE_CAPABILITY)
        {
            return ICBMClassicAPI.EXPLOSIVE_CAPABILITY.cast(capabilityExplosive);
        }
        return super.getCapability(capability, facing);
    }

    public void trigger(boolean setFire)
    {
        if (!hasBeenTriggered)
        {
            hasBeenTriggered = true;

            // TODO handle this better in 1.13+ as this is a temp work around for direction being funky for breach
            if(capabilityExplosive.getExplosiveData() == ICBMExplosives.BREACHING) {
                final IActionSource source = new ActionSource(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), null);
                final EnumFacing direction = this.getDirection().getOpposite();
                capabilityExplosive.getExplosiveData()
                    .create(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, source, new ActionFieldProvider().field(ActionFields.HOST_DIRECTION, () -> direction))
                    .doAction();
            }
            else {

                EntityExplosive entityExplosive = new EntityExplosive(world, new Pos(pos).add(0.5), getDirection(), capabilityExplosive.toStack());
                //TODO check for tick rate, trigger directly if tick is less than 3

                if (setFire) {
                    entityExplosive.setFire(100);
                }

                world.spawnEntity(entityExplosive);
            }
            world.setBlockToAir(pos);

            ICBMClassic.logger().info("TileEntityExplosive: Triggered ITEM{" + capabilityExplosive.toStack() + "] " + capabilityExplosive.getExplosiveData().getRegistryKey() + " at location " + getPos());
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public EnumFacing getDirection()
    {
        return EnumFacing.getFront(this.getBlockMetadata());
    }

    @Override
    public void setDirection(EnumFacing facingDirection)
    {
        IBlockState state = world.getBlockState(pos);
        state = state.withProperty(BlockExplosive.ROTATION_PROP, facingDirection);
        this.world.setBlockState(pos, state, 2);
    }
}
