package variants_lib.scripts;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import variants_lib.data.*;

import java.util.HashSet;
import java.util.Random;

public class FleetRandomizer {
    private static final Logger log = Global.getLogger(variants_lib.scripts.FleetRandomizer.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static final HashSet<String> DISALLOW_FLEET_MODS_FLAGS = new HashSet<String>() {{
        add(MemFlags.ENTITY_MISSION_IMPORTANT); add(MemFlags.STORY_CRITICAL);
        add(MemFlags.STATION_BASE_FLEET);       add(MemFlags.STATION_FLEET);
    }};

    private static boolean allowFleetModification(CampaignFleetAPI fleet) {
        if(!SettingsData.fleetEditingEnabled()) {
            return false;
        }

        if(fleet.getMemoryWithoutUpdate().contains(CommonStrings.FLEET_EDITED_MEMKEY)) {
            log.debug(CommonStrings.MOD_ID + ": fleet not edited, has " + CommonStrings.FLEET_EDITED_MEMKEY + " memkey");
            return false;
        }

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

        for(FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            if(member.isStation()) {
                log.debug("refused to modify because fleet had a station");
                return false;
            }
        }

        return true;
    }

    public static void modify(CampaignFleetAPI fleet) {
        final MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();

        log.info("trying to modify " + fleet.getFullName());
        if(!allowFleetModification(fleet)) {
            return;
        }
        fleetMemory.set(CommonStrings.FLEET_EDITED_MEMKEY, true);

        final VariantsLibFleetParams params = new VariantsLibFleetParams(fleet);
        final VariantsLibFleetFactory useToEdit = VariantsLibFleetFactory.pickFleetFactory(params);

        // get correct special fleet spawn rate
        double specialFleetSpawnRate = 0.0;
        final FactionData.FactionConfig config = FactionData.FACTION_DATA.get(params.faction);
        if(config.specialFleetSpawnRateOverrides.containsKey(params.fleetType)) {
            specialFleetSpawnRate = config.specialFleetSpawnRateOverrides.get(params.fleetType);
        } else {
            specialFleetSpawnRate = config.specialFleetSpawnRate;
        }

        final Random rand = new Random(params.seed);
        if(useToEdit != null && rand.nextDouble() < specialFleetSpawnRate) {
            useToEdit.editFleet(fleet, params);
            fleetMemory.set(CommonStrings.FLEET_VARIANT_KEY, useToEdit.id);
            log.info("fleet edited to " + useToEdit.id);
        } else if(SettingsData.noAutofitFeaturesEnabled()){
            if(FactionData.FACTION_DATA.get(params.faction).hasTag(CommonStrings.NO_AUTOFIT_TAG)) {
                fleet.setInflated(true);
                FleetBuildingUtils.addDMods(fleet, rand, params.quality);
                FleetBuildingUtils.addSMods(fleet, rand, params.averageSMods);
                fleetMemory.set(CommonStrings.NO_AUTOFIT_APPLIED, true);
            }
            log.info("fleet not edited");
        }
    }

    private FleetRandomizer() {}
}