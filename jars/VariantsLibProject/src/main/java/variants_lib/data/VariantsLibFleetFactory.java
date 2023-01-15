package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import variants_lib.scripts.HasHeavyIndustryTracker;
import variants_lib.scripts.UnofficeredPersonalitySetPlugin;

import java.util.*;

public class VariantsLibFleetFactory  {
    protected static final Logger log = Global.getLogger(VariantsLibFleetFactory.class);
    static {
        log.setLevel(Level.ALL);
    }
    protected static final HashSet<String> VALID_PERSONALITIES = new HashSet<String>() {{
        add("timid"); add("cautious"); add("steady"); add("reckless"); add("aggressive");
    }};
    protected static final String[] FALLBACK_HULLMODS = {"hardenedshieldemitter", "fluxdistributor",
            "fluxbreakers", "reinforcedhull", "targetingunit", "solar_shielding"};

    // derived from loaded jsons and csv
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

    // fields for fleet building process
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
            @NotNull final JSONObject fleetJson,
            @NotNull final JSONObject fleetJsonCsvRow,
            @NotNull String modOfOrigin
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
            @NotNull final JSONObject fleetJson,
            @NotNull final JSONObject fleetJsonCsvRow,
            @NotNull String modOfOrigin) throws Exception {
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

    /**
     * Choose a random fleet to spawn based on spawn weights in factions.csv
     * @param params Specifications to base selection on
     * @return a fleet factory that meets specifications, or null if no factories could be found
     */
    @Nullable
    public static VariantsLibFleetFactory pickFleetFactory(@NotNull VariantsLibFleetParams params) {
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
        for(VariantsLibFleetFactory factory : validFleetComps) {
            totalWeightsSum += factory.fleetTypeSpawnWeights.get(params.fleetType);
        }
        //log.debug("rand: " + random + " weightSum: " + totalWeightsSum);
        float runningWeightsSum = 0;
        for(VariantsLibFleetFactory factory : validFleetComps) {
            //log.debug("add: " + comp.spawnWeight / totalWeightsSum);
            runningWeightsSum += factory.fleetTypeSpawnWeights.get(params.fleetType) / totalWeightsSum;
            if(runningWeightsSum > random) {
                return factory;
            }
        }
        return null;
    }

    @NotNull
    private static ArrayList<VariantsLibFleetFactory> getValidFleetChoices(@NotNull VariantsLibFleetParams params) {
        ArrayList<VariantsLibFleetFactory> fleetFactories = new ArrayList<>(10);
        for(String factoryId : FactionData.FACTION_DATA.get(params.faction).customFleetIds) {
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

    /**
     * Creates a fleet from parameters
     * @param params Params to use
     * @return Fleet created based off of params
     */
    @NotNull
    public CampaignFleetAPI makeFleet(@NotNull VariantsLibFleetParams params) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(params.faction, params.fleetName, true);
        createFleet(fleet, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), false);
        return fleet;
    }

    /**
     * Edits a fleet based on the original fleet's composition. Preserves mothballed ships
     * @param original Fleet to edit
     */
    public void editFleet(@NotNull CampaignFleetAPI original) {
        final VariantsLibFleetParams params = new VariantsLibFleetParams(original);
        clearMembers(original);
        createFleet(original, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), true);
    }

    public void editFleet(@NotNull CampaignFleetAPI fleet, @NotNull VariantsLibFleetParams params) {
        clearMembers(fleet);
        createFleet(fleet, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed), true);
    }

    protected void clearMembers(CampaignFleetAPI fleetAPI) {
        FleetDataAPI fleetData = fleetAPI.getFleetData();
        List<FleetMemberAPI> members = fleetAPI.getMembersWithFightersCopy();
        for(FleetMemberAPI member : members) {
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
            @NotNull CampaignFleetAPI fleetAPI,
            @NotNull VariantsLibFleetParams params,
            @NotNull ArrayList<FleetMemberAPI> membersToKeep,
            @NotNull Random rand,
            boolean calledFromEditFleet
    ) {
        CreateFleetVariables vars = new CreateFleetVariables();
        vars.numShipsThatCanBeAdded = SettingsData.getMaxShipsInAIFleet() - membersToKeep.size();
        vars.totalDPRemaining = params.fleetPoints;

        addAlwaysIncludeMembers(vars, params);
        addAutoLogistics(vars, params);
        addPartitionShips(vars, params, rand);
        ArrayList<FleetMemberAPI> shipsToOfficer = chooseShipsToOfficer(vars, params);
        createOfficers(params, shipsToOfficer, rand);
        fleetAPI.setCommander(shipsToOfficer.get(0).getCaptain());

        Collections.sort(vars.combatShips, new SortByDP());
        Collections.sort(vars.civilianShips, new SortByDP());

        // add ships to fleet
        for(FleetMemberAPI member : vars.combatShips) {
            RepairTrackerAPI repairTracker = member.getRepairTracker();
            repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(FleetMemberAPI member : vars.civilianShips) {
            RepairTrackerAPI repairTracker = member.getRepairTracker();
            repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(FleetMemberAPI member : membersToKeep) {
            fleetAPI.getFleetData().addFleetMember(member);
        }

        addSMods(fleetAPI, params, rand);
        if(autofit) {
            if(!calledFromEditFleet) {
                DefaultFleetInflaterParams inflaterParams = new DefaultFleetInflaterParams();
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
    protected void addAutoLogistics(@NotNull CreateFleetVariables vars, @NotNull VariantsLibFleetParams params) {
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
    protected void addAlwaysIncludeMembers(@NotNull CreateFleetVariables vars, @NotNull VariantsLibFleetParams params) {
        for(AlwaysBuildMember member : alwaysInclude) {
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
            @NotNull CreateFleetVariables vars,
            @NotNull VariantsLibFleetParams params,
            @NotNull Random rand
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
                String variantId = pickVariant(partition, remainingDpThisPartition + maxOverBudget, rand);
                if(variantId == null) { // no more variants can be spawned with the dp
                    break;
                }

                FleetMemberAPI newMember = createShip(variantId);
                if(newMember.isCivilian()) {
                    vars.civilianShips.add(newMember);
                } else {
                    vars.combatShips.add(newMember);
                }

                int DPofVariant = getDPInt(variantId);
                remainingDpThisPartition -= DPofVariant;
                vars.totalDPRemaining -= DPofVariant;
                vars.numShipsThatCanBeAdded--;
                shipsRemainingThisPartition--;
            }
        }
    }

    private void addShipsToVars(@NotNull CreateFleetVariables vars, @NotNull List<FleetMemberAPI> ships) {
        for(FleetMemberAPI member : ships) {
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
    @NotNull
    protected ArrayList<FleetMemberAPI> chooseShipsToOfficer(@NotNull CreateFleetVariables vars, @NotNull VariantsLibFleetParams params) {
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
            @NotNull VariantsLibFleetParams params,
            @NotNull ArrayList<FleetMemberAPI> shipsToOfficer,
            @NotNull Random rand
    ) {
        if(shipsToOfficer.size() < 1) {
            return;
        }

        final OfficerFactory officerFactory = new OfficerFactory(Global.getSector().getFaction(params.faction));

        officerFactory.rand = rand;
        // make commander
        {
            officerFactory.level = Math.round(params.averageOfficerLevel + 2.0f);
            if(officerFactory.level > 10) {
                officerFactory.level = 10;
            }

            final String factionDefaultPersonality = UnofficeredPersonalitySetPlugin.getDefaultPersonality(params.faction);
            if(factionDefaultPersonality != null) {
                officerFactory.personality = factionDefaultPersonality;
            }

            officerFactory.skillsToAdd.addAll(commanderSkills);
            final VariantData.VariantDataMember variantData = VariantData.VARIANT_DATA.get(shipsToOfficer.get(0).getVariant().getOriginalVariant());
            if(variantData != null) {
                for(String tag : variantData.officerSpecifications) {
                    final String skill = CommonStrings.SKILL_EDIT_TAGS.get(tag);
                    final String personality = CommonStrings.PERSONALITY_EDIT_TAGS.get(tag);
                    if(skill != null) {
                        officerFactory.skillsToAdd.add(skill);
                    } else if(personality != null) {
                        officerFactory.personality = personality;
                    }
                }
            }
            shipsToOfficer.get(0).setCaptain(officerFactory.makeOfficer());
        }

        // make other officers
        for(int i = 1; i < shipsToOfficer.size(); i++) {
            final FleetMemberAPI toOfficer = shipsToOfficer.get(i);
            officerFactory.level = Math.round(params.averageOfficerLevel + 2 * rand.nextFloat() - 1.0f);
            if (officerFactory.level < 1) {
                officerFactory.level = 1;
            }

            final String factionDefaultPersonality = UnofficeredPersonalitySetPlugin.getDefaultPersonality(params.faction);
            if (factionDefaultPersonality != null) {
                officerFactory.personality = factionDefaultPersonality;
            }

            final VariantData.VariantDataMember variantData = VariantData.VARIANT_DATA.get(shipsToOfficer.get(0).getVariant().getOriginalVariant());
            if (variantData != null) {
                for (String tag : variantData.officerSpecifications) {
                    final String skill = CommonStrings.SKILL_EDIT_TAGS.get(tag);
                    final String personality = CommonStrings.PERSONALITY_EDIT_TAGS.get(tag);
                    if (skill != null) {
                        officerFactory.skillsToAdd.add(skill);
                    } else if (personality != null) {
                        officerFactory.personality = personality;
                    }
                }
            }
            toOfficer.setCaptain(officerFactory.makeOfficer());
        }
    }

    protected FleetMemberAPI createShip(@NotNull String variantId) {
        final FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
        ship.getVariant().setOriginalVariant(variantId);
        return ship;
    }

    protected double sumPartitionWeights(int start) {
        double sum = 0;
        for(int i = start; i < partitions.size(); i++) {
            sum += partitions.get(i).partitionWeight;
        }
        return sum;
    }

    @Nullable
    protected String pickVariant(@NotNull FleetPartition partition, int DPLimit, @NotNull Random rand) {
        Vector<FleetPartitionMember> pickableVariants = getPickableVariants(partition, DPLimit);
        if(pickableVariants.size() == 0) { // no eligible variants because not enough dp to spawn them
            return null;
        }

        double random = rand.nextDouble();
        double totalWeightsSum = sumWeights(pickableVariants);
        double runningWeightsSum = 0;
        for(FleetPartitionMember member : pickableVariants) {
            runningWeightsSum += member.weight / totalWeightsSum;
            if(runningWeightsSum >= random) {
                return member.id;
            }
        }
        return pickableVariants.get(pickableVariants.size() - 1).id; // handle possible edge case
    }

    protected double sumWeights(@NotNull Vector<FleetPartitionMember> members) {
        double sum = 0;
        for(FleetPartitionMember member : members) {
            sum += member.weight;
        }
        return sum;
    }

    protected @NotNull Vector<FleetPartitionMember> getPickableVariants(@NotNull FleetPartition partition, int DPLimit) {
        Vector<FleetPartitionMember> members = new Vector<FleetPartitionMember>(10);
        for(FleetPartitionMember member : partition.members) {
            if(getDPInt(member.id) <= DPLimit) {
                members.add(member);
            }
        }
        return members;
    }

    protected void addSMods(CampaignFleetAPI fleet, VariantsLibFleetParams params, Random rand) {
        if(params.averageSMods <= 0.0f) {
            return;
        }
        for(FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation() && !ship.isCivilian()) {
                int numSmodsToAdd = (int) Math.round(params.averageSMods + (rand.nextFloat() - 0.5));
                if(numSmodsToAdd < 1) {
                    continue;
                }

                String variantId = VariantData.isRegisteredVariant(ship);
                VariantData.VariantDataMember variantData = null;
                if(variantId != null) {
                    variantData = VariantData.VARIANT_DATA.get(variantId);
                }

                if(variantData == null || variantData.smods.size() == 0) {
                    ShipVariantAPI variant  = ship.getVariant();
                    Collection<String> hullMods = variant.getNonBuiltInHullmods();
                    int start = rand.nextInt() & Integer.MAX_VALUE; // get positive int
                    start = start % FALLBACK_HULLMODS.length;
                    int numHullModsAdded = 0;
                    for(int i = 0; i < FALLBACK_HULLMODS.length && numHullModsAdded < numSmodsToAdd; i++) {
                        int index = (start + i) % FALLBACK_HULLMODS.length;
                        if(!hullMods.contains(FALLBACK_HULLMODS[index])) {
                            variant.addPermaMod(FALLBACK_HULLMODS[index], true);
                            numHullModsAdded++;
                        }
                    }
                } else {
                    ShipVariantAPI variant  = ship.getVariant();
                    Collection<String> hullMods = variant.getNonBuiltInHullmods();
                    int numSmodsAdded = 0;
                    while(numSmodsAdded < numSmodsToAdd && numSmodsAdded < variantData.smods.size()) {
                        if(!hullMods.contains(variantData.smods.get(numSmodsAdded))) {
                            variant.addPermaMod(variantData.smods.get(numSmodsAdded), true);
                            numSmodsAdded++;
                        }
                    }
                    // fill in remaining hullmods
                    hullMods = variant.getNonBuiltInHullmods();
                    int start = rand.nextInt() & Integer.MAX_VALUE; // get positive int
                    start = start % FALLBACK_HULLMODS.length;
                    for(int i = 0; i < FALLBACK_HULLMODS.length && numSmodsAdded < numSmodsToAdd; i++) {
                        int index = (start + i) % FALLBACK_HULLMODS.length;
                        if(!hullMods.contains(FALLBACK_HULLMODS[index])) {
                            variant.addPermaMod(FALLBACK_HULLMODS[index], true);
                            numSmodsAdded++;
                        }
                    }
                }
            }
        }
    }

    protected void addDMods(CampaignFleetAPI fleet, VariantsLibFleetParams params, Random rand) {
        // add dmods
        float quality = params.quality + (0.05f * params.quality); // noticed an abnormal amount dmods in factions such as diktat
        for(FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation()) {
                int numExistingDmods = DModManager.getNumDMods(ship.getVariant());
                if(quality <= 0.0f) {
                    int numDmods = 5 - numExistingDmods;
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

    protected static int getDPInt(@NotNull String variantId) {
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
        @NotNull
        ArrayList<FleetMemberAPI> combatShips = new ArrayList<>(30);
        @NotNull
        ArrayList<FleetMemberAPI> civilianShips = new ArrayList<>(30);
        int numShipsThatCanBeAdded = 0;
        int totalDPRemaining = 0;
    }
}
