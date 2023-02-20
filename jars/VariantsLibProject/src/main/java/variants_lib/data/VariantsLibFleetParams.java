package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * params for building fleets with VariantsLibFleetFactory
 */
public class VariantsLibFleetParams {
    protected static final Logger log = Global.getLogger(VariantsLibFleetParams.class);
    static {
        log.setLevel(Level.ALL);
    }

    public String fleetName = "fleet";
    public String faction = "independent";
    public String fleetType = FleetTypes.PATROL_MEDIUM; // String to store under the memkey $fleetType
    public int fleetPoints = 100;
    public float quality = 1.0f; // 0.0f being max dmods, 1.0f being the least
    public float averageSMods = 0;
    public int numOfficers = 5;
    public float averageOfficerLevel = 5;
    public long seed = System.currentTimeMillis();

    public VariantsLibFleetParams() { }

    public VariantsLibFleetParams(CampaignFleetAPI fleet) {
        final MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();

        fleetName = fleet.getName();
        faction = fleet.getFaction().getId();
        if(fleetMemory.contains(MemFlags.MEMORY_KEY_FLEET_TYPE)) {
            fleetType = fleetMemory.getString(MemFlags.MEMORY_KEY_FLEET_TYPE);
        }
        if(fleetMemory.contains(MemFlags.SALVAGE_SEED)) {
            seed = fleetMemory.getLong(MemFlags.SALVAGE_SEED);
        }

        int numOfficers = 0;
        int sumOfficerLevels = 0;
        int totalDP = 0;
        for(FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            if(!member.isFighterWing()) {
                totalDP += member.getBaseDeploymentCostSupplies();
                // there isn't a hasOfficer method in the API. Let's get creative!
                final int level = member.getCaptain().getStats().getLevel();
                if(level <= 1) {
                    numOfficers++;
                    sumOfficerLevels += level;
                }
            }
        }
        fleetPoints = totalDP;
        this.numOfficers = numOfficers;
        averageOfficerLevel = ((float) sumOfficerLevels) / numOfficers;

        try {
            DefaultFleetInflaterParams inflaterParams = (DefaultFleetInflaterParams)fleet.getInflater().getParams();
            averageSMods = Math.round(inflaterParams.averageSMods);
        } catch(Exception e) {
            log.info("could not get average smods defaulting to none");
        }

        try {
            quality = fleet.getInflater().getQuality();
        } catch(Exception e) {
            log.info("could not get quality defaulting to max");
        }
    }

    @Override
    public String toString() {
        final JSONObject json = new JSONObject();
        try {
            json.put("fleetName", fleetName);
            json.put("faction", faction);
            json.put("fleetType", fleetType);
            json.put("fleetPoints", fleetPoints);
            json.put("quality", quality);
            json.put("averageSmods", averageSMods);
            json.put("numOfficers", numOfficers);
            json.put("averageOfficerLevel", averageOfficerLevel);
            json.put("seed", seed);
        } catch (Exception e) {

        }
        return json.toString();
    }
}
