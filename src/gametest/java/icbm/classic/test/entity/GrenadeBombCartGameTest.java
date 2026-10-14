package icbm.classic.test.entity;

import icbm.classic.ICBMConstants;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.entity.EntityBombCart;
import icbm.classic.content.entity.EntityGrenade;
import icbm.classic.content.items.ItemBombCart;
import icbm.classic.content.items.ItemGrenade;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Covers S4.05: the grenade ({@code icbmclassic:grenade}) and the bomb cart
 * ({@code icbmclassic:bomb_cart}).
 *
 * <ul>
 *     <li>the flat grenade and bomb cart items are the real behaviour classes and keep the 1.12 stack sizes,</li>
 *     <li>a spawned grenade runs the blast of its explosive when the fuse of the grenade content registry runs
 *     out (1.12 {@code EntityGrenade#tickFuse} → {@code CapabilityExplosiveEntity#doExplosion}),</li>
 *     <li>a bomb cart sitting on a powered activator rail primes (1.12 {@code ignite()} through
 *     {@code activateMinecart}) and runs the blast of its explosive when the fuse runs out.</li>
 * </ul>
 *
 * The condensed grenade/bomb cart blast is configured with a 6 block radius
 * ({@code ConfigBlast.condensed.energyScale}), which is wider than the 8x8x8 test structure, so both tests
 * reserve extra padding around their structure to keep the explosion clear of the neighbouring tests. The
 * fuse of the content registry is shortened through {@code ConfigBlast.FUSE_TIMES} (and restored) so the tests
 * do not have to wait the configured 100 ticks.
 */
public class GrenadeBombCartGameTest
{
    /** Center of the test structure, 1.12 style test coordinates */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);

    private static ItemStack stackOf(Item item)
    {
        if (item == null)
        {
            throw new IllegalStateException("Missing flat explosive item");
        }
        return new ItemStack(item);
    }

    /** {@code icbmclassic:<explosive>_grenade} */
    private static ItemStack grenade(String explosiveId)
    {
        return stackOf(ModItems.EXPLOSIVE_GRENADES.get(ICBMConstants.id(explosiveId)));
    }

    /** {@code icbmclassic:<explosive>_bomb_cart} */
    private static ItemStack bombCart(String explosiveId)
    {
        return stackOf(ModItems.EXPLOSIVE_BOMB_CARTS.get(ICBMConstants.id(explosiveId)));
    }

    /** Counts the stone blocks destroyed in the 3x3 floor around the blast center. */
    private static int destroyedFloor(GameTestHelper helper)
    {
        int destroyed = 0;
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                if (helper.getBlockState(CENTER.offset(x, -1, z)).isAir())
                {
                    destroyed++;
                }
            }
        }
        return destroyed;
    }

    /** Places the 3x3 stone floor the explosions are aimed at. */
    private static void buildStoneFloor(GameTestHelper helper)
    {
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.STONE);
            }
        }
    }

    /**
     * The flat grenade/bomb cart items are the S4.05 behaviour classes and keep the 1.12 stack sizes
     * ({@code ItemGrenade#setMaxStackSize(16)}, {@code ItemBombCart#setMaxStackSize(3)}).
     */
    @GameTest(maxTicks = 40)
    public void flatItemsAreTheBehaviourClasses(GameTestHelper helper)
    {
        final ItemStack grenade = grenade("condensed");
        final ItemStack cart = bombCart("condensed");

        helper.assertTrue(grenade.getItem() instanceof ItemGrenade,
                "the flat grenade item should be an ItemGrenade but is " + grenade.getItem());
        helper.assertTrue(cart.getItem() instanceof ItemBombCart,
                "the flat bomb cart item should be an ItemBombCart but is " + cart.getItem());
        helper.assertTrue(grenade.getMaxStackSize() == ItemGrenade.MAX_STACK_SIZE,
                "the grenade stack size should be " + ItemGrenade.MAX_STACK_SIZE + " but is " + grenade.getMaxStackSize());
        helper.assertTrue(cart.getMaxStackSize() == ItemBombCart.MAX_STACK_SIZE,
                "the bomb cart stack size should be " + ItemBombCart.MAX_STACK_SIZE + " but is " + cart.getMaxStackSize());

        helper.succeed();
    }

    /**
     * 1.12 {@code EntityGrenade}: the fuse of the grenade content registry runs out and the grenade runs the
     * blast of its explosive ({@code condensed}). The fuse is forced to 1 tick through
     * {@code ConfigBlast.FUSE_TIMES.GRENADES.CONVENTIONAL}.
     */
    @GameTest(maxTicks = 200, padding = 16)
    public void condensedGrenadeExplodesOnFuse(GameTestHelper helper)
    {
        buildStoneFloor(helper);
        helper.assertTrue(destroyedFloor(helper) == 0, "the stone floor should start intact");

        final int previousFuse = ConfigBlast.FUSE_TIMES.GRENADES.CONVENTIONAL;
        ConfigBlast.FUSE_TIMES.GRENADES.CONVENTIONAL = 1;

        final BlockPos spawn = helper.absolutePos(CENTER);
        final EntityGrenade grenade = new EntityGrenade(helper.getLevel())
                .setItemStack(grenade("condensed"));
        grenade.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(grenade);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertEntityNotPresent(ModEntities.GRENADE))
                .thenExecute(() ->
                {
                    ConfigBlast.FUSE_TIMES.GRENADES.CONVENTIONAL = previousFuse;
                    helper.assertTrue(destroyedFloor(helper) > 0,
                            "the grenade should have run the condensed blast when its fuse ran out, "
                                    + "but none of the stone blocks under it was destroyed");
                })
                .thenSucceed();
    }

    /**
     * 1.12 {@code EntityBombCart}: a cart on a powered activator rail is primed by the block
     * ({@code activateMinecart} → {@code ignite()}), the fuse of the bomb cart content registry runs out and
     * the ICBM blast runs instead of the vanilla TNT explosion. The fuse is forced to 5 ticks through
     * {@code ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONDENSED}.
     */
    @GameTest(maxTicks = 200, padding = 16)
    public void bombCartOnActivatorRailExplodes(GameTestHelper helper)
    {
        buildStoneFloor(helper);
        // 1.12 EntityBombCart was placed on rails; the activator rail (powered by the redstone block) is what
        // calls AbstractMinecart#activateMinecart(..., true) -> MinecartTNT#primeFuse
        helper.setBlock(CENTER, Blocks.ACTIVATOR_RAIL.defaultBlockState());
        helper.setBlock(CENTER.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.ACTIVATOR_RAIL),
                "the test should have placed an activator rail at the center");

        final int previousFuse = ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONDENSED;
        ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONDENSED = 5;

        final BlockPos spawn = helper.absolutePos(CENTER);
        final EntityBombCart cart = new EntityBombCart(helper.getLevel(),
                spawn.getX() + 0.5D, spawn.getY() + 0.0625D, spawn.getZ() + 0.5D, bombCart("condensed"));
        helper.getLevel().addFreshEntity(cart);

        helper.startSequence()
                // the powered activator rail primes the cart (1.12 ignite())
                .thenWaitUntil(() -> helper.assertTrue(cart.isPrimed(),
                        "the powered activator rail should have primed the bomb cart but its fuse is " + cart.getFuse()))
                .thenWaitUntil(() -> helper.assertEntityNotPresent(ModEntities.BOMB_CART))
                .thenExecute(() ->
                {
                    ConfigBlast.FUSE_TIMES.BOMB_CARTS.CONDENSED = previousFuse;
                    helper.assertTrue(destroyedFloor(helper) > 0,
                            "the bomb cart should have run the condensed blast when its fuse ran out, "
                                    + "but none of the stone blocks under the rail was destroyed");
                })
                .thenSucceed();
    }

    /**
     * A bomb cart whose health ran out must run its blast and drop its flat item, the 26.3 counterpart of
     * 1.12 {@code killMinecart(DamageSource)} (the card allows this observable assertion when the minecart
     * damage path is hard to drive from a test).
     */
    @GameTest(maxTicks = 80, padding = 16)
    public void bombCartBlowsUpWhenKilled(GameTestHelper helper)
    {
        buildStoneFloor(helper);

        final BlockPos spawn = helper.absolutePos(CENTER);
        final EntityBombCart cart = new EntityBombCart(helper.getLevel(),
                spawn.getX() + 0.5D, spawn.getY() + 0.0625D, spawn.getZ() + 0.5D, bombCart("condensed"));
        helper.getLevel().addFreshEntity(cart);

        // 1.12 killMinecart: fire damage discards the cart and runs explodeCart
        cart.destroy(helper.getLevel(), helper.getLevel().damageSources().onFire());

        helper.assertTrue(cart.isRemoved(), "the killed bomb cart should have been removed");
        helper.assertTrue(destroyedFloor(helper) > 0,
                "the killed bomb cart should have run the condensed blast, "
                        + "but none of the stone blocks under it was destroyed");
        helper.succeed();
    }
}
