package icbm.classic.content.missile.logic.flight;

import icbm.classic.content.missile.logic.flight.ArcFlightLogic.ArcParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S4.01：{@link ArcFlightLogic#calculateArcParameters(double, double, double, double, double, double, boolean, double)}
 * 的弧线参数计算（1.12 {@code ArcFlightLogic#calculatePath} 的纯数学部分）。
 * <p>
 * 期望值按 1.12 公式手算（2 tick/米、1000 上限、>100 米加 160 初始高度、>1000 米高度乘 3），
 * 不依赖实体、世界或注册表。
 */
class ArcFlightLogicMathTest {

    private static final double EPSILON = 1e-9;

    @Test
    void flatHundredBuildsArcWithParabolaConstants() {
        // flatDistance=100 -> heightToDistanceScale=1, initialArcHeight=0, arcHeightMax=100
        // flightTime=ceil(2*100)=200, acceleration=((100-1)*1)/(200/2)/(0.5*100)=99/5000
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(0, 64, 0, 100, 64, 0, false, 500);

        assertFalse(path.flightUpAlways());
        assertEquals(200, path.missileFlightTime());
        assertEquals(0.0198, path.acceleration(), EPSILON);
        assertEquals(100.0, path.deltaX(), EPSILON);
        assertEquals(0.0, path.deltaY(), EPSILON);
        assertEquals(0.0, path.deltaZ(), EPSILON);
    }

    @Test
    void flatAtLimitFallsBackToStraightUpFlight() {
        // 1.12 used `< arcDistanceLimit`, so a distance of exactly the limit is not an arc
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(0, 64, 0, 500, 64, 0, false, 500);

        assertTrue(path.flightUpAlways());
        assertEquals(0, path.missileFlightTime());
        assertEquals(0f, path.acceleration(), EPSILON);
        assertEquals(500.0, path.deltaX(), EPSILON);
    }

    @Test
    void flatFiveHundredBuildsArcWithInitialHeight() {
        // flatDistance=500 -> initialArcHeight=160, arcHeightMax=660, flightTime=1000
        // acceleration=((660-1.32)*1.32)/(1000/2)/(0.66*500)=869.4576/500/330
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(0, 64, 0, 500, 64, 0, false, 1000);

        assertFalse(path.flightUpAlways());
        assertEquals(1000, path.missileFlightTime());
        assertEquals(0.00526944, path.acceleration(), EPSILON);
    }

    @Test
    void flatOverThousandUsesTripleHeightScaleAndMaxHeight() {
        // flatDistance=1200 -> heightToDistanceScale=3, arcHeightMax=min(1000, 160+3600)=1000, flightTime=2400
        // acceleration=((1000-0.8333333)*0.8333333)/(2400/2)/(0.4166667*1200)
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(0, 64, 0, 1200, 64, 0, false, 5000);

        assertFalse(path.flightUpAlways());
        assertEquals(2400, path.missileFlightTime());
        assertEquals(0.0013877315, path.acceleration(), EPSILON);
    }

    @Test
    void playerRidingForcesArcPastTheDistanceLimit() {
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(0, 64, 0, 0, 64, 2000, true, 500);

        assertFalse(path.flightUpAlways());
        assertEquals(4000, path.missileFlightTime());
        assertTrue(path.acceleration() > 0);
        assertEquals(2000.0, path.deltaZ(), EPSILON);
    }

    @Test
    void deltaIsMeasuredFromStartToTargetOnAllAxes() {
        final ArcParameters path = ArcFlightLogic.calculateArcParameters(10, 20, 30, 40, 25, 80, false, 500);

        assertEquals(30.0, path.deltaX(), EPSILON);
        assertEquals(5.0, path.deltaY(), EPSILON);
        assertEquals(50.0, path.deltaZ(), EPSILON);
        // flatDistance=sqrt(30^2+50^2)=58.3095.. -> flightTime=ceil(2*58.3095..)=117
        assertEquals(117, path.missileFlightTime());
    }
}
