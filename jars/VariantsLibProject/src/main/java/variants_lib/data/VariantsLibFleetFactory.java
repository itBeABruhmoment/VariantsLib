package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class VariantsLibFleetFactory  {
    protected static final Logger log = Global.getLogger(VariantsLibFleetFactory.class);
    static {
        log.setLevel(Level.ALL);
    }
    protected static final HashSet<String> VALID_PERSONALITIES = new HashSet<String>() {{
        add("timid"); add("cautious"); add("steady"); add("reckless"); add("aggressive");
    }};

    public ArrayList<AlwaysBuildMember> alwaysInclude = new ArrayList<>(5);
    public ArrayList<FleetPartition> partitions = new ArrayList<>(5);
    public ArrayList<String> commanderSkills = new ArrayList<>(5);
    public String id = "";
    public HashMap<String, Float> fleetTypeSpawnWeights = new HashMap<>();
    public int minDP = 0;
    public int maxDP = 100000;
    public String defaultFleetWidePersonality = "steady";
    public boolean spawnIfNoIndustry = true;
    public AutofitOption autofit = AutofitOption.NO_PREF;
    public int setDPToAtLeast = 0; // set to zero if not defined
    public float freighterDp = 0.0f;
    public float linerDp = 0.0f;
    public float tankerDp = 0.0f;
    public float personnelDp = 0.0f;

    /**
     * Creates a default fleet factory from json
     * @param fleetJson The fleet json
     */
    public VariantsLibFleetFactory(JSONObject fleetJson, JSONObject fleetJsonCsvRow) throws Exception {
        id = JsonUtils.getString(CommonStrings.FLEET_DATA_ID, "", fleetJson);
        if(id.equals("")) {
            throw new Exception(CommonStrings.FLEET_DATA_ID + "field could not be read");
        }
        if(FleetBuildData.FLEET_DATA.containsKey(id)) {
            throw new Exception("already a fleet json with the " + CommonStrings.FLEET_DATA_ID + " \""  + id + "\"");
        }

        minDP = JsonUtils.getInt(CommonStrings.MIN_DP, 0, fleetJson);
        if(minDP < 0) {
            throw new Exception(CommonStrings.MIN_DP + " is negative");
        }
        maxDP = JsonUtils.getInt(CommonStrings.MAX_DP, 10000, fleetJson);
        if(maxDP < 0) {
            throw new Exception(CommonStrings.MAX_DP + " is negative");
        }
        if(minDP > maxDP) {
            throw new Exception(CommonStrings.MIN_DP + "is greater than " + CommonStrings.MAX_DP);
        }

        defaultFleetWidePersonality = JsonUtils.getString(CommonStrings.DEFAULT_FLEET_WIDE_PERSONALITY, "steady", fleetJson);
        if(!VALID_PERSONALITIES.contains(defaultFleetWidePersonality)) {
            throw new Exception(CommonStrings.DEFAULT_FLEET_WIDE_PERSONALITY + " is not set to a valid personality");
        }

        spawnIfNoIndustry = JsonUtils.getBool(fleetJson, CommonStrings.SPAWN_IF_NO_INDUSTRY, true);

        // set autofit field
        try {
            final boolean autofitJsonOption = fleetJson.getBoolean(CommonStrings.AUTOFIT);
            if(autofitJsonOption) {
                autofit = AutofitOption.AUTOFIT;
            } else {
                autofit = AutofitOption.NO_AUTOFIT;
            }
        } catch (Exception e) {
            autofit = AutofitOption.NO_PREF;
        }

        setDPToAtLeast = JsonUtils.getInt(CommonStrings.SET_DP_TO_AT_LEAST, 0, fleetJson);

        freighterDp = JsonUtils.getFloat(fleetJson, CommonStrings.AUTO_LOGISTICS_FREIGHTER, 0.0f);
        if(freighterDp > 1.0f || freighterDp < 0.0f) {
            throw new Exception(CommonStrings.AUTO_LOGISTICS_FREIGHTER + " is set to an invalid value");
        }

        tankerDp = JsonUtils.getFloat(fleetJson, CommonStrings.AUTO_LOGISTICS_TANKER, 0.0f);
        if(tankerDp > 1.0f || tankerDp < 0.0f) {
            throw new Exception(CommonStrings.AUTO_LOGISTICS_TANKER + " is set to an invalid value");
        }

        personnelDp = JsonUtils.getFloat(fleetJson, CommonStrings.AUTO_LOGISTICS_PERSONNEL, 0.0f);
        if(personnelDp > 1.0f || personnelDp < 0.0f) {
            throw new Exception(CommonStrings.AUTO_LOGISTICS_PERSONNEL + " is set to an invalid value");
        }

        linerDp = JsonUtils.getFloat(fleetJson, CommonStrings.AUTO_LOGISTICS_LINER, 0.0f);
        if(linerDp > 1.0f || linerDp < 0.0f) {
            throw new Exception(CommonStrings.AUTO_LOGISTICS_LINER + " is set to an invalid value");
        }


    }

    public CampaignFleetAPI makeFleet(VariantsLibFleetParams params) {
        return null;
    }

    public void editFleet(CampaignFleetAPI original) {

    }

    public enum AutofitOption {
        AUTOFIT,
        NO_AUTOFIT,
        NO_PREF
    }


}
