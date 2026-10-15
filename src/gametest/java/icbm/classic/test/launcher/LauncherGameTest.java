package icbm.classic.test.launcher;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.blocks.launcher.LauncherSolution;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.network.LauncherNetwork;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import icbm.classic.prefab.tile.BlockICBM;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LauncherGameTest
{
    @GameTest(maxTicks = 500, padding = 16)
    public void networkLaunchesCondensedMissileAndDestroysTarget(GameTestHelper helper)
    {
        final BlockPos basePos = new BlockPos(1, 1, 1);
        helper.setBlock(basePos, ModBlocks.LAUNCHER_BASE.defaultBlockState()
                .setValue(BlockICBM.ROTATION_PROP, Direction.UP));
        helper.setBlock(basePos.above(), ModBlocks.LAUNCHER_FRAME);
        helper.setBlock(basePos.above(2), ModBlocks.LAUNCHER_FRAME);

        final BlockPos absoluteBase = helper.absolutePos(basePos);
        final BlockPos targetPos = absoluteBase.relative(Direction.EAST, 30);
        helper.getLevel().setBlock(targetPos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

        final TileLauncherBase base = (TileLauncherBase) helper.getLevel().getBlockEntity(absoluteBase);
        helper.assertTrue(base != null, "launcher base block entity must be present");
        final LauncherNetwork network = base.getNetworkNode().getNetwork();
        helper.assertTrue(network != null && network.getComponents().size() == 3,
                "launcher network must contain base plus two frame nodes");

        base.inventory.setItem(0, new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        final Vec3 target = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        final icbm.classic.api.actions.status.IActionStatus status;
        try {
            ConfigMain.REQUIRES_POWER = false;
            status = base.missileLauncher.launch(new LauncherSolution(new BasicTargetData(target)), null, false);
        } finally {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
        }
        helper.assertTrue(!status.isBlocking(), "launcher should accept a loaded missile and valid target");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(EntityMissile.class,
                        new AABB(absoluteBase).inflate(64)).isEmpty(),
                "launcher must spawn a missile entity");

        helper.succeedWhen(() -> helper.assertBlockState(targetPos, state -> state.isAir(),
                state -> Component.literal("target block should be destroyed by the missile, was " + state)));
    }
}
