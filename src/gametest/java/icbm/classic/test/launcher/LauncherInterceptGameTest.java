package icbm.classic.test.launcher;

import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LauncherInterceptGameTest
{
    private static final String CHANNEL = "launcher-intercept-test";

    private static final class FrozenThreat extends EntityExplosiveMissile
    {
        private FrozenThreat(Level level)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
        }

        @Override
        public int getTicksInAir() { return 2; }

        @Override
        public void tick()
        {
            // Keep the threat in place while the radar and ABM exercise their real message and guidance paths.
        }
    }

    private record Fixture(TileRadarStation radar, FrozenThreat threat) { }

    private Fixture addThreat(GameTestHelper helper, BlockPos radarLocal)
    {
        helper.setBlock(radarLocal, ModBlocks.RADAR_STATION);
        final BlockPos radarPos = helper.absolutePos(radarLocal);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(radarPos);
        helper.assertTrue(radar != null, "radar block entity must be present");
        radar.setDetectionRange(100);
        radar.setTriggerRange(10);
        radar.getRadio().setChannel(CHANNEL);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());

        final FrozenThreat threat = new FrozenThreat(helper.getLevel());
        threat.explosive.setStack(new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        threat.setPos(radarPos.getX() + 0.5, radarPos.getY() + 40.5, radarPos.getZ() + 0.5);
        threat.setDeltaMovement(new Vec3(0, -1, 0));
        threat.setNoGravity(true);
        threat.setFreezeMotion(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(threat), "incoming threat missile must spawn");
        RadarRegistry.add(threat);
        return new Fixture(radar, threat);
    }

    @GameTest(maxTicks = 240, padding = 16)
    public void incomingNotificationDoesNotFireNuclearScreenLauncher(GameTestHelper helper)
    {
        final BlockPos baseLocal = new BlockPos(1, 1, 1);
        final BlockPos screenLocal = baseLocal.east();
        helper.setBlock(baseLocal, ModBlocks.LAUNCHER_BASE);
        helper.setBlock(screenLocal, ModBlocks.LAUNCHER_SCREEN);
        final BlockPos basePos = helper.absolutePos(baseLocal);
        final TileLauncherBase base = (TileLauncherBase) helper.getLevel().getBlockEntity(basePos);
        final TileLauncherScreen screen = (TileLauncherScreen) helper.getLevel().getBlockEntity(helper.absolutePos(screenLocal));
        helper.assertTrue(base != null && screen != null, "launcher base and screen must be present");
        base.inventory.setItem(0, new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.NUCLEAR.getRegistryKey())));
        base.energyStorage.setStored(base.energyStorage.getCapacity());
        screen.radioCap.setChannel(CHANNEL);

        final Fixture fixture = addThreat(helper, new BlockPos(5, 1, 1));
        final AABB launcherBounds = new AABB(basePos).inflate(16);
        final int initialMissiles = helper.getLevel().getEntitiesOfClass(EntityExplosiveMissile.class, launcherBounds).size();
        final long startTick = helper.getLevel().getGameTime();
        helper.succeedWhen(() -> {
            RadarRegistry.add(fixture.threat());
            if (helper.getLevel().getGameTime() - startTick < 200) return;
            final var track = fixture.radar().getTrackTable().get(fixture.threat().getUUID());
            helper.assertTrue(track != null && track.status == TrackStatus.THREAT,
                    "same-channel radar must detect the incoming missile as a threat");
            helper.assertTrue(screen.getLaunchersInGroup().size() == 1,
                    "screen must be connected to the loaded launcher base");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityExplosiveMissile.class, launcherBounds).size() == initialMissiles,
                    "incoming missile notification must not launch the loaded nuclear missile");
            helper.assertTrue(!base.getMissileStack().isEmpty(), "nuclear missile must remain loaded");
            fixture.threat().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 300, padding = 16)
    public void cruiseLauncherRespondsToRadarInterceptOrder(GameTestHelper helper)
    {
        final Fixture fixture = addThreat(helper, new BlockPos(2, 1, 2));
        final BlockPos launcherLocal = new BlockPos(10, 1, 2);
        helper.setBlock(launcherLocal, ModBlocks.CRUISE_LAUNCHER);
        final BlockPos launcherPos = helper.absolutePos(launcherLocal);
        final TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(launcherPos);
        helper.assertTrue(launcher != null, "cruise launcher block entity must be present");
        launcher.inventory.setItem(0, new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE));
        launcher.energyStorage.setStored(launcher.energyStorage.getCapacity());
        launcher.radio.setChannel(CHANNEL);

        helper.succeedWhen(() -> {
            RadarRegistry.add(fixture.threat());
            if (fixture.threat().isAlive()) return;
            helper.assertTrue(launcher.getMissileStack().isEmpty(), "intercept order must consume exactly the loaded ABM");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityAntiBallisticMissile.class,
                            new AABB(launcherPos).inflate(100)).isEmpty(),
                    "ABM must finish by intercepting and removing itself");
            helper.assertTrue(EngagementRegistry.wasIntercepted((net.minecraft.server.level.ServerLevel) helper.getLevel(),
                            fixture.threat().getUUID()), "ABM must mark the incoming target intercepted");
            helper.succeed();
        });
    }
}
