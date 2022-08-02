package variants_lib.data;

import java.util.HashMap;
import java.io.IOException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// loads data of "data/bettervariants/variant_tags.csv"
public class VariantData {
    private static final Logger log = Global.getLogger(variants_lib.data.VariantData.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static final HashMap<String, VariantDataMember> VARIANT_DATA = new HashMap<String, VariantDataMember>();
    private static final String CSV_FIRST_COLUMN_NAME = "variantID";
    private static final String CSV_SECOND_COLUMN_NAME = "officerInfo";
    private static final String CSV_THIRD_COLUMN_NAME = "smods";

    // if the variant is registered return the variantId, if not return null
    public static String isRegisteredVariant(FleetMemberAPI ship)
    {
        if(ship.isFighterWing()) {
            return null;
        }

        if(VARIANT_DATA.containsKey(ship.getVariant().getHullVariantId())) {
            return ship.getVariant().getHullVariantId();
        }

        if(VARIANT_DATA.containsKey(ship.getVariant().getOriginalVariant())) {
            return ship.getVariant().getOriginalVariant();
        }
        
        return null;
    }

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

    // check if all tags are recognised
    private static String hasInvalidOfficerSpecTags(Vector<String> officerSpec)
    {
        for(String tag : officerSpec) {
            if(!CommonStrings.SKILL_EDIT_TAGS.containsKey(tag)
            && !CommonStrings.PERSONALITY_EDIT_TAGS.containsKey(tag)
            && !tag.equals(CommonStrings.DO_NOT_EDIT_OFFICER)) {
                return tag;
            }
        }
        return null;
    }

    // check if all tags are recognised
    private static String hasInvalidSmodTags(Vector<String> smods)
    {
        for(String tag : smods) {
            try {
                if(Global.getSettings().getHullModSpec(tag) == null) {
                    return tag;
                }
            } catch(Exception e) {
                return tag;
            }
        }
        return null;
    }

    public static void loadData() throws IOException, JSONException, Exception
    {
        for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            String modId = mod.getId();
            log.debug(CommonStrings.MOD_ID + ": trying to load " + CommonStrings.VARIANT_TAGS_CSV_PATH + "from the mod " + modId);
            JSONArray data = null;
            try {
                data = Global.getSettings().loadCSV(CommonStrings.VARIANT_TAGS_CSV_PATH, modId);
            } catch(Exception e) {
                data = null;
                log.debug(CommonStrings.MOD_ID + ": " + CommonStrings.VARIANT_TAGS_CSV_PATH + " could not be opened for the mod " + modId + ", skipped");
            }
        
            if(data != null) {
                for(int i = 0; i < data.length(); i++) {
                    final JSONObject row = data.getJSONObject(i);
                    String variantId = row.optString(CSV_FIRST_COLUMN_NAME);
    
                    if(variantId.equals("")) {
                        continue;
                    }
    
                    if(VARIANT_DATA.containsKey(variantId)) {
                        throw new Exception(CommonStrings.MOD_ID + ": the variant \"" + variantId + 
                        "\" appears twice in " + CommonStrings.VARIANT_TAGS_CSV_PATH);
                    }
    
                    if(Global.getSettings().getVariant(variantId) == null && !ModdedVariantsData.VARIANTS.containsKey(variantId)) {
                        log.debug(CommonStrings.MOD_ID + ": WARNING, the variant \"" + variantId + 
                        "\" listed in the file " + CommonStrings.VARIANT_TAGS_CSV_PATH + " is not loaded");
                    }
    
                    String officerSpecRaw = row.optString(CSV_SECOND_COLUMN_NAME);
                    Vector<String> officerSpec = processTags(officerSpecRaw);
                    officerSpec.trimToSize();
    
                    if(hasDuplicateTags(officerSpec)) {
                        throw new Exception(CommonStrings.MOD_ID + ": the variant " + variantId + 
                        " has duplicate officer spec in" + CommonStrings.VARIANT_TAGS_CSV_PATH + ". Remove them");
                    }
    
                    String invalidTag = hasInvalidOfficerSpecTags(officerSpec);
                    if(invalidTag != null) {
                        throw new Exception(CommonStrings.MOD_ID + ": the variant " + variantId + 
                        " has the unrecognised tag \"" + invalidTag + "\" in "+ CommonStrings.VARIANT_TAGS_CSV_PATH);
                    }
    
                    String smodsRaw = row.optString(CSV_THIRD_COLUMN_NAME);
                    Vector<String> smods = processTags(smodsRaw);
                    smods.trimToSize();
    
                    if(hasDuplicateTags(smods)) {
                        throw new Exception(CommonStrings.MOD_ID + ": the variant " + variantId + 
                        " has duplicate smods tags in" + CommonStrings.VARIANT_TAGS_CSV_PATH + ". Remove them");
                    }
    
                    invalidTag = hasInvalidSmodTags(smods);
                    if(invalidTag != null) {
                        throw new Exception(CommonStrings.MOD_ID + ": the variant " + variantId + 
                        " has the unrecognised tag \"" + invalidTag + "\" in "+ CommonStrings.VARIANT_TAGS_CSV_PATH);
                    }
    
                    VARIANT_DATA.put(variantId, new VariantDataMember(officerSpec, smods));
                }
            }
        }
    }

    public static class VariantDataMember {
        public Vector<String> officerSpecifications;
        public Vector<String> smods;

        public VariantDataMember(Vector<String> OfficerSpecifications, Vector<String> Smods)
        {
            officerSpecifications = OfficerSpecifications;
            smods = Smods;
        }
    }

    private VariantData() {}
}
