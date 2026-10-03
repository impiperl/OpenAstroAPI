package astroapi.vedic.dto;

import java.util.List;

/**
 * Das Ashtakoota für <b>eine</b> Richtung der traditionellen Rollen.
 *
 * <p>Vier der acht Kootas sind richtungsabhängig. Wird keine Rolle vorgegeben, enthält die
 * Antwort zwei solche Blöcke und {@code roleDependent} nennt, welche Kootas sich zwischen ihnen
 * unterscheiden — damit die Oberfläche den Unterschied erklären kann, statt zwei Zahlen
 * unkommentiert nebeneinanderzustellen.
 */
public class AshtakootaOrientationDTO {

    /** Label der Person in der Braut-Rolle der Tabellen. */
    private String brideLabel;
    private String groomLabel;
    /** {@code a_as_bride} oder {@code b_as_bride}. */
    private String role;
    private List<KootaScoreDTO> kootas;
    private double totalPoints;
    private double maxPoints;
    /** Schlüssel der Kootas, die in dieser Konstellation von der Reihenfolge abhängen. */
    private List<String> roleDependent;

    public String getBrideLabel() { return brideLabel; }
    public void setBrideLabel(String brideLabel) { this.brideLabel = brideLabel; }
    public String getGroomLabel() { return groomLabel; }
    public void setGroomLabel(String groomLabel) { this.groomLabel = groomLabel; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<KootaScoreDTO> getKootas() { return kootas; }
    public void setKootas(List<KootaScoreDTO> kootas) { this.kootas = kootas; }
    public double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    public double getMaxPoints() { return maxPoints; }
    public void setMaxPoints(double maxPoints) { this.maxPoints = maxPoints; }
    public List<String> getRoleDependent() { return roleDependent; }
    public void setRoleDependent(List<String> roleDependent) { this.roleDependent = roleDependent; }
}
