package icbm.classic.prefab.tile;

import icbm.classic.lib.tile.ITick;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Base block entity of every ICBM machine, 1.12 {@code icbm.classic.prefab.tile.TileMachine} ported onto
 * {@link BlockEntity}.
 * <p>
 * The 1.12 class implemented {@code ITickable} and ran its {@link ITick} actions from {@code update()}.
 * 26.3 has no ticking block entity interface: the block registers a ticker (see the S5 phase notes,
 * {@code EntityBlock#getTicker} with {@code BaseEntityBlock#createTickerHelper}) whose static
 * {@code tick(Level, BlockPos, BlockState, T)} method calls {@link #clientTick()} on the client and
 * {@link #serverTick(ServerLevel)} on the server. Both drive the same 1.12 action list, so an action that
 * asked for the client side still runs there.
 * <p>
 * Differences to the 1.12 class:
 * <ul>
 *     <li>{@code world}/{@code pos} are {@code level}/{@code worldPosition} with the accessors
 *     {@link #getLevel()} / {@link #getBlockPos()}, {@code getBlockState()} already exists on
 *     {@link BlockEntity} and is not overridden,</li>
 *     <li>{@code shouldRefresh} is gone, 26.3 keeps the block entity when only the block state of the same
 *     block changes,</li>
 *     <li>{@code markDirty} is {@link #setChanged()} which also queues the block entity description packet
 *     for the client, replacing 1.12's {@code getUpdatePacket}/{@code getUpdateTag}/{@code onDataPacket}
 *     trio (porting-guide.md §2.3).</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin) on 1/9/2017.
 */
public class TileMachine extends BlockEntity
{
    /** Tick counter, starts at -1 so the first tick is 0 as in 1.12 */
    protected int ticks = -1;

    protected final List<ITick> tickActions = new ArrayList<>();

    public TileMachine(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    /**
     * Server side half of the 1.12 {@code ITickable#update()}, called by the block ticker.
     *
     * @param level server level, only handed over so the signature matches the other machines
     */
    public void serverTick(ServerLevel level)
    {
        advanceTick(true);
    }

    /** Client side half of the 1.12 {@code ITickable#update()}, called by the block ticker */
    public void clientTick()
    {
        advanceTick(false);
    }

    private void advanceTick(boolean isServer)
    {
        //Increase tick
        ticks++;
        if (ticks >= Integer.MAX_VALUE - 1)
        {
            ticks = 0;
        }

        tickActions.forEach(action -> action.update(ticks, isServer));
    }

    /**
     * 1.12 {@code markDirty()} on the tile entity. 26.3 {@link #setChanged()} keeps the same "mark for save" meaning
     * and additionally queues the description packet while on the server, so the client copy of the machine
     * data follows the server (1.12 relied on its own update packet for that).
     */
    @Override
    public void setChanged()
    {
        super.setChanged();
        if (isServer())
        {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isServer()
    {
        return level != null && !level.isClientSide();
    }

    public boolean isClient()
    {
        return level != null && level.isClientSide();
    }

    /**
     * 1.12 {@code TileMachine#getRotation()}, reads the facing off the block state. 26.3 block rotation is
     * {@link BlockICBM#ROTATION_PROP} and defaults to {@link Direction#NORTH} like 1.12.
     */
    @Deprecated
    public Direction getRotation()
    {
        final BlockState state = getBlockState();
        if (state.hasProperty(BlockICBM.ROTATION_PROP))
        {
            return state.getValue(BlockICBM.ROTATION_PROP);
        }
        return Direction.NORTH;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // description packet (1.12 getUpdatePacket/getUpdateTag/onDataPacket)
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** 1.12 {@code new SPacketUpdateTileEntity(pos, 0, getUpdateTag())} */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 1.12 {@code writeToNBT} with a new raw NBT tag */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        return this.saveWithoutMetadata(provider);
    }
}
