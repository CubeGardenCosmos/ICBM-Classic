package icbm.classic.content.blocks.radarstation.data;

import icbm.classic.content.blocks.radarstation.TileRadarStation;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server-generated radar plot, synchronized to open station menus through PacketCodex. */
public class RadarRenderData
{
    public static final int UV_SIZE = 54;

    private final TileRadarStation host;
    private final List<RadarRenderDot> dots = new ArrayList<>();

    public RadarRenderData(TileRadarStation host)
    {
        this.host = host;
    }

    public List<RadarRenderDot> getDots()
    {
        return dots;
    }

    public void update()
    {
        dots.clear();
        final int range = host.getDetectionRange();
        if (range <= 0) return;
        final double x = host.getBlockPos().getX() + 0.5;
        final double z = host.getBlockPos().getZ() + 0.5;
        addDot(x, z, RadarDotType.MARKER);
        addDot(x + range, z, RadarDotType.MARKER);
        addDot(x - range, z, RadarDotType.MARKER);
        addDot(x, z + range, RadarDotType.MARKER);
        addDot(x, z - range, RadarDotType.MARKER);
        final int trigger = host.getTriggerRange();
        addDot(x + trigger, z, RadarDotType.MARKER);
        addDot(x - trigger, z, RadarDotType.MARKER);
        addDot(x, z + trigger, RadarDotType.MARKER);
        addDot(x, z - trigger, RadarDotType.MARKER);
        host.getDetectedThreats().forEach(entity -> addDot(entity.getX(), entity.getZ(), RadarDotType.HOSTILE));
        host.getIncomingThreats().forEach(entity -> addDot(entity.x(), entity.z(), RadarDotType.INCOMING));
    }

    public void addDot(double ex, double ez, RadarDotType type)
    {
        final double deltaX = ex - (host.getBlockPos().getX() + 0.5);
        final double deltaZ = ez - (host.getBlockPos().getZ() + 0.5);
        final double vecX = deltaX / host.getDetectionRange();
        final double vecZ = deltaZ / host.getDetectionRange();
        final int x = (int) Math.floor(vecX * (UV_SIZE / 2f));
        final int z = (int) Math.floor(vecZ * (UV_SIZE / 2f));
        dots.add(new RadarRenderDot(x, z, type));
    }

    public void clear()
    {
        dots.clear();
    }

    public void setDots(List<RadarRenderDot> values)
    {
        dots.clear();
        dots.addAll(values);
    }

    public static List<RadarRenderDot> decodeDots(RegistryFriendlyByteBuf buf)
    {
        final List<RadarRenderDot> values = new ArrayList<>();
        final int dotCount = buf.readInt();
        for (int i = 0; i < dotCount; i++)
        {
            values.add(new RadarRenderDot(buf.readInt(), buf.readInt(), RadarDotType.get(buf.readByte())));
        }
        return values;
    }

    public static void encodeDots(RegistryFriendlyByteBuf buf, List<RadarRenderDot> values)
    {
        buf.writeInt(values.size());
        values.forEach(dot ->
        {
            buf.writeInt(dot.x());
            buf.writeInt(dot.y());
            buf.writeByte(dot.type().ordinal());
        });
    }
}
