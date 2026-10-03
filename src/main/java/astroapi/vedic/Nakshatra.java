package astroapi.vedic;

/**
 * Die 27 Nakshatras mit allem, was Ashtakoota aus ihnen zieht.
 *
 * <p>Jedes Nakshatra umfasst 13°20′ ({@link VedicMath#NAKSHATRA_SPAN}), gezählt ab dem siderischen
 * Nullpunkt; Abhijit, das 28. Nakshatra mancher Schulen, kommt in diesem 27-teiligen Schema nicht
 * vor und ist deshalb auch nicht aufgeführt.
 *
 * <p>Die Spalten Yoni/Geschlecht, Gana und Nadi stammen aus der Saravali-Referenz
 * (CC BY-SA 4.0). Das Yoni-Geschlecht ist dort über zwei Nakshatra-Spalten je Tier abgebildet
 * (männlich/weiblich) und steht hier als {@link #isYoniMale()}. Der Nakshatra-Herrscher ist der
 * Vimshottari-Herrscher; er geht in Version 1 in keine Wertung ein und dient allein der Anzeige.
 */
public enum Nakshatra {

    ASHWINI(1, "ashwini", "Ashwini", Yoni.HORSE, true, Gana.DEVA, Nadi.ADI, VedicBody.KETU),
    BHARANI(2, "bharani", "Bharani", Yoni.ELEPHANT, true, Gana.MANUSHYA, Nadi.MADHYA, VedicBody.VENUS),
    KRITTIKA(3, "krittika", "Krittika", Yoni.SHEEP, false, Gana.RAKSHASA, Nadi.ANTYA, VedicBody.SUN),
    ROHINI(4, "rohini", "Rohini", Yoni.SERPENT, true, Gana.MANUSHYA, Nadi.ANTYA, VedicBody.MOON),
    MRIGASHIRA(5, "mrigashira", "Mrigashira", Yoni.SERPENT, false, Gana.DEVA, Nadi.MADHYA, VedicBody.MARS),
    ARDRA(6, "ardra", "Ardra", Yoni.DOG, false, Gana.MANUSHYA, Nadi.ADI, VedicBody.RAHU),
    PUNARVASU(7, "punarvasu", "Punarvasu", Yoni.CAT, false, Gana.DEVA, Nadi.ADI, VedicBody.JUPITER),
    PUSHYA(8, "pushya", "Pushya", Yoni.SHEEP, true, Gana.DEVA, Nadi.MADHYA, VedicBody.SATURN),
    ASHLESHA(9, "ashlesha", "Ashlesha", Yoni.CAT, true, Gana.RAKSHASA, Nadi.ANTYA, VedicBody.MERCURY),
    MAGHA(10, "magha", "Magha", Yoni.RAT, true, Gana.RAKSHASA, Nadi.ANTYA, VedicBody.KETU),
    PURVA_PHALGUNI(11, "purva_phalguni", "Purva Phalguni", Yoni.RAT, false, Gana.MANUSHYA, Nadi.MADHYA, VedicBody.VENUS),
    UTTARA_PHALGUNI(12, "uttara_phalguni", "Uttara Phalguni", Yoni.COW, true, Gana.MANUSHYA, Nadi.ADI, VedicBody.SUN),
    HASTA(13, "hasta", "Hasta", Yoni.BUFFALO, false, Gana.DEVA, Nadi.ADI, VedicBody.MOON),
    CHITRA(14, "chitra", "Chitra", Yoni.TIGER, false, Gana.RAKSHASA, Nadi.MADHYA, VedicBody.MARS),
    SWATI(15, "swati", "Swati", Yoni.BUFFALO, true, Gana.DEVA, Nadi.ANTYA, VedicBody.RAHU),
    VISHAKHA(16, "vishakha", "Vishakha", Yoni.TIGER, true, Gana.RAKSHASA, Nadi.ANTYA, VedicBody.JUPITER),
    ANURADHA(17, "anuradha", "Anuradha", Yoni.DEER, false, Gana.DEVA, Nadi.MADHYA, VedicBody.SATURN),
    JYESHTHA(18, "jyeshtha", "Jyeshtha", Yoni.DEER, true, Gana.RAKSHASA, Nadi.ADI, VedicBody.MERCURY),
    MULA(19, "mula", "Mula", Yoni.DOG, true, Gana.RAKSHASA, Nadi.ADI, VedicBody.KETU),
    PURVA_ASHADHA(20, "purva_ashadha", "Purva Ashadha", Yoni.MONKEY, true, Gana.MANUSHYA, Nadi.MADHYA, VedicBody.VENUS),
    UTTARA_ASHADHA(21, "uttara_ashadha", "Uttara Ashadha", Yoni.MONGOOSE, true, Gana.MANUSHYA, Nadi.ANTYA, VedicBody.SUN),
    SHRAVANA(22, "shravana", "Shravana", Yoni.MONKEY, false, Gana.DEVA, Nadi.ANTYA, VedicBody.MOON),
    DHANISHTA(23, "dhanishta", "Dhanishta", Yoni.LION, false, Gana.RAKSHASA, Nadi.MADHYA, VedicBody.MARS),
    SHATABHISHA(24, "shatabhisha", "Shatabhisha", Yoni.HORSE, false, Gana.RAKSHASA, Nadi.ADI, VedicBody.RAHU),
    PURVA_BHADRAPADA(25, "purva_bhadrapada", "Purva Bhadrapada", Yoni.LION, true, Gana.MANUSHYA, Nadi.ADI, VedicBody.JUPITER),
    UTTARA_BHADRAPADA(26, "uttara_bhadrapada", "Uttara Bhadrapada", Yoni.COW, false, Gana.MANUSHYA, Nadi.MADHYA, VedicBody.SATURN),
    REVATI(27, "revati", "Revati", Yoni.ELEPHANT, false, Gana.DEVA, Nadi.ANTYA, VedicBody.MERCURY);

    private final int index;
    private final String key;
    private final String display;
    private final Yoni yoni;
    private final boolean yoniMale;
    private final Gana gana;
    private final Nadi nadi;
    private final VedicBody lord;

    Nakshatra(int index, String key, String display, Yoni yoni, boolean yoniMale,
              Gana gana, Nadi nadi, VedicBody lord) {
        this.index = index;
        this.key = key;
        this.display = display;
        this.yoni = yoni;
        this.yoniMale = yoniMale;
        this.gana = gana;
        this.nadi = nadi;
        this.lord = lord;
    }

    /** 1…27. */
    public int getIndex() { return index; }

    /** Stabiler technischer Schlüssel — Basis der Übersetzungsschlüssel. */
    public String getKey() { return key; }

    /** Sanskrit-Name in lateinischer Umschrift; wird nicht übersetzt. */
    public String getDisplay() { return display; }

    public Yoni getYoni() { return yoni; }

    /** Männliche oder weibliche Erscheinung derselben Yoni — Teil der Tabelle, nicht des Menschen. */
    public boolean isYoniMale() { return yoniMale; }

    public Gana getGana() { return gana; }

    public Nadi getNadi() { return nadi; }

    /** Vimshottari-Herrscher. Nur Anzeige — in Version 1 geht keine Dasha in die Wertung ein. */
    public VedicBody getLord() { return lord; }

    public static Nakshatra byIndex(int index1Based) {
        if (index1Based < 1 || index1Based > 27) {
            throw new IllegalArgumentException("Nakshatra index out of range: " + index1Based);
        }
        return values()[index1Based - 1];
    }

    /**
     * Das Nakshatra einer siderischen Länge.
     *
     * <p>Abgeleitet aus dem 108er-Abschnitt ({@code / 4}) — dieselbe Zahl, aus der auch Pada,
     * Rashi und Navamsa kommen. Siehe {@link VedicMath#absoluteIndex(double, int)}.
     */
    public static Nakshatra ofLongitude(double siderealLongitude) {
        return values()[VedicMath.absolutePada(siderealLongitude) / 4];
    }
}
