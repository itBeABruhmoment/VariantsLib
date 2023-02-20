package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import variants_lib.data.FleetBuildData;
import variants_lib.data.VariantsLibFleetFactory;
import variants_lib.data.VariantsLibFleetParams;

import java.util.Random;

/**
 * Test command that you probably shouldn't touch
 */
public class vlTestCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String s, @NotNull CommandContext commandContext) {
        int cmdNumber;
        try {
            cmdNumber = Integer.parseInt(s);
        } catch (Exception e) {
            Console.showMessage(e.toString());
            return CommandResult.ERROR;
        }

        if(cmdNumber == 0) {
            testOfficerFactory();
        }
        if(cmdNumber == 1) {
            testSMods();
        }
        return CommandResult.SUCCESS;
    }

    public void testOfficerFactory() {
        /*
        final OfficerFactory fact = new OfficerFactory();
        fact.percentEliteSkills = 0.25f;
        fact.level = 5;
        fact.skillsToAdd.add(Skills.POLARIZED_ARMOR);
        fact.skillsToAdd.add(Skills.BEST_OF_THE_BEST);
        final PersonAPI person = fact.makeOfficer();
        for(MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
            Console.showMessage(skill.getSkill().getId());
        }
        Console.showMessage(person.getPersonalityAPI().getId());

         */
    }

    public void testSMods() {
        VariantsLibFleetFactory fact = FleetBuildData.FLEET_DATA.get("example_fleet");
        VariantsLibFleetParams params = new VariantsLibFleetParams();
        params.averageSMods = 3.0f;
        params.fleetPoints = 200;
        CampaignFleetAPI fleet = fact.createFleet(params);
        FleetDataAPI data = Global.getSector().getPlayerFleet().getFleetData();
        for(FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            if(!member.isFighterWing()) {
                FleetMemberAPI memberAPI = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant().clone());
                FleetEncounterContext.prepareShipForRecovery(memberAPI, true, true, true,1f, 1f, new Random());
                for(String smod : member.getVariant().getSMods()) {
                    memberAPI.getVariant().addPermaMod(smod, true);
                }
                memberAPI.updateStats();
                data.addFleetMember(memberAPI);
            }
        }
    }
}
