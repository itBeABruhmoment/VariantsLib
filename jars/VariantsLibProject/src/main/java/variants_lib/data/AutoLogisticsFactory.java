package variants_lib.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Choose variant ids for the auto logistics feature
 */
public class AutoLogisticsFactory {
    protected static final Logger log = Global.getLogger(AutoLogisticsFactory.class);
    static {
        log.setLevel(Level.ALL);
    }

    /**
     * portion of dp dedicated to tankers
     */
    public float percentageTankers = 0.0f;
    /**
     * portion of dp dedicated to freighters
     */
    public float percentageFreighters = 0.0f;
    /**
     * portion of dp dedicated to liners
     */
    public float percentageLiners = 0.0f;
    /**
     * portion of dp dedicated to personnel
     */
    public float percentagePersonnel = 0.0f;
    /**
     * faction to base ship generation off of
     */
    public String faction = "independent";
    /**
     * target dp of entire fleet
     */
    public int fleetDP = 0;
    /**
     * max amount over the dp limit for a particular category of logistical ships can go over
     */
    public int maxOverBudget = 3;

    protected static final String[] FREIGHTER_CLASSES_IN_ORDER = {"freighterLarge", "freighterMedium", "freighterSmall"};
    protected static final String[] TANKER_CLASSES_IN_ORDER = {"tankerLarge", "tankerMedium", "tankerSmall"};
    protected static final String[] LINER_CLASSES_IN_ORDER = {"linerLarge", "linerMedium", "linerSmall"};
    protected static final String[] PERSONNEL_CLASSES_IN_ORDER = {"personnelLarge", "personnelMedium", "personnelSmall"};

    /**
     * Creates logistical ships based on fields
     * @return a AutoLogisticsReturn containing the logistical ships generated
     */
    public AutoLogisticsReturn createLogisticalShips() {
        final AutoLogisticsReturn returnParams = new AutoLogisticsReturn();
        final int freighterDp = Math.round(fleetDP * percentageFreighters);
        final int tankerDp = Math.round(fleetDP * percentageTankers);
        final int linerDp = Math.round(fleetDP * percentageLiners);
        final int personnelDp = Math.round(fleetDP * percentagePersonnel);
        final FactionAPI factionAPI = Global.getSector().getFaction(faction);

        if(freighterDp > 0) {
            returnParams.freighters = addShipType(FREIGHTER_CLASSES_IN_ORDER, factionAPI, freighterDp);
        }
        if(tankerDp > 0) {
            returnParams.tankers = addShipType(TANKER_CLASSES_IN_ORDER, factionAPI, tankerDp);
        }
        if(linerDp > 0) {
            returnParams.liners = addShipType(LINER_CLASSES_IN_ORDER, factionAPI, linerDp);
        }
        if(personnelDp > 0) {
            returnParams.personnel = addShipType(PERSONNEL_CLASSES_IN_ORDER, factionAPI, personnelDp);
        }

        return returnParams;
    }

    /**
     * Generates logistical ships of a specific role (ie tanker, freighter)
     * @param shipClassList FREIGHTER_CLASSES_IN_ORDER, TANKER_CLASSES_IN_ORDER, LINER_CLASSES_IN_ORDER, or PERSONNEL_CLASSES_IN_ORDER
     * @param faction faction to use ships from
     * @param availableDP amount of dp worth of ships that should be generated
     * @return ships generated
     */
    protected ArrayList<String> addShipType(String[] shipClassList, FactionAPI faction, int availableDP) {
        final ArrayList<String> addedShips = new ArrayList<>(10);
        final int dpAvailableOriginal = availableDP;
        int shipClass = 0;
        boolean continueLoop = true;
        while(continueLoop) {
            List<ShipRolePick> ship = faction.pickShip(shipClassList[shipClass], FactionAPI.ShipPickParams.priority());
            boolean shipPicked = ship != null && ship.size() > 0;
            boolean enoughDP = false;
            if(shipPicked) {
                String variantId = ship.get(0).variantId;
                enoughDP = availableDP - getDPInt(variantId) + maxOverBudget > 0;
                if(enoughDP) {
                    addedShips.add(variantId);
                    availableDP -= getDPInt(variantId);
                }
            }
            if(!enoughDP || !shipPicked) {
                shipClass++;
            }
            // if no dp is left or less than 20% of dp is unused coverage is sufficient
            boolean sufficientCoverage = availableDP <= 0 || ((float) availableDP) / dpAvailableOriginal < 0.2f;
            continueLoop = !sufficientCoverage && shipClass < shipClassList.length;
        }
        return addedShips;
    }

    protected int getDPInt(String variantId) {
        return Math.round(Global.getSettings().getVariant(variantId).getHullSpec().getSuppliesToRecover());
    }

    /**
     * Store variant id's of the ships selected
     */
    public static class AutoLogisticsReturn {
        public ArrayList<String> freighters = new ArrayList<>(10);
        public ArrayList<String> tankers = new ArrayList<>(10);
        public ArrayList<String> liners = new ArrayList<>(10);
        public ArrayList<String> personnel = new ArrayList<>(10);
    }
}
