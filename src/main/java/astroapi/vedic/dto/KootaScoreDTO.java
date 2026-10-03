package astroapi.vedic.dto;

import java.util.List;
import java.util.Map;

/**
 * Ein einzelnes Koota — Punkte plus der vollständige Rechenweg dorthin.
 *
 * <p>Nicht nur die Zahl: {@code inputs} nennt die verwendeten Eingangsdaten (etwa die beiden
 * Yoni-Tiere), {@code decision} die maschinenlesbare Regelentscheidung (etwa
 * {@code yoni.inimical_pair}), {@code explanationKey} den Übersetzungsschlüssel der sachlichen
 * Erklärung und {@code explanationArgs} deren Platzhalter. So kann jede Oberfläche in jeder
 * Sprache begründen, warum die Punktzahl so ausfällt, ohne dass hier Text in vier Sprachen
 * mitgeschleppt wird — und ohne dass eine KI den Grund erraten muss.
 */
public class KootaScoreDTO {

    /** Stabiler technischer Schlüssel: {@code varna}, {@code vashya}, {@code dina}, … */
    private String key;
    /** Übersetzungsschlüssel des Anzeigenamens, z. B. {@code vedic.koota.varna}. */
    private String nameKey;
    private double points;
    private double maxPoints;
    /** Die verwendeten Eingangsdaten, als technische Schlüssel. */
    private Map<String, Object> inputs;
    /** Maschinenlesbare Regelentscheidung. Nie ein Satz, immer eine Kennung. */
    private String decision;
    private String explanationKey;
    private Map<String, Object> explanationArgs;
    /**
     * Hinweise auf Sonder- oder Aufhebungsregeln, die zu diesem Regelwerk gehören — als
     * Übersetzungsschlüssel. Leer, wenn keine greift.
     */
    private List<String> noteKeys;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getNameKey() { return nameKey; }
    public void setNameKey(String nameKey) { this.nameKey = nameKey; }
    public double getPoints() { return points; }
    public void setPoints(double points) { this.points = points; }
    public double getMaxPoints() { return maxPoints; }
    public void setMaxPoints(double maxPoints) { this.maxPoints = maxPoints; }
    public Map<String, Object> getInputs() { return inputs; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getExplanationKey() { return explanationKey; }
    public void setExplanationKey(String explanationKey) { this.explanationKey = explanationKey; }
    public Map<String, Object> getExplanationArgs() { return explanationArgs; }
    public void setExplanationArgs(Map<String, Object> v) { this.explanationArgs = v; }
    public List<String> getNoteKeys() { return noteKeys; }
    public void setNoteKeys(List<String> noteKeys) { this.noteKeys = noteKeys; }
}
