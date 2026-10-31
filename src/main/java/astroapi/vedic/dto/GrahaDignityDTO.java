package astroapi.vedic.dto;

/**
 * Die Würde eines klassischen Grahas in D1 und D9.
 *
 * <p>Nur Schlüssel ({@code exalted}, {@code own_sign} …), keine Wertung in Worten — die Oberflächen
 * übersetzen, die Deutung erklärt.
 */
public class GrahaDignityDTO {

    private String body;
    private String rashi;
    /** Würde im Rashi-Chart, mit den Gradgrenzen aus BPHS. */
    private String dignity;
    private String navamsaRashi;
    /** Würde im Navamsa, nur nach Zeichen. */
    private String navamsaDignity;
    /** D1- und D9-Zeichen sind gleich — klassisch ein Zeichen von Beständigkeit. */
    private boolean vargottama;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getRashi() { return rashi; }
    public void setRashi(String rashi) { this.rashi = rashi; }
    public String getDignity() { return dignity; }
    public void setDignity(String dignity) { this.dignity = dignity; }
    public String getNavamsaRashi() { return navamsaRashi; }
    public void setNavamsaRashi(String navamsaRashi) { this.navamsaRashi = navamsaRashi; }
    public String getNavamsaDignity() { return navamsaDignity; }
    public void setNavamsaDignity(String navamsaDignity) { this.navamsaDignity = navamsaDignity; }
    public boolean isVargottama() { return vargottama; }
    public void setVargottama(boolean vargottama) { this.vargottama = vargottama; }
}
