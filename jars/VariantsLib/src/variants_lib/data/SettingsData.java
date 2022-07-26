package variants_lib.data;

import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;

// loads settings for this mod
public class SettingsData {
    private static final Logger log = Global.getLogger(variants_lib.data.SettingsData.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static boolean enableAutofit = false;
    private static float specialFleetSpawnMult = 1.0f;
    private static boolean enableOfficerEditing = true;

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
