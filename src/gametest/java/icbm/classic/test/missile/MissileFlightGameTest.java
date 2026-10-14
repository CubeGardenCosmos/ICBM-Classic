package icbm.classic.test.missile;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.content.missile.logic.flight.ArcFlightLogic;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.capability.missile.CapabilityMissileStack;
import icbm.classic.lib.lookup.IcbmLookups;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Game tests covering explosive missiles, actionable missiles, and missile items (S4.02).
 */
public class MissileFlightGameTest
{
    /**
     * Asserts that {@link ICBMClassicHelpers#isMissile(ItemStack)} returns true for missile items
     * via the {@link IcbmLookups#MISSILE_STACK} lookup, and false for non-missile items.
     */
    @GameTest(maxTicks = 40)
    public void missileItemIsMissileLookup(GameTestHelper helper)
    {
        final Item condensedMissileItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        helper.assertTrue(condensedMissileItem != null, "condensed missile item must exist");

        final ItemStack missileStack = new ItemStack(condensedMissileItem);
        helper.assertTrue(ICBMClassicHelpers.isMissile(missileStack), "condensed missile item must be a missile");

        final ItemStack steelStack = new ItemStack(ModItems.STEEL_INGOT);
        helper.assertTrue(!ICBMClassicHelpers.isMissile(steelStack), "steel ingot must not be a missile");

        final ICapabilityMissileStack cap = IcbmLookups.MISSILE_STACK.find(missileStack, null);
        helper.assertTrue(cap != null, "missile stack capability must be present");
        helper.assertTrue(cap.getMissileId().contains("condensed"), "missile id should contain 'condensed', got: " + cap.getMissileId());

        final IMissile created = cap.newMissile(helper.getLevel());
        helper.assertTrue(created != null, "newMissile must return a missile");
        helper.assertTrue(created.getMissileEntity() instanceof EntityExplosiveMissile, "entity must be EntityExplosiveMissile");

        final EntityExplosiveMissile exMissile = (EntityExplosiveMissile) created.getMissileEntity();
        helper.assertTrue(ICBMExplosives.CONDENSED.getRegistryKey().equals(exMissile.getExplosiveData().getRegistryKey()),
                "created missile must carry condensed explosive");

        helper.succeed();
    }

    /**
     * Spawns a condensed explosive missile in the test area, sets target to a ground stone block,
     * launches with ArcFlightLogic, and asserts that the stone block is destroyed by the blast within 400 ticks.
     */
    @GameTest(maxTicks = 400, padding = 16)
    public void condensedMissileFliesArcAndDestroysTargetStone(GameTestHelper helper)
    {
        // Place stone target at local (5, 1, 1) and supporting floor at (5, 0, 1)
        final BlockPos targetLocal = new BlockPos(5, 1, 1);
        final BlockPos targetAbs = helper.absolutePos(targetLocal);
        helper.setBlock(targetLocal, Blocks.STONE);
        helper.setBlock(targetLocal.below(), Blocks.STONE);

        // Place floor under origin at local (1, 0, 1)
        final BlockPos originLocal = new BlockPos(1, 1, 1);
        final BlockPos originAbs = helper.absolutePos(originLocal);
        helper.setBlock(originLocal.below(), Blocks.STONE);

        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        missile.setPos(originAbs.getX() + 0.5D, originAbs.getY() + 0.5D, originAbs.getZ() + 0.5D);

        final Item missileItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        missile.explosive.setStack(new ItemStack(missileItem));
        missile.initHealth(missile.getMaxHealth());

        missile.setTargetData(new BasicTargetData(targetAbs.getX() + 0.5D, targetAbs.getY() + 0.5D, targetAbs.getZ() + 0.5D));
        missile.setFlightLogic(new ArcFlightLogic());

        helper.getLevel().addFreshEntity(missile);
        missile.launch();

        helper.succeedWhen(() ->
                helper.assertBlockState(targetLocal, BlockState::isAir,
                        state -> Component.literal("target stone at " + targetLocal + " should be destroyed by missile blast, was " + state)));
    }

    /**
     * Verifies that EntityExplosiveMissile explosive stack and health survive a save/load round trip.
     */
    @GameTest(maxTicks = 40)
    public void explosiveMissileSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final BlockPos originAbs = helper.absolutePos(new BlockPos(2, 2, 2));

        final EntityExplosiveMissile source = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        source.setPos(originAbs.getX() + 0.5D, originAbs.getY() + 0.5D, originAbs.getZ() + 0.5D);
        final Item missileItem = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
        source.explosive.setStack(new ItemStack(missileItem));
        source.initHealth(source.getMaxHealth());

        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        source.addAdditionalSaveData(out);
        final CompoundTag tag = out.buildResult();

        final EntityExplosiveMissile loaded = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        final ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, provider, tag);
        loaded.readAdditionalSaveData(in);

        helper.assertTrue(loaded.getExplosiveData() != null, "loaded missile should have explosive data");
        helper.assertTrue(ICBMExplosives.CONDENSED.getRegistryKey().equals(loaded.getExplosiveData().getRegistryKey()),
                "loaded explosive should be condensed, was: " + loaded.getExplosiveData().getRegistryKey());
        helper.assertTrue(loaded.getMaxHealth() == source.getMaxHealth(), "max health should match");

        helper.succeed();
    }

    /**
     * Verifies that EntityMissileActionable originalStack survives a save/load round trip.
     */
    @GameTest(maxTicks = 40)
    public void actionableMissileSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final BlockPos originAbs = helper.absolutePos(new BlockPos(2, 2, 2));

        final EntityMissileActionable source = new EntityMissileActionable(ModEntities.GENERIC_MISSILE, helper.getLevel());
        source.setPos(originAbs.getX() + 0.5D, originAbs.getY() + 0.5D, originAbs.getZ() + 0.5D);
        final ItemStack stack = new ItemStack(ModItems.MISSILE_MODULE);
        source.setOriginalStack(stack);

        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        source.addAdditionalSaveData(out);
        final CompoundTag tag = out.buildResult();

        final EntityMissileActionable loaded = new EntityMissileActionable(ModEntities.GENERIC_MISSILE, helper.getLevel());
        final ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, provider, tag);
        loaded.readAdditionalSaveData(in);

        helper.assertTrue(!loaded.getOriginalStack().isEmpty(), "loaded original stack should not be empty");
        helper.assertTrue(loaded.getOriginalStack().is(ModItems.MISSILE_MODULE), "loaded stack should be missile module");

        helper.succeed();
    }
}
