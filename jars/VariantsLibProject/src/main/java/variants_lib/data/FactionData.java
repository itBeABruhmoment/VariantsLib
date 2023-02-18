package variants_lib.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
/*
data.BetterVariants_FactionData.FactionConfig da = (data.BetterVariants_FactionData.FactionConfig) data.BetterVariants_FactionData.FACTION_DATA.get("hegemony");
String s = da.toString();
Console.showMessage(s);
*/
import com.fs.starfarer.api.ModSpecAPI;

/**
 * loads and stores data found in faction_tags.csv
 */
public class FactionData {
    private static final Logger log = Global.getLogger(variants_lib.data.FactionData.class);
    static {
        log.setLevel(Level.ALL);
    }

    /**
     * loaded data from faction_tags.csv
     */
    public static final HashMap<String, FactionConfig> FACTION_DATA = new HashMap<String, FactionConfig>();
    private static final String CSV_FIRST_COLUMN_NAME = "factionID";
    private static final String CSV_SECOND_COLUMN_NAME = "fleets";
    private static final String CSV_THIRD_COLUMN_NAME = "specialFleetSpawnRate";
    private static final String CSV_FOURTH_COLUMN_NAME = "tags";
    private static final String CSV_FIFTH_COLUMN_NAME = "specialFleetSpawnRateOverrides";

    private static boolean hasDuplicate(String original, Vector<String> strings)
    {
        int duplicateCount = 0;
        for(String str : strings) {
            if(original.equals(str)) {
                duplicateCount++;
            }

            if(duplicateCount == 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDuplicateTags(Vector<String> strings)
    {
        for(String str : strings) {
            if(hasDuplicate(str, strings)) {
                return true;
            }
        }
        return false;
    }

    private static Vector<String> processTags(String tagsRaw)
    {
        String[] tagsMediumRare = tagsRaw.split(",");
        Vector<String> tagsDone = new Vector<String>();
        for(String tag : tagsMediumRare) {
            String trimmed = tag.trim();
            if(!trimmed.equals("")) {
                tagsDone.add(trimmed);
            }
        }
        tagsDone.trimToSize();
        return tagsDone;
    }

    /**
     * loads data from faction_tags.csv into the field FACTION_DATA, does some basic merging
     * @throws IOException
     * @throws JSONException
     * @throws Exception
     */
    public static void loadData() throws IOException, JSONException, Exception
    {
        for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            String modId = mod.getId();
            log.debug(CommonStrings.MOD_ID + ": trying to load " + CommonStrings.FACTION_TAGS_CSV_PATH + " from the mod " + modId);
            JSONArray data = null;
            try {
                data = Global.getSettings().loadCSV(CommonStrings.FACTION_TAGS_CSV_PATH, modId);
            } catch(Exception e) {
                log.debug(CommonStrings.MOD_ID + ": mod " + modId + " could not have the file " + CommonStrings.FACTION_TAGS_CSV_PATH + " opened, skipped");
                data = null;
            }

            if(data != null) {
                for(int i = 0; i < data.length(); i++) {
                    final JSONObject row = data.getJSONObject(i);

                    // read faction id, ignore rows without this field
                    String factionId = row.optString(CSV_FIRST_COLUMN_NAME);
                    if(factionId.equals("")) {
                        continue;
                    }

                    // read custom fleets the faction can spawn
                    String fleetRaw = row.optString(CSV_SECOND_COLUMN_NAME);
                    Vector<String> fleetIds = processTags(fleetRaw);

                    // read spawnrate of special fleets
                    String specialFleetSpawnRateRaw = row.optString(CSV_THIRD_COLUMN_NAME);
                    double specialFleetSpawnRate = 0;
                    try {
                        specialFleetSpawnRate = Double.parseDouble(specialFleetSpawnRateRaw);
                    } catch(NumberFormatException e) {
                        throw new Exception(CommonStrings.MOD_ID + ": the faction " + factionId + " has invalid \"specialFleetSpawnRateRaw\" field");
                    }
                    if(specialFleetSpawnRate < 0 || specialFleetSpawnRate > 1) {
                        throw new Exception(CommonStrings.MOD_ID + ": the faction " + factionId + " has invalid number in \"specialFleetSpawnRateRaw\" field");
                    }
                    // apply a setting
                    specialFleetSpawnRate *= SettingsData.getSpecialFleetSpawnMult();

                    // read specialFleetSpawnRateOverrides
                    HashMap<String, Double> weightOverrides = new HashMap<String, Double>();
                    String overridesRaw = row.optString(CSV_FIFTH_COLUMN_NAME);
                    if(!overridesRaw.equals("")) {
                        JSONObject weightOverridesJson = null;
                        try {
                            weightOverridesJson = new JSONObject("{ "+ overridesRaw + " }");
                        } catch(Exception e) {
                            throw new Exception(CommonStrings.MOD_ID + ": the faction " + factionId + " has impropery formatted " + CSV_FIFTH_COLUMN_NAME);
                        }
                        if(weightOverridesJson != null) {
                            for(String key : JSONObject.getNames(weightOverridesJson)) {
                                double weight = specialFleetSpawnRate;
                                try {
                                    weight = weightOverridesJson.getDouble(key);
                                } catch(Exception e) {
                                    throw new Exception(CommonStrings.MOD_ID + ": the faction " + factionId + " has impropery formatted double in " + CSV_FIFTH_COLUMN_NAME);
                                }
                                // apply setting
                                weight *= SettingsData.getSpecialFleetSpawnMult();
                                weightOverrides.put(key, weight);
                            }
                        }
                    }

                    // read tags
                    String tagsRaw = row.optString(CSV_FOURTH_COLUMN_NAME);
                    Vector<String> tags = processTags(tagsRaw);
                    HashSet<String> tagsHash = new HashSet<String>();
                    for(String tag : tags) {
                        tagsHash.add(tag);
                    }

                    if(hasDuplicateTags(tags)) {
                        throw new Exception(CommonStrings.MOD_ID + ": the faction " + factionId + " has duplicate tags. Remove them");
                    }


                    if(FACTION_DATA.containsKey(factionId)) {
                        FACTION_DATA.get(factionId).merge(new FactionConfig(tagsHash, fleetIds, specialFleetSpawnRate, weightOverrides));
                    } else {
                        FACTION_DATA.put(factionId, new FactionConfig(tagsHash, fleetIds, specialFleetSpawnRate, weightOverrides));
                    }
                }
            }
        }  
    }

    /**
     * Represents data for a particular faction
     */
    public static class FactionConfig
    {
        public HashMap<String, Double> specialFleetSpawnRateOverrides;
        public HashSet<String> tags;
        public Vector<String> customFleetIds;
        public double specialFleetSpawnRate;

        public boolean hasTag(String tag)
        {
            return tags.contains(tag);
        }

        public FactionConfig(HashSet<String> Tags, Vector<String> CustomFleetIds, double SpecialFleetSpawnRate, 
        HashMap<String, Double> SpecialFleetSpawnRateOverrides)
        {
            tags = Tags;
            customFleetIds = CustomFleetIds;
            specialFleetSpawnRate = SpecialFleetSpawnRate;
            specialFleetSpawnRateOverrides = SpecialFleetSpawnRateOverrides;
        }

        // when data conflicts the values in "other" get prioritized
        public void merge(FactionConfig other)
        {
            specialFleetSpawnRate = other.specialFleetSpawnRate;
            for(String key : other.specialFleetSpawnRateOverrides.keySet()) {
                specialFleetSpawnRateOverrides.put(key, other.specialFleetSpawnRateOverrides.get(key));
            }
            for(String key : other.tags) {
                tags.add(key);
            }
            for(String fleetId : other.customFleetIds) {
                if(!customFleetIds.contains(fleetId)) {
                    customFleetIds.add(fleetId);
                }
            }
        }

        @Override
        public String toString() {
            String str = "";

            str += "tags: \n";
            for(String tag : tags) {
                str += tag + ", ";
            }
            str += "\n";

            str += "fleetIds\n";
            for(String fleet : customFleetIds) {
                str += fleet + " ,";
            }
            str += "\n";


            str += "weight: " + specialFleetSpawnRate + "\n";
            str += "weight overrides\n";
            for(String key : specialFleetSpawnRateOverrides.keySet()) {
                str += "key: " + key + " weight: " + specialFleetSpawnRateOverrides.get(key) + ", ";
            }

            return str;
        }
    }

    private FactionData() {} // do nothing
}