package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import variants_lib.data.CommonStrings;
import variants_lib.data.FleetBuildData;
import variants_lib.data.VariantsLibFleetFactory;
import variants_lib.data.VariantsLibFleetParams;

import java.util.ArrayList;
import java.util.Random;

public class vlTestBattle implements BaseCommand {
    // vltestbattle bv_hegemony_eliteso 200 bv_hegemony_eliteso 200
    @Override
    public CommandResult runCommand(@NotNull String s, @NotNull CommandContext commandContext) {
        final String[] split = s.split("\\s+");
        if(split.length != 4) {
            Console.showMessage("need 4 arguments");
            return CommandResult.BAD_SYNTAX;
        }

        int side1FP = 0;
        int side2FP = 0;
        try {
            side1FP = Integer.parseInt(split[1]);
            side2FP = Integer.parseInt(split[3]);
        } catch (Exception e) {
            Console.showMessage("bad int syntax");
            return CommandResult.BAD_SYNTAX;
        }

        if(side1FP <= 0 || side2FP <= 0) {
            Console.showMessage("fp values need to be greater than 0");
            return CommandResult.BAD_SYNTAX;
        }

        final VariantsLibFleetFactory side1Factory = FleetBuildData.FLEET_DATA.get(split[0]);
        if(side1Factory == null) {
            Console.showMessage(split[0] + " is not a recognized fleet type");
            return CommandResult.BAD_SYNTAX;
        }

        final VariantsLibFleetFactory side2Factory = FleetBuildData.FLEET_DATA.get(split[2]);
        if(side2Factory == null) {
            Console.showMessage(split[2] + " is not a recognized fleet type");
            return CommandResult.BAD_SYNTAX;
        }

        final VariantsLibFleetParams params = new VariantsLibFleetParams();
        params.numOfficers = 10;
        params.averageOfficerLevel = 5;

        params.fleetPoints = side1FP;
        params.faction = Factions.INDEPENDENT;
        final CampaignFleetAPI fleet1 = side1Factory.createFleet(params);

        params.fleetPoints = side2FP;
        params.faction = Factions.PIRATES;
        final CampaignFleetAPI fleet2 = side2Factory.createFleet(params);

        //fleet1.getMemoryWithoutUpdate().set(CommonStrings.FLEET_EDITED_MEMKEY, true);
        fleet2.getMemoryWithoutUpdate().set(CommonStrings.FLEET_EDITED_MEMKEY, true);

        //Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0, 0, fleet1);
        final CampaignFleetAPI player = Global.getSector().getPlayerFleet();

        final PersonAPI playerPerson = player.getActivePerson();

        final ArrayList<FleetMemberAPI> restore = new ArrayList<>(30);
        restore.addAll(player.getFleetData().getMembersListCopy());
        for(FleetMemberAPI memberAPI : player.getFleetData().getMembersListCopy()) {
            player.getFleetData().removeFleetMember(memberAPI);
        }

        final FleetMemberAPI flag = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "dram_Light");

        flag.setCaptain(playerPerson);
        fleet1.getFleetData().addFleetMember(flag);
        player.getFleetData().setFlagship(flag);

        for(FleetMemberAPI memberAPI : fleet1.getFleetData().getMembersListCopy()) {
            if(!memberAPI.isFighterWing()) {
                memberAPI.setFlagship(false);
                player.getFleetData().addFleetMember(memberAPI);
            }
        }

        Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0, 0, fleet2);

        Global.getSector().addTransientScript(new StartAfterConsoleCloseScript(new StartBattleScript(fleet1, fleet2, restore)));

        player.setAIMode(true);

        return CommandResult.SUCCESS;
    }

    public static class StartBattleScript implements Runnable {
        CampaignFleetAPI ally;
        CampaignFleetAPI enemy;
        ArrayList<FleetMemberAPI> restore;

        StartBattleScript(CampaignFleetAPI ally, CampaignFleetAPI enemy, ArrayList<FleetMemberAPI> restore) {
            this.ally = ally;
            this.enemy = enemy;
            this.restore = restore;
        }

        @Override
        public void run() {
            final CampaignUIAPI ui = Global.getSector().getCampaignUI();
            ui.showInteractionDialog(new TestFleetInteraction(ally, enemy, restore), enemy);
        }
    }
}
