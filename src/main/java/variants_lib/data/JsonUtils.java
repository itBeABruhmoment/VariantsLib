package variants_lib.data;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import java.util.ArrayList;
import java.util.Iterator;

public class JsonUtils {
    private static final Logger log = Global.getLogger(variants_lib.data.JsonUtils.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static String getString(String key, String defaultVal, JSONObject json) {
        try {
            return json.getString(key);
        } catch (Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static ArrayList<String> getStringArrayList(String key, JSONObject json) {
        try {
            final JSONArray stringJsonArray = json.getJSONArray(key);
            final ArrayList<String> stringArray = new ArrayList<>(stringJsonArray.length());
            for(int i = 0; i < stringJsonArray.length(); i++) {
                stringArray.add(stringJsonArray.getString(i));
            }
            return stringArray;
        } catch (Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return new ArrayList<>();
        }
    }

    public static String[] getStringArray(String key, String loadedFileInfo, JSONObject json) {
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

    public static int getInt(String key, int defaultVal, JSONObject json) {
        try {
            return json.getInt(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static boolean getBool(JSONObject json, String key, boolean defaultVal) {
        try {
            return json.getBoolean(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static float getFloat(JSONObject json, String key, float defaultVal) {
        try {
            return (float) json.getDouble(key);
        } catch(Exception e) {
            log.debug(CommonStrings.MOD_ID + ": failed to read " + key + " field, set to a default value");
            return defaultVal;
        }
    }

    public static void forEachKey(JSONObject json, ForEachKey forEachKey) throws Exception{
        Iterator<String> iterate = json.keys();
        while (iterate.hasNext()) {
            final String key = iterate.next();
            forEachKey.runOnEach(key);
        }
    }

    public static interface ForEachKey {
        public void runOnEach(String key) throws Exception;
    }
}
