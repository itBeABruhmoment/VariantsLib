package variants_lib.data;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import org.json.JSONObject;

import java.util.Random;
public class VariantsLibFleetParams {
    public String fleetName = "fleet";
    public String faction = "independent";
    public String fleetType = FleetTypes.PATROL_MEDIUM; // String to store under the memkey $fleetType
    public int fleetPoints = 100;
    public float quality = 1.0f; // 0.0f being max dmods, 1.0f being the least
    public float averageSMods = 0;
    public int numOfficers = 5;
    public float averageOfficerLevel = 5;
    public long seed = System.currentTimeMillis();

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
