package icbm.classic.prefab.tile;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Marks a block entity as openable by a player, 1.12 {@code icbm.classic.prefab.tile.IGuiTile} ported onto the
 * 26.3 menu flow.
 * <p>
 * 1.12 declared {@code getServerGuiElement}/{@code getClientGuiElement} because Forge's {@code IGuiHandler}
 * needed both sides in one object (the client element was untyped {@code Object} for that reason). 26.3 splits
 * this: the server side opens the menu through {@link ExtendedMenuProvider} (this interface, for block
 * entities that already know their position) and the client screen is bound to the {@code MenuType} in the
 * client source set ({@code MenuScreens.register}, see S5.03). {@code getClientGuiElement} has no counterpart
 * here because the main source set must not reference {@code net.minecraft.client} (golden rule 4).
 * <p>
 * Usage on a machine (see the S5 phase notes):
 * <pre>
 * public class TileLauncherBase extends TileMachine implements IGuiTile {
 *     &#64;Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { ... }
 * }
 * </pre>
 * Created by Robin on 1/8/2015.
 */
public interface IGuiTile extends ExtendedMenuProvider<BlockPos>
{
    /**
     * Position of the machine, 1.12 the block entity position was passed to the GUI handler by Forge.
     *
     * @return block position of this machine, never null
     */
    BlockPos getBlockPos();

    /**
     * Data the client needs to build its menu instance, 26.3 hands the block position over (the client reads
     * the block entity from it, see S5.03).
     */
    @Override
    default BlockPos getScreenOpeningData(ServerPlayer player)
    {
        return getBlockPos();
    }
}
