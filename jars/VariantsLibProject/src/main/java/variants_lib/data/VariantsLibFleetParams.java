package variants_lib.data;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;

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
    public PersonAPI commander;
    public boolean enableAutoFit = true;
    public long seed = System.currentTimeMillis();
}
