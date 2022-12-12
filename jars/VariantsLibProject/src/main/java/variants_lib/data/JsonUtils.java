package variants_lib.data;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

public class JsonUtils {
    private static final Logger log = Global.getLogger(variants_lib.data.JsonUtils.class);
    static {
        log.setLevel(Level.ALL);
    }
    
    public static String[] getStringArray(String key, String loadedFileInfo, JSONObject json)
    {
        JSONArray jsonArr = null;
        try {
            jsonArr = json.getJSONArray(key);
        } catch(Exception e) {
            log.debug(loadedFileInfo + " could not have its \"" + key + "\" field read, set to null");
            jsonArr = null;
        }
        if(jsonArr == null) {
            return null;
        }

        String[] strArr = new String[jsonArr.length()];
        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                strArr[i] = jsonArr.getString(i);
            } catch(Exception e) {
                log.debug(loadedFileInfo + " had error while reading its \"" + key + "\" field read, set to null");
                return null;
            }
        }
        return strArr;
    }

    public static int getInt(JSONObject json, String key, int defaultVal)
    {
        try {
            return json.getInt(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static boolean getBool(JSONObject json, String key, boolean defaultVal)
    {
        try {
            return json.getBoolean(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static float getFloat(JSONObject json, String key, float defaultVal)
    {
        try {
            return (float) json.getDouble(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }
}
