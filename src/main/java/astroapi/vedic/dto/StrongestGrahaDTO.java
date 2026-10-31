package astroapi.vedic.dto;

import java.util.List;

/**
 * Der würdigste der sieben klassischen Grahas — und warum gerade er.
 *
 * <p>{@code decidedBy} sagt, welche Stufe der Rangfolge den Ausschlag gab: {@code d1_dignity}
 * (eindeutig nach der Würde im Rashi-Chart), {@code d9_dignity} (gleiche D1-Würde, das Navamsa
 * entschied) oder {@code natural_strength} (auch D9 gleich; entschieden hat die natürliche Stärke
 * nach BPHS). Steht hier {@code natural_strength}, ist der Vorsprung klein, und
 * {@code tiedOnDignity} nennt die Grahas, die nach der Würde gleichauf lagen. Eine Oberfläche, die
 * das verschweigt, behauptet mehr, als gerechnet wurde.
 */
public class StrongestGrahaDTO {

    private String body;
    private String dignity;
    private String navamsaDignity;
    private String decidedBy;
    private List<String> tiedOnDignity;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getDignity() { return dignity; }
    public void setDignity(String dignity) { this.dignity = dignity; }
    public String getNavamsaDignity() { return navamsaDignity; }
    public void setNavamsaDignity(String navamsaDignity) { this.navamsaDignity = navamsaDignity; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public List<String> getTiedOnDignity() { return tiedOnDignity; }
    public void setTiedOnDignity(List<String> tiedOnDignity) { this.tiedOnDignity = tiedOnDignity; }
}
