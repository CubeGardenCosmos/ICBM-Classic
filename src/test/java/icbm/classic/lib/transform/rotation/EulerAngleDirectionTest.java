package icbm.classic.lib.transform.rotation;

import com.builtbroken.jlib.data.vector.IPos3D;
import icbm.classic.lib.transform.vector.Pos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S1.01：EulerAngle 由 yaw/pitch 生成方向向量。
 * <p>
 * 期望值由 1.12 原公式在纸面推导（同一公式，见 {@link EulerAngle#x()} 等）：
 * <pre>
 * x() = -sin(toRadians(yaw)) * cos(toRadians(pitch))
 * y() =  sin(toRadians(pitch))
 * z() =  sin(-cos(toRadians(yaw)) * cos(toRadians(pitch)))
 * </pre>
 * pitch 固定 -45°，cos(45°)=sin(45°)=0.7071067811865476。
 * <ul>
 * <li>yaw=0：x=0，y=-0.7071067811865475，z=sin(-0.7071067811865476)=-0.6496369390800625</li>
 * <li>yaw=90：x=-0.7071067811865476，y=-0.7071067811865475，z=sin(-0.0·0.707…)=-4.329780281177467e-17</li>
 * <li>yaw=180：x=-8.659560562354934e-17，y=-0.7071067811865475，z=sin(0.7071067811865476)=0.6496369390800625</li>
 * </ul>
 */
class EulerAngleDirectionTest {

    private static final double COS_45 = 0.7071067811865476;
    private static final double SIN_45 = 0.7071067811865475;
    private static final double DELTA = 1e-6;

    @Test
    void yawZeroPitchMinus45() {
        final EulerAngle angle = new EulerAngle(0, -45);
        assertEquals(0.0, angle.x(), DELTA);
        assertEquals(-SIN_45, angle.y(), DELTA);
        assertEquals(-0.6496369390800625, angle.z(), DELTA);
    }

    @Test
    void yaw90PitchMinus45() {
        final EulerAngle angle = new EulerAngle(90, -45);
        assertEquals(-COS_45, angle.x(), DELTA);
        assertEquals(-SIN_45, angle.y(), DELTA);
        assertEquals(-4.329780281177467e-17, angle.z(), DELTA);
    }

    @Test
    void yaw180PitchMinus45() {
        final EulerAngle angle = new EulerAngle(180, -45);
        assertEquals(-8.659560562354934e-17, angle.x(), DELTA);
        assertEquals(-SIN_45, angle.y(), DELTA);
        assertEquals(0.6496369390800625, angle.z(), DELTA);
    }

    @Test
    void yawAndPitchAreStoredAsGiven() {
        final EulerAngle angle = new EulerAngle(90, -45, 12);
        assertEquals(90.0, angle.yaw(), DELTA);
        assertEquals(-45.0, angle.pitch(), DELTA);
        assertEquals(12.0, angle.roll(), DELTA);
        assertEquals(Math.toRadians(90.0), angle.yaw_radian(), DELTA);
        assertEquals(Math.toRadians(-45.0), angle.pitch_radian(), DELTA);
    }

    @Test
    void transformRotatesVectorUsingGeneratedQuaternion() {
        // yaw=0,pitch=0 时方向向量被原样保留（Quaternion(w=1) 为单位四元数）
        final EulerAngle angle = new EulerAngle(0, 0);
        final IPos3D transformed = angle.transform(new Pos(0, 0, 1));
        assertEquals(0.0, transformed.x(), DELTA);
        assertEquals(0.0, transformed.y(), DELTA);
        assertEquals(1.0, transformed.z(), DELTA);
    }

    @Test
    void moveToAngleClampsAndSteps() {
        assertEquals(10.0, EulerAngle.moveToAngle(0, 90, 10), DELTA);
        assertEquals(90.0, EulerAngle.moveToAngle(0, 90, 200), DELTA);
        assertEquals(5.0, EulerAngle.moveToAngle(5, 90, 0), DELTA);
        assertTrue(EulerAngle.clampAngleTo360(720) <= 360 && EulerAngle.clampAngleTo360(720) >= -360);
    }
}
