package icbm.classic.lib;

import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Series of helper classes for dealing with any kind of inventory.
 * <p>
 * 1.12 version used Forge's {@code IItemHandler} capability to find inventories, 26.3 uses the vanilla
 * {@link Container} interface (porting-guide.md §2.6). The item stack comparisons moved from NBT to data
 * components: {@code isItemEqual + NBT} is {@link ItemStack#isSameItemSameComponents} and the "NBT only"
 * comparison is the component patch of the stack.
 *
 * @author Calclavia, DarkCow(aka Darkguardsman, Robin)
 */
public class InventoryUtility
{
    /**
     * Attempts to drop the block at the location as an item. Does not check what the block is
     * and can fail if the block doesn't contain items.
     *
     * @param world   level to drop in
     * @param pos     position of the block
     * @param destroy - will break the block
     */
    public static List<ItemEntity> dropBlockAsItem(Level world, BlockPos pos, boolean destroy)
    {
        final List<ItemEntity> entities = new ArrayList<>();
        if (!world.isClientSide() && world instanceof ServerLevel serverLevel)
        {
            final BlockState state = world.getBlockState(pos);

            // 1.12: state.getBlock().getDrops(world, pos, state, 0)
            if (!state.isAir())
            {
                for (ItemStack itemStack : Block.getDrops(state, serverLevel, pos, world.getBlockEntity(pos)))
                {
                    final ItemEntity entityItem = dropItemStack(world, new Pos(pos.getX(), pos.getY(), pos.getZ()), itemStack, 10);
                    if (entityItem != null)
                    {
                        entities.add(entityItem);
                    }
                }
            }
            if (destroy)
            {
                world.removeBlock(pos, false); //1.12: world.setBlockToAir(pos)
            }
        }
        return entities;
    }

    public static ItemEntity dropItemStack(Level world, Pos position, ItemStack itemStack, int delay)
    {
        return dropItemStack(world, position, itemStack, delay, 0f);
    }

    public static ItemEntity dropItemStack(Level world, Pos position, ItemStack itemStack, int delay, float randomAmount)
    {
        return dropItemStack(world, position.x(), position.y(), position.z(), itemStack, delay, randomAmount);
    }

    /**
     * Drops every item of the inventory held by the block at the position, emptying it in the process.
     * <p>
     * 1.12 asked the block entity for the {@code IItemHandler} capability and dropped every slot by hand;
     * 26.3 block entity inventories are {@link Container}s and {@link Containers#dropContents(Level, BlockPos, Container)}
     * performs the same drop-and-clear.
     *
     * @param world level to drop in
     * @param pos   position of the block entity
     */
    public static void dropInventory(Level world, BlockPos pos)
    {
        if (world.getBlockEntity(pos) instanceof Container container)
        {
            Containers.dropContents(world, pos, container);
        }
    }

    public static ItemEntity dropItemStack(Level world, double x, double y, double z, ItemStack itemStack, int delay, float randomAmount)
    {
        //TODO fire drop events if not already done by forge
        //TODO add banned item filtering, prevent creative mode only items from being dropped
        if (world != null && !world.isClientSide() && !itemStack.isEmpty())
        {
            double randomX = 0;
            double randomY = 0;
            double randomZ = 0;

            if (randomAmount > 0)
            {
                randomX = world.getRandom().nextFloat() * randomAmount + (1.0F - randomAmount) * 0.5D;
                randomY = world.getRandom().nextFloat() * randomAmount + (1.0F - randomAmount) * 0.5D;
                randomZ = world.getRandom().nextFloat() * randomAmount + (1.0F - randomAmount) * 0.5D;
            }

            final ItemEntity entityitem = new ItemEntity(world, x + randomX, y + randomY, z + randomZ, itemStack);

            //1.12's EntityItem constructor left the motion at zero and only randomAmount jittered the position;
            //the 26.3 constructor throws the stack a little, so the motion is reset to match the 1.12 drop.
            entityitem.setDeltaMovement(0, 0, 0);

            //1.12 copied the tag compound of the dropped stack, 26.3 item stacks carry their components with
            //them and ItemEntity keeps the stack as given, so no copy is needed.
            entityitem.setPickUpDelay(delay); //1.12: setPickupDelay(delay)
            world.addFreshEntity(entityitem); //1.12: world.spawnEntity(entityitem)
            return entityitem;
        }
        return null;
    }

    /**
     * Checks if the two item stacks match each other exactly. Item, damage, components, stacksize
     *
     * @param stackA - item stack a
     * @param stackB - item stack a
     * @return true if they match
     */
    public static boolean stacksMatchExact(ItemStack stackA, ItemStack stackB)
    {
        if (!stackA.isEmpty() && !stackB.isEmpty())
        {
            //1.12: stackA.isItemEqual(stackB) && doesStackNBTMatch(stackA, stackB) && stackA.getCount() == stackB.getCount()
            return ItemStack.isSameItemSameComponents(stackA, stackB) && stackA.getCount() == stackB.getCount();
        }
        return stackA.isEmpty() && stackB.isEmpty();
    }

    /**
     * Checks if two item stacks match each other using item, damage and components to compare
     *
     * @param stackA - item stack a
     * @param stackB - item stack a
     * @return true if they match
     */
    public static boolean stacksMatch(ItemStack stackA, ItemStack stackB)
    {
        if (!stackA.isEmpty() && !stackB.isEmpty())
        {
            //1.12: stackA.isItemEqual(stackB) && doesStackNBTMatch(stackA, stackB)
            return ItemStack.isSameItemSameComponents(stackA, stackB);
        }
        return stackA.isEmpty() && stackB.isEmpty();
    }


    /**
     * Checks if two itemStack's data matches exactly. Does not check item, stacksize, or damage value.
     *
     * @param stackA - item stack a, can't be null
     * @param stackB - item stack a, can't be null
     * @return true if the stacks carry the same data
     */
    public static boolean doesStackNBTMatch(ItemStack stackA, ItemStack stackB)
    {
        //1.12 compared the tag compound of both stacks; 26.3 has no free form tag on the stack, the closest
        //counterpart of "the data stored on the stack" is the component patch (non default components)
        return stackA.getComponentsPatch().equals(stackB.getComponentsPatch());
    }

    /** 1.12 {@code doTagsMatch} of two raw NBT tags, kept for raw tags (saved data, payloads) */
    public static boolean doTagsMatch(final CompoundTag tag, final CompoundTag tag2)
    {
        final boolean firstTagEmpty = tag == null || tag.isEmpty();
        final boolean firstTagEmpty2 = tag2 == null || tag2.isEmpty();
        if (firstTagEmpty && firstTagEmpty2)
        {
            return true;
        }
        else if (!firstTagEmpty && firstTagEmpty2)
        {
            return false;
        }
        else if (firstTagEmpty)
        {
            return false;
        }
        return tag.equals(tag2);
    }
}
