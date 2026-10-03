package astroapi.vedic;

/**
 * Die zwölf siderischen Zeichen (Rashis) mit allem, was die vedische Wertung aus ihnen zieht.
 *
 * <p><b>Nicht zu verwechseln</b> mit {@code astroapi.utils.ZodiacSign}: das ist das tropische
 * Zeichen der westlichen Berechnung. Gleiche Namen, anderer Nullpunkt — die beiden liegen
 * derzeit gut 24° auseinander. Sie bewusst zu trennen verhindert, dass eine vedische Regel
 * versehentlich auf eine tropische Länge angewandt wird.
 *
 * <p>Der Index 0…11 entspricht Widder…Fische, gezählt ab dem siderischen Nullpunkt.
 */
public enum Rashi {

    MESHA(0, "aries", "Mesha", VedicBody.MARS, VarnaClass.KSHATRIYA, Mobility.MOVABLE),
    VRISHABHA(1, "taurus", "Vrishabha", VedicBody.VENUS, VarnaClass.SHUDRA, Mobility.FIXED),
    MITHUNA(2, "gemini", "Mithuna", VedicBody.MERCURY, VarnaClass.VAISHYA, Mobility.DUAL),
    KARKA(3, "cancer", "Karka", VedicBody.MOON, VarnaClass.BRAHMIN, Mobility.MOVABLE),
    SIMHA(4, "leo", "Simha", VedicBody.SUN, VarnaClass.KSHATRIYA, Mobility.FIXED),
    KANYA(5, "virgo", "Kanya", VedicBody.MERCURY, VarnaClass.SHUDRA, Mobility.DUAL),
    TULA(6, "libra", "Tula", VedicBody.VENUS, VarnaClass.VAISHYA, Mobility.MOVABLE),
    VRISHCHIKA(7, "scorpio", "Vrishchika", VedicBody.MARS, VarnaClass.BRAHMIN, Mobility.FIXED),
    DHANU(8, "sagittarius", "Dhanu", VedicBody.JUPITER, VarnaClass.KSHATRIYA, Mobility.DUAL),
    MAKARA(9, "capricorn", "Makara", VedicBody.SATURN, VarnaClass.SHUDRA, Mobility.MOVABLE),
    KUMBHA(10, "aquarius", "Kumbha", VedicBody.SATURN, VarnaClass.VAISHYA, Mobility.FIXED),
    MEENA(11, "pisces", "Meena", VedicBody.JUPITER, VarnaClass.BRAHMIN, Mobility.DUAL);

    /** Bewegungsart — entscheidet über den Startpunkt der Navamsa-Reihe. */
    public enum Mobility { MOVABLE, FIXED, DUAL }

    private final int index;
    private final String key;
    private final String sanskrit;
    private final VedicBody lord;
    private final VarnaClass varna;
    private final Mobility mobility;

    Rashi(int index, String key, String sanskrit, VedicBody lord, VarnaClass varna, Mobility mobility) {
        this.index = index;
        this.key = key;
        this.sanskrit = sanskrit;
        this.lord = lord;
        this.varna = varna;
        this.mobility = mobility;
    }

    public int getIndex() { return index; }

    /** Stabiler technischer Schlüssel ({@code aries} …) — Basis der Übersetzungsschlüssel. */
    public String getKey() { return key; }

    public String getSanskrit() { return sanskrit; }

    /** Zeichenherrscher — Grundlage des Graha-Maitri-Koota. */
    public VedicBody getLord() { return lord; }

    public VarnaClass getVarna() { return varna; }

    public Mobility getMobility() { return mobility; }

    /**
     * Die Vashya-Klasse. Braucht den Grad <b>innerhalb</b> des Zeichens, weil Steinbock und
     * Schütze geteilt sind (siehe {@link VashyaClass}).
     *
     * @param degreeInSign 0 (einschließlich) bis 30 (ausschließlich)
     */
    public VashyaClass vashya(double degreeInSign) {
        boolean firstHalf = degreeInSign < 15.0;
        switch (this) {
            case MESHA:
            case VRISHABHA:
                return VashyaClass.QUADRUPED;
            case MITHUNA:
            case KANYA:
            case TULA:
            case KUMBHA:
                return VashyaClass.HUMAN;
            case KARKA:
            case MEENA:
                return VashyaClass.JALACHARA;
            case SIMHA:
                return VashyaClass.LEO;
            case VRISHCHIKA:
                return VashyaClass.SCORPIO;
            case MAKARA:
                return firstHalf ? VashyaClass.QUADRUPED : VashyaClass.JALACHARA;
            case DHANU:
                // Ergänzung gegenüber der Quelltabelle — siehe VashyaClass.
                return firstHalf ? VashyaClass.HUMAN : VashyaClass.QUADRUPED;
            default:
                throw new IllegalStateException("Unmapped rashi: " + this);
        }
    }

    public static Rashi byIndex(int index) {
        int i = ((index % 12) + 12) % 12;
        return values()[i];
    }

    /**
     * Das Rashi einer siderischen Länge in Grad.
     *
     * <p>Abgeleitet aus dem 108er-Abschnitt ({@code / 9}), nicht aus einer eigenen Division durch
     * 30 — so stammen Zeichen, Nakshatra, Pada und Navamsa aus derselben Zahl und können an einer
     * Grenze nicht gegeneinander kippen. Siehe {@link VedicMath#absoluteIndex(double, int)}.
     */
    public static Rashi ofLongitude(double siderealLongitude) {
        return byIndex(VedicMath.absolutePada(siderealLongitude) / 9);
    }

    /**
     * Abstand in Zeichen, 1-basiert und inklusiv — so, wie die vedische Literatur zählt:
     * „vom eigenen Zeichen aus gezählt" ergibt für das Zeichen selbst 1, nicht 0.
     *
     * @return 1…12
     */
    public static int countFrom(Rashi from, Rashi to) {
        return (((to.index - from.index) % 12) + 12) % 12 + 1;
    }
}
