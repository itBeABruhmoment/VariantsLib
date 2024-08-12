package variants_lib.scripts;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
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
        if(!SettingsData.getInstance().fleetEditingEnabled()) {
            return false;
        }

        String s = "";
        for(String key : fleet.getMemoryWithoutUpdate().getKeys()) {
            s = s + key + " ";
        }
        log.debug(s);

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
            specialFleetSpawnRate = config.specialFleetSpawnRateOverrides.get(params.fleetType) * SettingsData.getInstance().getSpecialFleetSpawnMult();
        } else {
            specialFleetSpawnRate = config.specialFleetSpawnRate * SettingsData.getInstance().getSpecialFleetSpawnMult();
        }

        final Random rand = new Random(params.seed);
        if(useToEdit != null && rand.nextDouble() < specialFleetSpawnRate) {
            log.debug("editing fleet to " + useToEdit.id);
            useToEdit.editFleet(fleet, params);
            fleetMemory.set(CommonStrings.FLEET_VARIANT_KEY, useToEdit.id);
            log.debug("fleet edited to " + useToEdit.id);
        } else if(SettingsData.getInstance().noAutofitFeaturesEnabled()
                && FactionData.FACTION_DATA.get(params.faction).hasTag(CommonStrings.NO_AUTOFIT_TAG)){
            log.debug("applying no autofit features");
            fleet.setInflated(true);

            // edit officers
            final String faction = fleet.getFaction().getId();
            final OfficerFactory officerFactory = new OfficerFactory();
            for(final FleetMemberAPI memberAPI : fleet.getMembersWithFightersCopy()) {
                final ShipVariantAPI originalVariant = ModdedVariantsData.getVariant(memberAPI.getVariant().getHullVariantId());
                if(originalVariant != null) {
                    memberAPI.setVariant(originalVariant, false, true);
                }

                final PersonAPI officer = memberAPI.getCaptain();
                if(Util.isOfficer(officer)) {
                    final String variant = memberAPI.getVariant().getOriginalVariant();
                    final OfficerFactoryParams officerFactoryParams = new OfficerFactoryParams(
                            variant,
                            faction,
                            rand,
                            5
                    );

                    if(originalVariant != null) {
                        VariantData.VariantDataMember variantData = VariantData.VARIANT_DATA.get(originalVariant.getHullVariantId());
                        if(variantData != null) {
                            officerFactoryParams.skillsToAdd.addAll(variantData.getSkills());
                        }
                    }
                    officerFactoryParams.level = officer.getStats().getLevel();
                    officerFactory.editOfficer(officer, officerFactoryParams);
                }
            }

            // add an inflater that just adds smods and dmods
            final VariantsLibFleetInflater inflater = createInflater(fleet, rand.nextLong());
            if(inflater != null) {
                fleet.setInflater(inflater);
                fleet.inflateIfNeeded();
            } else {
                log.info("inflater not created");
            }

            fleetMemory.set(CommonStrings.NO_AUTOFIT_APPLIED, true);
            log.debug("finished applying");
        }
    }

    private static VariantsLibFleetInflater createInflater(CampaignFleetAPI fleet, long seed) {
        final FleetInflater unknownFleetInflater = fleet.getInflater();
        if(unknownFleetInflater instanceof DefaultFleetInflater) {
            DefaultFleetInflater inflater = (DefaultFleetInflater) unknownFleetInflater;
            int averageSMods = 0;
            try {
                DefaultFleetInflaterParams inflaterParams = (DefaultFleetInflaterParams)inflater.getParams();
                averageSMods = inflaterParams.averageSMods;
            } catch(Exception e) {
                log.info("could not get average smods defaulting to none");
            }

            float quality = inflater.getQuality();
            fleet.setInflated(true);

            DefaultFleetInflaterParams inflaterParams = null;
            final Object tempInflaterParams = inflater.getParams();
            if(tempInflaterParams instanceof DefaultFleetInflaterParams) {
                inflaterParams = (DefaultFleetInflaterParams) tempInflaterParams;
            } else {
                inflaterParams = new DefaultFleetInflaterParams();
                inflaterParams.factionId = fleet.getFaction().getId();
                inflaterParams.seed = seed;
            }
            return new VariantsLibFleetInflater(inflaterParams, quality, averageSMods);
        } else if(unknownFleetInflater == null) {
            final DefaultFleetInflaterParams inflaterParams = new DefaultFleetInflaterParams();
            inflaterParams.factionId = fleet.getFaction().getId();
            inflaterParams.seed = seed;
            fleet.setInflater(new VariantsLibFleetInflater(inflaterParams, 1.0f, 0.0f));
        }
        return null;
    }

    private FleetRandomizer() {}
}