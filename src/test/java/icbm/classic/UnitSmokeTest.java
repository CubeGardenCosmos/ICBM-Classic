package icbm.classic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit 5 冒烟测试：证明纯逻辑测试管线可用（S0.04）。
 * 不引导 Minecraft 注册表，只碰 {@link ICBMConstants#id(String)} 这类纯数据路径。
 */
class UnitSmokeTest {

    @Test
    void idUsesModNamespace() {
        assertEquals("icbmclassic:x", ICBMConstants.id("x").toString());
    }
}
