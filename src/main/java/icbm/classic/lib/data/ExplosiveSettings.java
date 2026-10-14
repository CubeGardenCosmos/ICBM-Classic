package icbm.classic.lib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Customization data carried by explosive items in the {@code icbmclassic:explosive_settings} data
 * component. 1.12 stored the same data through {@code IExplosiveCustomization} objects on the
 * explosive capability (item metadata + NBT); 26.3 items are flat so the customization is a component.
 * <p>
 * S2.01 started with an empty record so the component type exists. S3.07 adds the ender target
 * ({@code EnderBlastCustomization} in 1.12 stored the dimension plus a position on the item/block).
 * Later steps add fields (S4.06 cluster settings); dev builds do not need backwards compatibility, and
 * a save written without {@code ender_target} loads as {@link Optional#empty()} through
 * {@code optionalFieldOf}.
 *
 * @param enderTarget dimension + block position the ender explosive teleports its targets to, empty when
 *                    the explosive was never given a target (1.12 {@code EnderBlastCustomization} with a
 *                    null position, which made the ender blast search for a random target instead)
 */
public record ExplosiveSettings(Optional<GlobalPos> enderTarget, Optional<ClusterSettings> cluster)
{
    public static final ExplosiveSettings EMPTY = new ExplosiveSettings(Optional.empty(), Optional.empty());

    public static final Codec<ExplosiveSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("ender_target").forGetter(ExplosiveSettings::enderTarget),
            ClusterSettings.CODEC.optionalFieldOf("cluster").forGetter(ExplosiveSettings::cluster)
    ).apply(instance, ExplosiveSettings::new));

    public static final StreamCodec<ByteBuf, ExplosiveSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), ExplosiveSettings::enderTarget,
            ByteBufCodecs.optional(ClusterSettings.STREAM_CODEC), ExplosiveSettings::cluster,
            ExplosiveSettings::new);

    public ExplosiveSettings(GlobalPos enderTarget)
    {
        this(Optional.of(enderTarget), Optional.empty());
    }

    public ExplosiveSettings(ClusterSettings cluster)
    {
        this(Optional.empty(), Optional.of(cluster));
    }

    public record ClusterSettings(Identifier bombletExplosive, int count)
    {
        public static final Identifier DEFAULT_EXPLOSIVE = Identifier.fromNamespaceAndPath("icbmclassic", "condensed");
        public static final int DEFAULT_COUNT = 100;
        public static final ClusterSettings DEFAULT = new ClusterSettings(DEFAULT_EXPLOSIVE, DEFAULT_COUNT);

        public static final Codec<ClusterSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("bomblet_explosive").forGetter(ClusterSettings::bombletExplosive),
                Codec.INT.fieldOf("count").forGetter(ClusterSettings::count)
        ).apply(instance, ClusterSettings::new));

        public static final StreamCodec<ByteBuf, ClusterSettings> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, ClusterSettings::bombletExplosive,
                ByteBufCodecs.VAR_INT, ClusterSettings::count,
                ClusterSettings::new);
    }
}
