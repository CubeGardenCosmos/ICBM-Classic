package icbm.classic.content.gas;

import icbm.classic.config.ConfigMain;
import icbm.classic.config.util.ItemStackConfigList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Handles reading the armor protection values from config and summing them for an entity.
 * <p>
 * The rating of a worn armor piece is looked up by item in {@link #protectionValues} (config driven, see
 * {@link ConfigMain.ProtectiveArmorConfig#ITEMS}), summed over the armor slots and compared by the gas
 * blasts against their own minimum protection value.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>The config list is filled lazily, {@link #setup()} (or {@link #loadFromConfig()}) has to run after
 *     the config was read and the item registry is frozen; the values are resolved per lookup.</li>
 *     <li>Registry keys of the defaults are {@code namespace:path} strings, the 1.12 item metadata suffix no
 *     longer exists (see {@link ItemStackConfigList}).</li>
 *     <li>The head slot and armor iteration use {@link EquipmentSlot#HEAD} and {@link EquipmentSlotGroup#ARMOR}
 *     (limited to the humanoid slots, mirroring the 1.12 armor inventory list).</li>
 * </ul>
 */
public class ProtectiveArmorHandler {

    public static final ItemStackConfigList.FloatOut protectionValues = new ItemStackConfigList.FloatOut("[CargoHolder][Ban/Allow Config]", (configList) -> {
        configList.setDefault("atomicscience:hazmat_color_mask", 0.75f, 1);
        configList.setDefault("atomicscience:hazmat_color_body", 0.15f, 1);
        configList.setDefault("atomicscience:hazmat_color_leggings", 0.05f, 1);
        configList.setDefault("atomicscience:hazmat_color_boots", 0.05f, 1);

        configList.setDefault("atomicscience:hazmat_mask", 0.75f, 1);
        configList.setDefault("atomicscience:hazmat_body", 0.15f, 1);
        configList.setDefault("atomicscience:hazmat_leggings", 0.05f, 1);
        configList.setDefault("atomicscience:hazmat_boots", 0.05f, 1);

        // 1.12 label was 'icbmclassic/main/protective_armor/item_ratings' (a Forge config path); 26.3 reads
        // config/icbmclassic/main.json, section 'protectiveArmor', field 'item_ratings'
        configList.load("icbmclassic/main.json#protectiveArmor.item_ratings", ConfigMain.protectiveArmor.ITEMS);
    });

    private ProtectiveArmorHandler() {
    }

    public static void setup() {
        loadFromConfig();
    }

    public static float getValue(ItemStack itemStack) {
        Float value = protectionValues.getValue(itemStack);
        return value != null ? Math.min(1, Math.max(0, value)) : 0;
    }

    public static void loadFromConfig() {
        protectionValues.reload();
    }

    public static float getProtectionRating(LivingEntity entityLivingBase) {

        if(ConfigMain.protectiveArmor.requireHelmet && entityLivingBase.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return 0;
        }

        float value = 0;
        for(EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if(slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                value += getValue(entityLivingBase.getItemBySlot(slot));
            }
        }
        return value;
    }
}
