package variants_lib.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

public class FleetComposition {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetComposition.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static final String DEFAULT_FLEET_TYPES_MACRO = "%combat";
    public static final HashSet<String> DEFAULT_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add(FleetTypes.MERC_ARMADA);    add(FleetTypes.MERC_BOUNTY_HUNTER); add(FleetTypes.MERC_PATROL);
        add(FleetTypes.MERC_PRIVATEER); add(FleetTypes.MERC_SCOUT);         add(FleetTypes.PATROL_LARGE);
        add(FleetTypes.PATROL_MEDIUM);  add(FleetTypes.PATROL_SMALL);       
    }};

    public static final String NON_INVASION_FLEET_TYPES_MACRO = "%notinvasion";
    public static final HashSet<String> NON_INVASION_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add(FleetTypes.MERC_ARMADA);    add(FleetTypes.MERC_BOUNTY_HUNTER); add(FleetTypes.MERC_PATROL);
        add(FleetTypes.MERC_PRIVATEER); add(FleetTypes.MERC_SCOUT);         add(FleetTypes.PATROL_LARGE);
        add(FleetTypes.PATROL_MEDIUM);  add(FleetTypes.PATROL_SMALL);       add("vengeanceFleet");
        add("nex_specialForces");
    }};

    public static final String NON_INVASION_BOSS_FLEET_TYPES_MACRO = "%notinvasionboss";
    public static final HashSet<String> NON_INVASION_BOSS_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add("vengeanceFleet"); add("nex_specialForces");
    }};

    public static final String INVASION_FLEET_TYPES_MACRO = "%invasion";
    public static final HashSet<String> INVASION_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add(FleetTypes.TASK_FORCE);     add(FleetTypes.INSPECTION_FLEET);   add("exerelinInvasionFleet");
        add("exerelinInvasionSupportFleet");
    }};

    public static final String COMBAT_FLEET_TYPES_MACRO = "%combatplus";
    public static final HashSet<String> COMBAT_PRESET_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add(FleetTypes.MERC_ARMADA);    add(FleetTypes.MERC_BOUNTY_HUNTER); add(FleetTypes.MERC_PATROL);
        add(FleetTypes.MERC_PRIVATEER); add(FleetTypes.MERC_SCOUT);         add(FleetTypes.PATROL_LARGE);
        add(FleetTypes.PATROL_MEDIUM);  add(FleetTypes.PATROL_SMALL);       add(FleetTypes.TASK_FORCE);
        add(FleetTypes.INSPECTION_FLEET);add("vengeanceFleet");             add("nex_specialForces");
        add("exerelinInvasionFleet");   add("exerelinInvasionSupportFleet");
    }};

    public static final String BOSS_FLEET_TYPES_MACRO = "%boss";
    public static final HashSet<String> BOSS_PRESET_TARGET_FLEET_TYPES = new HashSet<String>() {{
        add(FleetTypes.TASK_FORCE);     add(FleetTypes.INSPECTION_FLEET);   add("vengeanceFleet");
        add("nex_specialForces");       add("exerelinInvasionFleet");   add("exerelinInvasionSupportFleet");
    }};

    public static final HashMap<String, HashSet<String>> FLEET_TYPE_MACROS = new HashMap<String, HashSet<String>>() {{
        put(DEFAULT_FLEET_TYPES_MACRO, DEFAULT_TARGET_FLEET_TYPES); put(NON_INVASION_FLEET_TYPES_MACRO, NON_INVASION_TARGET_FLEET_TYPES);
        put(INVASION_FLEET_TYPES_MACRO, INVASION_TARGET_FLEET_TYPES); put(COMBAT_FLEET_TYPES_MACRO, COMBAT_PRESET_TARGET_FLEET_TYPES);
        put(BOSS_FLEET_TYPES_MACRO, BOSS_PRESET_TARGET_FLEET_TYPES); put(NON_INVASION_BOSS_FLEET_TYPES_MACRO, NON_INVASION_BOSS_TARGET_FLEET_TYPES);
    }};

    public HashSet<String> targetFleetTypes;
    public AlwaysBuildMember[] alwaysInclude;
    public FleetPartition[] partitions;
    public String[] commanderSkills;
    public String id;
    public double spawnWeight;
    public int minDP;
    public int maxDP;
    public String defaultFleetWidePersonality;
    
    // runcode Console.showMessage(data.BetterVariants_FleetBuildData.FleetData.get("bv_tritachyon_doomhyperion").toString());
    @Override
    public String toString() 
    {
        String str = "";
        for(int i = 0; i < partitions.length; i++) {
            str += "part " + i + " weight: " + partitions[i].partitionWeight + " maxDP: " + maxDP + " minDP: " + minDP + "\n";

            for(int j = 0; j < partitions[i].members.length; j++) {
                str += partitions[i].members[j].id + " " + partitions[i].members[j].weight + "\n";
            }
        }
        
        if(alwaysInclude == null) {
            return str;
        }
        for(AlwaysBuildMember mem : alwaysInclude) {
            str += "id: " + mem.id + " amount: " + mem.amount + "\n";
        }
        return str;
    }

    // constuct from json
    public FleetComposition(JSONObject fleetDataJson, String dataId, String loadedFileInfo) throws Exception, IOException
    {
        id = dataId;
        
        // read spawn weight
        try {
            spawnWeight = fleetDataJson.getDouble("spawnWeight");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " could not have its \"spawnWeight\" field read. Check spelling/formatting");
        }
        if(spawnWeight < 0) {
            throw new Exception(loadedFileInfo + " has \"spawnWeight\" field less than zero");
        }

        // read "maxDP" and "minDp" fields
        try {
            minDP = fleetDataJson.getInt("minDP");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " could not have its \"minDP\" field read. Check formatting. Field should be integer");
        }
        if(minDP < 0) {
            throw new Exception(loadedFileInfo + " has negative \"minDP\" field");
        }

        try {
            maxDP = fleetDataJson.getInt("maxDP");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " could not have its \"maxDP\" field read. Check formatting. Field should be integer");
        }
        if(minDP < 0) {
            throw new Exception(loadedFileInfo + " has negative \"maxDP\" field");
        }
        if(minDP > maxDP) {
            throw new Exception(loadedFileInfo + " has \"maxDP\" field less than \"minDP\" field");
        }

        // read defaultFleetWidePersonality field
        try {
            defaultFleetWidePersonality = fleetDataJson.getString("defaultFleetWidePersonality");
        } catch(Exception e) {
            defaultFleetWidePersonality = Personalities.STEADY;
        }
        if(defaultFleetWidePersonality != null && !CommonStrings.PERSONALITIES.contains(defaultFleetWidePersonality)) {
            throw new Exception(loadedFileInfo + " has invalid personality in \"defaultFleetWidePersonality\" field");
        }

        // read "targetFleetTypes" field
        JSONArray targetFleetTypesJson = null;
        try {
            targetFleetTypesJson  = fleetDataJson.getJSONArray("targetFleetTypes");
        } catch(Exception e) {
            log.debug(loadedFileInfo + " has no \"targetFleetTypes\" field, setting to some default value");
            targetFleetTypesJson = null;
        }

        if(targetFleetTypesJson != null) {
            targetFleetTypes = new HashSet<String>();
            for(int i = 0; i < targetFleetTypesJson.length(); i++) {
                try {
                    String fleetType = targetFleetTypesJson.getString(i);
                    // check for macros
                    final HashSet<String> preset = FLEET_TYPE_MACROS.get(fleetType);
                    if(preset != null) {
                        targetFleetTypes = preset;
                        break;
                    }
                    targetFleetTypes.add(fleetType);
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " could not have element in \"targetFleetTypes\" field read");
                }
            }
        } else {
            targetFleetTypes = DEFAULT_TARGET_FLEET_TYPES;
        }

        // read "additionalCommanderSkills" field
        JSONArray commanderSkillsJson = null;
        try {
            commanderSkillsJson  = fleetDataJson.getJSONArray("additionalCommanderSkills");
        } catch(Exception e) {
            log.debug(loadedFileInfo + " has no \"additionalCommanderSkills\" field, setting to nothing");
            commanderSkillsJson = null;
        }

        if(commanderSkillsJson == null) {
            commanderSkills = null;
        } else {
            List<String> skillIds = Global.getSettings().getSkillIds();
            commanderSkills = new String[commanderSkillsJson.length()];
            for(int i = 0; i < commanderSkillsJson.length(); i++) {
                try {
                    commanderSkills[i] = commanderSkillsJson.getString(i);
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " could not have element in \"additionalCommanderSkills\" field read");
                }

                // verify if String is skill
                boolean found = false;
                for(String skillId : skillIds) {
                    if(skillId.equals(commanderSkills[i])) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    throw new Exception(loadedFileInfo + " has invalid skill \"" + commanderSkills[i] + "\"");
                }
            }

        }

        // read partitions data field
        final JSONArray fleetPartitionsData;
        try {
            fleetPartitionsData = fleetDataJson.getJSONArray("fleetPartitions");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " could not have its \"fleetPartitions\" field read. Check spelling/formatting");
        }
        if(fleetPartitionsData.length() == 0) {
            throw new Exception(loadedFileInfo + " has empty \"fleetPartitions\" field");
        }

        // read the individual partitions
        double partitionWeightSum = 0;
        partitions = new FleetPartition[fleetPartitionsData.length()];
        for(int i = 0; i < fleetPartitionsData.length(); i++) {
            final JSONObject partitionData = fleetPartitionsData.getJSONObject(i);
            partitions[i] = new FleetPartition(partitionData, loadedFileInfo, i);
            partitionWeightSum += partitions[i].partitionWeight;
        }

        // make partiton weights a percentage (number between 0 and 1)
        for(FleetPartition part : partitions) {
            part.makePartitionWeightPercentage(partitionWeightSum);
        }

        // load always include variants
        JSONArray alwaysIncludeData = null;
        try {
            alwaysIncludeData = fleetDataJson.getJSONArray("alwaysSpawn");
        } catch(Exception e) {
            alwaysIncludeData = null;
        }

        if(alwaysIncludeData == null) {
            alwaysIncludeData = null;
        } else {
            alwaysInclude = new AlwaysBuildMember[alwaysIncludeData.length()];
            for(int i = 0; i < alwaysIncludeData.length(); i++) {
                JSONObject alwaysIncludeMemberData = alwaysIncludeData.getJSONObject(i);

                String variantId = null;
                try {
                    variantId = alwaysIncludeMemberData.optString("id");
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " always include " + i + " failed to read \"id\"");
                }
                if(!Global.getSettings().doesVariantExist(variantId)) {
                    throw new Exception(loadedFileInfo + " always include " + i + " \""+  id + "\" is not a recognized variant");
                }

                int amount = -1;
                try {
                    amount = alwaysIncludeMemberData.getInt("amount");
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " always include " + i + " failed to read \"amount\"");
                }
                if(amount < 1) {
                    throw new Exception(loadedFileInfo + " always include " + i + " \"amount\" is invalid int");
                }

                alwaysInclude[i] = new AlwaysBuildMember(variantId, amount);
            }
        }
    }
}
