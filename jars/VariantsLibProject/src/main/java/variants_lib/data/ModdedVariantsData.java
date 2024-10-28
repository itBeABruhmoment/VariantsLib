package variants_lib.data;

import java.io.IOException;
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

        final String path = CommonStrings.MODDED_VARIANTS_FOLDER_PATH + variantId + ".variant";
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
            log.info(CommonStrings.MOD_ID + ": could not find or successfully load " + path + " in any enabled mod");
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

        JSONObject obj = null;
        try {
            obj = Global.getSettings().loadJSON(path, modId);
        } catch (IOException | RuntimeException e) {
            // RuntimeException gets thrown in practice as of 0.97a
            return null;
        } catch (JSONException e) {
            log.info(CommonStrings.MOD_ID + ": could not parse JSON from " + modId + " at " + path);
            return null;
        }

        try {
            final String displayName = obj.getString("displayName");
            final int fluxCapacitors = obj.getInt("fluxCapacitors");
            final int fluxVents = obj.getInt("fluxVents");
            boolean goalVariant = false;
            try {
                goalVariant = obj.getBoolean("goalVariant");
            } catch (JSONException ignored) {}
            String hullId = obj.getString("hullId");
            final JSONArray hullMods = obj.getJSONArray("hullMods");
            JSONArray modules = null;
            try {
                modules = obj.getJSONArray("modules");
            } catch (JSONException ignored) {}
            final JSONArray permaMods = obj.getJSONArray("permaMods");
            JSONArray sMods = null;
            try {
                sMods = obj.getJSONArray("sMods");
            } catch (JSONException ignored) {}
            //float quality = (float) obj.getDouble("quality"); not used/available in API
            String variantId = obj.getString("variantId");
            final JSONArray weaponGroups = obj.getJSONArray("weaponGroups");
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
                    final String sModId = sMods.getString(k);
                    if(Global.getSettings().getHullModSpec(sModId) == null) {
                        log.info(CommonStrings.MOD_ID + ": unknown sMod \"" + sModId + "\" from " + modId + " at " + path);
                        return null;
                    }
                    variant.addPermaMod(sModId, true);
                    variant.addMod(sModId);
                }
            }
            if(permaMods != null){
                for (int j = 0; j < permaMods.length(); j++) {
                    final String permaModId = permaMods.getString(j);
                    if(Global.getSettings().getHullModSpec(permaModId) == null) {
                        log.info(CommonStrings.MOD_ID + ": unknown permaMod \"" + permaModId + "\" from " + modId + " at " + path);
                        return null;
                    }
                    variant.addPermaMod(permaModId);
                    if(!variant.getHullMods().contains(permaModId)){
                        variant.addMod(permaModId);
                    }
                }
            }
            if(hullMods != null){
                for (int i = 0; i < hullMods.length(); i++) {
                    final String hullModId = hullMods.getString(i);
                    if(Global.getSettings().getHullModSpec(hullModId) == null) {
                        log.info(CommonStrings.MOD_ID + ": unknown hullMod \"" + hullModId + "\" from " + modId + " at " + path);
                        return null;
                    }
                    if(!variant.getHullMods().contains(hullModId)){
                        variant.addMod(hullModId);
                    }
                }
            }
            if (modules != null) {
                for (int m = 0; m < modules.length(); m++) {
                    final JSONObject module = modules.getJSONObject(m);
                    // todo this is a very inefficient way to do it (obj length always == 1)
                    //  but I don't want to deal with Iterators
                    final JSONArray slots = module.names();
                    for (int s = 0; s < slots.length(); s++) {
                        final String slotId = slots.getString(s);
                        final String moduleVariantId = module.getString(slotId);
                        if(Global.getSettings().getVariant(moduleVariantId) == null) {
                            log.info(CommonStrings.MOD_ID + ": unknown module \"" + moduleVariantId + "\" from " + modId + " at " + path);
                            return null;
                        }
                        //todo *** Given moduleVariantId instead of path, create ShipVariantAPI using loadVariant() ***
                        variant.setModuleVariant(slotId, Global.getSettings().getVariant(moduleVariantId));
                    }
                }
            }
            // todo maybe you can do something better with variant.getNonBuiltInWeaponSlots()?
            for (int wg = 0; wg < weaponGroups.length(); wg++) {
                final WeaponGroupSpec weaponGroupSpec = new WeaponGroupSpec(WeaponGroupType.LINKED);
                final JSONObject weaponGroup = weaponGroups.getJSONObject(wg);
                final boolean autofire = weaponGroup.getBoolean("autofire");
                final String mode = weaponGroup.getString("mode");
                final JSONObject weapons = weaponGroup.getJSONObject("weapons");
                final JSONArray slots = weapons.names();
                for (int s = 0; s < slots.length(); s++) {
                    final String slotId = slots.getString(s);
                    final String weaponId = weapons.getString(slotId);
                    // getWeaponSpec seems to throw an exception when invalid id is given rather than returning null
                    try {
                        if(Global.getSettings().getWeaponSpec(weaponId) == null) {
                            log.info(CommonStrings.MOD_ID + ": unknown weapon \"" + weaponId + "\" from " + modId + " at " + path);
                            return null;
                        }
                    } catch (Exception e) {
                        log.info(CommonStrings.MOD_ID + ": unknown weapon \"" + weaponId + "\" from " + modId + " at " + path);
                        return null;
                    }
                    variant.addWeapon(slotId, weaponId);
                    weaponGroupSpec.addSlot(slotId);
                }
                weaponGroupSpec.setAutofireOnByDefault(autofire);
                weaponGroupSpec.setType(WeaponGroupType.valueOf(mode));
                variant.addWeaponGroup(weaponGroupSpec);
            }
            if (wings != null) {
                final int numBuiltIn = Global.getSettings().getVariant(variant.getHullSpec().getHullId() + "_Hull").getFittedWings().size();
                for (int w = 0; w < wings.length(); w++) {
                    final String wingId = wings.getString(w);
                    // getFighterWingSpec seems to throw an exception when invalid id is given rather than returning null
                    try {
                        if(Global.getSettings().getFighterWingSpec(wingId) == null) {
                            log.info(CommonStrings.MOD_ID + ": unknown weapon \"" + wingId + "\" from " + modId + " at " + path);
                            return null;
                        }
                    } catch (Exception e) {
                        log.info(CommonStrings.MOD_ID + ": uunknown weapon \"" + wingId + "\" from " + modId + " at " + path);
                        return null;
                    }
                    variant.setWingId(numBuiltIn + w, wingId);
                }
            }
        } catch (Exception e) {
            log.info(e);
            for(final StackTraceElement st : e.getStackTrace()) {
                log.info(st);
            }
            log.info(CommonStrings.MOD_ID + ": error loading variant from " + modId + " at " + path);
            return null;
        }
        
        return variant;
    }
}
