package icbm.classic.test.reg;

import icbm.classic.ICBMClassic;
import icbm.classic.content.reg.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the S2.03 simple blocks:
 *
 * <ul>
 *     <li>all nine blocks can be placed and are still the block that was registered under that id,</li>
 *     <li>the normal spikes hurt a living entity that is inside them (1.12
 *     {@code onEntityCollidedWithBlock} behaviour).</li>
 * </ul>
 */
public class SimpleBlocksGameTest
{
    @GameTest(maxTicks = 100)
    public void simpleBlocksPlaceAndSpikesHurt(GameTestHelper helper)
    {
        final BlockPos concrete = new BlockPos(1, 2, 1);
        helper.setBlock(concrete, ModBlocks.CONCRETE);
        helper.assertBlockPresent(ModBlocks.CONCRETE, concrete);

        final BlockPos compact = new BlockPos(2, 2, 1);
        helper.setBlock(compact, ModBlocks.COMPACT_CONCRETE);
        helper.assertBlockPresent(ModBlocks.COMPACT_CONCRETE, compact);

        final BlockPos reinforced = new BlockPos(3, 2, 1);
        helper.setBlock(reinforced, ModBlocks.REINFORCED_CONCRETE);
        helper.assertBlockPresent(ModBlocks.REINFORCED_CONCRETE, reinforced);

        final BlockPos glass = new BlockPos(1, 2, 2);
        helper.setBlock(glass, ModBlocks.REINFORCED_GLASS);
        helper.assertBlockPresent(ModBlocks.REINFORCED_GLASS, glass);

        final BlockPos spikes = new BlockPos(2, 2, 2);
        helper.setBlock(spikes, ModBlocks.SPIKES);
        helper.assertBlockPresent(ModBlocks.SPIKES, spikes);

        final BlockPos poison = new BlockPos(3, 2, 2);
        helper.setBlock(poison, ModBlocks.POISON_SPIKES);
        helper.assertBlockPresent(ModBlocks.POISON_SPIKES, poison);

        final BlockPos fire = new BlockPos(4, 2, 2);
        helper.setBlock(fire, ModBlocks.FIRE_SPIKES);
        helper.assertBlockPresent(ModBlocks.FIRE_SPIKES, fire);

        final BlockPos button = new BlockPos(1, 2, 3);
        helper.setBlock(button, ModBlocks.GLASS_BUTTON);
        helper.assertBlockPresent(ModBlocks.GLASS_BUTTON, button);

        final BlockPos plate = new BlockPos(2, 2, 3);
        helper.setBlock(plate, ModBlocks.GLASS_PRESSURE_PLATE);
        helper.assertBlockPresent(ModBlocks.GLASS_PRESSURE_PLATE, plate);

        //A zombie standing in the spikes has to lose health; the spikes have no collision, so the block
        //below gives it something to stand on while its hitbox still covers the spike block.
        final BlockPos spikePos = new BlockPos(5, 2, 5);
        helper.setBlock(spikePos.below(), Blocks.STONE);
        helper.setBlock(spikePos, ModBlocks.SPIKES);

        final Zombie zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (zombie == null)
        {
            helper.fail("failed to create a zombie for the spike damage test");
            return;
        }
        zombie.setNoAi(true);
        //Mobs that are far away from every player are removed by the vanilla despawn roll, which would end
        //this test randomly; pin the zombie so it stays for the whole test.
        zombie.setPersistenceRequired();
        zombie.setPos(Vec3.atBottomCenterOf(helper.absolutePos(spikePos)));
        helper.getLevel().addFreshEntity(zombie);

        helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                "zombie should be hurt by the spikes, health is " + zombie.getHealth()
                        + " at " + zombie.position() + " in " + helper.getBlockState(spikePos)));
    }
}
