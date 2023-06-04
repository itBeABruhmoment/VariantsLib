package variants_lib.scripts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import org.jetbrains.annotations.NotNull;
import variants_lib.data.VariantData;
import variants_lib.data.VariantsLibFleetParams;

import java.util.Collection;
import java.util.Random;

public class FleetBuildingUtils {
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

    public static void addSMods(CampaignFleetAPI fleet, Random rand, float averageSMods) {
        if(averageSMods <= 0.0f) {
            return;
        }
        for(FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation() && !ship.isCivilian()) {
                int numSmodsToAdd = (int) Math.round(averageSMods + (rand.nextFloat() - 0.5));
                if(numSmodsToAdd < 1) {
                    continue;
                }

                String variantId = VariantData.isRegisteredVariant(ship);
                VariantData.VariantDataMember variantData = null;
                if(variantId != null) {
                    variantData = VariantData.VARIANT_DATA.get(variantId);
                }

                if(variantData == null || variantData.getSmods().size() == 0) {
                    ShipVariantAPI variant  = ship.getVariant();
                    Collection<String> hullMods = variant.getNonBuiltInHullmods();
                    int start = rand.nextInt() & Integer.MAX_VALUE; // get positive int
                    start = start % FALLBACK_HULLMODS.length;
                    int numHullModsAdded = 0;
                    for(int i = 0; i < FALLBACK_HULLMODS.length && numHullModsAdded < numSmodsToAdd; i++) {
                        int index = (start + i) % FALLBACK_HULLMODS.length;
                        if(!hullMods.contains(FALLBACK_HULLMODS[index])) {
                            variant.addPermaMod(FALLBACK_HULLMODS[index], true);
                            numHullModsAdded++;
                        }
                    }
                } else {
                    ShipVariantAPI variant  = ship.getVariant();
                    Collection<String> hullMods = variant.getNonBuiltInHullmods();
                    int numSmodsAdded = 0;
                    while(numSmodsAdded < numSmodsToAdd && numSmodsAdded < variantData.getSmods().size()) {
                        if(!hullMods.contains(variantData.getSmods().get(numSmodsAdded))) {
                            variant.addPermaMod(variantData.getSmods().get(numSmodsAdded), true);
                            numSmodsAdded++;
                        }
                    }
                    // fill in remaining hullmods
                    hullMods = variant.getNonBuiltInHullmods();
                    int start = rand.nextInt() & Integer.MAX_VALUE; // get positive int
                    start = start % FALLBACK_HULLMODS.length;
                    for(int i = 0; i < FALLBACK_HULLMODS.length && numSmodsAdded < numSmodsToAdd; i++) {
                        int index = (start + i) % FALLBACK_HULLMODS.length;
                        if(!hullMods.contains(FALLBACK_HULLMODS[index])) {
                            variant.addPermaMod(FALLBACK_HULLMODS[index], true);
                            numSmodsAdded++;
                        }
                    }
                }
            }
        }
    }
    private FleetBuildingUtils() {}
}
