package astroapi.vedic.dto;

import java.util.Map;

/**
 * Ein Vorbehalt zum Ergebnis — als Schlüssel, nicht als Satz.
 *
 * <p>Warnungen sind Teil des Ergebnisses, nicht ein Nebenkanal: eine fehlende Geburtszeit ändert,
 * wie belastbar Aszendent, Häuser und Manglik sind, und ein grenznaher Mond kann vier der acht
 * Kootas kippen. Beides muss in jeder Oberfläche und in jedem gespeicherten Ergebnis sichtbar
 * bleiben.
 */
public class VedicWarningDTO {

    /** {@code birth_time_unknown}, {@code moon_near_nakshatra_boundary}, … */
    private String code;
    /** {@code info}, {@code caution}. */
    private String level;
    /** Person, auf die sich der Hinweis bezieht; {@code null} für paarbezogene Hinweise. */
    private String label;
    private Map<String, Object> args;

    public VedicWarningDTO() {}

    public VedicWarningDTO(String code, String level, String label, Map<String, Object> args) {
        this.code = code;
        this.level = level;
        this.label = label;
        this.args = args;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Map<String, Object> getArgs() { return args; }
    public void setArgs(Map<String, Object> args) { this.args = args; }
}
