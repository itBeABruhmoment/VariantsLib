package variants_lib.scripts;

import java.io.IOException;
import java.util.HashMap;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import variants_lib.data.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;


// note to self, when disabling the mod fleets spawned with edited from this mod get converted to nebulas for some reason
public class VariantsLibModPlugin extends BaseModPlugin implements LunaSettingsListener {
    private static final Logger log = Global.getLogger(variants_lib.scripts.VariantsLibModPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    @Override
    public void onApplicationLoad() throws Exception {
        clearAndLoadData();
        LunaSettings.addSettingsListener(this);
    }

    private void clearAndLoadData() throws Exception {
        FleetBuildData.SCRIPTS.clear();
        FleetBuildData.FLEET_DATA.clear();
        FactionData.FACTION_DATA.clear();
        VariantData.VARIANT_DATA.clear();

        log.debug(CommonStrings.MOD_ID + ": loading settings");
        SettingsData.getInstance().loadSettings();
        log.debug(CommonStrings.MOD_ID + ": loading faction data");
        FactionData.loadData();
        // somewhat important for FleetBuildData to be loaded before VariantData
        log.debug(CommonStrings.MOD_ID + ": loading fleet build data");
        FleetBuildData.loadData();
        log.debug(CommonStrings.MOD_ID + ": loading variant data");
        VariantData.loadData();

        HashMap<String, VariantsLibPostApplicationLoadScript> scripts = SettingsData.getInstance().getPostVariantsLibApplicationLoadScript();
        for(String key : scripts.keySet()) {
            scripts.get(key).runPostApplicationLoadScript();
        }
    }


    @Override
    public void onDevModeF8Reload() {
        try {
            clearAndLoadData();
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        log.debug(CommonStrings.MOD_ID + ": adding listener");
        Global.getSector().addTransientListener(new VariantsLibListener(false));
        log.debug(CommonStrings.MOD_ID + ": initializing HasHeavyIndustryTracker");
        HasHeavyIndustryTracker.refreshHasHeavyIndustry();
        HasHeavyIndustryTracker.printEntries();
    }


    @Override
    public void settingsChanged(String modId) {

        boolean reloadData = false;
        for(VariantsLibPostApplicationLoadScript script : SettingsData.getInstance().getPostVariantsLibApplicationLoadScript().values()) {
            if(script.getOriginMod().equals(modId) && script.reloadWhenLunaSettingsForOriginModChanged()) {
                reloadData = true;
            }
        }
        if(reloadData) {
            log.info(CommonStrings.MOD_ID + ": settings changed, reloading data");
            try {
                clearAndLoadData();
            } catch (Exception e) {
                log.error("exception during loading variants lib related data");
                log.error(e);
            }
        }
    }
}
