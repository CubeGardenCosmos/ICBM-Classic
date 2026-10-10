package icbm.classic.test.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModDataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Covers the S2.01 registration skeleton:
 *
 * <ul>
 *     <li>every data component from registry-ids.md §6 (except the battery, which S6 maps to the Team
 *     Reborn Energy item component) is registered under its documented id,</li>
 *     <li>the static fields of {@link ModDataComponents} were assigned, so item code can reference them.</li>
 * </ul>
 */
public class RegistrySmokeGameTest
{
    @GameTest(maxTicks = 20)
    public void dataComponentsRegistered(GameTestHelper helper)
    {
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ICBMConstants.id("gps_data")),
                "gps_data data component is not registered");
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ICBMConstants.id("explosive_settings")),
                "explosive_settings data component is not registered");
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ICBMConstants.id("radio_channel")),
                "radio_channel data component is not registered");
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ICBMConstants.id("held_item")),
                "held_item data component is not registered");

        helper.assertTrue(ModDataComponents.GPS_DATA != null, "ModDataComponents.GPS_DATA was not assigned");
        helper.assertTrue(ModDataComponents.EXPLOSIVE_SETTINGS != null, "ModDataComponents.EXPLOSIVE_SETTINGS was not assigned");
        helper.assertTrue(ModDataComponents.RADIO_CHANNEL != null, "ModDataComponents.RADIO_CHANNEL was not assigned");
        helper.assertTrue(ModDataComponents.HELD_ITEM != null, "ModDataComponents.HELD_ITEM was not assigned");

        helper.succeed();
    }
}
