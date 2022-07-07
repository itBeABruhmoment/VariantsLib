package variants_lib.scripts.fleetedit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import variants_lib.data.CommonStrings;
import variants_lib.data.FleetBuildData;
import variants_lib.data.VariantData;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class OfficerEditing {

    private static final Logger log = Global.getLogger(variants_lib.scripts.fleetedit.OfficerEditing.class);
    static {
        log.setLevel(Level.ALL);
    }
    
    // used to store skills that can be changed
    private static final HashSet<String> MODIFYABLE_SKILLS = new HashSet<String>() {{
        add("helmsmanship");        add("combat_endurance");    add("impact_mitigation");   add("damage_control"); 
        add("field_modulation");    add("point_defense");       add("target_analysis");     add("ballistic_mastery"); 
        add("systems_expertise");   add("missile_specialization"); add("gunnery_implants"); add("energy_weapon_mastery"); 
        add("ordnance_expert");     add("polarized_armor");
    }};

    private static boolean hasOfficer(FleetMemberAPI fleetMember) 
    {
        // there isn't a hasOfficer method in the API. Let's get creative!
        if(fleetMember.getCaptain().getStats().getLevel() <= 1) {
            return false;
        }
        return true;
    }

    // creates a array with the elements {0, 1, 2, ... , n-1}
    private static int[] intArrayFromSize(int n)
    {
        int[] intArr = new int[n];
        for(int i = 0; i < n; i++) {
            intArr[i] = i;
        }
        return intArr;
    }

    // stack overflow ctrl-c ctrl-v
    private static void shuffleArray(int[] ar)
    {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    private static void editSkills(Vector<String> skillEditQueue, FleetMemberAPI fleetMember)
    {
        MutableCharacterStatsAPI officerStatsAPI = fleetMember.getCaptain().getStats();
        List<SkillLevelAPI> officerSkills = officerStatsAPI.getSkillsCopy();

        // random indices for choosing random skills to replace
        int[] randomizedIntSeries = intArrayFromSize(officerSkills.size()); 
        shuffleArray(randomizedIntSeries);

        // run until there are no more skills to give or no more that can be modified
        int frontOfQueue = 0;
        int intArrIndex = 0;
        while(frontOfQueue < skillEditQueue.size() && intArrIndex < randomizedIntSeries.length) {
            // if the skill in the queue is already known move on to applying the next
            if(officerStatsAPI.hasSkill(skillEditQueue.get(frontOfQueue))) {
                frontOfQueue++;
            } else {
                // iterates until a skill is replaced by the skill specified by skillEditQueue.get(frontOfQueue) or no skills
                // that are allowed to be replaced are left
                while(intArrIndex < randomizedIntSeries.length) {
                    // randomly select a skill from the list to replace
                    String skillIdToOverride = officerSkills.get(randomizedIntSeries[intArrIndex]).getSkill().getId();
                    if(MODIFYABLE_SKILLS.contains(skillIdToOverride) && !skillEditQueue.contains(skillIdToOverride)) {
                        Boolean skillIsElite = officerStatsAPI.getSkillLevel(skillIdToOverride) > 1.0f;
                        officerStatsAPI.setSkillLevel(skillIdToOverride, 0.0f);
                        if(skillIsElite) {
                            officerStatsAPI.setSkillLevel(skillEditQueue.get(frontOfQueue), 2.0f);
                        } else {
                            officerStatsAPI.setSkillLevel(skillEditQueue.get(frontOfQueue), 1.0f);
                        }

                        intArrIndex++;
                        frontOfQueue++;
                        break;
                    } else {
                        // if the skill should not be modified try to change another
                        intArrIndex++;
                    }
                }
            }
        }
    }

    public static void editAllOfficers(CampaignFleetAPI fleet, String fleetCompId)
    {
        for(FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            String variantId = VariantData.isRegisteredVariant(member);
            if(variantId != null) {
                OfficerEditing.editOfficer(member, variantId, fleetCompId);
            }
        }
    }
    
    public static void editOfficer(FleetMemberAPI fleetMember, String variantId, String fleetCompId)
    {
        if(!hasOfficer(fleetMember)) { // don't edit unofficered ships
            return;
        }
        
        try{
            Vector<String> officerSpec = null;
            if(VariantData.VARIANT_DATA.containsKey(variantId)) {
                officerSpec = VariantData.VARIANT_DATA.get(variantId).officerSpecifications;
            } else { // don't edit unregistered variants
                return;
            }

            if(fleetCompId != null) {
                fleetMember.getCaptain().setPersonality(FleetBuildData.FLEET_DATA.get(fleetCompId).defaultFleetWidePersonality);
            }

            Vector<String> skillEditQueue = new Vector<String>(10);
            for(String tag : officerSpec) {
                if(tag.equals(CommonStrings.DO_NOT_EDIT_OFFICER)) { // do not edit if this tag is present
                    return;
                } else if(CommonStrings.PERSONALITY_EDIT_TAGS.containsKey(tag)) { // edit personality if personality editing tag is identified
                    fleetMember.getCaptain().setPersonality(CommonStrings.PERSONALITY_EDIT_TAGS.get(tag));
                    // add flag to ensure it doesn't get overriden by an in battle feature
                    MemoryAPI memory = fleetMember.getCaptain().getMemoryWithoutUpdate();
                    if(memory != null && !memory.contains(CommonStrings.DO_NOT_CHANGE_PERSONALITY_KEY)) {
                        memory.set(CommonStrings.DO_NOT_CHANGE_PERSONALITY_KEY, true);
                    }
                } else if(CommonStrings.SKILL_EDIT_TAGS.containsKey(tag)) { // put tags that require skill editing in a queue
                    skillEditQueue.add(CommonStrings.SKILL_EDIT_TAGS.get(tag));
                }
            }
            editSkills(skillEditQueue, fleetMember);
        } catch(Exception e) {
            log.debug("failed to edit " + variantId + " !?!?!?!?!");
        }
    }

    private OfficerEditing() {} // do nothing
}


/*
testing commands 

runcode SectorEntityToken yourFleet = Global.getSector().getPlayerFleet();
        LocationAPI currentSystem = (LocationAPI)yourFleet.getContainingLocation();
        List<CampaignFleetAPI> fleets = currentSystem.getFleets();
        for(CampaignFleetAPI fleet : fleets) {
            List<FleetMemberAPI> members = fleet.getMembersWithFightersCopy();
            for(FleetMemberAPI member : members) {
                Console.showMessage(member.getVariant().getHullVariantId());
            }
        }

        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        FleetMemberAPI member = (FleetMemberAPI) members.get(2);
        member.getCaptain().setPersonality(Personalities.RECKLESS);

        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        Console.showMessage(members.get(2) == null);

        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        Console.showMessage(((FleetMemberAPI)members.get(2)).getCaptain().getNameString().length());

runcode SectorEntityToken yourFleet = Global.getSector().getPlayerFleet();
        LocationAPI currentSystem = (LocationAPI)yourFleet.getContainingLocation();
        List<CampaignFleetAPI> fleets = currentSystem.getFleets();
        for(CampaignFleetAPI fleet : fleets) {
            Console.showMessage("**********");
            List<FleetMemberAPI> members = fleet.getMembersWithFightersCopy();
            for(FleetMemberAPI member : members) {
                Console.showMessage(member.getCaptain().getNameString().length());
            }
        }


        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        FleetMemberAPI member = (FleetMemberAPI) members.get(14);
        Console.showMessage(member.getCaptain().getPersonalityAPI().getId());

        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        FleetMemberAPI member = (FleetMemberAPI) members.get(0);
        for(MutableCharacterStatsAPI.SkillLevelAPI skill : member.getCaptain().getStats().getSkillsCopy()) {
            Console.showMessage(skill.getSkill().getId());
        }

        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
        FleetMemberAPI member = (FleetMemberAPI) members.get(0);
        //Console.showMessage(member.getCaptain().getStats().getSkillLevel("systems_expertise"));
        //member.getCaptain().getStats().increaseSkill("systems_expertise");
        member.getCaptain().getStats().decreaseSkill("systems_expertise");
*/
