package icbm.classic.client.render.tile;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Extracted block models and interpolated motion for the legacy {@code TESREmpTower}. */
public final class EmpTowerRenderState extends BlockEntityRenderState
{
    public final BlockModelRenderState coil = new BlockModelRenderState();
    public final BlockModelRenderState electric = new BlockModelRenderState();
    public float rotation;
    public boolean hasPower;
    public boolean hasElectricModel;
}
