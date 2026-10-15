package icbm.classic.test.emptower;

import icbm.classic.config.ConfigMain;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.api.refs.ICBMExplosives;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class EmpTowerGameTest
{
    @GameTest(maxTicks = 20, padding = 8)
    public void fullyChargedTowerEmpProcessesMissile(GameTestHelper helper)
    {
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        final float previousKillChance = ConfigBlast.emp.missiles.missileKillChance;
        final float previousFuelExplosionChance = ConfigBlast.emp.missiles.engineFullBlowChance;
        try
        {
            ConfigMain.REQUIRES_POWER = true;
            ConfigBlast.emp.missiles.missileKillChance = 1f;
            ConfigBlast.emp.missiles.engineFullBlowChance = 0f;
            final BlockPos localTower = new BlockPos(2, 1, 2);
            helper.setBlock(localTower, ModBlocks.EMP_TOWER);
            final BlockPos towerPos = helper.absolutePos(localTower);
            final TileEMPTower tower = (TileEMPTower) helper.getLevel().getBlockEntity(towerPos);
            helper.assertTrue(tower != null, "EMP tower block entity must be present");
            tower.setRange(8);
            tower.energyStorage.setStored(tower.energyStorage.getCapacity());

            final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
            missile.explosive.setStack(new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
            missile.setPos(towerPos.getX() + 3.5, towerPos.getY() + 1.5, towerPos.getZ() + 0.5);
            missile.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().addFreshEntity(missile);

            helper.assertTrue(tower.isReady(), "fully charged tower must be ready");
            helper.assertTrue(tower.fire(null), "charged tower fire must succeed");
            helper.assertTrue(missile.isRemoved(), "missile within the EMP area must be processed and removed");
            helper.succeed();
        }
        finally
        {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
            ConfigBlast.emp.missiles.missileKillChance = previousKillChance;
            ConfigBlast.emp.missiles.engineFullBlowChance = previousFuelExplosionChance;
        }
    }
}
