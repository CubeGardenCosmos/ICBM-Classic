package icbm.classic.lib.data;

import net.minecraft.core.Direction;

import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.core.Direction.EAST;
import static net.minecraft.core.Direction.NORTH;
import static net.minecraft.core.Direction.SOUTH;
import static net.minecraft.core.Direction.UP;
import static net.minecraft.core.Direction.WEST;

/**
 * Rotation tables used to rotate a direction around a face, 1.12
 * {@code icbm.classic.lib.data.FaceRotations} with the 1.12 facing enum replaced by {@link Direction}.
 * <p>
 * Both enums declare the same order ({@code DOWN, UP, NORTH, SOUTH, WEST, EAST}) and the tables are indexed by
 * {@link Direction#ordinal()}, so {@link #CW_FACE} maps a face to its own clockwise rotation table. 1.12 wrote
 * the array under the assumption that {@code UP} came first (see its comment) while the 1.12 facing enum starts
 * with {@code DOWN}, so the array literal of the port is ordered {@code DOWN, UP, NORTH, SOUTH, WEST, EAST} to
 * put every table back on the face it rotates around.
 * <p>
 * Created by Dark(DarkGuardsman, Robin).
 */
public class FaceRotations
{
    public static final Direction[] CW_UP_AXIS = new RotationBuilder()
        .start(NORTH)
        .rotation(NORTH, EAST)
        .rotation(EAST, SOUTH)
        .rotation(SOUTH, WEST)
        .rotation(WEST, NORTH)
        .build();

    public static final Direction[] CCW_UP_AXIS = new RotationBuilder()
        .start(NORTH)
        .rotation(NORTH, WEST)
        .rotation(EAST, NORTH)
        .rotation(SOUTH, EAST)
        .rotation(WEST, SOUTH)
        .build();

    public static final Direction[] CW_NORTH_AXIS = new RotationBuilder()
        .start(UP)
        .rotation(UP, WEST)
        .rotation(WEST, DOWN)
        .rotation(DOWN, EAST)
        .rotation(EAST, UP)
        .build();

    public static final Direction[] CW_EAST_AXIS = new RotationBuilder()
        .start(UP)
        .rotation(UP, NORTH)
        .rotation(NORTH, DOWN)
        .rotation(DOWN, SOUTH)
        .rotation(SOUTH, UP)
        .build();

    public static final Direction[] CW_SOUTH_AXIS = new RotationBuilder()
        .start(UP)
        .rotation(UP, EAST)
        .rotation(EAST, DOWN)
        .rotation(DOWN, WEST)
        .rotation(WEST, UP)
        .build();

    public static final Direction[] CW_WEST_AXIS = new RotationBuilder()
        .start(UP)
        .rotation(UP, SOUTH)
        .rotation(SOUTH, DOWN)
        .rotation(DOWN, NORTH)
        .rotation(NORTH, UP)
        .build();

    public static final Direction[] CW_DOWN_AXIS = new RotationBuilder()
        .start(NORTH)
        .rotation(NORTH, WEST)
        .rotation(WEST, SOUTH)
        .rotation(SOUTH, EAST)
        .rotation(EAST, NORTH)
        .build();

    /** Rotates around the face clockwise, indexed by {@link Direction#ordinal()} */
    public static final Direction[][] CW_FACE = new Direction[][] {
        // DOWN
        CW_DOWN_AXIS,
        // UP
        CW_UP_AXIS,
        // North
        CW_NORTH_AXIS,
        // South
        CW_SOUTH_AXIS,
        // West
        CW_WEST_AXIS,
        // East
        CW_EAST_AXIS
    };

    /**
     * Rotates clockwise, towards right relative to self on a clock face
     * <p>
     * Clockwise {@see https://en.wikipedia.org/wiki/Clockwise}
     *
     * @param face    to rotate around
     * @param current rotation
     * @return next rotation, if current is same as face or inverse of face it will pick starting rotation
     */
    public static Direction rotateRight(Direction face, Direction current) {
        return CW_FACE[face.ordinal()][current.ordinal()];
    }

    /**
     * Rotates counter-clockwise, towards left relative to self on a clock face
     * <p>
     * Clockwise {@see https://en.wikipedia.org/wiki/Clockwise}
     *
     * @param face    to rotate around
     * @param current rotation
     * @return next rotation, if current is same as face or inverse of face it will pick starting rotation
     */
    public static Direction rotateLeft(Direction face, Direction current) {
        return CW_FACE[face.ordinal()][current.ordinal()].getOpposite();
    }

    public static class RotationBuilder
    {
        private final Direction[] rotations = new Direction[6];

        public RotationBuilder() {}

        public RotationBuilder(Direction[] sides)
        {
            System.arraycopy(sides, 0, rotations, 0, 6);
        }

        public RotationBuilder rotation(Direction input, Direction output)
        {
            rotations[input.ordinal()] = output;
            return this;
        }

        public RotationBuilder start(Direction face)
        {
            for (int i = 0; i < 6; i++)
            {
                this.rotations[i] = face;
            }
            return this;
        }

        public Direction[] build()
        {
            final Direction[] rotations = new Direction[6];
            System.arraycopy(this.rotations, 0, rotations, 0, 6);
            return rotations;
        }
    }
}
