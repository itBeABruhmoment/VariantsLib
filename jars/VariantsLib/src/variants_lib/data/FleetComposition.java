package variants_lib.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.text.DecimalFormat;

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

    public Vector<AlwaysBuildMember> alwaysInclude;
    public FleetPartition[] partitions;
    public String[] commanderSkills;
    public String id;
    public HashMap<String, Float> spawnWeights;
    public int minDP;
    public int maxDP;
    public String defaultFleetWidePersonality;
    
    // runcode Console.showMessage(data.BetterVariants_FleetBuildData.FleetData.get("bv_tritachyon_doomhyperion").toString());
    @Override
    public String toString() 
    {
        String newJsonStr = "spawnWeights:";
        for(String key: spawnWeights.keySet()) {
            newJsonStr += "\"" + key + "\":" + spawnWeights.get(key) + ",";
        }
        newJsonStr += "\n";
        newJsonStr +=  "\n{\n";
        try {
            DecimalFormat df = new DecimalFormat("#.00");
            newJsonStr += "\t\"" + "fleetDataId" + "\":\"" + id + "\",\n";
            newJsonStr += "\t\"" + "minDP" + "\":" + minDP + ",\n";
            newJsonStr += "\t\"" + "maxDP" + "\":" + maxDP + ",\n";

            if(defaultFleetWidePersonality != null) {
                newJsonStr += "\t\"" + "defaultFleetWidePersonality" + "\":\"" + defaultFleetWidePersonality + "\",\n";
            }

            if(alwaysInclude != null) {
                newJsonStr += "\t\"" + "alwaysInclude" + "\":[";
                for(AlwaysBuildMember member : alwaysInclude) {
                    newJsonStr += "\"" + member.id + "\":" + member.amount + ", ";
                }
                newJsonStr += "],\n";
            }

            if(commanderSkills != null) {
                newJsonStr += "\t\"" + "additionalCommanderSkills" + "\":[";
                for(String skill : commanderSkills) {
                    newJsonStr += "\"" + skill + "\",";
                }
                newJsonStr += "],\n";
            }

            newJsonStr += "],\n";
            newJsonStr += "\t\"" + "fleetPartitions" + "\":[\n";
            for(FleetPartition partition : partitions) {
                newJsonStr += "\t\t{\n";
                newJsonStr += "\t\t\t\"partitionWeight\":" + df.format(partition.partitionWeight * 100.0d) + ",\n";
                newJsonStr += "\t\t\t\"variants\":{\n";
                for(FleetPartitionMember member : partition.members) {
                    newJsonStr += "\t\t\t\t\"" + member.id + "\":" + df.format(member.weight * 100.0d) + ",\n";
                }
                newJsonStr += "\t\t\t}\n";
                newJsonStr += "\t\t},\n";
            }
            newJsonStr += "\t],\n";
            newJsonStr += "}";
        } catch(NullPointerException e) {
            log.debug("null");
        }
        return newJsonStr;
    }

    // constuct from json
    public FleetComposition(JSONObject fleetDataCSVRow, JSONObject fleetDataJson, String dataId, String loadedFileInfo) throws Exception, IOException
    {
        id = dataId;

        // construct weights field
        spawnWeights = new HashMap<>();
        Iterator keys = fleetDataCSVRow.keys();
        while(keys.hasNext()) {
            String fleetType = (String)keys.next();
            if(!fleetType.equals(CommonStrings.FLEETS_CSV_FIRST_COLUMN_NAME)) {
                float weight;
                try {
                    weight = (float) fleetDataCSVRow.optDouble(fleetType);
                } catch(Exception e) {
                    weight = 0.0f;
                }
                if(weight > 0.00001f) {
                    spawnWeights.put(fleetType, weight);
                }
            }
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
        float partitionWeightSum = 0;
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
        JSONObject alwaysIncludeData = null;
        try {
            alwaysIncludeData = fleetDataJson.getJSONObject("alwaysInclude");
        } catch(Exception e) {
            alwaysIncludeData = null;
        }

        if(alwaysIncludeData != null) {
            alwaysInclude = new Vector<>();
            Iterator iterate = alwaysIncludeData.keys();
            while(iterate.hasNext()) {
                String key = (String)iterate.next();
                int amount = 0;
                try {
                    amount = alwaysIncludeData.getInt(key);
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " could not have the amount of \"" + key + "\" read in \"alwaysInclude\" field. Check for errors in file");
                }
                if(amount < 0) {
                    throw new Exception(loadedFileInfo + " has invalid number in its \"alwaysInclude\" field");
                }
                alwaysInclude.add(new AlwaysBuildMember(key, amount));
            }
            alwaysInclude.trimToSize();
        } else {
            log.debug(loadedFileInfo + " has no \"alwaysInclude\" field. Set to nothing");
            alwaysInclude = null;
        }
    }
}
