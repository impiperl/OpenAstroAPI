package astroapi.vedic.panchanga;

import java.time.ZonedDateTime;

/**
 * Ein vedischer Tag: die fünf Glieder plus Sonnenaufgang und Wechselzeiten.
 *
 * <p>Die {@code ...End}-Felder dürfen {@code null} sein. Das ist kein Fehler, sondern eine
 * ehrliche Antwort: Findet die Nullstellensuche im Fenster keinen Übergang, wird keine Zeit
 * erfunden. Die Oberfläche zeigt dann den Abschnitt ohne Endzeit statt einer geratenen.
 *
 * @param sunrise      der Sonnenaufgang, der diesen vedischen Tag eröffnet
 * @param tithiEnd     wann die laufende Tithi endet
 * @param karanaEnd    wann das laufende Karana endet
 * @param yogaEnd      wann das laufende Yoga endet
 * @param nakshatraEnd wann der Mond das laufende Nakshatra verlässt
 */
public record PanchangaDay(
        Panchanga limbs,
        ZonedDateTime sunrise,
        ZonedDateTime tithiEnd,
        ZonedDateTime karanaEnd,
        ZonedDateTime yogaEnd,
        ZonedDateTime nakshatraEnd) {
}
