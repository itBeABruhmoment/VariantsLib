package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
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
    protected static final int MAX_NUM_COMBAT_SKILLS = 14;

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
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(params.faction, params.fleetName, true);
        createFleet(fleet, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed));
        return fleet;
    }

    /**
     * Edits a fleet based on the original fleet's composition
     * @param original Fleet to edit
     */
    public void editFleet(@NotNull CampaignFleetAPI original) {
        final VariantsLibFleetParams params = makeParamsFromFleet(original);
        clearMembers(original);
        createFleet(original, params, new ArrayList<FleetMemberAPI>(), new Random(params.seed));
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
            @NotNull Random rand
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

        for(FleetMemberAPI member : vars.combatShips) {
            log.info(member.getVariant().getOriginalVariant());
        }
        for(FleetMemberAPI member : vars.civilianShips) {
            log.info(member.getVariant().getOriginalVariant());
        }

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
        final OfficerFactory officerFactory = new OfficerFactory(Global.getSector().getFaction(params.faction));

        officerFactory.rand = rand;
        // make commander
        {
            officerFactory.level = Math.round(params.averageOfficerLevel + 1.0f);
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
            officerFactory.level = Math.round(params.averageOfficerLevel + rand.nextFloat() - 0.5f);
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
