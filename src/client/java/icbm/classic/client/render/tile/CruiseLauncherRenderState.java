package icbm.classic.client.render.tile;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Extracted per-frame data for the legacy {@code TESRCruiseLauncher}. */
public final class CruiseLauncherRenderState extends BlockEntityRenderState
{
    public final ItemStackRenderState missile = new ItemStackRenderState();
    public float yaw;
    public float pitch;
}
