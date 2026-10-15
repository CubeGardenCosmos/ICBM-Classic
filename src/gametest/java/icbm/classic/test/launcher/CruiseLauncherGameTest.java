package icbm.classic.test.launcher;

import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.blocks.launcher.status.LauncherStatus;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Verifies that the cruise launcher fires its loaded condensed missile into a target. */
public class CruiseLauncherGameTest
{
    @GameTest(maxTicks = 500, padding = 16)
    public void cruiseLauncherAimsAndDestroysTarget(GameTestHelper helper)
    {
        final BlockPos launcherPos = new BlockPos(1, 1, 1);
        helper.setBlock(launcherPos, ModBlocks.CRUISE_LAUNCHER);
        final BlockPos absoluteLauncher = helper.absolutePos(launcherPos);
        final BlockPos targetPos = absoluteLauncher.east(30);
        helper.getLevel().setBlock(targetPos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

        final TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(absoluteLauncher);
        helper.assertTrue(launcher != null, "cruise launcher block entity must be present");
        launcher.inventory.setItem(0, new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        launcher.energyStorage.setStored(launcher.energyStorage.getCapacity());
        final Vec3 target = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        launcher.setTarget(target);
        final var status = launcher.launcher.launch(new BasicTargetData(target), null, false);
        helper.assertTrue(status.getRegistryKey().equals(LauncherStatus.FIRING_AIMING.getRegistryKey()),
                "cruise launcher should queue firing while aiming, status was " + status.getRegistryKey());
        helper.succeedWhen(() -> {
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(EntityMissile.class,
                            new AABB(absoluteLauncher).inflate(64)).isEmpty(),
                    "cruise launcher should create a missile entity");
            helper.assertBlockState(targetPos, state -> state.isAir(),
                    state -> Component.literal("target block should be destroyed by the cruise missile, was " + state));
        });
    }
}
