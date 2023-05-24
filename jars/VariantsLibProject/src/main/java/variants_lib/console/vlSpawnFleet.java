package variants_lib.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;

import variants_lib.data.CommonStrings;
import variants_lib.data.FleetBuildData;
import variants_lib.data.VariantsLibFleetParams;

public class vlSpawnFleet implements BaseCommand{
    @Override
    public CommandResult runCommand(String args, BaseCommand.CommandContext context) {
        String[] splited = args.split("\\s+");

        if(context != CommandContext.CAMPAIGN_MAP) {
            return CommandResult.WRONG_CONTEXT;
        }
        if(splited.length != 3) {
            Console.showMessage("incorrect number or arguments");
            return CommandResult.BAD_SYNTAX;
        }
        if(Global.getSector().getFaction(splited[0]) == null) {
            Console.showMessage(splited[0] + " is not a recognised faction");
            return CommandResult.BAD_SYNTAX;
        }
        if(!FleetBuildData.FLEET_DATA.containsKey(splited[1])) {
            Console.showMessage(splited[1] + " is not a recognised fleet type");
            return CommandResult.BAD_SYNTAX;
        }

        int fp = 0;
        try {
            fp = Integer.parseInt(splited[2]);
        } catch(NumberFormatException e) {
            Console.showMessage(splited[2] + " is not a recognised integer");
            return CommandResult.BAD_SYNTAX;
        }

        if(fp < 1) {
            Console.showMessage("fp field is invalid number");
            return CommandResult.ERROR;
        }

        final VariantsLibFleetParams params = new VariantsLibFleetParams();
        params.fleetName = "fleet";
        params.faction = splited[0];
        params.fleetType = FleetTypes.PATROL_LARGE;
        params.fleetPoints = fp;
        params.quality = 1.0f;
        params.averageSMods = 0.0f;
        params.averageOfficerLevel = 5;
        params.numOfficers = 8;

        CampaignFleetAPI toSpawn = FleetBuildData.FLEET_DATA.get(splited[1]).createFleet(params);
        toSpawn.getMemoryWithoutUpdate().set(CommonStrings.FLEET_EDITED_MEMKEY, true);

        // Spawn fleet around player
        final Vector2f offset = new Vector2f(0, 0);
        Global.getSector().getCurrentLocation().spawnFleet(
                Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
        Global.getSector().addPing(toSpawn, "danger");

        return CommandResult.SUCCESS;
    }

}
