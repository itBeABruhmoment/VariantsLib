package variants_lib.data;

import lunalib.lunaSettings.LunaSettings;
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

    private static final SettingsData instance = new SettingsData();

    public static SettingsData getInstance() {
        return instance;
    }

    private int maxShipsInAIFleet = 30;
    private int maxOfficersInAIFleet = 10;
    private boolean enableNoAutofit = true;
    private float specialFleetSpawnMult = 1.0f;
    private boolean enableFleetEditing = true;
    private boolean enablePersonalitySet = true;
    private HashMap<String, FleetEditingScript> universalPreModificationScripts = new HashMap<>();
    private HashMap<String, FleetEditingScript> universalPostModificationScripts = new HashMap<>();

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
                maxOfficersInAIFleet = vanillaGameSettings.getInt("maxShipsInAIFleet");
            } catch(Exception e) {
                maxShipsInAIFleet = 30;
                log.debug("could not read maxShipsInAIFleet field, set to 30");
            }
            try {
                maxOfficersInAIFleet = vanillaGameSettings.getInt("maxOfficersInAIFleet");
            } catch(Exception e) {
                log.debug("could not read maxOfficersInAIFleet field, set to 10");
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

        try {
            enableNoAutofit = settings.getBoolean("enableNoAutofitFeatures");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + "failed to read \"enableNoAutofitFeatures\" in " + CommonStrings.SETTINGS_FILE_NAME);
        }

        try {
            specialFleetSpawnMult = (float) settings.getDouble("specialFleetSpawnMult");
        } catch(Exception e) {
            throw new Exception(CommonStrings.MOD_ID + "failed to read \"specialFleetSpawnMult\" in " + CommonStrings.SETTINGS_FILE_NAME);
        }
        if(specialFleetSpawnMult < 0.0f) {
            throw new Exception(CommonStrings.MOD_ID + "\"specialFleetSpawnMult\" from " + CommonStrings.SETTINGS_FILE_NAME + "has a negative value");
        }

        // merge settings with other mods, generally turning a feature off turns it off for everyone and script fields are appended
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

            boolean noAuto = true;
            try {
                noAuto = settingsJson.getBoolean("enableNoAutofitFeatures");
            } catch(Exception e) {
                noAuto = true;
                log.debug("enableNoAutofitFeatures field could not be read setting to default");
            }
            // merge so that one mod disabling it disables the feature
            enableNoAutofit = enableNoAutofit && noAuto;

            boolean fleetEdit = true;
            try {
                fleetEdit = settingsJson.getBoolean("enableFleetEditing");
            } catch(Exception e) {
                fleetEdit = true;
                log.debug("enableFleetEditing field could not be read setting to default");
            }
            enableFleetEditing = enableFleetEditing && fleetEdit;

            boolean personalitySet = true;
            try {
                personalitySet = settingsJson.getBoolean("enableDefaultPersonalitySetting");
            } catch(Exception e) {
                personalitySet = true;
                log.debug("enableDefaultPersonalitySetting field could not be read setting to default");
            }
            enablePersonalitySet = enablePersonalitySet && personalitySet;

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

    private void loadVariantsLibSettingsFromLunaLib() {
        Boolean temp1 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_NO_AUTOFIT);
        if(temp1 != null) {
            enableNoAutofit = temp1;
        } else {
            log.debug(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_NO_AUTOFIT + "\" LunaLib setting null");
        }

        Boolean temp2 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_FLEET_EDITING);
        if(temp2 != null) {
            enableFleetEditing = temp2;
        } else {
            log.debug(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_FLEET_EDITING + "\" LunaLib setting null");
        }

        Boolean temp3 = LunaSettings.getBoolean(CommonStrings.MOD_ID, CommonStrings.LUNA_PERSONALITY_SET);
        if(temp3 != null) {
            enablePersonalitySet = temp3;
        } else {
            log.debug(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_PERSONALITY_SET + "\" LunaLib setting null");
        }

        Double temp4 = LunaSettings.getDouble(CommonStrings.MOD_ID, CommonStrings.LUNA_SPECIAL_FLEET_MULT);
        if(temp4 != null) {
            specialFleetSpawnMult = temp4.floatValue();
        } else {
            log.debug(CommonStrings.MOD_ID + ": \"" + CommonStrings.LUNA_SPECIAL_FLEET_MULT + "\" LunaLib setting null");
        }
    }


    public void loadSettings() throws Exception {
        loadStarSectorSettings();
        loadVariantsLibSettingsFromFile();
        // overwrite settings with LunaLib ones if it is enabled
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            loadVariantsLibSettingsFromLunaLib();
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

    public int getMaxOfficersInAIFleet() {
        return maxOfficersInAIFleet;
    }

    public boolean fleetEditingEnabled() {
        return enableFleetEditing;
    }

    public boolean personalitySetEnabled() {
        return enablePersonalitySet;
    }

    public HashMap<String, FleetEditingScript> getUniversalPostModificationScripts() {
        return universalPostModificationScripts;
    }

    public HashMap<String, FleetEditingScript> getUniversalPreModificationScripts() {
        return universalPreModificationScripts;
    }

    SettingsData() {} // do nothing
}
