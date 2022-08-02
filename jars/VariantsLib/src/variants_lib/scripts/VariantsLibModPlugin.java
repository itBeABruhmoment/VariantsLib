package variants_lib.scripts;

import java.io.IOException;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import variants_lib.data.CommonStrings;
import variants_lib.data.FactionData;
import variants_lib.data.FleetBuildData;
import variants_lib.data.FleetComposition;
import variants_lib.data.FleetPartition;
import variants_lib.data.ModdedVariantsData;
import variants_lib.data.SettingsData;
import variants_lib.data.VariantData;

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
    public void onApplicationLoad() throws IOException, JSONException, Exception
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

        //for(FleetComposition part : FleetBuildData.FLEET_DATA.values()) {
        //    log.debug(part.toString());
        //}
        //log.debug("load test");
        //log.debug(ModdedVariantsData.addShipToStore("afflictor_d_pirates_Strike_test", CommonStrings.MOD_ID));
        //log.debug(ModdedVariantsData.VARIANTS.get("afflictor_d_pirates_Strike_test").getHullVariantId());
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        log.debug(CommonStrings.MOD_ID + ": adding listener");
        Global.getSector().addTransientListener(new VariantsLibListener(false));
        log.debug(CommonStrings.MOD_ID + ": initializing faction aggression values");
        UnofficeredPersonalitySetPlugin.innitDefaultAggressionValues();
        
        // BountyData.addBounty("bv_test", 999999.0f);
        // runcode com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.getInstance().addEventCreator(new better_variants.bar_events.BetterVariantsBarEventCreator());
    }
}
