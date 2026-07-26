package astroapi.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Data Transfer Object for synastry request, representing the astrological details of two partners.
 *
 * <p>This class holds the birth data of two individuals, typically used for compatibility (synastry) analysis.</p>
 */
public class SynastryRequest {

    @Valid
    @NotNull(message = "partnerOne must not be null")
    private AstroRequest partnerOne;

    @Valid
    @NotNull(message = "partnerTwo must not be null")
    private AstroRequest partnerTwo;

    /**
     * Retrieves the astrological details of the first partner.
     *
     * @return {@link AstroRequest} representing the first partner's birth data.
     */
    public AstroRequest getPartnerOne() {
        return partnerOne;
    }

    /**
     * Sets the astrological details of the first partner.
     *
     * @param partnerOne {@link AstroRequest} representing the first partner's birth data.
     */
    public void setPartnerOne(AstroRequest partnerOne) {
        this.partnerOne = partnerOne;
    }

    /**
     * Retrieves the astrological details of the second partner.
     *
     * @return {@link AstroRequest} representing the second partner's birth data.
     */
    public AstroRequest getPartnerTwo() {
        return partnerTwo;
    }

    /**
     * Sets the astrological details of the second partner.
     *
     * @param partnerTwo {@link AstroRequest} representing the second partner's birth data.
     */
    public void setPartnerTwo(AstroRequest partnerTwo) {
        this.partnerTwo = partnerTwo;
    }
}
