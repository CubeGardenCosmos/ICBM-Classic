package icbm.classic.test.regression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.gas.BlastColor;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.launcher.LauncherSolution;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.EntityMissile;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.IncomingMissileMessage;
import icbm.classic.content.reg.ModItems;
import icbm.classic.prefab.tile.BlockICBM;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** End-to-end P1 regression coverage added by S5.10. */
public final class P1RegressionGameTests
{
    private static final String LANG_RESOURCE = "assets/" + ICBMConstants.DOMAIN + "/lang/en_us.json";

    public P1RegressionGameTests() {}

    @GameTest(maxTicks = 40)
    public void allBlocksPlaceable(GameTestHelper helper)
    {
        final List<Block> blocks = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.BLOCK.keySet())
        {
            if (id.getNamespace().equals(ICBMConstants.DOMAIN)) blocks.add(BuiltInRegistries.BLOCK.getValue(id));
        }
        helper.assertTrue(!blocks.isEmpty(), "no icbmclassic blocks registered");
        for (int i = 0; i < blocks.size(); i++)
        {
            final Block block = blocks.get(i);
            final BlockPos pos = new BlockPos(i % 8, 1, i / 8);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, block.defaultBlockState());
            helper.assertBlockState(pos, state -> state.equals(block.defaultBlockState()),
                    state -> Component.literal("unexpected state for " + BuiltInRegistries.BLOCK.getKey(block) + ": " + state));
            helper.assertBlockPresent(block, pos);
            if (block instanceof EntityBlock)
                helper.assertTrue(((EntityBlock) block).newBlockEntity(helper.absolutePos(pos), block.defaultBlockState()) != null,
                        "block has no block entity factory: " + BuiltInRegistries.BLOCK.getKey(block));
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allItemsStackableAndNamed(GameTestHelper helper)
    {
        final JsonObject lang = readMergedEnglish(helper);
        int checked = 0;
        for (Identifier id : BuiltInRegistries.ITEM.keySet())
        {
            if (!id.getNamespace().equals(ICBMConstants.DOMAIN)) continue;
            final Item item = BuiltInRegistries.ITEM.getValue(id);
            final ItemStack stack = new ItemStack(item);
            helper.assertTrue(!stack.isEmpty(), "empty default stack for " + id);
            final String name = stack.getHoverName().getString();
            helper.assertTrue(name != null && !name.isBlank(), "empty hover name for " + id);
            final String prefix = BuiltInRegistries.BLOCK.containsKey(id) ? "block." : "item.";
            helper.assertTrue(lang.has(prefix + id.getNamespace() + "." + id.getPath()), "missing merged English name key for " + id);
            checked++;
        }
        helper.assertTrue(checked > 0, "no icbmclassic items checked");
        ICBMClassic.LOGGER.info("S5.10 items: checked {} registered icbmclassic items against merged en_us", checked);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allEntityTypesSpawnable(GameTestHelper helper)
    {
        int checked = 0;
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet())
        {
            if (!id.getNamespace().equals(ICBMConstants.DOMAIN)) continue;
            final var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            final Entity entity = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            helper.assertTrue(entity != null, "entity type failed to create: " + id);
            try
            {
                entity.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getX() + 0.5,
                        helper.absolutePos(new BlockPos(2, 2, 2)).getY(), helper.absolutePos(new BlockPos(2, 2, 2)).getZ() + 0.5);
                helper.assertTrue(helper.getLevel().addFreshEntity(entity), "entity type failed to spawn: " + id);
                helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity,
                        "spawned entity was not present in the level: " + id);
                checked++;
            }
            finally
            {
                entity.discard();
            }
        }
        helper.assertTrue(checked > 0, "no icbmclassic entity types checked");
        ICBMClassic.LOGGER.info("S5.10 entities: spawned and discarded {} icbmclassic entity types", checked);
        helper.succeed();
    }

    @GameTest(maxTicks = 800, padding = 8)
    public void allExplosivesTrigger(GameTestHelper helper)
    {
        final Set<String> alreadyCovered = Set.of(
                "condensed", "shrapnel", "incendiary", "debilitation", "chemical", "anvil", "repulsive",
                "attractive", "fragmentation", "sonic", "breaching", "thermobaric", "nuclear", "emp",
                "exothermic", "endothermic", "antigravitational", "ender", "antimatter", "rot", "mutation", "smoke");
        final float previousRedmatterDefaultSize = ConfigBlast.redmatter.DEFAULT_SIZE;
        final float previousRedmatterMaxSize = ConfigBlast.redmatter.MAX_SIZE;
        final BlockPos blastCenter = new BlockPos(4, 2, 4);
        helper.setBlock(blastCenter.below(), Blocks.STONE);
        final Zombie target = helper.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, blastCenter.getY() + 1.0, 4.5));
        target.setNoAi(true);
        int coveredByExistingTests = 0;
        int triggeredHere = 0;
        try
        {
            for (IExplosiveData explosive : ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosives())
            {
                final Identifier id = explosive.getRegistryKey();
                final String name = id.getPath();
                if (alreadyCovered.contains(name))
                {
                    coveredByExistingTests++;
                    continue;
                }
                final BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
                final Vec3 location = Vec3.atCenterOf(center);
                final IActionSource source = new ActionSource(helper.getLevel(), location, null);
                if (name.equals("redmatter"))
                {
                    ConfigBlast.redmatter.DEFAULT_SIZE = ConfigBlast.redmatter.MIN_SIZE;
                    ConfigBlast.redmatter.MAX_SIZE = ConfigBlast.redmatter.MIN_SIZE;
                }
                final IAction action = explosive.create(helper.getLevel(), location.x, location.y, location.z, source, null);
                helper.assertTrue(action != null, "failed to create explosive action: " + id);
                if (action instanceof IBlastInit blast) blast.setBlastSize(2);
                helper.assertTrue(!action.doAction().isError(), "explosive action returned error: " + id);
                switch (name)
                {
                    case "colors", "contagious" -> { }
                    case "redmatter" ->
                    {
                        helper.assertTrue(!helper.getEntities(ModEntities.REDMATTER).isEmpty(),
                                "redmatter explosive did not spawn its entity at minimum scale");
                        helper.getEntities(ModEntities.REDMATTER).forEach(EntityRedmatter::discard);
                    }
                    default -> helper.fail("explosive has no regression assertion or existing coverage: " + id);
                }
                triggeredHere++;
            }
            helper.assertTrue(coveredByExistingTests == alreadyCovered.size(),
                    "existing test coverage set does not match registry: found " + coveredByExistingTests + " / " + alreadyCovered.size());
            helper.assertTrue(triggeredHere == 3, "expected the three uncovered explosive effects, checked " + triggeredHere);
        }
        finally
        {
            ConfigBlast.redmatter.DEFAULT_SIZE = previousRedmatterDefaultSize;
            ConfigBlast.redmatter.MAX_SIZE = previousRedmatterMaxSize;
            helper.getEntities(ModEntities.REDMATTER).forEach(EntityRedmatter::discard);
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!BlastColor.PARTICLE_COLORS.isEmpty(),
                        "colors explosive did not generate its particle palette"))
                .thenWaitUntil(() -> helper.assertTrue(!target.isRemoved() && !target.getActiveEffects().isEmpty(),
                        "contagious explosive did not apply a virus effect to its nearby zombie"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 500, padding = 16)
    public void launchRadarEmpScenario(GameTestHelper helper)
    {
        // Geometry constraints of the scenario (all of them are fixture only, none is production):
        // - the launcher refuses targets closer than 10 blocks on both x and z (LauncherCapability#isTargetTooClose),
        // - the radar only turns a track into THREAT (hasIncomingMissiles) when the extrapolated impact lands
        //   within its trigger range of 16 blocks,
        // - the launched missile and its whole flight path have to stay inside the chunks the batch loaded for entity
        //   ticking, which are the chunks the test structures sit in (see docs/implementation/evidence/S7.07.md and
        //   the probe in this test's history). Only positive local offsets are covered by that: the grid puts the
        //   next structure of the row at +13 and the next row at +14, nothing is loaded below the origin.
        // The layout is therefore the original one and only the target moved from 30 blocks down range to 11: a
        // flight of 11 blocks clears the launcher minimum, keeps every entity at local +1 or higher and lands the
        // impact ~5 blocks from the radar, well inside its trigger range.
        final BlockPos localBase = new BlockPos(1, 1, 1);
        helper.setBlock(localBase, ModBlocks.LAUNCHER_BASE.defaultBlockState().setValue(BlockICBM.ROTATION_PROP, Direction.UP));
        helper.setBlock(localBase.above(), ModBlocks.LAUNCHER_FRAME);
        helper.setBlock(localBase.above(2), ModBlocks.LAUNCHER_FRAME);
        final BlockPos localRadar = localBase.east(6);
        final BlockPos localTower = localBase.east(8);
        helper.setBlock(localRadar, ModBlocks.RADAR_STATION);
        helper.setBlock(localTower, ModBlocks.EMP_TOWER);

        final BlockPos basePos = helper.absolutePos(localBase);
        final BlockPos radarPos = helper.absolutePos(localRadar);
        final BlockPos towerPos = helper.absolutePos(localTower);
        final BlockPos targetPos = basePos.relative(Direction.EAST, 11);
        helper.getLevel().setBlock(targetPos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);
        final TileLauncherBase base = (TileLauncherBase) helper.getLevel().getBlockEntity(basePos);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(radarPos);
        final TileEMPTower tower = (TileEMPTower) helper.getLevel().getBlockEntity(towerPos);
        helper.assertTrue(base != null && radar != null && tower != null, "launch/radar/EMP block entity missing");
        base.inventory.setItem(0, new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        // The launcher refuses to fire while its firing cost is missing from the buffer (LauncherCapability#getStatus).
        // The buffer is charged instead of flipping ConfigMain.REQUIRES_POWER, which is global state shared with every
        // test of the batch.
        base.energyStorage.setStored(base.energyStorage.getCapacity());
        helper.onEachTick(() -> base.energyStorage.setStored(base.energyStorage.getCapacity()));
        radar.setDetectionRange(32);
        radar.setTriggerRange(16);
        // The station turns its redstone output on as soon as it has a threat, and that signal reaches the EMP tower
        // of the fixture (TileEMPTower#tick fires on hasNeighborSignal), which kills the missile with the configured
        // chance before this test can observe it. The scenario under test is the radio path, so the redstone output
        // of the station is turned off and the tower is driven by the IncomingMissileMessage of this test only.
        radar.setOutputRedstone(false);
        // Both machines drain their buffer while the scenario runs (radar 1000 FE/tick against a 20000 buffer,
        // EMP tower 320 FE/tick against the charge its firing cost leaves) and stop working when it runs empty, so
        // the same recharge the other machine GameTests use keeps them running for the whole flight.
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
        helper.onEachTick(() -> radar.energyStorage.setStored(radar.energyStorage.getCapacity()));
        radar.getRadio().setChannel("s5.10-regression");
        tower.getRadio().setChannel("s5.10-regression");
        tower.getRadio().setDisabled(true);
        tower.setRange(32);
        tower.energyStorage.setStored(tower.energyStorage.getCapacity());
        helper.onEachTick(() -> {
            if (tower.getCooldown() <= 0) tower.energyStorage.setStored(tower.energyStorage.getCapacity());
        });

        // No global config is touched: the launcher gets its charge from the fixture above, so ConfigMain stays as
        // the other tests of the batch expect it.
        final Vec3 target = Vec3.atCenterOf(targetPos);
        final icbm.classic.api.actions.status.IActionStatus status =
                base.missileLauncher.launch(new LauncherSolution(new BasicTargetData(target)), null, false);
        helper.assertTrue(status != null && !status.isBlocking(), "launcher rejected the loaded missile/target");
        // Every gate below throws until the radar has classified the missile and it is inside the tower range, so the
        // sequence cannot pass on the first tick. Firing is one-shot (config change and popMessage are not idempotent);
        // later attempts only wait for the detected missile to be removed.
        final boolean[] fired = {false};
        final Entity[] detected = {null};
        helper.succeedWhen(() -> {
            if (!fired[0])
            {
                helper.assertTrue(radar.hasIncomingMissiles() && !radar.getIncomingThreats().isEmpty(),
                        "radar has not classified the launched missile as incoming yet");
                final var missile = radar.getIncomingThreats().get(0);
                final Entity missileEntity = missile.getMissileEntity();
                helper.assertTrue(missileEntity != null, "radar detected a missile without a live entity");
                // Same test as RadioEmpTower#onMessage: distance from the tower block centre to the missile < range
                helper.assertTrue(missile.getVec3().distanceTo(Vec3.atCenterOf(towerPos)) < tower.getRange(),
                        "missile has not entered the EMP tower range yet");
                helper.assertTrue(tower.isReady(), "EMP tower did not remain charged and ready");
                fired[0] = true;
                detected[0] = missileEntity;
                final float previousMissileKillChance = ConfigBlast.emp.missiles.missileKillChance;
                final float previousFuelExplosionChance = ConfigBlast.emp.missiles.engineFullBlowChance;
                try
                {
                    ConfigBlast.emp.missiles.missileKillChance = 1f;
                    ConfigBlast.emp.missiles.engineFullBlowChance = 0f;
                    tower.getRadio().setDisabled(false);
                    RadioRegistry.popMessage(helper.getLevel(), radar.getRadio(),
                            new IncomingMissileMessage(radar.getRadio().getChannel(), missile, true));
                }
                finally
                {
                    ConfigBlast.emp.missiles.missileKillChance = previousMissileKillChance;
                    ConfigBlast.emp.missiles.engineFullBlowChance = previousFuelExplosionChance;
                }
            }
            helper.assertTrue(detected[0].isRemoved(),
                    "same-channel EMP tower did not process the detected missile");
        });
    }

    private static JsonObject readMergedEnglish(GameTestHelper helper)
    {
        try (InputStream stream = P1RegressionGameTests.class.getClassLoader().getResourceAsStream(LANG_RESOURCE))
        {
            helper.assertTrue(stream != null, "missing merged language resource " + LANG_RESOURCE);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (Exception e)
        {
            helper.fail("failed to read merged English resource " + LANG_RESOURCE + ": " + e);
            return new JsonObject();
        }
    }
}
