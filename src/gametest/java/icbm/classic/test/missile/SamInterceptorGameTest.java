package icbm.classic.test.missile;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.refs.ICBMEntities;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.anti.EntitySurfaceToAirMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.logic.flight.DirectFlightLogic;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.lookup.IcbmLookups;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for the Surface-to-Air Missile (SAM) / Anti-Ballistic Missile (S4.04).
 */
public class SamInterceptorGameTest
{
    /**
     * Asserts that MISSILE_SAM is registered as icbmclassic:anti_ballistic_missile,
     * and the matching item has the missile stack capability returning missile.sam.
     */
    @GameTest(maxTicks = 40)
    public void antiBallisticMissileRegistration(GameTestHelper helper)
    {
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.MISSILE_SAM).equals(ICBMEntities.MISSILE_SAM),
                "MISSILE_SAM entity type must be registered as " + ICBMEntities.MISSILE_SAM);

        helper.assertTrue(ModItems.ANTI_BALLISTIC_MISSILE != null,
                "ANTI_BALLISTIC_MISSILE item must be registered");

        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ModItems.ANTI_BALLISTIC_MISSILE).equals(ICBMEntities.MISSILE_SAM),
                "ANTI_BALLISTIC_MISSILE item key must match " + ICBMEntities.MISSILE_SAM);

        final ItemStack samStack = new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
        helper.assertTrue(ICBMClassicHelpers.isMissile(samStack),
                "ANTI_BALLISTIC_MISSILE item must be recognized as a missile");

        final ICapabilityMissileStack cap = IcbmLookups.MISSILE_STACK.find(samStack, null);
        helper.assertTrue(cap != null, "missile stack capability must be present on SAM stack");
        helper.assertTrue(cap.getMissileId().equals("icbmclassic:missile.sam"),
                "missile id must be icbmclassic:missile.sam, got: " + cap.getMissileId());

        final IMissile spawned = cap.newMissile(helper.getLevel());
        helper.assertTrue(spawned != null && spawned.getMissileEntity() instanceof EntitySurfaceToAirMissile,
                "newMissile must produce an EntitySurfaceToAirMissile");

        helper.succeed();
    }

    /**
     * Verifies that SAMTargetData scans and acquires a nearby flying missile target via AABB.
     */
    @GameTest(maxTicks = 40)
    public void samScanLogicAcquiresTarget(GameTestHelper helper)
    {
        final BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        final BlockPos targetPos = helper.absolutePos(new BlockPos(4, 4, 4));

        final EntitySurfaceToAirMissile sam = new EntitySurfaceToAirMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        sam.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(sam);

        final EntityExplosiveMissile targetMissile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        targetMissile.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        final Item condensedItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        targetMissile.explosive.setStack(new ItemStack(condensedItem));
        targetMissile.initHealth(targetMissile.getMaxHealth());
        helper.getLevel().addFreshEntity(targetMissile);

        sam.scanLogic.refreshTargets();

        helper.assertTrue(sam.scanLogic.getTarget() == targetMissile,
                "SAM scanLogic should acquire the nearby target missile");

        sam.discard();
        targetMissile.discard();
        helper.succeed();
    }

    /**
     * Verifies the direct proximity impact logic: when distance <= FLIGHT_SPEED, the SAM
     * damages the target and is discarded.
     */
    @GameTest(maxTicks = 40)
    public void samProximityImpactDestroysTarget(GameTestHelper helper)
    {
        final BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));

        final EntityExplosiveMissile targetMissile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        targetMissile.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final Item condensedItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        targetMissile.explosive.setStack(new ItemStack(condensedItem));
        targetMissile.initHealth(ConfigMissile.TIER_1_HEALTH);
        helper.getLevel().addFreshEntity(targetMissile);

        final EntitySurfaceToAirMissile sam = new EntitySurfaceToAirMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        // Place SAM 1 block away (distance 1.0 <= FLIGHT_SPEED 4.0)
        sam.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(sam);

        sam.assignTarget(targetMissile);

        // Tick the SAM to execute proximity check
        sam.tick();

        helper.assertTrue(!targetMissile.isAlive(),
                "target missile should be destroyed by SAM proximity attack");
        helper.assertTrue(sam.isRemoved(),
                "SAM should be discarded upon impact");

        helper.succeed();
    }

    /**
     * Spawns a target missile flying with DirectFlightLogic and a SAM with assignTarget.
     * Asserts that within 200 ticks the target missile is destroyed by the SAM interceptor.
     */
    @GameTest(maxTicks = 200, padding = 16)
    public void samInterceptsAndDestroysTargetMissile(GameTestHelper helper)
    {
        // Support floors
        final BlockPos originLocal = new BlockPos(1, 1, 1);
        final BlockPos originAbs = helper.absolutePos(originLocal);
        helper.setBlock(originLocal.below(), Blocks.STONE);

        final BlockPos targetLocal = new BlockPos(1, 6, 1);
        final BlockPos targetAbs = helper.absolutePos(targetLocal);

        // Target missile flying horizontally in +X direction
        final EntityExplosiveMissile targetMissile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        targetMissile.setPos(targetAbs.getX() + 0.5D, targetAbs.getY() + 0.5D, targetAbs.getZ() + 0.5D);
        final Item condensedItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        targetMissile.explosive.setStack(new ItemStack(condensedItem));
        targetMissile.initHealth(ConfigMissile.TIER_1_HEALTH);
        targetMissile.setTargetData(new BasicTargetData(targetAbs.getX() + 30.5D, targetAbs.getY() + 0.5D, targetAbs.getZ() + 0.5D));
        targetMissile.setFlightLogic(new DirectFlightLogic(200));

        // SAM entity
        final EntitySurfaceToAirMissile sam = new EntitySurfaceToAirMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        sam.setPos(originAbs.getX() + 0.5D, originAbs.getY() + 0.5D, originAbs.getZ() + 0.5D);

        helper.getLevel().addFreshEntity(targetMissile);
        helper.getLevel().addFreshEntity(sam);

        targetMissile.launch();
        sam.assignTarget(targetMissile);
        sam.launch();

        helper.succeedWhen(() ->
                helper.assertTrue(!targetMissile.isAlive(),
                        "target missile should be destroyed by SAM within 200 ticks"));
    }
}
