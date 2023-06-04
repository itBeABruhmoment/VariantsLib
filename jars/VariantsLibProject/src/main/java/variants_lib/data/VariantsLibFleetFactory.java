package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import variants_lib.scripts.HasHeavyIndustryTracker;

import java.util.*;

/**
 * Generates fleets specified by fleet jsons. Contains both the fields from the fleet json and the logic for creating fleets
 */
public class VariantsLibFleetFactory  {
    protected static final Logger log = Global.getLogger(VariantsLibFleetFactory.class);
    static {
        log.setLevel(Level.ALL);
    }
    protected static final HashSet<String> VALID_PERSONALITIES = new HashSet<String>() {{
        add(Personalities.TIMID); add(Personalities.CAUTIOUS); add(Personalities.STEADY); add(Personalities.RECKLESS); add(Personalities.AGGRESSIVE);
    }};
    protected static final String[] FALLBACK_HULLMODS = {"hardenedshieldemitter", "fluxdistributor",
            "fluxbreakers", "reinforcedhull", "targetingunit", "solar_shielding"};

    // aggression is from 1 to 5
    private static final String[] AGGRESSION_TO_PERSONALITY = {null, Personalities.CAUTIOUS, Personalities.TIMID,
            Personalities.STEADY, Personalities.AGGRESSIVE, Personalities.RECKLESS};

    // derived from loaded jsons and csv
    public ArrayList<AlwaysBuildMember> alwaysInclude = new ArrayList<>(5);
    public ArrayList<FleetPartition> partitions = new ArrayList<>(5);
    public ArrayList<String> commanderSkills = new ArrayList<>(5);
    public String id = "";
    public HashMap<String, Float> fleetTypeSpawnWeights = new HashMap<>();
    public int minDP = 0;
    public int maxDP = Integer.MAX_VALUE;
    public boolean defaultFleetWidePersonalitySet = true;
    public String defaultFleetWidePersonality = "steady";
    public boolean spawnIfNoIndustry = true;
    public boolean autofit = true;
    public int setDPToAtLeast = 0;

    /**
     * as a fraction of the fleet's dp
     */
    public float freighterDp = 0.0f;
    /**
     * as a fraction of the fleet's dp
     */
    public float linerDp = 0.0f;
    /**
     * as a fraction of the fleet's dp
     */
    public float tankerDp = 0.0f;
    /**
     * as a fraction of the fleet's dp
     */
    public float personnelDp = 0.0f;

    /**
     * used for fleet building purposes
     */
    protected int maxOverBudget = 0;

    /**
     * Create a VariantsLibFleetFactory with fields set to some default values. The resulting factory won't be able to
     * create fleets and needs to be initialized (ie. by calling initializeFromJson())
     */
    public VariantsLibFleetFactory() {}

    /**
     * Create a VariantsLibFleetFactory. Calls initializeFromJson()
     * @param fleetJson The fleet json
     * @param fleetJsonCsvRow The fleet json's row in fleets.csv
     * @param modOfOrigin The mod the fleet json is from
     * @throws Exception An exception containing some message on fields that are set to invalid values or failed to load
     */
    public VariantsLibFleetFactory(
            final JSONObject fleetJson,
            final JSONObject fleetJsonCsvRow,
            final String modOfOrigin
    ) throws Exception {
        initializeFromJson(fleetJson, fleetJsonCsvRow, modOfOrigin);
    }

    /**
     * Initialize the fields of this factory based of the JSON parameters.
     * @param fleetJson The fleet json
     * @param fleetJsonCsvRow The fleet json's row in fleets.csv
     * @param modOfOrigin The mod the fleet json is from
     * @throws Exception An exception containing some message on fields that are set to invalid values or failed to load
     */
    public void initializeFromJson(
            final JSONObject fleetJson,
            final JSONObject fleetJsonCsvRow,
            final String modOfOrigin) throws Exception {
        id = JsonUtils.getString(CommonStrings.FLEET_DATA_ID, "", fleetJson);
        if(id.equals("")) {
            throw new Exception(CommonStrings.FLEET_DATA_ID + "field could not be read");
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

        defaultFleetWidePersonality = JsonUtils.getString(CommonStrings.DEFAULT_FLEET_WIDE_PERSONALITY, "none", fleetJson);
        if(defaultFleetWidePersonality.equals("none")) {
            defaultFleetWidePersonality = Personalities.STEADY;
            defaultFleetWidePersonalitySet = false;
        } else if(!VALID_PERSONALITIES.contains(defaultFleetWidePersonality)) {
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
        for(final AlwaysBuildMember member : alwaysInclude) {
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

    /**
     * Choose a random fleet to spawn based on spawn weights in factions.csv
     * @param params Specifications to base selection on (ie. don't choose a fleet with a minDP of 100 when trying to make a 50 dp fleet)
     * @return a fleet factory that meets specifications, or null if no factories could be found
     */
    public static VariantsLibFleetFactory pickFleetFactory(final VariantsLibFleetParams params) {
        if(!FactionData.FACTION_DATA.containsKey(params.faction)) {
            log.debug(params.faction + " not a recognised faction");
            return null;
        }
        final Random rand = new Random(params.seed);
        final ArrayList<VariantsLibFleetFactory> validFleetComps = getValidFleetChoices(params);

        if(validFleetComps.size() == 0) {
            return null;
        }

        final float random = rand.nextFloat();
        float totalWeightsSum = 0;
        for(final VariantsLibFleetFactory factory : validFleetComps) {
            totalWeightsSum += factory.fleetTypeSpawnWeights.get(params.fleetType);
        }
        float runningWeightsSum = 0;
        for(final VariantsLibFleetFactory factory : validFleetComps) {
            runningWeightsSum += factory.fleetTypeSpawnWeights.get(params.fleetType) / totalWeightsSum;
            if(runningWeightsSum > random) {
                return factory;
            }
        }
        return null;
    }

    private static ArrayList<VariantsLibFleetFactory> getValidFleetChoices(final VariantsLibFleetParams params) {
        final ArrayList<VariantsLibFleetFactory> fleetFactories = new ArrayList<>(10);
        for(final String factoryId : FactionData.FACTION_DATA.get(params.faction).customFleetIds) {
            final VariantsLibFleetFactory factory = FleetBuildData.FLEET_DATA.get(factoryId);
            if(factory != null
                    && factory.maxDP >= params.fleetPoints
                    && params.fleetPoints >= factory.minDP
                    && factory.fleetTypeSpawnWeights.containsKey(params.fleetType)
                    && (factory.spawnIfNoIndustry || HasHeavyIndustryTracker.hasHeavyIndustry(params.faction))) {
                fleetFactories.add(factory);
            }
        }
        return fleetFactories;
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
            for(final AlwaysBuildMember member : alwaysInclude) {
                json.append(CommonStrings.ALWAYS_INCLUDE, new JSONObject().put(member.id, member.amount));
            }
            json.put(CommonStrings.ADDITIONAL_COMMANDER_SKILLS, commanderSkills);
            for (final FleetPartition part : partitions) {
                json.append(CommonStrings.FLEET_PARTITIONS, part.toJson());
            }
            for(final String fleetType: fleetTypeSpawnWeights.keySet()) {
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

    /**
     * Creates a fleet from parameters
     * @param params
     * @return Fleet created based off of params
     */
    public CampaignFleetAPI createFleet(final VariantsLibFleetParams params) {
        final CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(params.faction, params.fleetName, true);
        createFleet(fleet, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), false);
        return fleet;
    }

    /**
     * Edits a fleet based on the original fleet's composition
     * @param original Fleet to edit
     */
    public void editFleet(final CampaignFleetAPI original) {
        final VariantsLibFleetParams params = new VariantsLibFleetParams(original);
        clearMembers(original);
        createFleet(original, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), true);
    }

    public void editFleet(final CampaignFleetAPI fleet, final VariantsLibFleetParams params) {
        clearMembers(fleet);
        createFleet(fleet, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), true);
    }

    protected void clearMembers(final CampaignFleetAPI fleetAPI) {
        final FleetDataAPI fleetData = fleetAPI.getFleetData();
        final List<FleetMemberAPI> members = fleetAPI.getMembersWithFightersCopy();
        for(final FleetMemberAPI member : members) {
            fleetData.removeFleetMember(member);
        }
    }

    /**
     * Create a fleet
     * @param fleetAPI Some empty CampaignFleetAPI which will contain the new fleet
     * @param params Params on how to generate fleet
     * @param membersToKeep Ships to always add to the fleet
     * @param rand Random number generator used to generate fleet
     */
    protected void createFleet(
            final CampaignFleetAPI fleetAPI,
            final VariantsLibFleetParams params,
            final ArrayList<FleetMemberAPI> membersToKeep,
            final Random rand,
            final boolean calledFromEditFleet
    ) {
        final CreateFleetVariables vars = new CreateFleetVariables();
        vars.numShipsThatCanBeAdded = SettingsData.getMaxShipsInAIFleet() - membersToKeep.size();
        vars.totalDPRemaining = params.fleetPoints;

        addAlwaysIncludeMembers(vars, params);
        addAutoLogistics(vars, params);
        addPartitionShips(vars, params, rand);
        final ArrayList<FleetMemberAPI> shipsToOfficer = chooseShipsToOfficer(vars, params);
        createOfficers(params, shipsToOfficer, rand);
        if(shipsToOfficer.size() > 0) {
            fleetAPI.setCommander(shipsToOfficer.get(0).getCaptain());
        } else {
            fleetAPI.setCommander(Global.getFactory().createPerson());
        }


        Collections.sort(vars.combatShips, new SortByDP());
        Collections.sort(vars.civilianShips, new SortByDP());

        // add ships to fleet
        for(final FleetMemberAPI member : vars.combatShips) {
            final RepairTrackerAPI repairTracker = member.getRepairTracker();
            repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(final FleetMemberAPI member : vars.civilianShips) {
            final RepairTrackerAPI repairTracker = member.getRepairTracker();
            repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(final FleetMemberAPI member : membersToKeep) {
            fleetAPI.getFleetData().addFleetMember(member);
        }

        addSMods(fleetAPI, params, rand);
        if(autofit) {
            if(!calledFromEditFleet) {
                final DefaultFleetInflaterParams inflaterParams = new DefaultFleetInflaterParams();
                inflaterParams.factionId = params.faction;
                inflaterParams.seed = params.seed;
                inflaterParams.quality = params.quality;
                inflaterParams.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
                fleetAPI.setInflater(new DefaultFleetInflater(inflaterParams));
            }
            fleetAPI.getInflater().inflate(fleetAPI);
            fleetAPI.setInflated(true);
        } else {
            addDMods(fleetAPI, params, rand);
            fleetAPI.setInflated(true);
        }

        final MemoryAPI fleetMemory = fleetAPI.getMemoryWithoutUpdate();
        fleetMemory.set(CommonStrings.FLEET_VARIANT_KEY, id);
        fleetMemory.set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);
    }

    /**
     * Generate and add ships for auto logistics feature and update fields of vars accordingly. Called in createFleet method
     * @param vars variables to update
     * @param params params used to make fleet
     */
    protected void addAutoLogistics(final CreateFleetVariables vars, final VariantsLibFleetParams params) {
        final AutoLogisticsFactory makeAutoLogistics = new AutoLogisticsFactory();
        makeAutoLogistics.faction = params.faction;
        makeAutoLogistics.percentagePersonnel = personnelDp;
        makeAutoLogistics.percentageTankers = tankerDp;
        makeAutoLogistics.percentageFreighters = freighterDp;
        makeAutoLogistics.percentageLiners = linerDp;
        makeAutoLogistics.fleetDP = vars.totalDPRemaining;
        final AutoLogisticsFactory.AutoLogisticsReturn logisticsShips = makeAutoLogistics.createLogisticalShips();

        addShipsToVars(vars, logisticsShips.freighters);
        addShipsToVars(vars, logisticsShips.tankers);
        addShipsToVars(vars, logisticsShips.personnel);
        addShipsToVars(vars, logisticsShips.liners);
    }

    /**
     * Create and add always include members and update vars accordingly. Called in createFleet method
     * @param vars vars to update
     * @param params params for making the fleet
     */
    protected void addAlwaysIncludeMembers(final CreateFleetVariables vars, final VariantsLibFleetParams params) {
        for(final AlwaysBuildMember member : alwaysInclude) {
            for(int i = 0; i < member.amount; i++) {
                final FleetMemberAPI newMember = createShip(member.id);
                if(newMember.isCivilian()) {
                    vars.civilianShips.add(newMember);
                } else {
                    vars.combatShips.add(newMember);
                }
                vars.totalDPRemaining -= Math.round(newMember.getBaseDeploymentCostSupplies());
                vars.numShipsThatCanBeAdded--;
            }
        }
    }

    /**
     * Create members based on fleet partitions and update vars accordingly. Called in createFleet method
     * @param vars vars to update
     * @param params params used to make fleet
     * @param rand random number generator to use
     */
    protected void addPartitionShips(
            final CreateFleetVariables vars,
            final VariantsLibFleetParams params,
            final Random rand
    ) {
        for(int i = 0; i < partitions.size(); i++) {
            final FleetPartition partition = partitions.get(i);

            // calculate dp for partition
            int remainingDpThisPartition = ((int)Math.round(vars.totalDPRemaining * (partition.partitionWeight / sumPartitionWeights(i))));
            if(remainingDpThisPartition > partition.maxDPForPartition) {
                remainingDpThisPartition = partition.maxDPForPartition;
            }

            int shipsRemainingThisPartition = partitions.get(i).maxShipsForPartition;
            while(remainingDpThisPartition > 0 && vars.numShipsThatCanBeAdded > 0 && shipsRemainingThisPartition > 0) {
                final String variantId = pickVariant(partition, remainingDpThisPartition + maxOverBudget, rand);
                if(variantId == null) { // no more variants can be spawned with the dp
                    break;
                }

                final FleetMemberAPI newMember = createShip(variantId);
                if(newMember.isCivilian()) {
                    vars.civilianShips.add(newMember);
                } else {
                    vars.combatShips.add(newMember);
                }

                final int DPofVariant = getDPInt(variantId);
                remainingDpThisPartition -= DPofVariant;
                vars.totalDPRemaining -= DPofVariant;
                vars.numShipsThatCanBeAdded--;
                shipsRemainingThisPartition--;
            }
        }
    }

    private void addShipsToVars(final CreateFleetVariables vars, final List<String> ships) {
        for(final String variantId : ships) {
            final FleetMemberAPI member = createShip(variantId);
            if(vars.totalDPRemaining > 0 && vars.numShipsThatCanBeAdded > 0) {
                if(member.isCivilian()) {
                    vars.civilianShips.add(member);
                } else {
                    vars.combatShips.add(member);
                }
                vars.totalDPRemaining -= Math.round(member.getBaseDeploymentCostSupplies());
                vars.numShipsThatCanBeAdded--;
            } else {
                break;
            }
        }
    }

    /**
     * Choose fleet members to give officers, with the first in the list being the flagship
     * @param vars vars for providing selection of fleet members to officer
     * @param params params used to generate fleet
     * @return list of fleet members to give officers with first being the flagship
     */
    protected ArrayList<FleetMemberAPI> chooseShipsToOfficer(final CreateFleetVariables vars, final VariantsLibFleetParams params) {
        if(vars.civilianShips.size() + vars.combatShips.size() < 1 || params.numOfficers < 1) {
            return new ArrayList<>();
        }
        int numShipsToOfficer = params.numOfficers;
        final ArrayList<FleetMemberAPI> shipsToOfficer = new ArrayList<>(numShipsToOfficer);

        // find ship to use as flagship
        FleetMemberAPI flagShip = null;
        if(vars.combatShips.size() > 0) {
            int maxIndex = 0;
            for(int i = 1; i < vars.combatShips.size(); i++) {
                if(vars.combatShips.get(i).getBaseDeploymentCostSupplies() > vars.combatShips.get(maxIndex).getBaseDeploymentCostSupplies()) {
                    maxIndex = i;
                }
            }
            flagShip = vars.combatShips.get(maxIndex);
        } else {
            int maxIndex = 0;
            for(int i = 1; i < vars.civilianShips.size(); i++) {
                if(vars.civilianShips.get(i).getBaseDeploymentCostSupplies() > vars.civilianShips.get(maxIndex).getBaseDeploymentCostSupplies()) {
                    maxIndex = i;
                }
            }
            flagShip = vars.civilianShips.get(maxIndex);
        }
        numShipsToOfficer--;
        shipsToOfficer.add(flagShip);

        // choose the rest of the ships to officer
        Collections.shuffle(vars.combatShips);
        Collections.shuffle(vars.civilianShips);

        for(int i = 0; i < vars.combatShips.size() && numShipsToOfficer > 0; i++) {
            final FleetMemberAPI member = vars.combatShips.get(i);
            if(member != flagShip) {
                shipsToOfficer.add(member);
                numShipsToOfficer--;
            }
        }
        for(int i = 0; i < vars.civilianShips.size() && numShipsToOfficer > 0; i++) {
            final FleetMemberAPI member = vars.civilianShips.get(i);
            if(member != flagShip) {
                shipsToOfficer.add(member);
                numShipsToOfficer--;
            }
        }
        return shipsToOfficer;
    }

    /**
     * Create officers for the list of FleetMemberAPIs passed in, with the first in the list containing the fleet's commander
     * @param params params used to make fleet
     * @param shipsToOfficer ships to generate officers for
     * @param rand random number generator used
     */
    protected void createOfficers(
            final VariantsLibFleetParams params,
            final ArrayList<FleetMemberAPI> shipsToOfficer,
            final Random rand
    ) {
        if(shipsToOfficer.size() < 1) {
            return;
        }

        String personalityToUse = defaultFleetWidePersonality;
        if(!defaultFleetWidePersonalitySet) {
            final int aggressionVal = Global.getSector().getFaction(params.faction).getDoctrine().getAggression();
            personalityToUse = AGGRESSION_TO_PERSONALITY[aggressionVal];
        }


        final OfficerFactory officerFactory = createOfficerFactory(params);
        final PersonAPI commander = createCommander(
                officerFactory,
                params,
                rand,
                shipsToOfficer.get(0).getVariant().getOriginalVariant(),
                personalityToUse
        );
        shipsToOfficer.get(0).setCaptain(commander);

        // make other officers
        for(int i = 1; i < shipsToOfficer.size(); i++) {
            final FleetMemberAPI toOfficer = shipsToOfficer.get(i);
            toOfficer.setCaptain(createOfficer(
                    officerFactory,
                    params,
                    rand,
                    toOfficer.getVariant().getOriginalVariant(),
                    personalityToUse
            ));
        }
    }

    protected PersonAPI createCommander(
            final OfficerFactory officerFactory,
            final VariantsLibFleetParams fleetParams,
            final Random rand,
            final String variantId,
            final String defaultPersonality
    ) {
        OfficerFactoryParams officerFactoryParams = new OfficerFactoryParams(
                variantId,
                fleetParams.faction,
                rand,
                fleetParams.averageOfficerLevel + 2.0f
        );
        if(officerFactoryParams.level > 10) {
            officerFactoryParams.level = 10;
        }
        officerFactoryParams.skillsToAdd.addAll(commanderSkills);
        officerFactoryParams.personality = defaultPersonality;
        return officerFactory.createOfficer(officerFactoryParams);
    }

    protected PersonAPI createOfficer(
            final OfficerFactory officerFactory,
            final VariantsLibFleetParams fleetParams,
            final Random rand,
            final String variantId,
            final String defaultPersonality
    ) {


        OfficerFactoryParams officerFactoryParams = new OfficerFactoryParams(
                variantId,
                fleetParams.faction,
                rand,
                fleetParams.averageOfficerLevel
        );
        officerFactoryParams.personality = defaultPersonality;
        return officerFactory.createOfficer(officerFactoryParams);
    }

    protected OfficerFactory createOfficerFactory(final VariantsLibFleetParams params) {
        return new OfficerFactory();
    }

    protected FleetMemberAPI createShip(final String variantId) {
        final FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
        ship.getVariant().setOriginalVariant(variantId);
        return ship;
    }

    protected double sumPartitionWeights(final int start) {
        double sum = 0;
        for(int i = start; i < partitions.size(); i++) {
            sum += partitions.get(i).partitionWeight;
        }
        return sum;
    }

    protected String pickVariant(final FleetPartition partition, final int DPLimit, final Random rand) {
        final Vector<FleetPartitionMember> pickableVariants = getPickableVariants(partition, DPLimit);
        if(pickableVariants.size() == 0) { // no eligible variants because not enough dp to spawn them
            return null;
        }

        final double random = rand.nextDouble();
        final double totalWeightsSum = sumWeights(pickableVariants);
        double runningWeightsSum = 0;
        for(final FleetPartitionMember member : pickableVariants) {
            runningWeightsSum += member.weight / totalWeightsSum;
            if(runningWeightsSum >= random) {
                return member.id;
            }
        }
        return pickableVariants.get(pickableVariants.size() - 1).id; // handle possible edge case
    }

    protected double sumWeights(final Vector<FleetPartitionMember> members) {
        double sum = 0;
        for(final FleetPartitionMember member : members) {
            sum += member.weight;
        }
        return sum;
    }

    protected Vector<FleetPartitionMember> getPickableVariants(final FleetPartition partition, final int DPLimit) {
        final Vector<FleetPartitionMember> members = new Vector<FleetPartitionMember>(10);
        for(final FleetPartitionMember member : partition.members) {
            if(getDPInt(member.id) <= DPLimit) {
                members.add(member);
            }
        }
        return members;
    }

    protected void addSMods(final CampaignFleetAPI fleet, final VariantsLibFleetParams params, final Random rand) {
        if(params.averageSMods <= 0.0f) {
            return;
        }
        for(final FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation() && !ship.isCivilian()) {
                final int numSmodsToAdd = (int) Math.round(params.averageSMods + (rand.nextFloat() - 0.5));
                if(numSmodsToAdd > 0) {
                    final String variantId = VariantData.isRegisteredVariant(ship);
                    VariantData.VariantDataMember variantData = null;
                    if(variantId != null) {
                        variantData = VariantData.VARIANT_DATA.get(variantId);
                    }
                    int numAdded =  0;
                    if(variantData != null) {
                        numAdded = addVariantDataSMods(ship.getVariant(), variantData, fleet.getCommander(), numSmodsToAdd);
                    }
                    addRandomSModsToVariant(fleet.getCommander(), ship.getVariant(), rand, numSmodsToAdd - numAdded);
                }
            }
        }
    }

    protected int addVariantDataSMods(
            final ShipVariantAPI variant,
            final VariantData.VariantDataMember variantData,
            final PersonAPI captain,
            final int amount
    ) {
       int i = 0;
       int hullModsAdded = 0;
       while(i < variantData.smods.size() && i < amount) {
           if(attemptToAddHullMod(variant, variantData.getSmods().get(i), captain, true)) {
               hullModsAdded++;
           }
           i++;
       }
       return hullModsAdded;
    }

    /**
     * Adds random(ish) hullMods to a variant. First tries to build in existing hullmods, then random hullmods that are generally useful. Tries filling any unused dp
     * @param captain
     * @param variant
     * @param rand
     * @param numSMods
     */
    protected void addRandomSModsToVariant(
            final PersonAPI captain,
            final ShipVariantAPI variant,
            Random rand,
            final int numSMods
    ) {
        final ShipHullSpecAPI specs = variant.getHullSpec();

        // init some values to consider when adding hullmods
        final boolean lowCr = specs.getNoCRLossSeconds() < 200.0f || variant.hasHullMod(HullMods.SAFETYOVERRIDES);
        boolean reliesOnArmour = specs.getArmorRating() >= 350.0f;

        final ShipAPI.HullSize hullSize = specs.getHullSize();
        int minDpToConsiderSModding = 5;
        switch(hullSize) {
            case DESTROYER:
                minDpToConsiderSModding = 10;
                reliesOnArmour = specs.getArmorRating() >= 650.0f;
                break;
            case CRUISER:
                minDpToConsiderSModding = 15;
                reliesOnArmour = specs.getArmorRating() >= 1200.0f;
                break;
            case CAPITAL_SHIP:
                minDpToConsiderSModding = 20;
                reliesOnArmour = specs.getArmorRating() >= 1450.0f;
                break;
        }

        // first try building in any hullmods the ship may have
        int numHullModsAdded = 0;
        for(String hullMod : variant.getNonBuiltInHullmods()) {
            if(numHullModsAdded >= numSMods) {
                break;
            }

            final int hullModCost = Global.getSettings().getHullModSpec(hullMod).getCostFor(hullSize);
            if(hullModCost >= minDpToConsiderSModding && attemptToAddHullMod(variant, hullMod, captain, true)) {
                numHullModsAdded++;
            }
        }

        // some special cases based off hull
        if(numHullModsAdded < numSMods && lowCr && attemptToAddHullMod(variant, HullMods.HARDENED_SUBSYSTEMS, captain, true)) {
            numHullModsAdded++;
        }
        if(numHullModsAdded < numSMods && reliesOnArmour && attemptToAddHullMod(variant, HullMods.HEAVYARMOR, captain, true)) {
            numHullModsAdded++;
        }

        // fill with some expensive always applicable hullmods
        final int[] randomIndices = Util.createRandomNumberSequence(FALLBACK_HULLMODS.length, rand);
        int i = 0; // reused for adding hullmods to fill dp
        if(numHullModsAdded < numSMods) {
            while(i < FALLBACK_HULLMODS.length && numHullModsAdded < numSMods) {
                final String hullMod = FALLBACK_HULLMODS[randomIndices[i]];
                if(attemptToAddHullMod(variant, hullMod, captain, true)) {
                    numHullModsAdded++;
                }
                i++;
            }
        }

        // try to fill remaining dp
        while(variant.getUnusedOP(captain.getStats()) > 15 && i < randomIndices.length) {
            attemptToAddHullMod(variant, FALLBACK_HULLMODS[i], captain, false);
            i++;
        }
        final CoreAutofitPlugin autoFit = new CoreAutofitPlugin(captain);
        autoFit.addExtraVents(variant);
        autoFit.addExtraCaps(variant);
    }

    /**
     * Add hullmod if present
     * @param variant
     * @param hullMod
     * @param captain
     * @param smod true to add as a smod, false otherwise
     * @return whether the hullmod was not present before and successfully added
     */
    protected boolean attemptToAddHullMod(
            final ShipVariantAPI variant,
            final String hullMod,
            final PersonAPI captain,
            final boolean smod
    ) {
        final HullModSpecAPI hullModSpecs = Global.getSettings().getHullModSpec(hullMod);
        if(smod) {
            if(hullModSpecs.hasTag(Tags.HULLMOD_NO_BUILD_IN) || variant.getSMods().contains(hullMod)) {
                return false;
            }
            if(variant.hasHullMod(hullMod)) {
                variant.removeMod(hullMod);
            }
            variant.addPermaMod(hullMod, true);
        } else {
            final int unusedDP = variant.getUnusedOP(captain.getStats());
            if(variant.hasHullMod(hullMod) || hullModSpecs.getCostFor(variant.getHullSize()) > unusedDP) {
                return false;
            }
            variant.addMod(hullMod);
        }
        return true;
    }

    protected void addDMods(final CampaignFleetAPI fleet, final VariantsLibFleetParams params, final Random rand) {
        // add dmods
        final float quality = params.quality + (0.05f * params.quality); // noticed an abnormal amount dmods in factions such as diktat
        for(final FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation()) {
                final int numExistingDmods = DModManager.getNumDMods(ship.getVariant());
                if(quality <= 0.0f) {
                    final int numDmods = 5 - numExistingDmods;
                    if(numDmods > 0) {
                        DModManager.addDMods(ship, true, numDmods, rand);
                    } // otherwise do nothing
                } else if(quality <= 1.0f) {
                    int numDmods = Math.round(5.0f - (quality + (rand.nextFloat() / 5.0f - 0.1f)) * 5.0f);
                    if(numDmods < 0) {
                        numDmods = 0;
                    }
                    if(numDmods > 5) {
                        numDmods = 5;
                    }
                    numDmods = numDmods - numExistingDmods;
                    if(numDmods > 0) {
                        DModManager.addDMods(ship, true, numDmods, rand);
                    }
                }
            }
        }
    }

    protected static int getDPInt(String variantId) {
        return Math.round(Global.getSettings().getVariant(variantId).getHullSpec().getSuppliesToRecover());
    }

    protected static class SortByDP implements Comparator<FleetMemberAPI>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(FleetMemberAPI a, FleetMemberAPI b)
        {
            if(b.getVariant().getHullVariantId() == null || a.getVariant().getHullVariantId() == null) {
                log.debug("no variant found rippy dippy");
            }
            return getDPInt(b.getVariant().getHullVariantId()) - getDPInt(a.getVariant().getHullVariantId());
        }
    }

    /**
     * Variables that need to be passed around in the createFleet method
     */
    protected static class CreateFleetVariables {
        ArrayList<FleetMemberAPI> combatShips = new ArrayList<>(30);
        ArrayList<FleetMemberAPI> civilianShips = new ArrayList<>(30);
        int numShipsThatCanBeAdded = 0;
        int totalDPRemaining = 0;
    }
}
