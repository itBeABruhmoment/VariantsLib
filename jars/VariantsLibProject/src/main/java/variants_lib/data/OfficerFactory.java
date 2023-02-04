package variants_lib.data;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

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

    @NotNull
    public FactionAPI faction;
    @NotNull
    public String personality = "steady";
    @NotNull
    public ArrayList<String> skillsToAdd = new ArrayList<>();
    public int level = 5;
    public float percentEliteSkills = 0.25f;
    @NotNull
    public Random rand = new Random();

    public OfficerFactory(@NotNull FactionAPI faction) {
        this.faction = faction;
    }

    /**
     * Make officer using variables in fields
     * @return An officer
     */

    @NotNull
    public PersonAPI makeOfficer() {
        final PersonAPI officer = faction.createRandomPerson(rand);
        final MutableCharacterStatsAPI stats = officer.getStats();
        int skillsAdded = 0;

        stats.setLevel(level);
        officer.setPersonality(personality);

        // add skills in skillsToAdd until done or level does not allow it
        while (skillsAdded < level && skillsAdded < skillsToAdd.size()) {
            final String skill = skillsToAdd.get(skillsAdded);
            if (VALID_ELITE_SKILLS.contains(skill) && rand.nextFloat() < percentEliteSkills) {
                stats.setSkillLevel(skill, 2.0f);
            } else {
                stats.setSkillLevel(skill, 1.0f);
            }
            skillsAdded++;
        }

        // fill remaining levels with empty skills
        if (skillsAdded < level) {
            final int[] randomIndices = Util.createRandomNumberSequence(FILLER_SKILLS.length, rand);
            int i = 0;
            while (i < randomIndices.length && skillsAdded < level) {
                final String skill = FILLER_SKILLS[randomIndices[i]];
                if (!stats.hasSkill(skill)) {
                    float skillLevel;
                    if (rand.nextFloat() < percentEliteSkills) {
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

        return officer;
    }
}
