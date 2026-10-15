package icbm.classic.client.render.tile;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Extracted per-frame data for the legacy {@code TESRLauncherBase}. */
public final class LauncherBaseRenderState extends BlockEntityRenderState
{
    public final ItemStackRenderState missile = new ItemStackRenderState();
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float yaw;
    public float pitch;
}
