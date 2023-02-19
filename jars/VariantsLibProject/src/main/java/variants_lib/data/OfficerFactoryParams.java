package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import variants_lib.scripts.UnofficeredPersonalitySetPlugin;

import java.util.ArrayList;
import java.util.Random;

public class OfficerFactoryParams {
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
     */
    public OfficerFactoryParams(
            final String variantId,
            final String faction,
            final Random rand,
            final float targetLevel
    ) {
        this.level = Math.round(targetLevel + 2 * rand.nextFloat() - 1.0f);
        if (this.level < 1) {
            this.level = 1;
        }

        final String factionDefaultPersonality = UnofficeredPersonalitySetPlugin.getDefaultPersonality(faction);
        if (factionDefaultPersonality != null) {
            this.personality = factionDefaultPersonality;
        }

        final VariantData.VariantDataMember variantData = VariantData.VARIANT_DATA.get(variantId);
        if (variantData != null) {
            for (final String tag : variantData.officerSpecifications) {
                final String skill = CommonStrings.SKILL_EDIT_TAGS.get(tag);
                final String personality = CommonStrings.PERSONALITY_EDIT_TAGS.get(tag);
                if (skill != null) {
                    this.skillsToAdd.add(skill);
                } else if (personality != null) {
                    this.personality = personality;
                }
            }
        }
    }
}
