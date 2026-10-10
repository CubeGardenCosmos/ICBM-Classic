package icbm.classic.content.blocks.launcher.base;

import icbm.classic.client.render.entity.RenderMissile;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TESRLauncherBase extends TileEntitySpecialRenderer<TileLauncherBase>
{
    private static final float missileOffset = 2f; //TODO add custom render type for missiles as static blocks so offset can be driven by model
    private static final List<Vec3d> FACE_OFFSETS = Arrays.stream(EnumFacing.values())
        .map(direction -> new Vec3d(
            0.5 + direction.getFrontOffsetX() * missileOffset,
            0.5 + direction.getFrontOffsetY() * missileOffset,
            0.5 + direction.getFrontOffsetZ() * missileOffset
        )).collect(Collectors.toList());

    @Override
    public void render(TileLauncherBase launcher, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        //Render missile
        if (!launcher.getMissileStack().isEmpty())
        {
            final Vec3d offset = FACE_OFFSETS.get(launcher.getLaunchDirection().ordinal());
            RenderMissile.INSTANCE.renderItem(launcher.getMissileStack(), launcher.getWorld(),
                x + offset.x, y + offset.y, z + offset.z,
                launcher.getMissileYaw(true), launcher.getMissilePitch(true), partialTicks);
        }
    }
}
