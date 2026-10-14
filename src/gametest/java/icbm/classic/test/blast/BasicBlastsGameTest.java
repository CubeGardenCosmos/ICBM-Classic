package icbm.classic.test.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.IActionFieldReceiver;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Runs every blast ported by S3.04 (the "normal" explosives) and asserts the effect the 1.12 code produced.
 * <ul>
 *     <li>condensed / breaching / sonic → stone blocks are destroyed,</li>
 *     <li>incendiary → fire appears,</li>
 *     <li>shrapnel / anvil / fragmentation → fragments are spawned,</li>
 *     <li>repulsive / attractive → a zombie is pushed away from / pulled towards the blast,</li>
 *     <li>exothermic / endothermic → blocks are mutated (water → lava/ice),</li>
 *     <li>antigravitational → flying blocks are spawned,</li>
 *     <li>mutation → a villager is converted,</li>
 *     <li>primed_explosive → the primed block entity runs its explosive (carried over from S3.03).</li>
 * </ul>
 * Blasts are triggered through {@link ICBMClassicAPI#EXPLOSIVE_REGISTRY} so the registered factories are what
 * the game uses, and the size is forced to 3 to keep the tests fast (step card S3.04).
 */
public class BasicBlastsGameTest
{
    /** Center of the blast, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Size every blast is forced to, keeps the affected area inside the test structure */
    private static final int TEST_SIZE = 3;

    private static final Predicate<BlockState> IS_AIR = BlockState::isAir;

    /**
     * Triggers an explosive by id through the registry, mirroring how the game creates a blast.
     *
     * @param helper      test helper
     * @param explosiveId explosive id inside the mod namespace
     * @return the action that was started
     */
    private static IAction triggerBlast(GameTestHelper helper, String explosiveId)
    {
        return triggerBlast(helper, explosiveId, Direction.UP);
    }

    private static IAction triggerBlast(GameTestHelper helper, String explosiveId, Direction direction)
    {
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id(explosiveId), false);
        final BlockPos pos = helper.absolutePos(CENTER);
        final Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final IActionSource source = new ActionSource(helper.getLevel(), center, new CauseByTimer("test", 0));

        final IAction action = data.create(helper.getLevel(), center.x, center.y, center.z, source, null);
        if (action instanceof IBlastInit init)
        {
            init.setBlastSize(TEST_SIZE);
        }
        if (action instanceof IActionFieldReceiver receiver)
        {
            receiver.setValue(ActionFields.HOST_DIRECTION, direction);
        }
        action.doAction();
        return action;
    }

    /** Places a solid stone cube of radius 1 around the blast center */
    private static void buildStoneCube(GameTestHelper helper)
    {
        for (int x = -1; x <= 1; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -1; z <= 1; z++)
                {
                    helper.setBlock(CENTER.offset(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    private static List<BlockPos> blocksAround(GameTestHelper helper, int radius)
    {
        final List<BlockPos> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++)
        {
            for (int y = -radius; y <= radius; y++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    final BlockPos pos = CENTER.offset(x, y, z);
                    if (pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                            && pos.getX() < 8 && pos.getY() < 8 && pos.getZ() < 8)
                    {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    private static int countBlocks(GameTestHelper helper, int radius, Predicate<BlockState> test)
    {
        int count = 0;
        for (BlockPos pos : blocksAround(helper, radius))
        {
            if (test.test(helper.getBlockState(pos)))
            {
                count++;
            }
        }
        return count;
    }

    /** Waits until at least one block around the center matches the predicate */
    private static void succeedWhenBlock(GameTestHelper helper, Predicate<BlockState> test, String message)
    {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(countBlocks(helper, TEST_SIZE + 1, test) > 0, message))
                .thenSucceed();
    }

    @GameTest(maxTicks = 60)
    public void condensedBlastDestroysBlocks(GameTestHelper helper)
    {
        buildStoneCube(helper);
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0, "test cube should start solid");

        triggerBlast(helper, "condensed");

        helper.assertTrue(countBlocks(helper, 1, IS_AIR) > 0,
                "condensed blast did not destroy any of the stone blocks");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void breachingBlastDestroysBlocks(GameTestHelper helper)
    {
        buildStoneCube(helper);

        triggerBlast(helper, "breaching");

        helper.assertTrue(countBlocks(helper, 1, IS_AIR) > 0,
                "breaching blast did not destroy any of the stone blocks");
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void sonicBlastDestroysBlocks(GameTestHelper helper)
    {
        buildStoneCube(helper);

        triggerBlast(helper, "sonic");

        // sonic is threaded, the blocks go away over several ticks
        succeedWhenBlock(helper, IS_AIR, "sonic blast did not destroy any of the stone blocks");
    }

    @GameTest(maxTicks = 60)
    public void incendiaryBlastPlacesFire(GameTestHelper helper)
    {
        // flammable floor and walls, fire is placed on top of solid blocks
        for (int x = -2; x <= 2; x++)
        {
            for (int z = -2; z <= 2; z++)
            {
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.OAK_PLANKS);
            }
        }
        helper.setBlock(CENTER, Blocks.OAK_PLANKS);

        triggerBlast(helper, "incendiary");

        helper.assertTrue(countBlocks(helper, TEST_SIZE, state -> state.is(Blocks.FIRE)) > 0,
                "incendiary blast did not place any fire");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void shrapnelBlastSpawnsFragments(GameTestHelper helper)
    {
        triggerBlast(helper, "shrapnel");

        helper.assertTrue(!helper.getEntities(ModEntities.FRAGMENT).isEmpty(),
                "shrapnel blast did not spawn any fragments");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void anvilBlastSpawnsFragments(GameTestHelper helper)
    {
        triggerBlast(helper, "anvil");

        helper.assertTrue(!helper.getEntities(ModEntities.FRAGMENT).isEmpty(),
                "anvil blast did not spawn any fragments");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void fragmentationBlastSpawnsFragments(GameTestHelper helper)
    {
        triggerBlast(helper, "fragmentation");

        helper.assertTrue(!helper.getEntities(ModEntities.FRAGMENT).isEmpty(),
                "fragmentation blast did not spawn any fragments");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void repulsiveBlastPushesZombieAway(GameTestHelper helper)
    {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        final Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(6.5D, CENTER.getY() + 1.0D, 4.5D));
        final Vec3 center = Vec3.atCenterOf(helper.absolutePos(CENTER));
        final double startX = zombie.getX();

        triggerBlast(helper, "repulsive");

        // the zombie sits on the +x side of the blast, repulsive has to move it further out
        helper.assertTrue(zombie.getDeltaMovement().x > 0.0D,
                "repulsive blast did not push the zombie outwards, impulse was " + zombie.getDeltaMovement());

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(zombie.getX() - startX > 1.0D,
                        "repulsive blast did not move the zombie away from the blast, it moved "
                                + (zombie.getX() - startX) + " blocks in x from " + startX
                                + " (distance to blast " + zombie.position().distanceTo(center) + ")"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void attractiveBlastPullsZombieIn(GameTestHelper helper)
    {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        final Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(7.0D, CENTER.getY() + 1.0D, 4.5D));
        final Vec3 center = Vec3.atCenterOf(helper.absolutePos(CENTER));
        final double startX = zombie.getX();

        triggerBlast(helper, "attractive");

        // attractive pulls the zombie back towards the blast center, it sits on the +x side
        helper.assertTrue(zombie.getDeltaMovement().x < 0.0D,
                "attractive blast did not pull the zombie inwards, impulse was " + zombie.getDeltaMovement());

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(startX - zombie.getX() > 1.0D,
                        "attractive blast did not move the zombie towards the blast, it moved "
                                + (startX - zombie.getX()) + " blocks in -x from " + startX
                                + " (distance to blast " + zombie.position().distanceTo(center) + ")"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 400)
    public void exothermicBlastMutatesBlocks(GameTestHelper helper)
    {
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                helper.setBlock(CENTER.offset(x, 0, z), Blocks.WATER);
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.STONE);
            }
        }

        triggerBlast(helper, "exothermic");

        // beam style blast: the mutation runs after the two worker passes, so the water is not gone right away
        succeedWhenBlock(helper, state -> !state.is(Blocks.WATER),
                "exothermic blast did not mutate any of the water blocks");
    }

    @GameTest(maxTicks = 400)
    public void endothermicBlastMutatesBlocks(GameTestHelper helper)
    {
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                helper.setBlock(CENTER.offset(x, 0, z), Blocks.WATER);
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.STONE);
            }
        }

        triggerBlast(helper, "endothermic");

        // beam style blast: the freeze runs after the two worker passes
        succeedWhenBlock(helper, state -> state.is(Blocks.ICE),
                "endothermic blast did not freeze any of the water blocks");
    }

    @GameTest(maxTicks = 200)
    public void antiGravitationalBlastLiftsBlocks(GameTestHelper helper)
    {
        helper.setBlock(CENTER, Blocks.STONE);

        triggerBlast(helper, "antigravitational");

        // threaded + ticking blast, the flying blocks appear once the worker reported its positions
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!helper.getEntities(ModEntities.FLYING_BLOCK).isEmpty(),
                        "antigravitational blast did not spawn any flying blocks"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 60)
    public void mutationBlastConvertsVillager(GameTestHelper helper)
    {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        final Villager villager = helper.spawn(EntityTypes.VILLAGER, new Vec3(4.5D, CENTER.getY() + 1.0D, 4.5D));

        triggerBlast(helper, "mutation");

        // vanilla Mob#convertTo spawns the converted mob and discards the villager, so the new entity is
        // asserted instead of the type of the removed one
        helper.assertTrue(!helper.getEntities(EntityTypes.ZOMBIE_VILLAGER).isEmpty(),
                "mutation blast did not convert the villager, villager removed: " + villager.isRemoved());
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void primedExplosiveRunsItsBlast(GameTestHelper helper)
    {
        buildStoneCube(helper);

        final BlockPos pos = helper.absolutePos(CENTER);
        final EntityExplosive explosive = new EntityExplosive(helper.getLevel(),
                new icbm.classic.lib.transform.vector.Pos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
                Direction.UP, ICBMConstants.id("condensed"));
        explosive.setFuse(1);
        helper.getLevel().addFreshEntity(explosive);

        // S3.03 could not assert the explosion because the factories still threw, S3.04 wired them
        succeedWhenBlock(helper, IS_AIR, "the primed explosive did not run its blast");
    }
}
