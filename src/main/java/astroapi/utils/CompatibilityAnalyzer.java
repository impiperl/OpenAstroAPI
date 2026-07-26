package astroapi.utils;

import astroapi.dto.EphiDatenDTO;
import astroapi.dto.CompatibilityResult;
import org.springframework.stereotype.Component;

/**
 * The CompatibilityAnalyzer class analyzes the astrological compatibility between two individuals
 * based on their natal charts. It evaluates the aspects formed between corresponding planets
 * and determines the overall compatibility level.
 */
@Component
public class CompatibilityAnalyzer {

    // Orb values for various astrological aspects
    private static final double CONJUNCTION_ORB = 8.0;
    private static final double SEXTILE_ORB = 6.0;
    private static final double TRINE_ORB = 6.0;
    private static final double SQUARE_ORB = 8.0;
    private static final double OPPOSITION_ORB = 8.0;
    private static final double QUINCUNX_ORB = 2.0;
    private static final double SEMISEXTILE_ORB = 2.0;
    private static final double SESQUIQUADRATE_ORB = 4.0;
    private static final double QUINTILE_ORB = 4.0;
    private static final double BIQUINTILE_ORB = 4.0;
    private static final double NOVILE_ORB = 1.0;

    /**
     * Analyzes the astrological compatibility between two partners based on their natal data.
     *
     * @param partnerOne The first partner's natal data.
     * @param partnerTwo The second partner's natal data.
     * @return A CompatibilityResult object containing the analysis results.
     */
    public CompatibilityResult analyzeCompatibility(EphiDatenDTO partnerOne, EphiDatenDTO partnerTwo) {
        CompatibilityResult result = new CompatibilityResult();

        // Iterate through each planet in the first partner's chart
        for (Planet planetOne : Planet.values()) {
            Double degreeOne = partnerOne.getPlanets().get(planetOne.getName());
            if (degreeOne == null) continue; // Ensure the planet is present

            // Iterate through each planet in the second partner's chart
            for (Planet planetTwo : Planet.values()) {
                Double degreeTwo = partnerTwo.getPlanets().get(planetTwo.getName());
                if (degreeTwo == null) continue; // Ensure the planet is present

                // Calculate the minimal angle between the two planet positions
                double aspect = calculateAspect(degreeOne, degreeTwo);

                // Determine the type of aspect based on the calculated angle
                AspectType aspectType = determineAspectType(aspect);
                if (aspectType != null) {
                    // Categorize the aspect as harmonious or challenging
                    if (isHarmoniousAspect(aspectType)) {
                        result.incrementHarmoniousAspects();
                        result.addAspectSummary(String.format("%s aspect between %s and %s.", aspectType.getDescription(), planetOne.getName(), planetTwo.getName()));
                    } else if (isChallengingAspect(aspectType)) {
                        result.incrementChallengingAspects();
                        result.addAspectSummary(String.format("%s aspect between %s and %s.", aspectType.getDescription(), planetOne.getName(), planetTwo.getName()));
                    }
                }
            }
        }

        // Evaluate compatibility level based on the number of harmonious vs challenging aspects
        if (result.getHarmoniousAspects() > result.getChallengingAspects()) {
            result.setCompatibilityLevel("High");
        } else if (result.getHarmoniousAspects() < result.getChallengingAspects()) {
            result.setCompatibilityLevel("Low");
        } else {
            result.setCompatibilityLevel("Moderate");
        }

        return result;
    }

    /**
     * Calculates the minimal angle between two degree values on the ecliptic.
     *
     * @param degreeOne The first degree value.
     * @param degreeTwo The second degree value.
     * @return The minimal angle between the two degrees.
     */
    private double calculateAspect(double degreeOne, double degreeTwo) {
        double diff = Math.abs(degreeOne - degreeTwo) % 360;
        return diff > 180 ? 360 - diff : diff;
    }

    /**
     * Determines the type of aspect based on the given angle.
     *
     * @param aspect The angle between two planets.
     * @return The corresponding AspectType enum or null if no significant aspect is found.
     */
    private AspectType determineAspectType(double aspect) {
        for (AspectType aspectEnum : AspectType.values()) {
            if (Math.abs(aspect - aspectEnum.getExactDegree()) <= aspectEnum.getOrb()) {
                return aspectEnum;
            }
        }
        return null;
    }

    /**
     * Determines if the given aspect type is considered harmonious.
     *
     * @param aspectType The type of aspect to evaluate.
     * @return True if the aspect is harmonious; otherwise, false.
     */
    private boolean isHarmoniousAspect(AspectType aspectType) {
        return aspectType == AspectType.SEXTILE || aspectType == AspectType.TRINE ||
                aspectType == AspectType.CONJUNCTION || aspectType == AspectType.QUINTILE ||
                aspectType == AspectType.BIQUINTILE || aspectType == AspectType.NOVILE;
    }

    /**
     * Determines if the given aspect type is considered challenging.
     *
     * @param aspectType The type of aspect to evaluate.
     * @return True if the aspect is challenging; otherwise, false.
     */
    private boolean isChallengingAspect(AspectType aspectType) {
        return aspectType == AspectType.SQUARE || aspectType == AspectType.OPPOSITION ||
                aspectType == AspectType.QUINCUNX || aspectType == AspectType.SEMISEXTILE ||
                aspectType == AspectType.SESQUIQUADRATE;
    }
}
