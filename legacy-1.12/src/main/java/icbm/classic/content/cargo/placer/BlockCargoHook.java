package icbm.classic.content.cargo.placer;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Logic block that is placed by parachute, or other cargo projectile, to act as a cargo hook and trigger
 * mechanism.
 */
public class BlockCargoHook extends BlockContainer {

    public static final PropertyBool TICKING_PROPERTY = PropertyBool.create("ticking");

    protected BlockCargoHook() {
        super(Material.IRON);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, TICKING_PROPERTY);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        if(meta == 1) {
            return new TileEntityCargoHook();
        }
        return new TileEntityCargoHookTicking();
    }
}
