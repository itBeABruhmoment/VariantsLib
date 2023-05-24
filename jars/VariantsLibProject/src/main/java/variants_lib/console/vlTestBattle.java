package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import variants_lib.data.FleetBuildData;
import variants_lib.data.VariantsLibFleetFactory;
import variants_lib.data.VariantsLibFleetParams;

public class vlTestBattle implements BaseCommand {
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

        final BattleAPI battleAPI = Global.getFactory().createBattle(Global.getSector().getPlayerFleet(), fleet2);
        battleAPI.getSideOne().add(fleet1);
        battleAPI.genCombined();

        final BattleCreationContext battleCreationContext = new BattleCreationContext(
                battleAPI.getPlayerCombined(),
                FleetGoal.ATTACK,
                battleAPI.getNonPlayerCombined(),
                FleetGoal.ATTACK
        );
        battleCreationContext.objectivesAllowed = true;

        Global.getSector().addTransientScript(new StartAfterConsoleCloseScript(new StartBattleScript(battleCreationContext)));
        return CommandResult.SUCCESS;
    }

    public static class StartBattleScript implements Runnable {
        BattleCreationContext context = null;
        StartBattleScript(final BattleCreationContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            Global.getSector().getCampaignUI().startBattle(context);
        }
    }
}
