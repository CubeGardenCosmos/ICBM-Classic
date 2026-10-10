package icbm.classic.test.api;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.lib.lookup.IcbmLookups;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Covers the S1.10 API lookup layer:
 *
 * <ul>
 *     <li>every lookup registers the id it is documented with (a renamed id would silently break third
 *     party integrations, so the ids are asserted instead of just being non-null),</li>
 *     <li>{@link ICBMClassicHelpers#getMissile} returns null for null and for non missile entities,</li>
 *     <li>a vanilla stone stack is not a missile,</li>
 *     <li>the EMP default registration performed by {@code IcbmLookups#init()} actually answers for a
 *     creeper and a dropped item.</li>
 * </ul>
 */
public class ApiLookupsGameTest
{
    @GameTest(maxTicks = 40)
    public void lookupIdsAndMissileQueries(GameTestHelper helper)
    {
        helper.assertTrue(ICBMConstants.id("missile").equals(IcbmLookups.MISSILE.getId()),
                "missile lookup id mismatch, expected " + ICBMConstants.id("missile") + " but got " + IcbmLookups.MISSILE.getId());
        helper.assertTrue(ICBMConstants.id("missile_stack").equals(IcbmLookups.MISSILE_STACK.getId()),
                "missile stack lookup id mismatch, expected " + ICBMConstants.id("missile_stack") + " but got " + IcbmLookups.MISSILE_STACK.getId());
        helper.assertTrue(ICBMConstants.id("missile_launcher").equals(IcbmLookups.MISSILE_LAUNCHER.getId()),
                "launcher lookup id mismatch, expected " + ICBMConstants.id("missile_launcher") + " but got " + IcbmLookups.MISSILE_LAUNCHER.getId());
        helper.assertTrue(ICBMConstants.id("missile_holder").equals(IcbmLookups.MISSILE_HOLDER.getId()),
                "holder lookup id mismatch, expected " + ICBMConstants.id("missile_holder") + " but got " + IcbmLookups.MISSILE_HOLDER.getId());
        helper.assertTrue(ICBMConstants.id("emp_receiver").equals(IcbmLookups.EMP_RECEIVER_ENTITY.getId()),
                "emp entity lookup id mismatch, expected " + ICBMConstants.id("emp_receiver") + " but got " + IcbmLookups.EMP_RECEIVER_ENTITY.getId());

        helper.assertTrue(ICBMClassicHelpers.getMissile(null) == null,
                "getMissile(null) should be null");
        helper.assertTrue(!ICBMClassicHelpers.isMissile((net.minecraft.world.entity.Entity) null),
                "isMissile(null) should be false");

        //A plain vanilla item stack is not a missile
        helper.assertTrue(!ICBMClassicHelpers.isMissile(new ItemStack(Items.STONE)),
                "a stone stack should not be a missile");

        //A non missile entity has no missile attached
        final Creeper creeper = EntityTypes.CREEPER.create(helper.getLevel(), EntitySpawnReason.EVENT);
        if (creeper == null)
        {
            helper.fail("failed to create a creeper for the entity lookup test");
            return;
        }
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        creeper.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(creeper);

        helper.assertTrue(ICBMClassicHelpers.getMissile(creeper) == null,
                "a creeper should not be a missile");
        helper.assertTrue(!ICBMClassicHelpers.isMissile(creeper),
                "a creeper should not be a missile");

        //The vanilla EMP defaults registered by IcbmLookups#init() must answer
        final IEMPReceiver creeperEmp = IcbmLookups.EMP_RECEIVER_ENTITY.find(creeper, null);
        helper.assertTrue(creeperEmp != null,
                "creeper should receive EMP through the emp_receiver entity lookup");
        if (creeperEmp != null)
        {
            helper.assertTrue(creeperEmp.getClass().getSimpleName().equals("CapabilityEmpCreeper"),
                    "creeper EMP receiver should be CapabilityEmpCreeper but was " + creeperEmp.getClass().getSimpleName());
        }

        //Dropped items must be EMP receivers too (1.12 AttachCapabilitiesEvent<Entity> - EntityItem)
        final ItemEntity dropped = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5,
                new ItemStack(Items.STONE));
        helper.getLevel().addFreshEntity(dropped);
        helper.assertTrue(IcbmLookups.EMP_RECEIVER_ENTITY.find(dropped, null) != null,
                "dropped items should receive EMP through the emp_receiver entity lookup");

        helper.succeed();
    }
}
