package icbm.classic.test.command;

import icbm.classic.ICBMConstants;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Covers S3.10, the {@code /icbm} command tree (1.12 {@code CommandEntryPoint}/{@code CommandGroup}):
 *
 * <ul>
 *     <li>{@code /icbm blast trigger <id> <dim> <x> <y> <z> <size>} runs a real blast,</li>
 *     <li>{@code /icbm blast spread <count> <distance> <id> <dim> <x> <y> <z> <scale>} triggers a grid,</li>
 *     <li>{@code /icbm remove <type> <dim> <x> <y> <z> [radius]} and {@code /icbm lag [radius]} clear the
 *     ICBM entities of an area,</li>
 *     <li>{@code /icbm help} and {@code /icbm blast list} describe the command surface,</li>
 *     <li>a bad explosive id, a scale of zero and a console {@code ~} are rejected with the migrated
 *     {@code icbmclassic.command.*} messages and do not throw out of the dispatcher.</li>
 * </ul>
 *
 * Every command goes through {@code MinecraftServer#getCommands()#performPrefixedCommand(...)}, so the real
 * Brigadier tree, argument types, permission check and message formatting are exercised. The 1.12 command
 * surface resolves {@code ~} through {@code CommandUtils}, which rejected it for the console (a
 * {@code CommandSourceStack} without an entity), so the tests pass absolute coordinates like the console has
 * to and the relative form is asserted to be rejected.
 */
public class CommandGameTest
{
    /** Center of the command target, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Size the command passes, keeps the affected area inside the test structure */
    private static final float TEST_SIZE = 2.0F;
    /** Radius used by the remove/lag commands, has to reach the center from the test position */
    private static final int TEST_RADIUS = 4;

    private static final Predicate<BlockState> IS_AIR = BlockState::isAir;

    /**
     * Collects the command feedback so a test can read what the command told the sender. 1.12 read the same
     * text out of {@code ICommandSender#sendMessage}, the 26.3 source stack forwards the messages of a
     * {@link CommandSource} (success and failure, 1.12 had no such split).
     */
    private static final class RecordingSource implements CommandSource
    {
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component message)
        {
            messages.add(message);
        }

        @Override
        public boolean acceptsSuccess()
        {
            return true;
        }

        @Override
        public boolean acceptsFailure()
        {
            return true;
        }

        @Override
        public boolean shouldInformAdmins()
        {
            return false;
        }

        private String text()
        {
            return messages.stream().map(Component::getString).collect(Collectors.joining("\n"));
        }
    }

    /** Builds the source stack of a console-like sender (no entity) standing in the test structure */
    private static CommandSourceStack source(GameTestHelper helper, RecordingSource recorder)
    {
        final BlockPos pos = helper.absolutePos(CENTER);
        final MinecraftServer server = helper.getLevel().getServer();
        return server.createCommandSourceStack()
                .withSource(recorder)
                .withLevel(helper.getLevel())
                .withPosition(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
    }

    /** Runs a command through the real server dispatcher, returns the collected feedback */
    private static RecordingSource run(GameTestHelper helper, String command)
    {
        return run(helper, null, command);
    }

    /**
     * Runs a command through the real server dispatcher with the given permissions (1.12
     * {@code ICommand#getRequiredPermissionLevel()}), null keeps the console permissions of the server stack.
     *
     * @return the collected feedback
     */
    private static RecordingSource run(GameTestHelper helper, LevelBasedPermissionSet permissions, String command)
    {
        final RecordingSource recorder = new RecordingSource();
        CommandSourceStack source = source(helper, recorder);
        if (permissions != null)
        {
            source = source.withPermission(permissions);
        }
        //performPrefixedCommand trims the optional '/' and runs the real dispatcher, the same way the
        //1.12 CommandEntryPoint received the already split arguments
        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, command);
        return recorder;
    }

    /**
     * Absolute position of the command target, as the command strings need it.
     * <p>
     * The values are formatted with {@link String#format} on purpose: {@code Double.toString} switches to
     * scientific notation from 1e7 on (a structure can be placed that far out, the batch origins in the test
     * log are past -1e7) and Brigadier's {@code StringReader#readDouble} rejects the {@code E} exponent, so
     * "1.00748645E7" makes the Vec3 argument report {@code argument.pos3d.incomplete} instead of parsing the
     * coordinates.
     */
    private static String position(BlockPos pos)
    {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f",
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /** Dimension the command has to name, 1.12 used the numeric dimension id here */
    private static String dimension(GameTestHelper helper)
    {
        return helper.getLevel().dimension().identifier().toString();
    }

    /** Places a solid stone cube of the given radius around the command target */
    private static void buildStoneCube(GameTestHelper helper, int radius)
    {
        for (int x = -radius; x <= radius; x++)
        {
            for (int y = -radius; y <= radius; y++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    helper.setBlock(CENTER.offset(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /** Counts the blocks matching the predicate around the target, mirroring BasicBlastsGameTest */
    private static int countBlocks(GameTestHelper helper, int radius, Predicate<BlockState> test)
    {
        int count = 0;
        for (int x = -radius; x <= radius; x++)
        {
            for (int y = -radius; y <= radius; y++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    final BlockPos pos = CENTER.offset(x, y, z);
                    if (pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                            && pos.getX() < 8 && pos.getY() < 8 && pos.getZ() < 8
                            && test.test(helper.getBlockState(pos)))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Spawns a primed explosive that will not run its blast during the test, 1.12 EntityExplosive */
    private static EntityExplosive spawnPrimedExplosive(GameTestHelper helper)
    {
        // GameTestHelper#spawn resolves its Vec3 through GameTestHelper#absoluteVec, i.e. the coordinates are
        // relative to the test structure, the same convention GameTestHelper#setBlock(BlockPos) uses. Passing
        // absolute coordinates here would place the entity a structure origin away from the command target,
        // where the entity queries of the command cannot see it.
        final EntityExplosive explosive = helper.spawn(ModEntities.PRIMED_EXPLOSIVE,
                new Vec3(CENTER.getX() + 0.5D, CENTER.getY(), CENTER.getZ() + 0.5D));
        explosive.setExplosiveId(ICBMExplosives.CONDENSED.getRegistryKey());
        explosive.setFuse(400);
        return explosive;
    }

    /**
     * {@code /icbm blast trigger} has to run the registered blast factory, so the stone cube around the
     * target is destroyed like the direct call in {@code BasicBlastsGameTest} does.
     */
    @GameTest(maxTicks = 60)
    public void blastTriggerCommandRemovesStone(GameTestHelper helper)
    {
        buildStoneCube(helper, 1);
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0, "test cube should start solid");

        final RecordingSource recorder = run(helper, "/icbm blast trigger " + ICBMConstants.DOMAIN + ":condensed "
                + dimension(helper) + " "
                + position(helper.absolutePos(CENTER)) + " "
                + TEST_SIZE);

        helper.assertTrue(countBlocks(helper, 1, IS_AIR) > 0,
                "the blast trigger command did not destroy any of the stone blocks, feedback was: " + recorder.text());
        helper.succeed();
    }

    /**
     * {@code /icbm blast spread} triggers the (count * 2 + 1)^2 grid, every blast is a real condensed blast.
     */
    @GameTest(maxTicks = 100)
    public void blastSpreadCommandTriggersGrid(GameTestHelper helper)
    {
        //The grid is centred on the command position, so the 3x3 blasts of count=1/distance=1 land inside the
        //stone cube of radius 1 around the target
        buildStoneCube(helper, 1);
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0, "test cube should start solid");

        final RecordingSource recorder = run(helper, "/icbm blast spread 1 1 " + ICBMConstants.DOMAIN + ":condensed "
                + dimension(helper) + " "
                + position(helper.absolutePos(CENTER)) + " "
                + TEST_SIZE);

        helper.assertTrue(countBlocks(helper, 1, IS_AIR) > 0,
                "the blast spread command did not destroy any of the stone blocks, feedback was: " + recorder.text());
        helper.succeed();
    }

    /**
     * {@code /icbm remove explosions <dim> <x> <y> <z> <radius>} removes the ICBM entities of the area,
     * 1.12 {@code CommandRemove#buildSelector} for the "explosions" keyword.
     */
    @GameTest(maxTicks = 40)
    public void removeCommandRemovesPrimedExplosives(GameTestHelper helper)
    {
        final EntityExplosive explosive = spawnPrimedExplosive(helper);

        // A spawned entity is queued in the level's entity manager and only shows up in the entity sections
        // on the next tick (26.3 ServerLevel#addFreshEntity -> EntityManager#addNewEntity), so the command
        // has to run one tick later or its entity query finds nothing
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() ->
                {
                    final RecordingSource recorder = run(helper, "/icbm remove explosions "
                            + dimension(helper) + " "
                            + position(helper.absolutePos(CENTER)) + " "
                            + TEST_RADIUS);

                    helper.assertTrue(explosive.isRemoved(),
                            "the remove command did not remove the primed explosive, feedback was: " + recorder.text());
                })
                .thenSucceed();
    }

    /**
     * {@code /icbm lag [radius]} clears the ICBM entities and the queued blasts around the sender,
     * 1.12 {@code CommandLag}.
     */
    @GameTest(maxTicks = 40)
    public void lagCommandRemovesIcbmEntities(GameTestHelper helper)
    {
        final EntityExplosive explosive = spawnPrimedExplosive(helper);

        // see removeCommandRemovesPrimedExplosives: the fresh entity is only visible to entity queries from
        // the next tick on
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() ->
                {
                    final RecordingSource recorder = run(helper, "/icbm lag " + TEST_RADIUS);

                    helper.assertTrue(explosive.isRemoved(),
                            "the lag command did not remove the primed explosive, feedback was: " + recorder.text());
                    helper.assertTrue(recorder.messages.size() > 0,
                            "the lag command has to report back (icbmclassic.command.lag.remove)");
                })
                .thenSucceed();
    }

    /**
     * {@code /icbm help} lists the registered commands and {@code /icbm blast list} names every registered
     * explosive id. Both command groups of 1.12 (root and blast) have their own help.
     */
    @GameTest(maxTicks = 40)
    public void helpAndListDescribeTheCommandSurface(GameTestHelper helper)
    {
        final RecordingSource rootHelp = run(helper, "/icbm help");
        helper.assertTrue(rootHelp.text().contains("/icbm help"),
                "help has to list itself, feedback was: " + rootHelp.text());
        helper.assertTrue(rootHelp.text().contains("/icbm blast"),
                "help has to list the blast command, feedback was: " + rootHelp.text());
        helper.assertTrue(rootHelp.text().contains("/icbm remove <all/missiles/explosions> <dim> <x> <y> <z> <radius>"),
                "help has to list the remove usage of 1.12, feedback was: " + rootHelp.text());
        //the world only form of remove/lag is hidden from a sender without an entity, 1.12 printed it only
        //when ICommandSender#getCommandSenderEntity() was not null
        helper.assertTrue(!rootHelp.text().contains("[radius]"),
                "the world only help lines must not be shown to a sender outside the world, feedback was: " + rootHelp.text());

        final RecordingSource blastHelp = run(helper, "/icbm blast help");
        helper.assertTrue(blastHelp.text().contains("/icbm blast trigger <id> <dim> <x> <y> <z> <size>"),
                "the blast help has to list the trigger usage of 1.12, feedback was: " + blastHelp.text());
        helper.assertTrue(blastHelp.text().contains("/icbm blast spread <count> <distance> <id> <dim> <x> <y> <z> <scale>"),
                "the blast help has to list the spread usage of 1.12, feedback was: " + blastHelp.text());

        final RecordingSource list = run(helper, "/icbm blast list");
        helper.assertTrue(list.text().startsWith("Explosive Types: "),
                "the blast list has to name the registered explosives, feedback was: " + list.text());
        helper.assertTrue(list.text().contains(ICBMConstants.DOMAIN + ":condensed"),
                "the blast list has to contain condensed, feedback was: " + list.text());
        helper.succeed();
    }

    /**
     * The whole {@code /icbm} tree keeps the permission level 2 of 1.12
     * ({@code CommandEntryPoint#getRequiredPermissionLevel()}, 26.3 {@code Commands.LEVEL_GAMEMASTERS}).
     */
    @GameTest(maxTicks = 40)
    public void permissionLevelTwoIsRequired(GameTestHelper helper)
    {
        final RecordingSource moderator = run(helper, LevelBasedPermissionSet.MODERATOR, "/icbm help");
        //the help output of both groups only exists as "/icbm ..." lines, the "unknown command" context shows
        //the trimmed input without the slash, so this tells the two apart
        helper.assertTrue(!moderator.text().contains("/icbm"),
                "a level 1 sender must not get the icbm help, feedback was: " + moderator.text());
        helper.assertTrue(moderator.messages.size() > 0,
                "a level 1 sender has to be told that the command is not available");

        final RecordingSource gameMaster = run(helper, LevelBasedPermissionSet.GAMEMASTER, "/icbm help");
        helper.assertTrue(gameMaster.text().contains("/icbm help"),
                "a level 2 sender has to get the icbm help, feedback was: " + gameMaster.text());
        helper.succeed();
    }

    /**
     * An unregistered explosive id is rejected with {@code icbmclassic.command.blast.error.explosive.id}
     * (1.12 {@code CommandBlastTrigger#getExplosive}) and the dispatcher does not throw out of the command.
     */
    @GameTest(maxTicks = 40)
    public void unknownExplosiveIdFailsWithoutThrowing(GameTestHelper helper)
    {
        buildStoneCube(helper, 1);
        final String unknownId = ICBMConstants.DOMAIN + ":not_a_real_explosive";

        final RecordingSource recorder = run(helper, "/icbm blast trigger " + unknownId + " "
                + dimension(helper) + " "
                + position(helper.absolutePos(CENTER)) + " "
                + TEST_SIZE);

        helper.assertTrue(recorder.text().contains(unknownId),
                "the command has to report that " + unknownId + " could not be found, feedback was: " + recorder.text());
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0,
                "an unknown explosive id must not run a blast, feedback was: " + recorder.text());
        helper.succeed();
    }

    /**
     * A scale that is not above zero is rejected with
     * {@code icbmclassic.command.blast.error.scale.zero} (1.12 {@code CommandBlastTrigger#getScale}) and the
     * dispatcher does not throw out of the command.
     */
    @GameTest(maxTicks = 40)
    public void zeroScaleFailsWithoutThrowing(GameTestHelper helper)
    {
        buildStoneCube(helper, 1);

        final RecordingSource recorder = run(helper, "/icbm blast trigger " + ICBMConstants.DOMAIN + ":condensed "
                + dimension(helper) + " "
                + position(helper.absolutePos(CENTER)) + " 0");

        helper.assertTrue(recorder.text().contains("Scale must be greater than zero"),
                "the command has to report the 1.12 scale error, feedback was: " + recorder.text());
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0,
                "a zero scale must not run a blast, feedback was: " + recorder.text());
        helper.succeed();
    }

    /**
     * 1.12 {@code CommandUtils#getNumber} rejected {@code ~} for the console, and the short form of
     * {@code CommandBlastTrigger} ({@code <id> <size>}) was only available to a sender in the world. The 26.3
     * tree reports both errors for a sender without an entity instead of using its position.
     */
    @GameTest(maxTicks = 40)
    public void consoleSenderHasToUseTheLongForm(GameTestHelper helper)
    {
        buildStoneCube(helper, 1);

        final RecordingSource relative = run(helper, "/icbm blast trigger " + ICBMConstants.DOMAIN + ":condensed "
                + dimension(helper) + " ~ ~ ~ " + TEST_SIZE);

        helper.assertTrue(relative.text().contains("'~' can't be used from console"),
                "the command has to reject '~' from a sender outside the world, feedback was: " + relative.text());
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0,
                "a rejected command must not run a blast, feedback was: " + relative.text());

        final RecordingSource shortForm = run(helper, "/icbm blast trigger " + ICBMConstants.DOMAIN + ":condensed "
                + TEST_SIZE);

        helper.assertTrue(shortForm.text().contains("Unknown command. See /icbm blast trigger help for more info."),
                "the short form has to report the 1.12 unknown command message for a sender outside the world, "
                        + "feedback was: " + shortForm.text());
        helper.assertTrue(countBlocks(helper, 1, IS_AIR) == 0,
                "a rejected command must not run a blast, feedback was: " + shortForm.text());
        helper.succeed();
    }
}
