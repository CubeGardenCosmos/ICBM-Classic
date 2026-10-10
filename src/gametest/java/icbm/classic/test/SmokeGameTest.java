package icbm.classic.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public class SmokeGameTest {
    @GameTest
    public void stoneCanBePlaced(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }
}
