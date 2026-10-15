package icbm.classic.test.items;

import icbm.classic.ICBMConstants;
import icbm.classic.content.entity.EntityBombCart;
import icbm.classic.content.entity.EntityGrenade;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

/** Redstone-driven regression tests for the S5.09 dispenser behavior registrations. */
public class DispenserBehaviorGameTest
{
    private static final BlockPos DISPENSER = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 40)
    public void redstoneDispenserLaunchesCondensedGrenade(GameTestHelper helper)
    {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        final DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        final Item grenadeItem = ModItems.EXPLOSIVE_GRENADES.get(ICBMConstants.id("condensed"));
        final ItemStack grenadeStack = new ItemStack(grenadeItem);
        dispenser.setItem(0, grenadeStack);
        helper.pulseRedstone(DISPENSER.west(), 2L);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!helper.getEntities(ModEntities.GRENADE).isEmpty(),
                        "a powered dispenser did not launch a grenade"))
                .thenExecute(() ->
                {
                    final EntityGrenade grenade = helper.getEntities(ModEntities.GRENADE).get(0);
                    helper.assertTrue(grenade.explosive.toStack().getItem() == grenadeItem,
                            "the launched grenade should carry the dispenser's condensed grenade item");
                    helper.assertTrue(grenade.getDeltaMovement().x > 0.0D,
                            "the launched grenade should move in the dispenser's east-facing direction");
                    helper.assertTrue(dispenser.getItem(0).isEmpty(),
                            "the dispenser should consume one grenade from its inventory");
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 40)
    public void redstoneDispenserPlacesBombCartOnRail(GameTestHelper helper)
    {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        helper.setBlock(DISPENSER.east(), Blocks.RAIL);
        final DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        final Item cartItem = ModItems.EXPLOSIVE_BOMB_CARTS.get(ICBMConstants.id("condensed"));
        final ItemStack cartStack = new ItemStack(cartItem);
        dispenser.setItem(0, cartStack);
        helper.pulseRedstone(DISPENSER.west(), 2L);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!helper.getEntities(ModEntities.BOMB_CART).isEmpty(),
                        "a powered dispenser facing a rail did not place a bomb cart"))
                .thenExecute(() ->
                {
                    final EntityBombCart cart = helper.getEntities(ModEntities.BOMB_CART).get(0);
                    final BlockPos expectedRail = helper.absolutePos(DISPENSER.east());
                    helper.assertTrue(cart.explosive.toStack().getItem() == cartItem,
                            "the placed cart should carry the dispenser's condensed bomb-cart item");
                    helper.assertTrue(cart.blockPosition().equals(expectedRail),
                            "the cart should be placed on the rail in front of the dispenser; expected "
                                    + expectedRail + " but got " + cart.blockPosition());
                    helper.assertTrue(dispenser.getItem(0).isEmpty(),
                            "the dispenser should consume one bomb cart from its inventory");
                })
                .thenSucceed();
    }
}
