package variants_lib.data;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

import variants_lib.scripts.FleetEditingScript;
import variants_lib.scripts.VariantsLibPostApplicationLoadScript;

// loads settings for this mod
public class SettingsData {
    private static final Logger log = Global.getLogger(variants_lib.data.SettingsData.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static final SettingsData instance = new SettingsData();

    public static SettingsData getInstance() {
        return instance;
    }

    private int maxShipsInAIFleet = 30;
    private boolean enableNoAutofit = true;
    private float specialFleetSpawnMult = 1.0f;
    private boolean enableFleetEditing = true;
    private boolean enablePersonalitySet = true;
    private boolean universalNoAutofit = false;
    private HashMap<String, FleetEditingScript> universalPreModificationScripts = new HashMap<>();
    private HashMap<String, FleetEditingScript> universalPostModificationScripts = new HashMap<>();
    private HashMap<String, VariantsLibPostApplicationLoadScript> postVariantsLibApplicationLoadScript = new HashMap<>();

    private void loadStarSectorSettings() {
        log.debug("getting important settings from base game settings");
        JSONObject vanillaGameSettings = null;
        try {
            vanillaGameSettings = Global.getSettings().getSettingsJSON();
        } catch(Exception e) {
            log.debug("vanilla settings could not be loaded, setting max ships and officers in fleet to default values");
        }
        if(vanillaGameSettings != null) {
            try {
                maxShipsInAIFleet = vanillaGameSettings.getInt("maxShipsInAIFleet");
            } catch(Exception e) {
                maxShipsInAIFleet = 30;
                log.debug("could not read maxShipsInAIFleet field, set to 30");
            }
        }
    }

    private void loadVariantsLibSettingsFromFile() throws Exception{
        // load settings json
        final JSONObject settings;
        try {
            settings = Global.getSettings().loadJSON(CommonStrings.SETTINGS_FILE_NAME, CommonStrings.MOD_ID);
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + ": failed to read " + CommonStrings.SETTINGS_FILE_NAME);
        }

        enableNoAutofit = JsonUtils.getBool(settings, CommonStrings.SETTING_NO_AUTOFIT, enableNoAutofit);
        specialFleetSpawnMult = JsonUtils.getFloat(settings, CommonStrings.SETTING_SPECIAL_FLEET_SPAWN_MULT, specialFleetSpawnMult);
        if(specialFleetSpawnMult < 0.0f) {
            throw new Exception(CommonStrings.MOD_ID + "\"specialFleetSpawnMult\" from " + CommonStrings.SETTINGS_FILE_NAME + "has a negative value");
        }
        enableFleetEditing = JsonUtils.getBool(settings, CommonStrings.SETTING_ENABLE_FLEET_EDITING, enableFleetEditing);
        enablePersonalitySet = JsonUtils.getBool(settings, CommonStrings.SETTING_ENABLE_PERSONALITY_SET, enablePersonalitySet);
        universalNoAutofit = JsonUtils.getBool(settings, CommonStrings.SETTING_UNIVERSAL_NO_AUTOFIT, universalNoAutofit);

        // merge settings with other mods, generally turning a feature off turns it off for everyone and script fields are appended
        for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            String modId = mod.getId();
            log.debug(CommonStrings.MOD_ID + ": trying to load " + CommonStrings.SETTINGS_FILE_NAME + " from the mod " + modId);
            JSONObject settingsJson = null;
            try {
                settingsJson = Global.getSettings().loadJSON(CommonStrings.SETTINGS_FILE_NAME, modId);
            } catch (Exception e) {
                log.debug(CommonStrings.MOD_ID + ": mod " + modId + " could not have the file " + CommonStrings.SETTINGS_FILE_NAME + " opened, skipped");
                settingsJson = null;
            }

            if (settingsJson == null) {
                continue;
            }

            // if one mod sets it to false it stays false
            enableNoAutofit = enableNoAutofit && JsonUtils.getBool(settings, CommonStrings.SETTING_NO_AUTOFIT, enableNoAutofit);
            enableFleetEditing = enableFleetEditing && JsonUtils.getBool(settings, CommonStrings.SETTING_ENABLE_FLEET_EDITING, enableFleetEditing);
            enablePersonalitySet = enablePersonalitySet && JsonUtils.getBool(settings, CommonStrings.SETTING_ENABLE_PERSONALITY_SET, enablePersonalitySet);
            universalNoAutofit = universalNoAutofit && JsonUtils.getBool(settings, CommonStrings.SETTING_UNIVERSAL_NO_AUTOFIT, universalNoAutofit);

            loadScripts(CommonStrings.SETTING_UNIVERSAL_PRE_MODIFICATION_SCRIPTS, universalPreModificationScripts, settingsJson, modId);
            loadScripts(CommonStrings.SETTING_UNIVERSAL_POST_MODIFICATION_SCRIPTS, universalPostModificationScripts, settingsJson, modId);
            loadScripts(CommonStrings.SETTING_POST_VARIANTS_LIB_APPLICATION_LOAD_SCRIPT, postVariantsLibApplicationLoadScript, settingsJson, modId);
        }
    }

    private void loadVariantsLibSettingsFromLunaLib() {
        Boolean temp1 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_NO_AUTOFIT);
        if(temp1 != null) {
            enableNoAutofit = temp1;
        } else {
            log.error(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_NO_AUTOFIT + "\" LunaLib setting null");
        }

        Boolean temp2 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_FLEET_EDITING);
        if(temp2 != null) {
            enableFleetEditing = temp2;
        } else {
            log.error(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_FLEET_EDITING + "\" LunaLib setting null");
        }

        Boolean temp3 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_PERSONALITY_SET);
        if(temp3 != null) {
            enablePersonalitySet = temp3;
        } else {
            log.error(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_PERSONALITY_SET + "\" LunaLib setting null");
        }

        Double temp4 = LunaSettings.getDouble(CommonStrings.MOD_ID, CommonStrings.LUNA_SPECIAL_FLEET_MULT);
        if(temp4 != null) {
            specialFleetSpawnMult = temp4.floatValue();
        } else {
            log.error(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_SPECIAL_FLEET_MULT + "\" LunaLib setting null");
        }

        Boolean temp5 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_UNIVERSAL_NO_AUTOFIT);
        if(temp5 != null) {
            universalNoAutofit = temp5;
        } else {
            log.error(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_UNIVERSAL_NO_AUTOFIT + "\" LunaLib setting null");
        }
    }

    // loads an array of classpaths into a hashmap, with classpath as the key
    private <T> void loadScripts(
            String fieldName,
            HashMap<String, T> scriptStore,
            JSONObject settingsJson,
            String settingsModId
    ) throws Exception {
        JSONArray scripts = null;
        try {
            scripts = settingsJson.getJSONArray(fieldName);
        } catch(Exception e) {
            scripts = null;
            log.debug(CommonStrings.MOD_ID + ": mod " + settingsModId + " could not have the field \"" + fieldName + "\" in " + CommonStrings.SETTINGS_FILE_NAME + " opened, skipped");
        }
        if(scripts != null) {
            for(int i = 0; i < scripts.length(); i++) {
                String classPath = scripts.getString(i);
                try {
                    T script = (T) Class.forName(classPath).newInstance();
                    scriptStore.put(classPath, script);
                } catch(ClassNotFoundException e) {
                    throw new Exception(CommonStrings.MOD_ID + ": failed to find the class \"" + classPath + "\"");
                } catch(ClassCastException e) {
                    throw new Exception(CommonStrings.MOD_ID + ": \"" + classPath + "\" is not a class that implements specified interface\"");
                } catch(Exception e) {
                    throw new Exception(CommonStrings.MOD_ID + ": failed to create the class \"" + classPath + "\"");
                }
            }
        }
    }

    public void loadSettings() throws Exception {
        loadStarSectorSettings();
        loadVariantsLibSettingsFromFile();
        log.info(CommonStrings.MOD_ID + ":finished loading settings from file \n" + this.toString());
        // overwrite settings with LunaLib ones if it is enabled
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            loadVariantsLibSettingsFromLunaLib();
            log.info(CommonStrings.MOD_ID + ":finished loading settings from luna lib\n" + this.toString());
        }
    }

    public boolean noAutofitFeaturesEnabled() {
        return enableNoAutofit;
    }

    public float getSpecialFleetSpawnMult() {
        return specialFleetSpawnMult;
    }

    public int getMaxShipsInAIFleet() {
        return maxShipsInAIFleet;
    }

    public boolean fleetEditingEnabled() {
        return enableFleetEditing;
    }

    public boolean personalitySetEnabled() {
        return enablePersonalitySet;
    }

    public boolean universalNoAutofitEnabled() { return universalNoAutofit; }

    public HashMap<String, FleetEditingScript> getUniversalPostModificationScripts() {
        return universalPostModificationScripts;
    }

    public HashMap<String, FleetEditingScript> getUniversalPreModificationScripts() {
        return universalPreModificationScripts;
    }

    public HashMap<String, VariantsLibPostApplicationLoadScript> getPostVariantsLibApplicationLoadScript() {
        return postVariantsLibApplicationLoadScript;
    }

    @Override
    public String toString() {
        return "SettingsData{" +
                "maxShipsInAIFleet=" + maxShipsInAIFleet +
                ", enableNoAutofit=" + enableNoAutofit +
                ", specialFleetSpawnMult=" + specialFleetSpawnMult +
                ", enableFleetEditing=" + enableFleetEditing +
                ", enablePersonalitySet=" + enablePersonalitySet +
                ", universalNoAutofit=" + universalNoAutofit +
                ", universalPreModificationScripts=" + universalPreModificationScripts +
                ", universalPostModificationScripts=" + universalPostModificationScripts +
                ", postVariantsLibApplicationLoadScript=" + postVariantsLibApplicationLoadScript +
                '}';
    }

    SettingsData() {} // do nothing
}
