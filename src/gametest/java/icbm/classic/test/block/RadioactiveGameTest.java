package icbm.classic.test.block;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.radioactive.RadioactiveHandler;
import icbm.classic.content.reg.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Covers S3.08, the radioactive blocks:
 *
 * <ul>
 *     <li>the restored 1.12 rot table ({@code RadioactiveHandler.radioactiveBlockSwaps}) is loaded during start
 *     up and maps the 1.12 defaults (stone -&gt; radioactive stone, the dirt family -&gt; radioactive dirt,
 *     plants/leaves -&gt; air),</li>
 *     <li>the {@code rot} explosive of S3.04 turns the stone around its center into radioactive blocks,</li>
 *     <li>the decay pass of a radioactive block hurts a living entity standing next to it. 1.12 ran it from
 *     {@code randomTick}/{@code updateTick}, so the test bootstraps the 1.12 self rescheduling loop with one
 *     scheduled tick instead of calling the tick hooks by hand.</li>
 * </ul>
 */
public class RadioactiveGameTest
{
    /** Center of the blast, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Size every blast is forced to, keeps the affected area inside the test structure */
    private static final int TEST_SIZE = 3;

    /**
     * Triggers an explosive by id through the registry, mirroring how the game creates a blast
     * (copied from {@code BasicBlastsGameTest}).
     *
     * @param helper      test helper
     * @param explosiveId explosive id inside the mod namespace
     * @return the action that was started
     */
    private static IAction triggerBlast(GameTestHelper helper, String explosiveId)
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
        action.doAction();
        return action;
    }

    /** Counts the radioactive blocks inside the blast area of the test structure. */
    private static int countRadioactiveBlocks(GameTestHelper helper)
    {
        int count = 0;
        for (int x = -TEST_SIZE; x <= TEST_SIZE; x++)
        {
            for (int y = -TEST_SIZE; y <= TEST_SIZE; y++)
            {
                for (int z = -TEST_SIZE; z <= TEST_SIZE; z++)
                {
                    final BlockState state = helper.getBlockState(CENTER.offset(x, y, z));
                    if (state.is(ModBlocks.RADIOACTIVE_DIRT) || state.is(ModBlocks.RADIOACTIVE_STONE))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Asserts that the rot table replaces the given block with air, the 1.12
     * {@code Material.PLANTS}/{@code LEAVES}/{@code VINE} rows.
     */
    private static void assertAirRow(GameTestHelper helper, Block block, String label)
    {
        final BlockReplacementData row = RadioactiveHandler.radioactiveBlockSwaps.getValue(block.defaultBlockState());
        helper.assertTrue(row != null && row.getBlockState() != null && row.getBlockState().isAir(),
                label + " should be removed by the rot blast, table says " + (row == null ? "no row" : row.getBlockState())
                        + ". The plant rows come from the vanilla block tags, so the table has to be built after "
                        + "the data pack is loaded (CommonLifecycleEvents.TAGS_LOADED / "
                        + "ServerLifecycleEvents.SERVER_STARTING)");
    }

    /**
     * The rot table is read from the config and the two radioactive blocks, it has to be set up during start
     * up (1.12 {@code ICBMClassic#init} called {@code RadioactiveHandler.setup()}). An empty table makes the
     * rot explosive do nothing, so the wiring is asserted before the blast test.
     */
    @GameTest(maxTicks = 40)
    public void rotTableHoldsTheLegacyDefaults(GameTestHelper helper)
    {
        helper.assertTrue(RadioactiveHandler.radioactiveBlockSwaps.getValue(Blocks.STONE.defaultBlockState()) != null,
                "the rot table is empty, RadioactiveHandler.setup() has to run during start up");

        // 1.12: stone -> radioactive stone, chance 0.2
        final BlockReplacementData stoneRow = RadioactiveHandler.radioactiveBlockSwaps.getValue(Blocks.STONE.defaultBlockState());
        helper.assertTrue(stoneRow.getBlockState() != null && stoneRow.getBlockState().is(ModBlocks.RADIOACTIVE_STONE),
                "stone should rot into radioactive stone, table says " + stoneRow.getBlockState());
        helper.assertTrue(stoneRow.getChance() == 0.2f,
                "the 1.12 stone row used setChance(0.2f), table says " + stoneRow.getChance());

        // 1.12: dirt, farmland, grass path, grass and mycelium -> radioactive dirt
        for (Block dirtLike : new Block[]{
                Blocks.DIRT, Blocks.FARMLAND, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK, Blocks.MYCELIUM})
        {
            final BlockState replacement = RadioactiveHandler.radioactiveBlockSwaps
                    .getValue(dirtLike.defaultBlockState()).getBlockState();
            helper.assertTrue(replacement != null && replacement.is(ModBlocks.RADIOACTIVE_DIRT),
                    dirtLike + " should rot into radioactive dirt, table says " + replacement);
        }

        // 1.12: Material.PLANTS / LEAVES / VINE rows replaced the block with air. These rows come from block
        // tags, which the table can only read once the server data pack is loaded, so this also checks that
        // RadioactiveHandler.setup() is wired to a tag/server start hook and not to the early mod init.
        assertAirRow(helper, Blocks.SHORT_GRASS, "grass");
        assertAirRow(helper, Blocks.OAK_LEAVES, "leaves");
        helper.succeed();
    }

    /**
     * The rot explosive runs on the threaded blast framework (S3.04), it swaps the stone around the center
     * with a 0.2 chance per block.
     */
    @GameTest(maxTicks = 400)
    public void rotBlastSpreadsRadioactiveBlocks(GameTestHelper helper)
    {
        for (int x = -TEST_SIZE; x <= TEST_SIZE; x++)
        {
            for (int y = -TEST_SIZE; y <= TEST_SIZE; y++)
            {
                for (int z = -TEST_SIZE; z <= TEST_SIZE; z++)
                {
                    helper.setBlock(CENTER.offset(x, y, z), Blocks.STONE);
                }
            }
        }
        helper.assertTrue(countRadioactiveBlocks(helper) == 0, "test cube should start without radioactive blocks");

        triggerBlast(helper, "rot");

        // threaded blast: the worker collects the positions and the server thread applies the swap over
        // several ticks, and every block only has a 0.2 chance of being swapped
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(countRadioactiveBlocks(helper) > 0,
                        "the rot blast did not turn any stone into a radioactive block"))
                .thenSucceed();
    }

    /**
     * A zombie standing next to a radioactive block is hurt by the decay pass
     * (1.12 {@code BlockRadioactive#tryDecayEntities}).
     */
    @GameTest(maxTicks = 200)
    public void radioactiveBlockDecayHurtsEntities(GameTestHelper helper)
    {
        final BlockPos blockPos = CENTER;
        helper.setBlock(blockPos, ModBlocks.RADIOACTIVE_STONE);
        helper.assertBlockPresent(ModBlocks.RADIOACTIVE_STONE, blockPos);

        final Zombie zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (zombie == null)
        {
            helper.fail("failed to create a zombie for the radioactive decay test");
            return;
        }
        zombie.setNoAi(true);
        //Mobs that are far away from every player are removed by the vanilla despawn roll, which would end
        //this test randomly; pin the zombie so it stays for the whole test.
        zombie.setPersistenceRequired();
        zombie.setPos(Vec3.atBottomCenterOf(helper.absolutePos(blockPos.above())));
        helper.getLevel().addFreshEntity(zombie);

        //1.12 only ran the decay from the block's randomTick/updateTick, which rescheduled itself after every
        //pass: one scheduled tick bootstraps that loop on the 26.3 side (the block schedules the next one).
        helper.getLevel().scheduleTick(helper.absolutePos(blockPos), ModBlocks.RADIOACTIVE_STONE, 1);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                        "the radioactive block should hurt the zombie standing next to it, health is "
                                + zombie.getHealth() + " at " + zombie.position() + " in "
                                + helper.getBlockState(blockPos)))
                .thenSucceed();
    }
}
