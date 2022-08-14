package variants_lib.scripts;

import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;

import variants_lib.scripts.fleetedit.FleetBuilding;

// code for testing stuff that you shouldn't touch
public class TestCode {

    // runcode variants_lib.scripts.TestCode.spanFleetWithSmods();
    public static void spanFleetWithSmods()
    {
        // Create fleet
        final FactionAPI faction = Global.getSector().getFaction("hegemony");
        final FactionDoctrineAPI doctrine = faction.getDoctrine();
        final int totalFP = 100;
        final float freighterFP = totalFP * doctrine.getCombatFreighterProbability(); // TEMP
        final FleetParamsV3 params = new FleetParamsV3(
                null, // Hyperspace location
                faction.getId(), // Faction ID
                null, // Quality override (null disables)
                "breh", // Fleet type
                totalFP, // Combat FP
                freighterFP * .3f, // Freighter FP
                freighterFP * .3f, // Tanker FP
                freighterFP * .1f, // Transport FP
                freighterFP * .1f, // Liner FP
                freighterFP * .1f, // Utility FP
                0f); // Quality bonus
        final CampaignFleetAPI toSpawn = FleetFactoryV3.createFleet(params);
        FleetFactoryV3.addCommanderAndOfficers(toSpawn, params, new Random());
        toSpawn.setName("flyeet");

        // Spawn fleet around player
        final Vector2f offset = new Vector2f(0, 0);
        Global.getSector().getCurrentLocation().spawnFleet(
                Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
        Global.getSector().addPing(toSpawn, "danger");
        FleetBuilding.editFleet(toSpawn, "bv_smod_test", 2.0);
    }

    // runcode variants_lib.scripts.TestCode.spawnFleet();
    public static void spawnFleet()
    {
        FleetBuilding.VariantsLibFleetParams params = new FleetBuilding.VariantsLibFleetParams(
            "fleet",
            "hegemony",
            FleetTypes.PATROL_LARGE,
            "bv_hegemony_lowtech",
            200,
            1.0f,
            0.0f,
            8,
            5.0f,
            false,
            true,
            true
        );

        CampaignFleetAPI toSpawn = FleetBuilding.createFleet(params);

        // Spawn fleet around player
        final Vector2f offset = new Vector2f(0, 0);
        Global.getSector().getCurrentLocation().spawnFleet(
                Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
        Global.getSector().addPing(toSpawn, "danger");
    }
}
