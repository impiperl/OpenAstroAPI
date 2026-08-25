package astroapi.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AngleMathTest {

    private static final double EPS = 1e-9;

    @Test
    void signedDelta_isZeroForEqualAngles() {
        assertEquals(0.0, AngleMath.signedDelta(42.0, 42.0), EPS);
        assertEquals(0.0, AngleMath.signedDelta(402.0, 42.0), EPS);
    }

    @Test
    void signedDelta_staysWithinHalfCircle() {
        for (double a = 0; a < 360; a += 7.5) {
            for (double b = 0; b < 360; b += 7.5) {
                double d = AngleMath.signedDelta(a, b);
                assertTrue(d >= -180.0 && d < 180.0, "signedDelta(" + a + ", " + b + ") = " + d);
            }
        }
    }

    /**
     * Der Kern der ganzen Ereigniserkennung: Über den Nullpunkt hinweg darf die Differenz nicht
     * springen, sonst findet die Bisektion Nullstellen, wo keine sind.
     */
    @Test
    void signedDelta_doesNotJumpAcrossZeroDegrees() {
        double justBefore = AngleMath.signedDelta(359.9, 0.0);
        double justAfter = AngleMath.signedDelta(0.1, 0.0);

        assertTrue(justBefore < 0, "kurz vor 0° muss die Differenz negativ sein");
        assertTrue(justAfter > 0, "kurz nach 0° muss die Differenz positiv sein");
        assertEquals(0.2, justAfter - justBefore, 1e-9, "der Sprung über 0° darf nur 0,2° groß sein");
    }

    @Test
    void signedDelta_switchesSignAtTheTargetAngle() {
        // Ein Quadrat bei 90°: davor negativ, danach positiv
        assertTrue(AngleMath.signedDelta(89.5, 90.0) < 0);
        assertTrue(AngleMath.signedDelta(90.5, 90.0) > 0);
    }

    @Test
    void separation_isNeverMoreThanHalfCircle() {
        assertEquals(0.0, AngleMath.separation(10.0, 10.0), EPS);
        assertEquals(180.0, AngleMath.separation(0.0, 180.0), EPS);
        assertEquals(20.0, AngleMath.separation(350.0, 10.0), EPS);
        assertEquals(90.0, AngleMath.separation(300.0, 30.0), EPS);
    }

    @Test
    void signHelpers_splitTheCircleIntoTwelve() {
        assertEquals(0, AngleMath.signIndex(0.0));
        assertEquals(0, AngleMath.signIndex(29.99));
        assertEquals(1, AngleMath.signIndex(30.0));
        assertEquals(11, AngleMath.signIndex(359.99));

        assertEquals(0.0, AngleMath.signStart(15.0), EPS);
        assertEquals(120.0, AngleMath.signStart(130.0), EPS);
        assertEquals(10.0, AngleMath.degreeInSign(130.0), EPS);
    }

    @Test
    void norm360_wrapsNegativeAngles() {
        assertEquals(350.0, AngleMath.norm360(-10.0), EPS);
        assertEquals(10.0, AngleMath.norm360(370.0), EPS);
        assertEquals(0.0, AngleMath.norm360(720.0), EPS);
    }
}
