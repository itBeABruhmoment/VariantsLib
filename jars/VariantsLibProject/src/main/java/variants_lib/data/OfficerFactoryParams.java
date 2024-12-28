package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import variants_lib.scripts.UnofficeredPersonalitySetPlugin;

import java.util.ArrayList;
import java.util.Random;

public class OfficerFactoryParams {
    private static final Logger log = Global.getLogger(OfficerFactoryParams.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static final String[] AGGRESSION_TO_PERSONALITY = {Personalities.CAUTIOUS, Personalities.TIMID,
            Personalities.STEADY, Personalities.AGGRESSIVE, Personalities.RECKLESS};

    public String faction = Factions.INDEPENDENT;
    public String personality = Personalities.STEADY;
    public ArrayList<String> skillsToAdd = new ArrayList<>();
    public int level = 5;
    public float percentEliteSkills = 0.25f;
    public Random rand = new Random();

    public OfficerFactoryParams() {}

    /**
     * Create params to make a random officer
     * @param variantId For applying variant_tags.csv tags. Can be null
     * @param faction
     * @param rand
     * @param targetLevel
     * @param defaultPersonality Can be null
     */
    public OfficerFactoryParams(
            final String variantId,
            final String faction,
            final Random rand,
            final float targetLevel,
            final String defaultPersonality
    ) {
        this.level = Math.round(targetLevel + 2 * rand.nextFloat() - 1.0f);
        if (this.level < 1) {
            this.level = 1;
        }

        final VariantData.VariantDataMember variantData = VariantData.VARIANT_DATA.get(variantId);
        boolean variantHasPersonality = false;
        if(variantData != null) {
            this.skillsToAdd.addAll(variantData.skills);
            variantHasPersonality = !variantData.getPersonality().equals(VariantData.VariantDataMember.NO_PERSONALITY_SET);
        }

        if(defaultPersonality == null && !variantHasPersonality) {
            // give personality based on faction
            final int aggressionVal = Global.getSector().getFaction(faction).getDoctrine().getAggression();
            personality = AGGRESSION_TO_PERSONALITY[aggressionVal - 1];
        } else if(defaultPersonality != null && !variantHasPersonality){
            // give personality based on default passed in
            personality = defaultPersonality;
        } else {
            // variantData != null, so give personality based on variant tag
            this.personality = variantData.getPersonality();
        }
    }
}
