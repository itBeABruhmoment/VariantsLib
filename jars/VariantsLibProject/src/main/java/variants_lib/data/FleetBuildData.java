package variants_lib.data;

import java.util.HashMap;
import java.util.List;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;

import variants_lib.scripts.FleetEditingScript;
import variants_lib.data.JsonUtils;


// loads data on fleet types
public class FleetBuildData {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetBuildData.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static final HashMap<String, VariantsLibFleetFactory> FLEET_DATA = new HashMap<>();
    public static final HashMap<String, FleetEditingScript> SCRIPTS = new HashMap<>();

    /*public static void testJson() throws JSONException, IOException
    {
        org.json.JSONObject data = Global.getSettings().loadJSON("data/bettervariants/fleets/why.json", "better_variants");
        org.json.JSONObject data = Global.getSettings().loadJSON("mod_info.json", "better_variants");
        Global.getSettings().loadJSON("data/config/settings.json");
        Console.showMessage(data);
        org.json.JSONArray partitions = data.getJSONArray("fleetPartitions");
        org.json.JSONObject partition1 = partitions.getJSONObject(0);
        org.json.JSONArray variants = partition1.getJSONArray("variants");
        Console.showMessage(variants.getJSONObject(0).getInt("weight"));
    }*/

    public static void addScriptToStore(String classPath) throws Exception
    {
        try {
            FleetEditingScript script = (FleetEditingScript) Class.forName(classPath).newInstance();
            SCRIPTS.put(classPath, script);
        } catch(ClassNotFoundException e) {
            throw new Exception(CommonStrings.MOD_ID + ": failed to find the class \"" + classPath + "\"");
        } catch(ClassCastException e) {
            throw new Exception(CommonStrings.MOD_ID + ": \"" + classPath + "\" is not a class that implements \"FleetEditingScript\"");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + ": failed to create the class \"" + classPath + "\"");
        }
    }

    public static void loadFleetJson(String fileName, String modId, JSONObject fleetDataCSVRow) throws Exception, IOException
    {
        // for error messages
        String loadedFileInfo = CommonStrings.MOD_ID + ": the file \"" + fileName + "\" from the mod \"" + modId + "\"";

        // load the json
        log.debug("trying to read " + fileName + " from " + modId);
        JSONObject fleetDataJson = null;
        try {
            fleetDataJson = Global.getSettings().loadJSON(fileName, modId);
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " could not be opened. Ensure everything is formated correctly (check your spelling, file structure, formatting, check for duplicate keys, etc)");
        }
        
        String fleetDataId = fleetDataJson.optString("fleetDataId");
        if(fleetDataId.equals("")) {
            throw new Exception(loadedFileInfo + " has no \"fleetDataId\" field, check spelling and formatting");
        }
        if(FLEET_DATA.containsKey(fleetDataId)) {
            throw new Exception(CommonStrings.MOD_ID + ": more than one fleets have the fleetDataId \"" + fleetDataId + "\"");
        }

        // check whether to load the fleet
        String[] requiredMods = JsonUtils.getStringArray("requiredMods", loadedFileInfo, fleetDataJson);
        boolean shouldLoad = false;
        if(requiredMods == null) {
            shouldLoad = true;
        } else {
            shouldLoad = true;
            for(String id : requiredMods) {
                if(!Global.getSettings().getModManager().isModEnabled(id)) {
                    shouldLoad = false;
                    break;
                }
            }
        }

        if(shouldLoad) {
            //FleetComposition comp = new FleetComposition(fleetDataCSVRow, fleetDataJson, fleetDataId, loadedFileInfo, modId);
            // TODO: remove test code
            VariantsLibFleetFactory fleetFactory = new VariantsLibFleetFactory(fleetDataJson, fleetDataCSVRow, modId);
            FLEET_DATA.put(fleetDataId, fleetFactory);
            log.debug(fleetFactory.toString());
        } else {
            log.debug(loadedFileInfo + " was not loaded due to unenabled required mods");
        }
    }

    public static void loadData() throws Exception, IOException
    {
        for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            // load csv for mod
            String modId = mod.getId();
            JSONArray fleetDataRegister = null;
            try {
                fleetDataRegister = Global.getSettings().loadCSV(CommonStrings.FLEETS_CSV_PATH, modId);
            } catch(Exception e) {
                log.debug(CommonStrings.MOD_ID + ": " + CommonStrings.FLEETS_CSV_PATH + " could not be opened for the mod " + modId);
                fleetDataRegister = null;
            }

            if(fleetDataRegister != null) {
                for(int i = 0; i < fleetDataRegister.length(); i++) {
                    // get info for loading the fleet data json
                    final JSONObject row = fleetDataRegister.getJSONObject(i);
                    String fileName = row.optString(CommonStrings.FLEETS_CSV_FIRST_COLUMN_NAME);
                    if(fileName.equals("")) {
                        continue;
                    }
                    fileName = CommonStrings.FLEETS_FOLDER_PATH + fileName;
        
                    loadFleetJson(fileName, modId, row);
                }
            }
        }
    }
    
    private FleetBuildData() {} // do nothing
}
