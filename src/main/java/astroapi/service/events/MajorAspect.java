package astroapi.service.events;

/**
 * Die fünf Hauptaspekte, auf die die Event-Erkennung sich beschränkt.
 *
 * <p>Die vorhandene {@code AspectType}-Aufzählung kennt zusätzlich Quincunx, Halbsextil, Quintil und
 * andere. Für einen Ereignis-Feed sind die zu fein: sie treten häufig auf, tragen aber selten eine
 * Aussage, die eine eigene Karte rechtfertigt. Die kleinen Aspekte bleiben deshalb dem
 * Tageshoroskop vorbehalten, wo sie als Kontext erscheinen und nicht als Ereignis.
 */
public enum MajorAspect {

    CONJUNCTION("Conjunction", 0.0),
    SEXTILE("Sextile", 60.0),
    SQUARE("Square", 90.0),
    TRINE("Trine", 120.0),
    OPPOSITION("Opposition", 180.0);

    private final String displayName;
    private final double angle;

    MajorAspect(String displayName, double angle) {
        this.displayName = displayName;
        this.angle = angle;
    }

    public String getDisplayName() { return displayName; }
    public double getAngle() { return angle; }

    /**
     * Die Zielwinkel als vorzeichenbehaftete Differenzen.
     *
     * <p>Ein Quadrat kann als +90° oder −90° stehen — das sind zwei getrennte Zeitpunkte. Konjunktion
     * und Opposition dagegen fallen mit ihrem Spiegelbild zusammen und werden nur einmal gesucht.
     */
    public double[] signedTargets() {
        if (angle == 0.0 || angle == 180.0) return new double[]{angle};
        return new double[]{angle, -angle};
    }
}
