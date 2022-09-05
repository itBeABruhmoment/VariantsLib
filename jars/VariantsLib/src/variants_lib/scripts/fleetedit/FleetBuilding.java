package variants_lib.scripts.fleetedit;

import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;

import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;

import variants_lib.data.AlwaysBuildMember;
import variants_lib.data.CommonStrings;
import variants_lib.data.FactionData;
import variants_lib.data.FleetBuildData;
import variants_lib.data.FleetComposition;
import variants_lib.data.FleetPartition;
import variants_lib.data.FleetPartitionMember;
import variants_lib.data.SettingsData;
import variants_lib.data.VariantData;
import variants_lib.data.FactionData.FactionConfig;
import variants_lib.data.VariantData.VariantDataMember;
import variants_lib.scripts.HasHeavyIndustryTracker;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.console.Console;

public class FleetBuilding {
    private static final String[] FREIGHTER_CLASSES_IN_ORDER = {"freighterLarge", "freighterMedium", "freighterSmall"};
    private static final String[] TANKER_CLASSES_IN_ORDER = {"tankerLarge", "tankerMedium", "tankerSmall"};
    private static final String[] LINER_CLASSES_IN_ORDER = {"linerLarge", "linerMedium", "linerSmall"};
    private static final String[] PERSONNEL_CLASSES_IN_ORDER = {"personnelLarge", "personnelMedium", "personnelSmall"};
    private static final int MAX_OVERBUDGET = 3;
    private static final Random RAND = new Random();
    private static final String[] FALLBACK_HULLMODS = {"hardenedshieldemitter", "fluxdistributor", 
    "fluxbreakers", "reinforcedhull", "targetingunit", "solar_shielding"};

    private static final Logger log = Global.getLogger(variants_lib.scripts.fleetedit.FleetBuilding.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static void FBLog(Object o) 
    {
        log.debug(o);
    }
    
    private static double getDPDouble(String variantId)
    {
        return Global.getSettings().getVariant(variantId).getHullSpec().getSuppliesToRecover();
    }

    private static autoLogiReturn addShipType(FleetInfo info, Vector<FleetMemberAPI> combatShips, Vector<FleetMemberAPI> civilianShips, 
    String[] shipClassList, FactionAPI faction, int dpAvailible, int numShips)
    {
        final int numShipsOriginal = numShips;
        final int dpAvailibleOriginal = dpAvailible;
        int shipClassOriginal = 0;
        int shipClass = shipClassOriginal;
        boolean continueLoop = true;
        while(continueLoop) {
            List<ShipRolePick> ship = faction.pickShip(shipClassList[shipClass], FactionAPI.ShipPickParams.priority());
            boolean shipPicked = ship != null && ship.size() > 0;
            boolean enoughDP = false;
            if(shipPicked) {
                String variantId = ship.get(0).variantId;
                enoughDP = dpAvailible - getDPInt(variantId) + MAX_OVERBUDGET > 0;
                if(enoughDP) {
                    FleetMemberAPI newMember = createShip(variantId, info);
                    if(newMember.isCivilian()) {
                        civilianShips.add(newMember);
                    } else {
                        combatShips.add(newMember);
                    }
                    int shipDp = getDPInt(variantId);
                    dpAvailible -= shipDp;
                    numShips++;
                }
            }
            if(!enoughDP || !shipPicked) {
                shipClass++;
            }
            // if no dp is left or less than 20% of dp is unused coverage is sufficient
            boolean sufficientCoverage = dpAvailible <= 0 || ((float) dpAvailible) / dpAvailibleOriginal < 0.2f;
            continueLoop = numShips < SettingsData.getMaxShipsInAIFleet() && !sufficientCoverage && shipClass < shipClassList.length;
        }
        return new autoLogiReturn(numShips - numShipsOriginal, dpAvailibleOriginal - dpAvailible);
    }

    private static autoLogiReturn applyAutoLogistics(FleetInfo info, FleetComposition fleetCompData, 
    Vector<FleetMemberAPI> combatShips, Vector<FleetMemberAPI> civilianShips) {
        int freighterDp = Math.round(info.originalDP * fleetCompData.freighterDp);
        int tankerDp = Math.round(info.originalDP * fleetCompData.tankerDp);
        int linerDp = Math.round(info.originalDP * fleetCompData.linerDp);
        int personnelDp = Math.round(info.originalDP * fleetCompData.personnelDp);
        final int numShipsOriginal = combatShips.size() + civilianShips.size();
        int numShips = numShipsOriginal;
        int dpShipsAdded = 0;
        FactionAPI faction = info.faction;
        if(info.faction == null) {
            faction = Global.getSector().getFaction("independent");
        }

        if(freighterDp > 0) {
            autoLogiReturn returnInfo = addShipType(info, combatShips, civilianShips, FREIGHTER_CLASSES_IN_ORDER, faction, freighterDp, numShips);
            numShips += returnInfo.shipsAdded;
            dpShipsAdded += returnInfo.dpShipsAdded;
        }
        if(tankerDp > 0) {
            autoLogiReturn returnInfo = addShipType(info, combatShips, civilianShips, TANKER_CLASSES_IN_ORDER, faction, tankerDp, numShips);
            numShips += returnInfo.shipsAdded;
            dpShipsAdded += returnInfo.dpShipsAdded;
        }
        if(linerDp > 0) {
            autoLogiReturn returnInfo = addShipType(info, combatShips, civilianShips, LINER_CLASSES_IN_ORDER, faction, linerDp, numShips);
            numShips += returnInfo.shipsAdded;
            dpShipsAdded += returnInfo.dpShipsAdded;
        }
        if(personnelDp > 0) {
            autoLogiReturn returnInfo = addShipType(info, combatShips, civilianShips, PERSONNEL_CLASSES_IN_ORDER, faction, personnelDp, numShips);
            numShips += returnInfo.shipsAdded;
            dpShipsAdded += returnInfo.dpShipsAdded;
        }

        return new autoLogiReturn(numShips - numShipsOriginal, dpShipsAdded);
    }

    private static class autoLogiReturn {
        int shipsAdded;
        int dpShipsAdded;

        autoLogiReturn(int ShipsAdded, int DpShipsAdded) {
            shipsAdded = ShipsAdded;
            dpShipsAdded = DpShipsAdded;
        }
    }

    private static int getDPInt(String variantId)
    {
        return Math.round(Global.getSettings().getVariant(variantId).getHullSpec().getSuppliesToRecover());
    }

    private static Vector<FleetPartitionMember> getPickableVariants(FleetPartition partition, int DPLimit)
    {
        Vector<FleetPartitionMember> members = new Vector<FleetPartitionMember>(10);
        for(FleetPartitionMember member : partition.members) {
            if(getDPInt(member.id) <= DPLimit) {
                members.add(member);
            }
        }
        return members;
    }

    private static double sumWeights(Vector<FleetPartitionMember> members)
    {
        double sum = 0;
        for(FleetPartitionMember member : members) {
            sum += member.weight;
        }
        return sum;
    }

    private static String pickVariant(FleetPartition partition, int DPLimit, Random rand)
    {
        Vector<FleetPartitionMember> pickableVariants = getPickableVariants(partition, DPLimit);
        if(pickableVariants.size() == 0) { // no elligible varaints because not enough dp to spawn them
            return null;
        }

        double random = rand.nextDouble();
        double totalWeightsSum = sumWeights(pickableVariants);
        double runningWeightsSum = 0;
        for(FleetPartitionMember member : pickableVariants) {
            runningWeightsSum += member.weight / totalWeightsSum;
            if(runningWeightsSum >= random) {
                return member.id;
            }
        }
        return pickableVariants.get(pickableVariants.size() - 1).id; // handle possible edge case
    }

    private static FleetMemberAPI createShip(String variantId, FleetInfo info)
    {
        return Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
    }

    private static double sumPartitionWeights(int start, FleetComposition fleetCompData)
    {
        double sum = 0;
        for(int i = start; i < fleetCompData.partitions.length; i++) {
            sum += fleetCompData.partitions[i].partitionWeight;
        }
        return sum;
    }

    private static void createFleet(CampaignFleetAPI fleetAPI, FleetInfo info, FleetComposition fleetCompData, Random rand)
    {
        Vector<FleetMemberAPI> combatShips = new Vector<FleetMemberAPI>(30);
        Vector<FleetMemberAPI> civilianShips = new Vector<FleetMemberAPI>(10);
        int maxShipsThatCanBeAdded = SettingsData.getMaxShipsInAIFleet() - info.mothballedShips.size();
        int totalDPRemaining = info.originalDP;

        if(fleetCompData.alwaysInclude != null) {
            for(AlwaysBuildMember member : fleetCompData.alwaysInclude) {
                for(int i = 0; i < member.amount; i++) {
                    FleetMemberAPI newMember = createShip(member.id, info);
                    if(newMember.isCivilian()) {
                        civilianShips.add(newMember);
                    } else {
                        combatShips.add(newMember);
                    }
                    totalDPRemaining -= getDPInt(member.id);
                    maxShipsThatCanBeAdded--;
                }
            }
        }

        // auto logistics
        autoLogiReturn addedShipsInfo = applyAutoLogistics(info, fleetCompData, combatShips, civilianShips);
        maxShipsThatCanBeAdded -= addedShipsInfo.shipsAdded;
        totalDPRemaining -= addedShipsInfo.dpShipsAdded;

        for(int i = 0; i < fleetCompData.partitions.length; i++) {
            // calculate dp for partition
            int remainingDpThisPartition = ((int)Math.round(totalDPRemaining * 
            (fleetCompData.partitions[i].partitionWeight / sumPartitionWeights(i, fleetCompData))));
            if(remainingDpThisPartition > fleetCompData.partitions[i].maxDPForPartition) {
                remainingDpThisPartition = fleetCompData.partitions[i].maxDPForPartition;
            }

            int shipsRemainingThisPartition = fleetCompData.partitions[i].maxShipsForPartition;
            while(remainingDpThisPartition > 0 && maxShipsThatCanBeAdded > 0 && shipsRemainingThisPartition > 0) {
                String variantId = pickVariant(fleetCompData.partitions[i], remainingDpThisPartition + MAX_OVERBUDGET, rand);
                if(variantId == null) { // no more variants can be spawned with the dp
                    break;
                }

                FleetMemberAPI newMember = createShip(variantId, info);
                if(newMember.isCivilian()) {
                    civilianShips.add(newMember);
                } else {
                    combatShips.add(newMember);
                }

                int DPofVariant = getDPInt(variantId);
                remainingDpThisPartition -= DPofVariant;
                totalDPRemaining -= DPofVariant;
                maxShipsThatCanBeAdded--;
                shipsRemainingThisPartition--;
            }
        }

        // assign officers
        if(combatShips.size() == 0) { // if there is only civilian ships for some reason
            Collections.shuffle(civilianShips);
            for(int i = 0; i < civilianShips.size() && i < info.officers.size(); i++) {
                civilianShips.get(i).setCaptain(info.officers.get(i));
            }

            // ensure flagship is set
            if(civilianShips.size() < info.officers.size()) {
                civilianShips.get(0).setCaptain(info.captain);
            }

        } else {
            // assign officers
            Collections.shuffle(combatShips);
            int flagShipIndex = -1;
            for(int i = 0; i < combatShips.size() && i < info.officers.size(); i++) {
                if(info.officers.get(i) == info.captain) {
                    flagShipIndex = i;
                }
                combatShips.get(i).setCaptain(info.officers.get(i));
            }

            // find highest dp ship
            int highestDP = getDPInt(combatShips.get(0).getVariant().getHullVariantId());
            int highestDPIndex = 0;
            for(int i = 1; i < combatShips.size(); i++) {
                int dp = getDPInt(combatShips.get(i).getVariant().getHullVariantId());
                if(highestDP < dp) {
                    highestDP = dp;
                    highestDPIndex = i;
                }
            }

            // set flagship to highest dp ship
            if(flagShipIndex == -1) {
                combatShips.get(highestDPIndex).setCaptain(info.captain);
            } else {
                PersonAPI temp = combatShips.get(highestDPIndex).getCaptain();
                combatShips.get(highestDPIndex).setCaptain(info.captain);
                combatShips.get(flagShipIndex).setCaptain(temp);
            }
        }

        Collections.sort(combatShips, new SortByDP());
        Collections.sort(civilianShips, new SortByDP());

        // add ships to fleet
        for(FleetMemberAPI member : combatShips) {
            //RepairTrackerAPI repairTracker = member.getRepairTracker();
            //repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(FleetMemberAPI member : civilianShips) {
            //RepairTrackerAPI repairTracker = member.getRepairTracker();
            //repairTracker.setCR(0.7f);
            fleetAPI.getFleetData().addFleetMember(member);
        }
        for(FleetMemberAPI member : info.mothballedShips)
        {
            fleetAPI.getFleetData().addFleetMember(member);
        }
    }

    // gather info relevent for generating new fleet
    private static FleetInfo getInfo(CampaignFleetAPI fleetAPI) 
    {
        PersonAPI captain = fleetAPI.getCommander();
        Vector<PersonAPI> officers = new Vector<PersonAPI>(15);
        float totalDp = 0;
        Vector<FleetMemberAPI> mothballedShips = new Vector<FleetMemberAPI>(5);
        int numShips = 0;
        boolean isStationFleet = false;


        // count dp, count dmods and save officers
        List<FleetMemberAPI> members = fleetAPI.getMembersWithFightersCopy();
        for(FleetMemberAPI member : members) {
            if(member.isStation()) {
                isStationFleet = true;
            }

            if(member.isMothballed()) {
                mothballedShips.add(member);
            } else {
                officers.add(member.getCaptain());
                totalDp += member.getBaseDeploymentCostSupplies();
                numShips++;
            }
        }

        return new FleetInfo(captain, officers, Math.round(totalDp), mothballedShips, isStationFleet, fleetAPI.getFaction());
    }

    // delete all members in fleet
    private static void clearMembers(CampaignFleetAPI fleetAPI) 
    {
        FleetDataAPI fleetData = fleetAPI.getFleetData();
        List<FleetMemberAPI> members = fleetAPI.getMembersWithFightersCopy();
        for(FleetMemberAPI member : members) {
            fleetData.removeFleetMember(member);
        }
    }

    private static class SortByDP implements Comparator<FleetMemberAPI>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(FleetMemberAPI a, FleetMemberAPI b)
        {
            if(b.getVariant().getHullVariantId() == null || a.getVariant().getHullVariantId() == null) {
                log.debug("no variant found rippy dippy");
            }
            return getDPInt(b.getVariant().getHullVariantId()) - getDPInt(a.getVariant().getHullVariantId());
        }
    }

    private static Vector<FleetComposition> getValidFleetChoices(FleetInfo info, String factionId, String fleetType)
    {
        Vector<FleetComposition> fleetComps = new Vector<FleetComposition>(5);
        for(String compId : FactionData.FACTION_DATA.get(factionId).customFleetIds) {
            FleetComposition comp = FleetBuildData.FLEET_DATA.get(compId);
            if(comp != null 
            && comp.maxDP >= info.originalDP 
            && info.originalDP >= comp.minDP 
            && comp.spawnWeights.containsKey(fleetType)
            && (comp.spawnIfNoIndustry || HasHeavyIndustryTracker.hasHeavyIndustry(factionId))) {
                fleetComps.add(comp);
            }
        }
        return fleetComps;
    }

    private static FleetComposition pickFleet(FleetInfo info, String factionId, String fleetType, Random rand)
    {
        if(!FactionData.FACTION_DATA.containsKey(factionId)) {
            log.debug(factionId + " not registered");
            return null;
        }

        // get correct special fleet spawn rate
        double specialFleetSpawnRate = 0.0;
        FactionConfig config = FactionData.FACTION_DATA.get(factionId);
        if(config.specialFleetSpawnRateOverrides.containsKey(fleetType)) {
            specialFleetSpawnRate = config.specialFleetSpawnRateOverrides.get(fleetType);
        } else {
            specialFleetSpawnRate = config.specialFleetSpawnRate;
        }

        if(specialFleetSpawnRate < rand.nextDouble()) {
            return null;
        }

        Vector<FleetComposition> validFleetComps = getValidFleetChoices(info, factionId, fleetType);
        //String info1 = "";
        //for(FleetComposition comp : validFleetComps) {
        //    info1 += comp.id + " " + comp.spawnWeight + ", ";
        //}
        //log.debug(info1);

        if(validFleetComps.size() == 0) {
            return null;
        }

        float random = rand.nextFloat();
        float totalWeightsSum = 0;
        for(FleetComposition comp : validFleetComps) {
            totalWeightsSum += comp.spawnWeights.get(fleetType);
        }
        //log.debug("rand: " + random + " weightSum: " + totalWeightsSum);
        float runningWeightsSum = 0;
        for(FleetComposition comp : validFleetComps) {
            //log.debug("add: " + comp.spawnWeight / totalWeightsSum);
            runningWeightsSum += comp.spawnWeights.get(fleetType) / totalWeightsSum;
            if(runningWeightsSum > random) {
                return comp;
            }
        }
        return null;
    }

    public static void addSmods(CampaignFleetAPI fleet, float averageSmods, Random rand)
    {
        if(averageSmods < 0.01) {
            return;
        }
        for(FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if(!ship.isFighterWing() && !ship.isStation() && !ship.isCivilian()) {
                int numSmodsToAdd = (int) Math.round(averageSmods + (rand.nextFloat() - 0.5));
                if(numSmodsToAdd < 1) {
                    continue;
                }

                String variantId = VariantData.isRegisteredVariant(ship);
                VariantDataMember variantData = null;
                if(variantId != null) {
                    variantData = VariantData.VARIANT_DATA.get(variantId);
                }
                
                if(variantData == null || variantData.smods.size() == 0) {
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
                    while(numSmodsAdded < numSmodsToAdd && numSmodsAdded < variantData.smods.size()) {
                        if(!hullMods.contains(variantData.smods.get(numSmodsAdded))) {
                            variant.addPermaMod(variantData.smods.get(numSmodsAdded), true);
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

    public static void addDmods(CampaignFleetAPI fleet, float quality, Random rand)
    {
        // add dmods
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

    public static String editFleet(CampaignFleetAPI fleetAPI, Random rand) 
    {
        log.debug("editing " + fleetAPI.getFullName());
        MemoryAPI fleetMemory = fleetAPI.getMemoryWithoutUpdate();
        String factionId = fleetAPI.getFaction().getId();
        String fleetType = fleetMemory.getString(MemFlags.MEMORY_KEY_FLEET_TYPE);

        FleetInfo info = getInfo(fleetAPI);
        FleetComposition compInfo = pickFleet(info, factionId, fleetType, rand);
        if(compInfo == null) {
            log.debug("fleet not edited");
            return null;
        }

        if(info.originalDP < compInfo.setDPToAtLeast) {
            info.originalDP = compInfo.setDPToAtLeast;
        }

        if(fleetType == null) {
            log.debug("edit failed, fleet has no fleet type");
        }

        //log.debug(info.toString());

        if(info.isStationFleet) {
            log.debug("edit failed, station");
            return null;
        }

        log.debug("changing to " + compInfo.id);
        clearMembers(fleetAPI);
        createFleet(fleetAPI, info, compInfo, rand);

        return compInfo.id;
    }

    // fleetType is the String that is mapped to the fleet's "$fleetType" memkey
    public static String editFleet(CampaignFleetAPI fleetAPI, String fleetVariantId, double averageSmods) 
    {
        log.debug("editing " + fleetAPI.getFullName());

        FleetInfo info = getInfo(fleetAPI);

        if(info.isStationFleet) {
            log.debug("edit failed, station");
            return null;
        }

        FleetComposition compInfo = FleetBuildData.FLEET_DATA.get(fleetVariantId);
        if(compInfo == null) {
            log.debug("fleet not edited");
            return null;
        }

        log.debug("changing to " + compInfo.id);
        clearMembers(fleetAPI);
        createFleet(fleetAPI, info, compInfo, RAND);

        return compInfo.id;
    }

    public static void setProperCr(CampaignFleetAPI fleetAPI) {
        for(FleetMemberAPI memberAPI : fleetAPI.getMembersWithFightersCopy()) {
            RepairTrackerAPI repairs = memberAPI.getRepairTracker();
            repairs.setCR(Math.max(repairs.getCR(), repairs.getMaxCR()));
        }
    }

    // store important info on fleet before editing
    private static class FleetInfo 
    {
        public PersonAPI captain;
        public Vector<PersonAPI> officers;
        public int originalDP;
        public Vector<FleetMemberAPI> mothballedShips;
        public boolean isStationFleet;
        public FactionAPI faction;

        public FleetInfo(PersonAPI Captain, Vector<PersonAPI> Officers, int TotalDp, 
        Vector<FleetMemberAPI> MothballedShips, boolean IsStationFleet, FactionAPI Faction)
        {
            captain = Captain;
            officers = Officers;
            originalDP = TotalDp;
            mothballedShips = MothballedShips;
            isStationFleet = IsStationFleet;
            faction = Faction;
        }

        @Override
        public String toString()
        {
            return "cap: " + captain + " officers: " + officers + " DP: " + originalDP;
        }
    }

    public static class VariantsLibFleetParams 
    {
        String fleetName;
        String faction;
        String fleetType; // String to store under the memkey $fleetType
        String fleetDataId;
        int fleetPoints;
        float quality; // 0.0f being max dmods, 1.0f being the least
        float averageSmods;
        int numOfficers;
        float averageOfficerLevel;
        PersonAPI commander;
        boolean enableAutofit;
        boolean enableOfficerEditing;
        boolean runAssociatedScripts; // whether to run the fleets post modification scripts

        // contructor with all the fields as arguments
        public VariantsLibFleetParams(String FleetName, String Faction, String FleetType, String FleetDataId, int FleetPoints, float Quality, float AverageSmods,
            int NumOfficers, float AverageOfficerLevel, PersonAPI Commander, boolean EnableAutofit, boolean EnableOfficerEditing, 
            boolean RunAssociatedScripts) {
            fleetName = FleetName;
            faction = Faction;
            fleetType = FleetType;
            fleetDataId = FleetDataId;
            fleetPoints = FleetPoints;
            quality = Quality;
            averageSmods = AverageSmods;
            numOfficers = NumOfficers;
            averageOfficerLevel = AverageOfficerLevel;
            commander = Commander;
            enableAutofit = EnableAutofit;
            enableOfficerEditing = EnableOfficerEditing;
            runAssociatedScripts = RunAssociatedScripts;
        }

        // constructor that auto generates a officer
        public VariantsLibFleetParams(String FleetName, String Faction, String FleetType, String FleetDataId, int FleetPoints, float Quality, float AverageSmods,
            int NumOfficers, float AverageOfficerLevel, boolean EnableAutofit, boolean EnableOfficerEditing, boolean RunAssociatedScripts) {
            fleetName = FleetName;
            faction = Faction;
            fleetType = FleetType;
            fleetDataId = FleetDataId;
            fleetPoints = FleetPoints;
            quality = Quality;
            averageSmods = AverageSmods;
            numOfficers = NumOfficers;
            averageOfficerLevel = AverageOfficerLevel;
            enableAutofit = EnableAutofit;
            enableOfficerEditing = EnableOfficerEditing;
            runAssociatedScripts = RunAssociatedScripts;

            commander = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(faction), Math.round(averageOfficerLevel));
        }
    }

    // Create a fleet from a fleet json with a FleetBuilding.VariantsLibFleetParams object. Sets the memkey CommonStrings.FLEET_EDITED_MEMKEY
    // and CommonStrings.FLEET_VARIANT_KEY
    public static CampaignFleetAPI createFleet(VariantsLibFleetParams params)
    {
        int numOfficers = params.numOfficers;

        float averageOfficerLevel = params.averageOfficerLevel;
        if(averageOfficerLevel < 1.0f) {
            averageOfficerLevel = 1.0f;
        } else if(averageOfficerLevel > 10.0f) {
            averageOfficerLevel = 10.0f;
        }

        FactionAPI faction = Global.getSector().getFaction(params.faction);

        Vector<PersonAPI> officers = new Vector<>();
        for(int i = 0; i < numOfficers; i++) {
            int level = Math.round(averageOfficerLevel + (RAND.nextFloat() - 0.5f));
            if(level < 1) {
                level = 1;
            } else if(level > 10) {
                level = 10;
            }
            officers.add(OfficerManagerEvent.createOfficer(faction, level));
        }

        FleetInfo buildData = new FleetInfo(params.commander, officers, params.fleetPoints, 
            new Vector<FleetMemberAPI>(), false, Global.getSector().getFaction(params.faction));
    
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(params.faction, params.fleetName, true);
        fleet.setCommander(params.commander);
        clearMembers(fleet);

        FleetComposition comp = FleetBuildData.FLEET_DATA.get(params.fleetDataId);
        createFleet(fleet, buildData, comp, RAND);
        addSmods(fleet, params.averageSmods, RAND);

        log.debug("1:" + fleet.getMembersWithFightersCopy().size());

        if(params.enableAutofit) {
            DefaultFleetInflaterParams inflaterParams = new DefaultFleetInflaterParams();
            inflaterParams.allWeapons = false;
            inflaterParams.averageSMods = 0;
            inflaterParams.factionId = params.faction;
            inflaterParams.mode = ShipPickMode.ALL;
            inflaterParams.quality = params.quality;
            inflaterParams.rProb = 1.0f - params.quality;
            inflaterParams.seed = RAND.nextLong();
            inflaterParams.persistent = false;

            DefaultFleetInflater inflater = new DefaultFleetInflater(inflaterParams);
            inflater.inflate(fleet);
            fleet.setInflated(true);
        } else {
            addDmods(fleet, params.quality, RAND);
            fleet.setInflated(true);
        }
        if(params.enableOfficerEditing) {
            OfficerEditing.editAllOfficers(fleet, params.fleetDataId);
        }
        setProperCr(fleet);

        // run any post modification scripts
        if(params.runAssociatedScripts && comp.postModificationScripts != null) {
            for(String scriptPath : comp.postModificationScripts) {
                FleetBuildData.SCRIPTS.get(scriptPath).run(fleet);
            }
        }

        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(CommonStrings.FLEET_EDITED_MEMKEY, true);
        fleetMemory.set(CommonStrings.FLEET_VARIANT_KEY, params.fleetDataId);
        fleetMemory.set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);
        
        log.debug("2:" + fleet.getMembersWithFightersCopy().size());
        return fleet;
    }

    private FleetBuilding() {} // do nothing
}