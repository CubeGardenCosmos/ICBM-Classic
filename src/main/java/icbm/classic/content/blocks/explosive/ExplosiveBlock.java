package icbm.classic.content.blocks.explosive;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.BlockActivateFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;
import java.util.function.BiConsumer;

/**
 * Explosive block. 1.12 had one block with an explosive metadata value and a block entity holding the
 * explosive capability; ADR 0002 flattens it into one block per explosive (23), all sharing the
 * {@link ExplosiveBlockEntity}.
 * <p>
 * Port of {@code BlockExplosive} ({@code icbmclassic:explosives}) plus the parts of {@code BlockICBM} it
 * inherited. Behaviour kept from 1.12:
 * <ul>
 *     <li>the placement rotation ({@code BlockICBM.ROTATION_PROP}, {@code PropertyDirection.create("rotation")}
 *     with all six directions) is {@link BlockStateProperties#FACING}; 1.12's
 *     {@code getStateForPlacement} used the <b>clicked face</b> ({@code facing}), not
 *     {@code placer.getHorizontalFacing()}, and so does {@link BlockPlaceContext#getClickedFace()}. The
 *     default value of both properties is {@code NORTH}, so an unplaced state matches the 1.12 default
 *     state,</li>
 *     <li>the placement trigger and the fire/lava neighbour scan of {@code onBlockPlacedBy}
 *     ({@link #setPlacedBy}),</li>
 *     <li>the redstone trigger of {@code neighborChanged} ({@link #neighborChanged}),</li>
 *     <li>the chain trigger of {@code onBlockExploded} ({@link #onExplosionHit}) and the "explosions never
 *     drop this block" of {@code canDropFromExplosion} ({@link #dropFromExplosion}),</li>
 *     <li>flint and steel / fire charge of {@code onBlockActivated} ({@link #useItemOn}),</li>
 *     <li>the burning arrow trigger of {@code onEntityCollidedWithBlock} ({@link #onProjectileHit}),</li>
 *     <li>the detonation itself, 1.12 {@code TileEntityExplosive#trigger} in {@link ExplosiveBlockEntity}.</li>
 * </ul>
 * Parts of the 1.12 class that are flattened away: {@code EX_PROP}/{@code getActualState}/
 * {@code getSubBlocks} (one block per explosive), {@code damageDropped}/{@code getPickBlock} (no metadata,
 * the block item is the explosive), {@code createNewTileEntity} (26.3 creates the block entity in
 * {@link #newBlockEntity}) and the {@code isNormalCube}/{@code isTopSolid}/{@code isOpaqueCube}/
 * {@code getRenderType} block overrides (26.3 cube defaults plus the {@code block/*} model definitions the
 * datagen provider emits).
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 1/6/2019.
 */
public class ExplosiveBlock extends Block implements EntityBlock
{
    /**
     * 1.12 {@code BlockICBM.ROTATION_PROP} ({@code PropertyDirection.create("rotation")}, all six directions,
     * default NORTH). Both 1.12 and 26.3 store the face the block was placed against.
     */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final Identifier explosiveId;

    public ExplosiveBlock(Identifier explosiveId, BlockBehaviour.Properties properties)
    {
        super(properties);
        this.explosiveId = explosiveId;
        // 1.12 PropertyDirection defaults to NORTH, BlockStateProperties.FACING also starts at NORTH; spelled
        // out so the default state of an unplaced explosive block stays NORTH as in 1.12
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** Explosive this block represents, see {@code docs/architecture/registry-ids.md} §1. */
    public Identifier getExplosiveId()
    {
        return explosiveId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // block state
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** 1.12 {@code createBlockState} added {@code ROTATION_PROP}; the explosive property is the block now. */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    /**
     * 1.12 {@code getStateForPlacement} started from the default state, set the rotation property to the
     * clicked face and read the explosive off the item metadata. The metadata branch is gone with the flat
     * blocks: the block itself is the explosive, so an unknown explosive can not be placed (the 1.12
     * "fallback to id 0 and setItemDamage(0)" branch has no counterpart).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // placement
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // 1.12 BlockExplosive#onBlockAdded was left empty on purpose (kept as a comment, it is the 26.3 onPlace):
    //   // Can't be implemented as we lack blockState for explosive at this point
    //   //super.onBlockAdded(world, pos, state);
    //   //if (!world.isRemote && world.isBlockPowered(pos)) { BlockExplosive.triggerExplosive(world, pos, false); }
    // The powered check moved to onBlockPlacedBy, which runs once the block entity exists, so 26.3 has
    // nothing to do in onPlace either.


    /**
     * Called when the block is placed in the world, 1.12 {@code onBlockPlacedBy}.
     * <p>
     * 1.12 copied the placed item stack into the block entity explosive capability here; 26.3 copies the
     * {@code icbmclassic:explosive_settings} component in {@code BlockItem#place}
     * ({@link BlockEntity#applyImplicitComponents}) before this hook runs, so the trigger checks are all
     * that is left.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ExplosiveBlockEntity explosive)
        {
            if (level.hasNeighborSignal(pos)) //1.12: world.isBlockPowered(pos)
            {
                triggerExplosive(level, pos, false);
            }

            // Check to see if there is fire nearby.
            // If so, then detonate.
            for (Direction rotation : Direction.Plane.HORIZONTAL) //1.12: iterated the four horizontal facings
            {
                final BlockPos position = pos.relative(rotation);
                //1.12: blockId == Blocks.FIRE || blockId == Blocks.FLOWING_LAVA || blockId == Blocks.LAVA
                // 26.3 has one lava block for both fluid states (flowing lava is a fluid state of Blocks.LAVA)
                final Block blockId = level.getBlockState(position).getBlock();

                if (blockId == Blocks.FIRE || blockId == Blocks.LAVA)
                {
                    triggerExplosive(level, pos, true);
                    break;
                }
            }

            if (placer != null)
            {
                //TODO turn into event and logger
                ICBMClassic.logger().info("ICBMClassic>>BlockExplosive#onBlockPlacedBy: " + placer.getName().getString()
                        + " placed " + explosive.getExplosiveId() + " in: " + pos);
            }
        }
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed
     * (coordinates passed are their own) Args: x, y, z, neighbor block
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston)
    {
        if (level.hasNeighborSignal(pos)) //1.12: world.isBlockPowered(pos)
        {
            triggerExplosive(level, pos, false);
        }
    }

    /*
     * Called to detonate the TNT. Args: world, x, y, z, metaData, CauseOfExplosion (0, intentional,
     * 1, exploded, 2 burned)
     */
    public static void triggerExplosive(Level level, BlockPos pos, boolean setFire)
    {
        if (!level.isClientSide()) //1.12: !world.isRemote
        {
            final BlockEntity tileEntity = level.getBlockEntity(pos);

            if (tileEntity instanceof ExplosiveBlockEntity explosive)
            {
                explosive.trigger(setFire);
            }
        }
    }

    /**
     * Called upon the block being destroyed by an explosion, 1.12 {@code onBlockExploded}
     * ({@code triggerExplosive} then {@code super.onBlockExploded}).
     * <p>
     * {@link #onExplosionHit} is the hook that runs at the same point: {@code ServerExplosion#interactWithBlocks}
     * calls {@code BlockState#onExplosionHit} <b>before</b> the block is replaced with air, which is when the
     * 1.12 hook ran and the block entity holding the explosive still exists. The nominal
     * {@link Block#wasExploded} hook is called from {@code BlockBehaviour#onExplosionHit} <b>after</b>
     * {@code Level#setBlockAndUpdate(pos, AIR)} (the block entity is removed by then) and has no other caller
     * in 26.3, so it can not carry the trigger.
     */
    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit)
    {
        triggerExplosive(level, pos, false);
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    /**
     * 1.12 {@code canDropFromExplosion} returned false: an explosive block that is destroyed by an explosion
     * spawns the primed explosive instead of dropping itself.
     */
    @Override
    public boolean dropFromExplosion(Explosion explosion)
    {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // interaction
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Called upon block activation (left or right click on the block). 1.12 {@code onBlockActivated}, the
     * flint and steel / fire charge branch.
     * <p>
     * The 1.12 {@code super.onBlockActivated} fallback becomes {@code super.useItemOn}, which answers
     * {@link InteractionResult#TRY_WITH_EMPTY_HAND} so the vanilla interaction chain continues into
     * {@link #useWithoutItem} with the same hand ({@code ServerPlayerGameMode#useItemOn}).
     */
    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        if (!itemStack.isEmpty() && (itemStack.is(Items.FLINT_AND_STEEL) || itemStack.is(Items.FIRE_CHARGE)))
        {
            triggerExplosive(level, pos, false);

            if (itemStack.is(Items.FLINT_AND_STEEL))
            {
                //1.12: itemstack.damageItem(1, player); 26.3 damages on the server only and keeps the 1.12
                // creative mode exemption (Item#hurtAndBreak skips players with infinite materials)
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
            else
            {
                //1.12: if (!player.capabilities.isCreativeMode) itemstack.shrink(1); 26.3 ItemStack#consume
                // does the same creative check
                itemStack.consume(1, player);
            }

            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    /**
     * Right click with an empty hand (or with an item that did not consume the interaction), 1.12 had no
     * counterpart: the 1.12 {@code onBlockActivated} fell through to {@code super.onBlockActivated} without
     * ever invoking the registered activation callback (nothing in 1.12 called
     * {@code BlockActivateFunction#onActivate}, the map of {@code ExBlockContentReg} was write only).
     * <p>
     * 26.3 invokes it so the ender explosive can be given a target with a GPS item
     * ({@code ICBMClassicAPI.EX_BLOCK_REGISTRY}, see {@code ExplosiveInit#enderBlockCoordSet}). The hand is
     * the main hand: {@code ServerPlayerGameMode#useItemOn} only falls through to {@code useWithoutItem} for
     * {@code MAIN_HAND} interactions.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                              BlockHitResult hitResult)
    {
        final BlockActivateFunction activation = ICBMClassicAPI.EX_BLOCK_REGISTRY.getActivationListener(getExplosiveId());
        if (activation != null && !level.isClientSide()
                && activation.onActivate(level, pos, player, InteractionHand.MAIN_HAND, hitResult.getDirection(),
                (float) (hitResult.getLocation().x - pos.getX()),
                (float) (hitResult.getLocation().y - pos.getY()),
                (float) (hitResult.getLocation().z - pos.getZ())))
        {
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    /**
     * 1.12 {@code onEntityCollidedWithBlock}: a burning arrow ignites the explosive. 26.3 splits the entity
     * collision hook into projectiles ({@code onProjectileHit}) and living entities
     * ({@code entityInside}), only the projectile one carried the 1.12 behaviour.
     */
    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile)
    {
        if (!level.isClientSide() && projectile instanceof AbstractArrow //1.12: entityIn instanceof EntityArrow
                && projectile.isOnFire()) //1.12: entityarrow.isBurning()
        {
            triggerExplosive(level, hitResult.getBlockPos(), false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // block entity
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** 1.12 {@code createNewTileEntity} returned a new {@code TileEntityExplosive} for every metadata. */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ExplosiveBlockEntity(pos, state);
    }
}
