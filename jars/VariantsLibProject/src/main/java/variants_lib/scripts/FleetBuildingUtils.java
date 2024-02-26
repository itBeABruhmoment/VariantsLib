package variants_lib.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import variants_lib.data.Util;
import variants_lib.data.VariantData;
import variants_lib.data.VariantsLibFleetFactory;
import variants_lib.data.VariantsLibFleetParams;

import java.util.Collection;
import java.util.Random;

public class FleetBuildingUtils {
    protected static final Logger log = Global.getLogger(FleetBuildingUtils.class);
    static {
        log.setLevel(Level.ALL);
    }
    private static final String[] FALLBACK_HULLMODS = {"hardenedshieldemitter", "fluxdistributor",
            "fluxbreakers", "reinforcedhull", "targetingunit", "solar_shielding"};

    public static void addDMods(@NotNull CampaignFleetAPI fleet, @NotNull Random rand, float quality) {
        quality = quality + (0.05f * quality); // noticed an abnormal amount dmods in factions such as diktat
        for(FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation()) {
                int numExistingDmods = DModManager.getNumDMods(ship.getVariant());
                if(quality <= 0.0f) {
                    int numDmods = 5 - numExistingDmods;
                    if(numDmods > 0) {
                        DModManager.addDMods(ship, true, numDmods, rand);
                    } // otherwise do nothing
                } else if(quality <= 1.0f) {
                    int numDmods = Math.round(5.0f - (quality + (rand.nextFloat() / 5.0f - 0.1f)) * 5.0f);
                    if(numDmods < 0) {
                        numDmods = 0;
                    }
                    if(numDmods > 5) {
                        numDmods = 5;
                    }
                    numDmods = numDmods - numExistingDmods;
                    if(numDmods > 0) {
                        DModManager.addDMods(ship, true, numDmods, rand);
                    }
                }
            }
        }
    }

    public static void addSMods(final CampaignFleetAPI fleet, final Random rand, float averageSMods) {
        if(averageSMods <= 0.0f) {
            return;
        }

        for(final FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation() && !ship.isCivilian()) {
                int numSmodsToAdd = (int) Math.round(averageSMods + (rand.nextFloat() - 0.5));
                numSmodsToAdd = numSmodsToAdd - ship.getVariant().getSMods().size();
                if(numSmodsToAdd > 0) {
                    final String variantId = VariantData.isRegisteredVariant(ship);
                    VariantData.VariantDataMember variantData = null;
                    if(variantId != null) {
                        variantData = VariantData.VARIANT_DATA.get(variantId);
                    }
                    int numAdded =  0;
                    if(variantData != null) {
                        numAdded = addVariantDataSMods(ship.getVariant(), variantData, fleet.getCommander(), numSmodsToAdd);
                    }
                    addRandomSModsToVariant(fleet.getCommander(), ship.getVariant(), rand, numSmodsToAdd - numAdded);
                }
            }
        }
    }

    protected static int addVariantDataSMods(
            final ShipVariantAPI variant,
            final VariantData.VariantDataMember variantData,
            final PersonAPI captain,
            final int amount
    ) {
        int i = 0;
        int hullModsAdded = 0;
        while(i < variantData.getSmods().size() && i < amount) {
            if(attemptToAddHullMod(variant, variantData.getSmods().get(i), captain, true)) {
                hullModsAdded++;
            }
            i++;
        }
        return hullModsAdded;
    }

    protected static void addRandomSModsToVariant(
            final PersonAPI captain,
            final ShipVariantAPI variant,
            Random rand,
            final int numSMods
    ) {
        final ShipHullSpecAPI specs = variant.getHullSpec();

        // init some values to consider when adding hullmods
        final boolean lowCr = specs.getNoCRLossSeconds() < 200.0f || variant.hasHullMod(HullMods.SAFETYOVERRIDES);
        boolean reliesOnArmour = specs.getArmorRating() >= 350.0f;

        final ShipAPI.HullSize hullSize = specs.getHullSize();
        int minDpToConsiderSModding = 5;
        switch(hullSize) {
            case DESTROYER:
                minDpToConsiderSModding = 10;
                reliesOnArmour = specs.getArmorRating() >= 650.0f;
                break;
            case CRUISER:
                minDpToConsiderSModding = 15;
                reliesOnArmour = specs.getArmorRating() >= 1200.0f;
                break;
            case CAPITAL_SHIP:
                minDpToConsiderSModding = 20;
                reliesOnArmour = specs.getArmorRating() >= 1450.0f;
                break;
        }

        // first try building in any hullmods the ship may have
        int numHullModsAdded = 0;
        for(String hullMod : variant.getNonBuiltInHullmods()) {
            if(numHullModsAdded >= numSMods) {
                break;
            }

            final int hullModCost = Global.getSettings().getHullModSpec(hullMod).getCostFor(hullSize);
            if(hullModCost >= minDpToConsiderSModding && attemptToAddHullMod(variant, hullMod, captain, true)) {
                numHullModsAdded++;
            }
        }

        // some special cases based off hull
        if(numHullModsAdded < numSMods && lowCr && attemptToAddHullMod(variant, HullMods.HARDENED_SUBSYSTEMS, captain, true)) {
            numHullModsAdded++;
        }
        if(numHullModsAdded < numSMods && reliesOnArmour && attemptToAddHullMod(variant, HullMods.HEAVYARMOR, captain, true)) {
            numHullModsAdded++;
        }

        // fill with some expensive always applicable hullmods
        final int[] randomIndices = Util.createRandomNumberSequence(FALLBACK_HULLMODS.length, rand);
        int i = 0; // reused for adding hullmods to fill dp
        if(numHullModsAdded < numSMods) {
            while(i < FALLBACK_HULLMODS.length && numHullModsAdded < numSMods) {
                final String hullMod = FALLBACK_HULLMODS[randomIndices[i]];
                if(attemptToAddHullMod(variant, hullMod, captain, true)) {
                    numHullModsAdded++;
                }
                i++;
            }
        }

        // try to fill remaining dp
        while(variant.getUnusedOP(captain.getStats()) > 15 && i < randomIndices.length) {
            attemptToAddHullMod(variant, FALLBACK_HULLMODS[i], captain, false);
            i++;
        }
        final CoreAutofitPlugin autoFit = new CoreAutofitPlugin(captain);
        autoFit.addExtraVents(variant);
        autoFit.addExtraCaps(variant);
    }

    protected static boolean attemptToAddHullMod(
            final ShipVariantAPI variant,
            final String hullMod,
            final PersonAPI captain,
            final boolean smod
    ) {
        final HullModSpecAPI hullModSpecs = Global.getSettings().getHullModSpec(hullMod);
        if(smod) {
            if(hullModSpecs.hasTag(Tags.HULLMOD_NO_BUILD_IN) || variant.getSMods().contains(hullMod)) {
                return false;
            }
            if(variant.hasHullMod(hullMod)) {
                variant.removeMod(hullMod);
            }
            variant.addPermaMod(hullMod, true);
        } else {
            final int unusedDP = variant.getUnusedOP(captain.getStats());
            if(variant.hasHullMod(hullMod) || hullModSpecs.getCostFor(variant.getHullSize()) > unusedDP) {
                return false;
            }
            variant.addMod(hullMod);
        }
        return true;
    }

    private FleetBuildingUtils() {}
}
