package icbm.classic.content.blast.redmatter.logic;

import icbm.classic.api.data.Int3Consumer;
import net.minecraft.core.Direction;

/**
 * Builds the shell of relative block offsets the redmatter traces towards each cycle, one wall face at a
 * time.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>The 1.12 facing enum is {@link Direction} and {@code getFrontOffsetX/Y/Z()} became
 *     {@code getStepX/Y/Z()} ({@code Direction#values()} keeps the 1.12 order:
 *     {@code DOWN, UP, NORTH, SOUTH, WEST, EAST}).</li>
 *     <li>Nothing in this class reads the world, it only does integer math over the six directions, so it
 *     runs on whichever thread the caller is on. See {@link RedmatterLogic} for why that caller stays on
 *     the server thread (the S1.09 worker framework is not needed here).</li>
 * </ul>
 * Created by Robin Seifert on 8/7/2021.
 */
public class RedmatterBlockCollector
{
    /**
     * Collects the offsets of all six wall faces of the cube formed by the given size.
     *
     * @param size     half size of the cube, offsets run from {@code -size} to {@code +size}
     * @param consumer receives the x, y and z offsets
     */
    public static void collectBlocksOnWallEdges(int size, Int3Consumer consumer)
    {
        //Loop all wall faces
        for (Direction facing : Direction.values())
        {
            collectBlocksOnWall(size, facing, consumer);
        }
    }

    /**
     * Collects the offsets of a single wall face of the cube formed by the given size.
     *
     * @param size     half size of the cube, offsets run from {@code -size} to {@code +size}
     * @param wall     face to collect
     * @param consumer receives the x, y and z offsets
     */
    public static void collectBlocksOnWall(int size, Direction wall, Int3Consumer consumer)
    {
        //Loop wall face from negative corner to positive corner (-a,-b) to (+a,+b)
        for (int stepA = -size; stepA <= size; stepA++)
        {
            for (int stepB = -size; stepB <= size; stepB++)
            {
                //Offset by wall center point
                //1.12: wall.getFrontOffsetX/Y/Z()
                int rx = wall.getStepX() * size;
                int ry = wall.getStepY() * size;
                int rz = wall.getStepZ() * size;

                //Offset by step position on the wall based on facing
                if (wall == Direction.DOWN || wall == Direction.UP)
                {
                    rx += stepA;
                    rz += stepB;
                }
                else if (wall == Direction.EAST || wall == Direction.WEST)
                {
                    ry += stepA;
                    rz += stepB;
                }
                else if (wall == Direction.NORTH || wall == Direction.SOUTH)
                {
                    ry += stepA;
                    rx += stepB;
                }

                consumer.apply(rx, ry, rz);
            }
        }
    }
}
