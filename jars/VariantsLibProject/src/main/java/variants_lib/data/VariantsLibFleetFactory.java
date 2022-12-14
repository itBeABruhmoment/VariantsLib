package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class VariantsLibFleetFactory  {
    protected static final Logger log = Global.getLogger(VariantsLibFleetFactory.class);
    static {
        log.setLevel(Level.ALL);
    }
    protected static final HashSet<String> VALID_PERSONALITIES = new HashSet<String>() {{
        add("timid"); add("cautious"); add("steady"); add("reckless"); add("aggressive");
    }};

    @NotNull public ArrayList<AlwaysBuildMember> alwaysInclude = new ArrayList<>(5);
    @NotNull public ArrayList<FleetPartition> partitions = new ArrayList<>(5);
    @NotNull public ArrayList<String> commanderSkills = new ArrayList<>(5);
    @NotNull public String id = "";
    @NotNull public HashMap<String, Float> fleetTypeSpawnWeights = new HashMap<>();
    public int minDP = 0;
    public int maxDP = Integer.MAX_VALUE;
    public String defaultFleetWidePersonality = "steady";
    public boolean spawnIfNoIndustry = true;
    public boolean autofit = true;
    public int setDPToAtLeast = 0; // set to zero if not defined
    public float freighterDp = 0.0f;
    public float linerDp = 0.0f;
    public float tankerDp = 0.0f;
    public float personnelDp = 0.0f;

    /**
     * Create a VariantsLibFleetFactory
     * @param fleetJson The fleet json
     * @param fleetJsonCsvRow The fleet json's row in fleets.csv
     * @param modOfOrigin The mod the fleet json is from
     * @throws Exception An exception containing some message on fields that are set to invalid values or failed to load
     */
    public VariantsLibFleetFactory(
            @NotNull final JSONObject fleetJson,
            @NotNull final JSONObject fleetJsonCsvRow,
            @NotNull String modOfOrigin
    ) throws Exception {
        id = JsonUtils.getString(CommonStrings.FLEET_DATA_ID, "", fleetJson);
        if(id.equals("")) {
            throw new Exception(CommonStrings.FLEET_DATA_ID + "field could not be read");
        }
        /*
        if(FleetBuildData.FLEET_DATA.containsKey(id)) {
            throw new Exception("already a fleet json with the " + CommonStrings.FLEET_DATA_ID + " \""  + id + "\"");
        }

         */

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

        autofit = JsonUtils.getBool(fleetJson, CommonStrings.AUTOFIT, true);

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

        // load alwaysInclude
        JSONObject alwaysIncludeJson = null;
        try {
            alwaysIncludeJson = fleetJson.getJSONObject(CommonStrings.ALWAYS_INCLUDE);
        } catch (Exception e) {
            log.info(CommonStrings.MOD_ID + ": " + CommonStrings.ALWAYS_INCLUDE + "field could not be read, set to nothing");
        }
        if(alwaysIncludeJson != null) {
            final JSONObject alwaysIncludeJsonTemp = alwaysIncludeJson;
            JsonUtils.forEachKey(alwaysIncludeJson, new JsonUtils.ForEachKey() {
                @Override
                public void runOnEach(String key) throws Exception{
                    try {
                        final int amount = alwaysIncludeJsonTemp.getInt(key);
                        alwaysInclude.add(new AlwaysBuildMember(key, amount));
                    } catch (Exception e) {
                        throw new Exception("could not read int in " + CommonStrings.ALWAYS_INCLUDE);
                    }
                }
            });
        }
        for(AlwaysBuildMember member : alwaysInclude) {
            if(member.amount < 0) {
                throw new Exception("the " + member.id + " field of " + CommonStrings.ALWAYS_INCLUDE + " has a negative amount");
            }
            if(Global.getSettings().getVariant(member.id) == null && ModdedVariantsData.addVariantToStore(member.id, modOfOrigin)) {
                throw new Exception("the " + member.id + " field of " + CommonStrings.ALWAYS_INCLUDE + " is not a recognized variant");
            }
        }

        commanderSkills = JsonUtils.getStringArrayList(CommonStrings.ADDITIONAL_COMMANDER_SKILLS, fleetJson);

        // load spawn weights
        JsonUtils.forEachKey(fleetJsonCsvRow, new JsonUtils.ForEachKey() {
            @Override
            public void runOnEach(String key) throws Exception {
                if(!key.equals(CommonStrings.FLEETS_CSV_FIRST_COLUMN_NAME)) {
                    float weight = 0.0f;
                    try {
                        weight = (float) fleetJsonCsvRow.getDouble(key);
                    } catch(Exception e) {
                        log.info("invalid number in csv row, setting weight to zero");
                        weight = 0.0f;
                    }
                    if(weight > 0.0f) {
                        fleetTypeSpawnWeights.put(key, weight);
                    }
                }
            }
        });

        JSONArray partitionsJSONArray = null;
        try {
            partitionsJSONArray = fleetJson.getJSONArray(CommonStrings.FLEET_PARTITIONS);
        } catch (Exception e) {
            log.info(CommonStrings.FLEET_PARTITIONS + " field could not be read, setting to nothing");
        }

        if(partitionsJSONArray != null) {
            for(int i = 0; i < partitionsJSONArray.length(); i++) {
                partitions.add(new FleetPartition(partitionsJSONArray.getJSONObject(i), i, modOfOrigin));
            }
        }
    }

    @Override
    public String toString() {
        final JSONObject json = new JSONObject();
        try {
            json.put(CommonStrings.FLEET_DATA_ID, id);
            json.put(CommonStrings.MIN_DP, minDP);
            json.put(CommonStrings.MAX_DP, maxDP);
            json.put(CommonStrings.DEFAULT_FLEET_WIDE_PERSONALITY, defaultFleetWidePersonality);
            json.put(CommonStrings.SPAWN_IF_NO_INDUSTRY, spawnIfNoIndustry);
            json.put(CommonStrings.AUTOFIT, autofit);
            json.put(CommonStrings.SET_DP_TO_AT_LEAST, setDPToAtLeast);
            json.put(CommonStrings.AUTO_LOGISTICS_FREIGHTER, freighterDp);
            json.put(CommonStrings.AUTO_LOGISTICS_TANKER, tankerDp);
            json.put(CommonStrings.AUTO_LOGISTICS_LINER, linerDp);
            json.put(CommonStrings.AUTO_LOGISTICS_PERSONNEL, personnelDp);
            for(AlwaysBuildMember member : alwaysInclude) {
                json.append(CommonStrings.ALWAYS_INCLUDE, new JSONObject().put(member.id, member.amount));
            }
            json.put(CommonStrings.ADDITIONAL_COMMANDER_SKILLS, commanderSkills);
            for (FleetPartition part : partitions) {
                json.append(CommonStrings.FLEET_PARTITIONS, part.toJson());
            }
            for(String fleetType: fleetTypeSpawnWeights.keySet()) {
                final JSONObject weightAndFleetType = new JSONObject();
                weightAndFleetType.put("fleetType", fleetType);
                weightAndFleetType.put("weight", fleetTypeSpawnWeights.get(fleetType));
                json.append("csvRow", weightAndFleetType);
            }
            return json.toString();
        } catch (Exception e) {
            return "failed to convert to string";
        }
    }

    @NotNull
    public static VariantsLibFleetParams makeParamsFromFleet(@NotNull CampaignFleetAPI fleet) {
        final VariantsLibFleetParams params = new VariantsLibFleetParams();
        final MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();

        params.fleetName = fleet.getName();
        params.faction = fleet.getFaction().getId();
        if(fleetMemory.contains(MemFlags.MEMORY_KEY_FLEET_TYPE)) {
            params.fleetType = fleetMemory.getString(MemFlags.MEMORY_KEY_FLEET_TYPE);
        }
        if(fleetMemory.contains(MemFlags.SALVAGE_SEED)) {
            params.seed = fleetMemory.getLong(MemFlags.SALVAGE_SEED);
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
        params.fleetPoints = totalDP;
        params.numOfficers = numOfficers;
        params.averageOfficerLevel = ((float) sumOfficerLevels) / numOfficers;

        try {
            DefaultFleetInflaterParams inflaterParams = (DefaultFleetInflaterParams)fleet.getInflater().getParams();
            params.averageSMods = params.averageSMods;
        } catch(Exception e) {
            log.info("could not get average smods defaulting to none");
        }

        try {
            params.quality = fleet.getInflater().getQuality();
        } catch(Exception e) {
            log.info("could not get quality defaulting to max");
        }

        return params;
    }

    /**
     * Creates a fleet from parameters
     * @param params
     * @return
     */
    @NotNull
    public CampaignFleetAPI makeFleet(@NotNull VariantsLibFleetParams params) {
        return null;
    }

    /**
     * Edits a fleet based on the original fleet's composition
     * @param original Fleet to edit
     */
    public void editFleet(@NotNull CampaignFleetAPI original) {

    }
}
