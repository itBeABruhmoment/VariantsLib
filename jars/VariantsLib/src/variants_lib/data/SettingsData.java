package variants_lib.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

import variants_lib.scripts.FleetEditingScript;

// loads settings for this mod
public class SettingsData {
    private static final Logger log = Global.getLogger(variants_lib.data.SettingsData.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static boolean enableAutofit = false;
    private static float specialFleetSpawnMult = 1.0f;
    private static boolean enableOfficerEditing = true;
    public static HashMap<String, FleetEditingScript> universalPreModificationScripts = new HashMap<>();
    public static HashMap<String, FleetEditingScript> universalPostModificationScripts = new HashMap<>();

    public static void loadSettings() throws Exception
    {
        final JSONObject settings;
        try {
            settings = Global.getSettings().loadJSON(CommonStrings.SETTINGS_FILE_NAME, CommonStrings.MOD_ID);
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + ": failed to read " + CommonStrings.SETTINGS_FILE_NAME);
        }

        try {
            enableAutofit = settings.getBoolean("enableAutofit");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + "failed to read \"enableAutofit\" in " + CommonStrings.SETTINGS_FILE_NAME);
        }

        try {
            specialFleetSpawnMult = (float) settings.getDouble("specialFleetSpawnMult");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + "failed to read \"specialFleetSpawnMult\" in " + CommonStrings.SETTINGS_FILE_NAME);
        }
        if(specialFleetSpawnMult < 0.0f) {
            throw new Exception(CommonStrings.MOD_ID + "\"specialFleetSpawnMult\" from " + CommonStrings.SETTINGS_FILE_NAME + "has a negative value");
        }

        try {
            enableOfficerEditing = settings.getBoolean("enableOfficerEditing");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + "failed to read \"enableOfficerEditing\" in " + CommonStrings.SETTINGS_FILE_NAME);
        }

        // get all universalPreModificationScripts and universalPostModificationScripts
        for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            String modId = mod.getId();
            log.debug(CommonStrings.MOD_ID + ": trying to load " + CommonStrings.SETTINGS_FILE_NAME + " from the mod " + modId);
            JSONObject settingsJson = null;
            try {
                settingsJson = Global.getSettings().loadJSON(CommonStrings.SETTINGS_FILE_NAME, modId);
            } catch(Exception e) {
                log.debug(CommonStrings.MOD_ID + ": mod " + modId + " could not have the file " + CommonStrings.SETTINGS_FILE_NAME + " opened, skipped");
                settingsJson = null;
            }

            if(settingsJson == null) {
                continue;
            }

            JSONArray universalPreModificationScriptsArr = null;
            try {
                universalPreModificationScriptsArr = settingsJson.getJSONArray("universalPreModificationScripts");
            } catch(Exception e) {
                universalPreModificationScriptsArr = null;
                log.debug(CommonStrings.MOD_ID + ": mod " + modId + " could not have the field \"universalPreModificationScripts\" in " + CommonStrings.SETTINGS_FILE_NAME + " opened, skipped");
            }
            if(universalPreModificationScriptsArr != null) {
                for(int i = 0; i < universalPreModificationScriptsArr.length(); i++) {
                    String classPath = universalPreModificationScriptsArr.getString(i);
                    try {
                        FleetEditingScript script = (FleetEditingScript) Class.forName(classPath).newInstance();
                        universalPreModificationScripts.put(classPath, script);
                    } catch(ClassNotFoundException e) {
                        throw new Exception(CommonStrings.MOD_ID + ": failed to find the class \"" + classPath + "\"");
                    } catch(ClassCastException e) {
                        throw new Exception(CommonStrings.MOD_ID + ": \"" + classPath + "\" is not a class that implements \"FleetEditingScript\"");
                    } catch(Exception e) {
                        throw new Exception(CommonStrings.MOD_ID + ": failed to create the class \"" + classPath + "\"");
                    }
                }
            }

            JSONArray universalPostModificationScriptsArr = null;
            try {
                universalPostModificationScriptsArr = settingsJson.getJSONArray("universalPostModificationScripts");
            } catch(Exception e) {
                universalPostModificationScriptsArr = null;
                log.debug(CommonStrings.MOD_ID + ": mod " + modId + " could not have the field \"universalPostModificationScripts\" in " + CommonStrings.SETTINGS_FILE_NAME + " opened, skipped");
            }
            if(universalPostModificationScriptsArr != null) {
                for(int i = 0; i < universalPostModificationScriptsArr.length(); i++) {
                    String classPath = universalPostModificationScriptsArr.getString(i);
                    try {
                        FleetEditingScript script = (FleetEditingScript) Class.forName(classPath).newInstance();
                        universalPostModificationScripts.put(classPath, script);
                    } catch(ClassNotFoundException e) {
                        throw new Exception(CommonStrings.MOD_ID + ": failed to find the class \"" + classPath + "\"");
                    } catch(ClassCastException e) {
                        throw new Exception(CommonStrings.MOD_ID + ": \"" + classPath + "\" is not a class that implements \"FleetEditingScript\"");
                    } catch(Exception e) {
                        throw new Exception(CommonStrings.MOD_ID + ": failed to create the class \"" + classPath + "\"");
                    }
                }
            }
        }  
    }

    public static boolean OfficerEditingEnabled()
    {
        return enableOfficerEditing;
    }

    public static boolean autofitEnabled()
    {
        return enableAutofit;
    }

    public static float getSpecialFleetSpawnMult()
    {
        return specialFleetSpawnMult;
    }

    SettingsData() {} // do nothing
}
