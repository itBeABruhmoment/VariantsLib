package variants_lib.scripts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

import variants_lib.data.CommonStrings;
import variants_lib.data.FactionData;
import variants_lib.data.FleetBuildData;
import variants_lib.data.FleetComposition;
import variants_lib.data.SettingsData;
import variants_lib.scripts.fleetedit.FleetBuilding;
import variants_lib.scripts.fleetedit.OfficerEditing;

import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.campaign.FleetInflater;


/*
testing commands:

runcode
for(String str : (String[]) data.BetterVariants_FactionData.FACTION_DATA.get("luddic_church")) {
    Console.showMessage(str);
}

runcode
for(String str : (String[]) data.BetterVariants_VariantData.VARIANT_DATA.get("afflictor_d_pirates_Strike_bv")) {
    Console.showMessage("|" + str + "|");
}
*/


public class FleetRandomizer {
    private static final Logger log = Global.getLogger(variants_lib.scripts.FleetRandomizer.class);
    static {
        log.setLevel(Level.ALL);
    }

    public boolean scriptEnded = false;
    private static final HashSet<String> DISALLOW_FLEET_MODS_FLAGS = new HashSet<String>() {{
        add(MemFlags.ENTITY_MISSION_IMPORTANT); add(MemFlags.MEMORY_KEY_MISSION_IMPORTANT); add(MemFlags.STORY_CRITICAL);
        add(MemFlags.STATION_BASE_FLEET);       add(MemFlags.STATION_FLEET);
    }};
    
    private static HashMap<String, FleetInflater> inflators = new HashMap<String, FleetInflater>();
    private static final Random rand = new Random();

    private static String getFleetType(CampaignFleetAPI fleet)
    {
        String type = fleet.getMemoryWithoutUpdate().getString(MemFlags.MEMORY_KEY_FLEET_TYPE);
        if(type == null) {
            type = "";
        }
        return type;
    }

    private static boolean allowModificationFleet(CampaignFleetAPI fleet)
    {
        
        // don't modify fleets from unregistered factions
        if(!FactionData.FACTION_DATA.containsKey(fleet.getFaction().getId())) {
            log.debug("refused to modify fleet because faction is not registered");
            return false;
        }

        // don't modify special/important fleets
        for(String flag : DISALLOW_FLEET_MODS_FLAGS) {
            if(fleet.getMemoryWithoutUpdate().contains(flag)) {
                log.debug("refused to modify because fleet had the flag " + flag);
                return false;
            }
        }

        return true;
    }

    

    public static void modify(CampaignFleetAPI fleet)
    {
        String factionId = fleet.getFaction().getId();
   
        if(fleet.getMemoryWithoutUpdate().contains(CommonStrings.FLEET_EDITED_MEMKEY)) {
            return;
        }

        log.debug("trying to modify " + fleet.getFullName());
        fleet.getMemoryWithoutUpdate().set(CommonStrings.FLEET_EDITED_MEMKEY, true);
        if(!allowModificationFleet(fleet)) {
            log.debug("modification barred");
            return;
        }

        // edit ships in fleet
        String fleetCompId = null;
        if(SettingsData.fleetEditingEnabled()) {
            fleetCompId = FleetBuilding.editFleet(fleet);
            if(fleetCompId != null) {
                fleet.getMemoryWithoutUpdate().set(CommonStrings.FLEET_VARIANT_KEY, fleetCompId);
            }
        }

        // manage no autofit stuff
        if(SettingsData.noAutofitFeaturesEnabled()
        && FactionData.FACTION_DATA.get(factionId) != null 
        && FactionData.FACTION_DATA.get(factionId).hasTag(CommonStrings.NO_AUTOFIT_TAG)) {
            float quality = 1.0f;
            try {
                quality = fleet.getInflater().getQuality();
            } catch(Exception e) {
                quality = 1.0f;
                log.debug("could not get quality defaulting to max");
            }

            float averageSMods = 0.0f;
            try {
                DefaultFleetInflaterParams params = (DefaultFleetInflaterParams)fleet.getInflater().getParams();
                averageSMods = params.averageSMods;
            } catch(Exception e) {
                averageSMods = 0.0f;
                log.debug("could not get average smods defaulting to none");
            }
            FleetBuilding.addDmods(fleet, quality);
            FleetBuilding.addSmods(fleet, averageSMods);
            fleet.setInflated(true);
        }

        // edit officers of fleet
        if(SettingsData.OfficerEditingEnabled()) {
            OfficerEditing.editAllOfficers(fleet, fleetCompId);
        }

        FleetBuilding.setProperCr(fleet);

        if(fleetCompId != null) {
            // run any post modification scripts
            FleetComposition comp = FleetBuildData.FLEET_DATA.get(fleetCompId);
            if(comp.postModificationScripts != null) {
                for(String scriptPath : comp.postModificationScripts) {
                    FleetBuildData.SCRIPTS.get(scriptPath).run(fleet);
                }
            }
        }
    }

    public static boolean alreadyModified(CampaignFleetAPI fleet)
    {
        return fleet.getMemoryWithoutUpdate().contains(CommonStrings.FLEET_EDITED_MEMKEY);
    }

    private FleetRandomizer() {}
}
/*
SectorEntityToken yourFleet = Global.getSector().getPlayerFleet();
        LocationAPI currentSystem = (LocationAPI)yourFleet.getContainingLocation();
        List<CampaignFleetAPI> fleets = currentSystem.getFleets();
        for(CampaignFleetAPI fleetAPI : fleets) {
            try {
                Console.showMessage(fleetAPI.getInflater());
                Console.showMessage(fleetAPI.getFullName() + " " + fleetAPI.getInflater().getQuality());
            } catch(Exception e) {

            }
            
        }
*/