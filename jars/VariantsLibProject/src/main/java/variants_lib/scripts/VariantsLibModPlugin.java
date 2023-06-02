package variants_lib.scripts;

import java.io.IOException;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import variants_lib.data.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;


// note to self, when disabling the mod fleets spawned with edited from this mod get converted to nebulas for some reason
public class VariantsLibModPlugin extends BaseModPlugin {
    private static final Logger log = Global.getLogger(variants_lib.scripts.VariantsLibModPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    @Override
    public void onApplicationLoad() throws Exception
    {
        log.debug(CommonStrings.MOD_ID + ": loading settings");
        SettingsData.loadSettings();
        log.debug(CommonStrings.MOD_ID + ": loading faction data");
        FactionData.loadData();
        // somewhat important for FleetBuildData to be loaded before VariantData
        log.debug(CommonStrings.MOD_ID + ": loading fleet build data");
        FleetBuildData.loadData();
        log.debug(CommonStrings.MOD_ID + ": loading variant data");
        VariantData.loadData();
    }

    @Override
    public void onDevModeF8Reload() {
        try {
            FleetBuildData.SCRIPTS.clear();
            FleetBuildData.FLEET_DATA.clear();
            FactionData.FACTION_DATA.clear();
            VariantData.VARIANT_DATA.clear();
            onApplicationLoad();
        } catch (Exception e) {
            log.info(e);
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
}
