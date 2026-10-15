package icbm.classic.test.missile;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.refs.ICBMEntities;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.lookup.IcbmLookups;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Registration regression coverage for the anti-ballistic missile. */
public class SamInterceptorGameTest
{
    @GameTest(maxTicks = 40)
    public void antiBallisticMissileRegistration(GameTestHelper helper)
    {
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.MISSILE_SAM).equals(ICBMEntities.MISSILE_SAM),
                "ABM entity type keeps its registry ID");
        helper.assertTrue(ModItems.ANTI_BALLISTIC_MISSILE != null, "ABM item must be registered");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ModItems.ANTI_BALLISTIC_MISSILE).equals(ICBMEntities.MISSILE_SAM),
                "ABM item keeps its registry ID");

        ItemStack stack = new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
        helper.assertTrue(ICBMClassicHelpers.isMissile(stack), "ABM item must be recognized as a missile");
        ICapabilityMissileStack capability = IcbmLookups.MISSILE_STACK.find(stack, null);
        helper.assertTrue(capability != null, "ABM stack capability must be present");
        IMissile missile = capability.newMissile(helper.getLevel());
        helper.assertTrue(missile != null && missile.getMissileEntity() instanceof EntityAntiBallisticMissile,
                "ABM stack must create EntityAntiBallisticMissile");
        helper.succeed();
    }
}
