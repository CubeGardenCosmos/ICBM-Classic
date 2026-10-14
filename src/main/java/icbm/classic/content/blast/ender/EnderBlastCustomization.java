package icbm.classic.content.blast.ender;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Target coordinates the ender explosive teleports its victims to, set on the item/block before the
 * explosive is triggered.
 * <p>
 * 1.12 stored a numeric dimension id plus a position ({@code Integer dim} + {@code Vec3 pos}) and pushed
 * both into the explosive capability of the item/block. 26.3 items are flat, so the target lives in the
 * {@code icbmclassic:explosive_settings} component as {@link ExplosiveSettings#enderTarget()} and this
 * class is the view the blast and the tooltips keep using:
 * <ul>
 *     <li>the dimension id is an {@link Identifier} now (1.12 {@code Integer}, see
 *     {@link icbm.classic.api.caps.IGPSData#getWorldId()}),</li>
 *     <li>{@link #toGlobalPos()} / {@link #fromSettings(ExplosiveSettings)} convert between the two
 *     shapes; a {@link GlobalPos} needs both halves, 1.12 allowed a position without a dimension so the
 *     conversion returns an empty optional when the dimension is missing,</li>
 *     <li>the dimension is <b>only</b> used by the tooltip. 1.12 {@code apply(...)} only forwarded the
 *     position and the blast never changed dimension, the port keeps that behaviour.</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class EnderBlastCustomization implements IExplosiveCustomization, ISavable
{
    // 1.12 built the key as the mod domain + "ender" (icbmclassic:ender)
    public static final Identifier NAME = ICBMConstants.id("ender");

    private Identifier dim;
    private Vec3 pos;

    private String posTooltip;
    private String dimTooltip;

    public EnderBlastCustomization()
    {
        // Empty for save/load and the registry builder
    }

    public EnderBlastCustomization(Identifier dim, Vec3 pos)
    {
        this.dim = dim;
        this.pos = pos;
    }

    /**
     * Builds the customization from the value stored in the explosive settings component
     * (1.12 {@code ExplosiveInit#encodeEnderCoordSet} created this object from {@code IGPSData}).
     *
     * @param target dimension + block position of the target
     */
    public EnderBlastCustomization(GlobalPos target)
    {
        // 1.12 stored the raw GPS position, GlobalPos only carries the block position of the target
        this(target.dimension().identifier(), Vec3.atCenterOf(target.pos()));
    }

    public Identifier getDim()
    {
        return dim;
    }

    public void setDim(Identifier dim)
    {
        if (!Objects.equals(dim, this.dim))
        {
            dimTooltip = null;
        }
        this.dim = dim;
    }

    public Vec3 getPos()
    {
        return pos;
    }

    public void setPos(Vec3 pos)
    {
        if (!Objects.equals(pos, this.pos))
        {
            posTooltip = null;
        }
        this.pos = pos;
    }

    /**
     * 26.3 view of the 1.12 dimension id + position pair, used to move the target in and out of the
     * {@code icbmclassic:explosive_settings} data component.
     *
     * @return the target, empty when the position or the dimension is missing (1.12 allowed a position
     * without a dimension, a {@link GlobalPos} requires both)
     */
    public Optional<GlobalPos> toGlobalPos()
    {
        if (dim == null || pos == null)
        {
            return Optional.empty();
        }
        return Optional.of(new GlobalPos(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.containing(pos)));
    }

    /**
     * Reads the ender target of an explosive item/block.
     * <p>
     * 1.12 built this customization during the item/block interaction and attached it to the explosive
     * capability of the stack ({@code ICBMClassicAPI.EXPLOSIVE_CUSTOMIZATION_REGISTRY} was only used to
     * save/load that list). S3.09 (block) and S5 (missile) call this when they create the blast, the
     * customization is then applied with {@link #apply(IExplosiveData, IBlast)}.
     *
     * @param settings component value of the explosive item/block
     * @return customization carrying the target, null when the component has no ender target
     */
    @Nullable
    public static EnderBlastCustomization fromSettings(ExplosiveSettings settings)
    {
        return settings.enderTarget().map(EnderBlastCustomization::new).orElse(null);
    }

    @Override
    public void collectCustomizationInformation(Consumer<String> collector)
    {
        if (pos != null)
        {
            if (posTooltip == null)
            {
                //1.12: LanguageUtility.buildToolTipString(new TextComponentTranslation("explosive.icbmclassic:ender.pos", pos.x, pos.y, pos.z))
                posTooltip = Component.translatable("icbmclassic.explosive.ender.pos", pos.x, pos.y, pos.z).getString();
            }
            collector.accept(posTooltip);
        }
        if (dim != null)
        {
            if (dimTooltip == null)
            {
                //1.12 resolved the name through DimensionManager#getWorld(dim) -> WorldInfo#getWorldName and
                //fell back to "???" when the dimension was not loaded. 26.3 has no global level lookup and
                //this tooltip has no server context, so the 1.12 fallback is used.
                final String worldName = "???";
                //1.12: LanguageUtility.buildToolTipString(new TextComponentTranslation("explosive.icbmclassic:ender.world", dim, worldName))
                dimTooltip = Component.translatable("icbmclassic.explosive.ender.world", dim.toString(), worldName).getString();
            }
            collector.accept(dimTooltip);
        }
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return NAME;
    }

    @NonNull
    @Override
    public IBuilderRegistry<IExplosiveCustomization> getRegistry()
    {
        return ICBMClassicAPI.EXPLOSIVE_CUSTOMIZATION_REGISTRY;
    }

    @Override
    public void apply(IExplosiveData explosiveData, IBlast blast)
    {
        //1.12: if(blast instanceof BlastEnder) { ((BlastEnder) blast).setTeleportTarget(pos); }
        if (blast instanceof BlastEnder enderBlast)
        {
            enderBlast.setTeleportTarget(pos);
        }
    }

    @Override
    public void save(ValueOutput out) //1.12: serializeNBT()
    {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) //1.12: deserializeNBT(nbt compound)
    {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<EnderBlastCustomization> SAVE_LOGIC = new SaveHandler<EnderBlastCustomization>()
        .mainRoot()
        //1.12 stored the position with nodeVec3d and the dimension with nodeInteger (numeric id)
        /* */.nodeVec3("pos", EnderBlastCustomization::getPos, EnderBlastCustomization::setPos)
        //1.12: .nodeInteger("dim", EnderBlastCustomization::getDim, EnderBlastCustomization::setDim)
        /* */.nodeIdentifier("dim", EnderBlastCustomization::getDim, EnderBlastCustomization::setDim)
        .base();
}
