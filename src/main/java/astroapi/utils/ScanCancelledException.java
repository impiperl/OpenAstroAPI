package astroapi.utils;

/**
 * Signalisiert, dass ein laufender Scan abgebrochen wurde.
 *
 * <p>Hintergrund: {@code SwissEphemerisExecutor.runWithTimeout} ruft nach Ablauf des Zeitbudgets
 * {@code future.cancel(true)} auf. Das <b>unterbricht</b> den Worker-Thread nur — es beendet ihn
 * nicht. Eine Schleife, die den Interrupt nie prüft, rechnet danach im Hintergrund weiter und
 * belegt einen Pool-Thread, obwohl niemand mehr auf das Ergebnis wartet. Bei wiederholten Timeouts
 * wäre der ganze Ephemeriden-Pool nach kurzer Zeit blockiert.
 *
 * <p>Deshalb prüfen die langen Schleifen den Interrupt und werfen diese Ausnahme. Sie darf
 * <b>nicht</b> wie ein gewöhnlicher Detektorfehler behandelt und weggeloggt werden: ein Abbruch
 * betrifft den gesamten Scan, nicht einen einzelnen Ereignistyp.
 */
public class ScanCancelledException extends RuntimeException {

    public ScanCancelledException(String message) {
        super(message);
    }

    /**
     * Wirft, wenn der aktuelle Thread unterbrochen wurde.
     *
     * <p>Der Interrupt-Status bleibt dabei erhalten, damit der Pool-Thread ihn nicht verliert.
     */
    public static void throwIfInterrupted(String where) {
        if (Thread.currentThread().isInterrupted()) {
            throw new ScanCancelledException("Scan cancelled during " + where);
        }
    }
}
