package icbm.classic.content.blast.gas;

import icbm.classic.lib.colors.ColorB;
import icbm.classic.lib.colors.ColorHelper;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Robin Seifert on 4/1/2022.
 * <p>
 * The {@code colors} explosive: a decorative gas cloud that cycles through {@link #COLOR_COUNT} random bright
 * colours. It applies no entity effects ({@link #canEffectEntities()} is false).
 * <p>
 * 26.3 port: {@code world.rand.nextFloat()} -> {@code world().getRandom().nextFloat()}. The colour list is
 * still generated once per game. Note {@code ColorB} stores components as signed bytes, so the 1.12 code
 * already fed negative colour floats to the particle for any component above 127; that is kept (do not fix
 * 1.12 bugs).
 */
public class BlastColor extends BlastGasBase
{
    public static final int DURATION = 20 * 30; //TODO move to config

    /** Number of particle colors to use */
    public static final int COLOR_COUNT = 100;

    /** Random set of particle colors */
    public static final List<ColorB> PARTICLE_COLORS = new ArrayList<>(COLOR_COUNT);

    /** True of random colors have been setup, done once per game */
    private static boolean hasSetupColors = false;

    private int currentColorIndex = 0;

    public BlastColor()
    {
        setDuration(DURATION);
    }

    @Override
    protected boolean setupBlast()
    {
        if (!hasSetupColors)
        {
            generateRandomColors();
            hasSetupColors = true;
        }
        return true;
    }

    @Override
    protected boolean canEffectEntities()
    {
        return false;
    }

    private void generateRandomColors()
    {
        for (int i = 0; i < COLOR_COUNT; i++)
        {
            final float hue = world().getRandom().nextFloat();
            final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
            final float luminance = 0.5f; //1.0 for brighter, 0.0 for black
            PARTICLE_COLORS.add(ColorHelper.HSBtoRGB(hue, saturation, luminance));
        }
    }

    @Override
    protected void spawnGasParticles(final Vec3i pos)
    {
        super.spawnGasParticles(pos);
        currentColorIndex = (currentColorIndex + 1) % COLOR_COUNT;
    }

    @Override
    protected float getParticleColorRed(final Vec3i pos)
    {
        if(currentColorIndex >= 0 && currentColorIndex < PARTICLE_COLORS.size())
        {
            return PARTICLE_COLORS.get(currentColorIndex).getRed();
        }
        return 1;
    }

    @Override
    protected float getParticleColorGreen(final Vec3i pos)
    {
        if(currentColorIndex >= 0 && currentColorIndex < PARTICLE_COLORS.size())
        {
            return PARTICLE_COLORS.get(currentColorIndex).getGreen();
        }
        return 1;
    }

    @Override
    protected float getParticleColorBlue(final Vec3i pos)
    {
        if(currentColorIndex >= 0 && currentColorIndex < PARTICLE_COLORS.size())
        {
            return PARTICLE_COLORS.get(currentColorIndex).getBlue();
        }
        return 1;
    }
}
