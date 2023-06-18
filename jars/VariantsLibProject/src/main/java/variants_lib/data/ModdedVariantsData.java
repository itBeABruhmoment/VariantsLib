package variants_lib.data;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;

/**
 * Stores variants that require non-vanilla assets
 */
public class ModdedVariantsData {
    private static final Logger log = Global.getLogger(variants_lib.data.ModdedVariantsData.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static final HashMap<String, ShipVariantAPI> VARIANTS = new HashMap<>();

    public static ShipVariantAPI getVariant(String id) {
        final ShipVariantAPI checkModdedVariants = VARIANTS.get(id);
        if(checkModdedVariants != null) {
            return checkModdedVariants;
        }
        return Global.getSettings().getVariant(id);
    }

    // return false if succeeded and true if failed
    public static boolean addVariantToStore(String variantId, String modId) throws Exception {
        if(VARIANTS.containsKey(variantId)) {
            return false;
        }

        String path = CommonStrings.MODDED_VARIANTS_FOLDER_PATH + variantId + ".variant";
        ShipVariantAPI variant = loadVariant(modId, path);
        if(variant == null) { // search other mods
            for(ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
                modId = mod.getId();
                variant = loadVariant(modId, path);
                if(variant != null) {
                    break;
                }
            }
        }

        if(variant != null) {
            VARIANTS.put(variantId, variant);
            return false;
        } else {
            return true;
        }
    }



    
    // I ctrlC ctrlV'ed this from Magic Lib
    /**
     * Creates a ship variant from a regular variant file.
     * Used to create variants that requires different mods to be loaded.
     * @param path variant file full path.
     * @return ship variant object
     */
    //Credit to Rubi
    public static ShipVariantAPI loadVariant(String modId, String path) {
        ShipVariantAPI variant = null;
        try {
            JSONObject obj = Global.getSettings().loadJSON(path, modId);
            String displayName = obj.getString("displayName");
            int fluxCapacitors = obj.getInt("fluxCapacitors");
            int fluxVents = obj.getInt("fluxVents");
            boolean goalVariant = false;
            try {
                goalVariant = obj.getBoolean("goalVariant");
            } catch (JSONException ignored) {}
            String hullId = obj.getString("hullId");
            JSONArray hullMods = obj.getJSONArray("hullMods");
            JSONArray modules = null;
            try {
                modules = obj.getJSONArray("modules");
            } catch (JSONException ignored) {}
            JSONArray permaMods = obj.getJSONArray("permaMods");
            JSONArray sMods = null;
            try {
                sMods = obj.getJSONArray("sMods");
            } catch (JSONException ignored) {}
            //float quality = (float) obj.getDouble("quality"); not used/available in API
            String variantId = obj.getString("variantId");
            JSONArray weaponGroups = obj.getJSONArray("weaponGroups");
            JSONArray wings = null;
            try {
                wings = obj.getJSONArray("wings");
            } catch (JSONException ignored) {}

            variant = Global.getSettings().createEmptyVariant(variantId, Global.getSettings().getHullSpec(hullId));
            variant.setVariantDisplayName(displayName);
            variant.setNumFluxCapacitors(fluxCapacitors);
            variant.setNumFluxVents(fluxVents);
            variant.setGoalVariant(goalVariant);
            // todo: check if order matters
            if (sMods != null) {
                for (int k = 0; k < sMods.length(); k++) {
                    String sModId = hullMods.getString(k);
                    variant.addPermaMod(sModId, true);
//                    variant.addPermaMod(sModId);
                    variant.addMod(sModId);
                }
            }
            if(permaMods != null){
                for (int j = 0; j < permaMods.length(); j++) {
                    String permaModId = hullMods.getString(j);
                    variant.addPermaMod(permaModId);
                    if(!variant.getHullMods().contains(permaModId)){
                        variant.addMod(permaModId);
                    }
                }
            }
            if(hullMods != null){
                for (int i = 0; i < hullMods.length(); i++) {
                    String hullModId = hullMods.getString(i);
                    if(!variant.getHullMods().contains(hullModId)){
                        variant.addMod(hullModId);
                    }
                }
            }
            if (modules != null) {
                for (int m = 0; m < modules.length(); m++) {
                    JSONObject module = modules.getJSONObject(m);
                    // todo this is a very inefficient way to do it (obj length always == 1)
                    //  but I don't want to deal with Iterators
                    JSONArray slots = module.names();
                    for (int s = 0; s < slots.length(); s++) {
                        String slotId = slots.getString(s);
                        String moduleVariantId = module.getString(slotId);
                        //todo *** Given moduleVariantId instead of path, create ShipVariantAPI using loadVariant() ***
                        variant.setModuleVariant(slotId, Global.getSettings().getVariant(moduleVariantId));
                    }
                }
            }
            // todo maybe you can do something better with variant.getNonBuiltInWeaponSlots()?
            for (int wg = 0; wg < weaponGroups.length(); wg++) {
                WeaponGroupSpec weaponGroupSpec = new WeaponGroupSpec(WeaponGroupType.LINKED);
                JSONObject weaponGroup = weaponGroups.getJSONObject(wg);
                boolean autofire = weaponGroup.getBoolean("autofire");
                String mode = weaponGroup.getString("mode");
                JSONObject weapons = weaponGroup.getJSONObject("weapons");
                JSONArray slots = weapons.names();
                for (int s = 0; s < slots.length(); s++) {
                    String slotId = slots.getString(s);
                    String weaponId = weapons.getString(slotId);
                    variant.addWeapon(slotId, weaponId);
                    weaponGroupSpec.addSlot(slotId);
                }
                weaponGroupSpec.setAutofireOnByDefault(autofire);
                weaponGroupSpec.setType(WeaponGroupType.valueOf(mode));
                variant.addWeaponGroup(weaponGroupSpec);
            }
            if (wings != null) {
                int numBuiltIn = Global.getSettings().getVariant(variant.getHullSpec().getHullId() + "_Hull").getFittedWings().size();
                for (int w = 0; w < wings.length(); w++) {
                    variant.setWingId(numBuiltIn + w, wings.getString(w));
                }
            }
        } catch (Exception e) {
            log.info("could not load ship variant from" + modId + " at " + path);
            variant = null;
        }
        
        return variant;
    }
}
