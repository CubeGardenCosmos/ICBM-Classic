package icbm.classic.lib.capability.ex;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Used by any item that has explosive data.
 * <p>
 * 1.12 read the explosive id from the item metadata and qualified as a Forge capability provider
 * ({@code ICapabilitySerializable}). 26.3 items are flat, so the id is passed in by the item and the
 * optional {@code icbmclassic:explosive_settings} data component is added in S2.02. Save data goes
 * through {@link ISavable} ({@code docs/architecture/porting-guide.md} §4).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class CapabilityExplosiveStack implements IExplosive, ISavable
{
    public static final String CUSTOMIZATIONS_KEY = "customizations";

    private final ItemStack stack;
    private final Identifier explosiveId;
    private final List<IExplosiveCustomization> customizationList = new ArrayList();

    /**
     * @param stack       item the explosive belongs to, may be null for a detached instance
     * @param explosiveId id of the explosive, see {@code docs/architecture/registry-ids.md} §3
     */
    // TODO(S2.05) replace the explicit id with the icbmclassic:explosive_settings data component once the
    //  flat explosive items exist (ModItems, S2.05); 1.12 used the item metadata for this
    public CapabilityExplosiveStack(ItemStack stack, Identifier explosiveId)
    {
        this.stack = stack;
        this.explosiveId = explosiveId;
    }

    /**
     * Builds the explosive of a flat explosive item (ADR 0002), the 26.3 replacement of
     * {@code ItemStack#getCapability(ICBMClassicAPI.EXPLOSIVE_CAPABILITY, null)}.
     * <p>
     * A block explosive carries its id on the block (1.12 used the item metadata, and the flat block item is
     * the only explosive item of this step); the flat missile, grenade and bomb cart items have their own id
     * and are added by S4/S5.
     *
     * @param stack to read
     * @return explosive of the stack, null when the item is not an explosive (1.12 {@code hasCapability})
     */
    @Nullable
    public static CapabilityExplosiveStack fromStack(ItemStack stack)
    {
        if (stack != null && !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ExplosiveBlock explosiveBlock)
        {
            return new CapabilityExplosiveStack(stack, explosiveBlock.getExplosiveId());
        }
        return null;
    }

    public Identifier getExplosiveId()
    {
        return explosiveId;
    }

    @Nullable
    @Override
    public IExplosiveData getExplosiveData()
    {
        return explosiveId == null ? null : ICBMClassicHelpers.getExplosive(explosiveId, true);
    }

    @Override
    public void applyCustomizations(IBlast blast) {
        customizationList.forEach(c -> c.apply(getExplosiveData(), blast));
    }

    @Override
    public void addCustomization(IExplosiveCustomization customization) {
        customizationList.add(customization);
    }

    @Override
    public void collectInformation(Consumer<String> collector){
        //TODO debug explosive id and name if F3+something keybind is enabled
        customizationList.forEach(c -> c.collectCustomizationInformation(collector));
    }

    @Nullable
    @Override
    public ItemStack toStack()
    {
        if (stack == null)
        {
            return ItemStack.EMPTY;
        }

        //TODO save customizations
        return stack.copyWithCount(1);
    }

    @Override
    public void save(ValueOutput out)
    {
        final ValueOutput.ValueOutputList list = out.childrenList(CUSTOMIZATIONS_KEY);
        for (IExplosiveCustomization customization : customizationList)
        {
            ICBMClassicAPI.EXPLOSIVE_CUSTOMIZATION_REGISTRY.save(customization, list.addChild());
        }
    }

    @Override
    public void load(ValueInput in)
    {
        in.childrenList(CUSTOMIZATIONS_KEY).ifPresent(list ->
                ICBMClassicAPI.EXPLOSIVE_CUSTOMIZATION_REGISTRY.load(list, customizationList));
    }
}
