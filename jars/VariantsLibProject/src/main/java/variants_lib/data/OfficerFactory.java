package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.HashSet;

/**
 * For making officers
 */
public class OfficerFactory {
    private static final HashSet<String> VALID_ELITE_SKILLS = new HashSet<String>() {{
        add(Skills.HELMSMANSHIP); add(Skills.COMBAT_ENDURANCE); add(Skills.IMPACT_MITIGATION); add(Skills.DAMAGE_CONTROL);
        add(Skills.FIELD_MODULATION); add(Skills.POINT_DEFENSE); add(Skills.TARGET_ANALYSIS); add(Skills.BALLISTIC_MASTERY);
        add(Skills.SYSTEMS_EXPERTISE); add(Skills.MISSILE_SPECIALIZATION); add(Skills.GUNNERY_IMPLANTS);
        add(Skills.ENERGY_WEAPON_MASTERY); add(Skills.ORDNANCE_EXPERTISE); add(Skills.POLARIZED_ARMOR);
    }};

    private static final String[] FILLER_SKILLS = {Skills.HELMSMANSHIP, Skills.COMBAT_ENDURANCE,
            Skills.IMPACT_MITIGATION, Skills.DAMAGE_CONTROL, Skills.FIELD_MODULATION, Skills.POINT_DEFENSE,
            Skills.TARGET_ANALYSIS, Skills.BALLISTIC_MASTERY, Skills.SYSTEMS_EXPERTISE, Skills.MISSILE_SPECIALIZATION,
            Skills.GUNNERY_IMPLANTS, Skills.ENERGY_WEAPON_MASTERY, Skills.ORDNANCE_EXPERTISE, Skills.POLARIZED_ARMOR};


    public OfficerFactory() {}

    /**
     * Make officer using params. params.rand used for random number generation
     * @return An officer
     */
    public PersonAPI createOfficer(final OfficerFactoryParams params) {
        final FactionAPI faction = Global.getSector().getFaction(params.faction);
        final PersonAPI officer = faction.createRandomPerson(params.rand);
        addSkills(officer, params);
        return officer;
    }

    /**
     * Does the same thing as createOfficer but allows the user to create the officer
     * @param officer
     * @param params
     */
    public void editOfficer(final PersonAPI officer, final OfficerFactoryParams params) {
        final MutableCharacterStatsAPI stats = officer.getStats();
        for(MutableCharacterStatsAPI.SkillLevelAPI skill : stats.getSkillsCopy()) {
            skill.setLevel(0.0f);
        }
        addSkills(officer, params);
    }

    protected void addSkills(final PersonAPI officer, final OfficerFactoryParams params) {
        final MutableCharacterStatsAPI stats = officer.getStats();
        int skillsAdded = 0;

        stats.setLevel(params.level);
        officer.setPersonality(params.personality);

        // add skills in skillsToAdd until done or level does not allow it
        while (skillsAdded < params.level && skillsAdded < params.skillsToAdd.size()) {
            final String skill = params.skillsToAdd.get(skillsAdded);
            if (VALID_ELITE_SKILLS.contains(skill) && params.rand.nextFloat() < params.percentEliteSkills) {
                stats.setSkillLevel(skill, 2.0f);
            } else {
                stats.setSkillLevel(skill, 1.0f);
            }
            skillsAdded++;
        }

        // fill remaining levels with empty skills
        if (skillsAdded < params.level) {
            final int[] randomIndices = Util.createRandomNumberSequence(FILLER_SKILLS.length, params.rand);
            int i = 0;
            while (i < randomIndices.length && skillsAdded < params.level) {
                final String skill = FILLER_SKILLS[randomIndices[i]];
                if (!stats.hasSkill(skill)) {
                    float skillLevel;
                    if (params.rand.nextFloat() < params.percentEliteSkills) {
                        skillLevel = 2.0f;
                    } else {
                        skillLevel = 1.0f;
                    }
                    stats.setSkillLevel(skill, skillLevel);
                    skillsAdded++;
                }
                i++;
            }
        }
    }
}
