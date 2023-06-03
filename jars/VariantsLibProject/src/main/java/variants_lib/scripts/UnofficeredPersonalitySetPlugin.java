package variants_lib.scripts;

import java.util.List;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener;
import com.fs.starfarer.api.input.InputEventAPI;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.mission.FleetSide;
import variants_lib.data.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class UnofficeredPersonalitySetPlugin implements EveryFrameCombatPlugin, FleetMemberDeploymentListener {
    private static final Logger log = Global.getLogger(variants_lib.scripts.UnofficeredPersonalitySetPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    public String aggression = Personalities.STEADY;

    private static final String[] AGGRESSION_TO_PERSONALITY = {null, Personalities.CAUTIOUS, Personalities.TIMID,
            Personalities.STEADY, Personalities.AGGRESSIVE, Personalities.RECKLESS};

    /**
     * The correct default fleet-wide aggression, or null if it is not set
     * @return the correct default fleet-wide aggression, or null if it is not set
     */
    private String getAppropriateAggression(CampaignFleetAPI fleet) {
        final String fleetType = fleet.getMemoryWithoutUpdate().getString(CommonStrings.FLEET_VARIANT_KEY);
        if(fleetType == null) {
            log.debug(CommonStrings.MOD_ID + ": not a variants lib created fleet");
            return null;
        }

        final VariantsLibFleetFactory fleetFactory = FleetBuildData.FLEET_DATA.get(fleetType);
        if(fleetFactory == null) {
            log.debug(CommonStrings.MOD_ID + ": fleet factory could not be found");
            return null;
        }

        if(!fleetFactory.defaultFleetWidePersonalitySet) {
            log.debug(CommonStrings.MOD_ID + ": defaultFleetWidePersonalitySet false, using default aggression values");
            return null;
        }
        return fleetFactory.defaultFleetWidePersonality;
    }

    private boolean shouldEditMember(final DeployedFleetMemberAPI memberAPI) {
        final PersonAPI person = memberAPI.getMember().getCaptain();
        final boolean hasOfficer = Util.isOfficer(person);
        return !(memberAPI.isAlly()
                || hasOfficer
                || memberAPI.isStation()
                || memberAPI.isStationModule()
                || memberAPI.isFighterWing());
    }

    private void setAI(final DeployedFleetMemberAPI memberAPI, String aggression) {
        final ShipAPI ship = memberAPI.getShip();
        final ShipAIConfig aiConf = new ShipAIConfig();
        aiConf.personalityOverride = aggression;
        final ShipAIPlugin aiPlugin = Global.getSettings().createDefaultShipAI(ship, aiConf);
        ship.setShipAI(aiPlugin);
        ship.getShipAI().forceCircumstanceEvaluation();
    }

    @Override
    public void init(CombatEngineAPI combatEngine) {
        if(!SettingsData.personalitySetEnabled()) {
            return;
        }

        if(combatEngine == null) {
            log.debug(CommonStrings.MOD_ID + ": combat engine null");
            return;
        }
        if(Global.getCurrentState() == GameState.TITLE) {
            log.debug(CommonStrings.MOD_ID + ": title");
            return;
        }
        if(combatEngine.getListenerManager().hasListenerOfClass(UnofficeredPersonalitySetPlugin.class)) {
            log.debug(CommonStrings.MOD_ID + ": already a listener");
            return;
        }
        final CampaignFleetAPI enemyFleet = combatEngine.getContext().getOtherFleet();
        if(enemyFleet == null) {
            log.debug(CommonStrings.MOD_ID + ": enemy fleet null(huh?)");
            return;
        }

        aggression = getAppropriateAggression(enemyFleet);
        if(aggression == null) {
            return;
        }

        log.debug("setting personalities");
        final List<DeployedFleetMemberAPI> deployedMembers = combatEngine.getFleetManager(FleetSide.ENEMY).getAllEverDeployedCopy();
        for(final DeployedFleetMemberAPI deployed : deployedMembers) {
            try {
                if(shouldEditMember(deployed)) {
                    setAI(deployed, aggression);
                }
            } catch (Exception e) {
                log.debug("failed to set a personality");
                log.debug(e);
            }
        }

        combatEngine.getListenerManager().addListener(this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> arg1) {
        // do nothing
    }

    @Override
    public void processInputPreCoreControls(float arg0, List<InputEventAPI> arg1) {
        // do nothing
    }

    @Override
    public void renderInUICoords(ViewportAPI arg0) {
        // do nothing 
    }

    @Override
    public void renderInWorldCoords(ViewportAPI arg0) {
        // do nothing
    }

    @Override
    public void reportFleetMemberDeployed(DeployedFleetMemberAPI deployedFleetMemberAPI) {
        if(shouldEditMember(deployedFleetMemberAPI)) {
            log.debug("set ai");
            setAI(deployedFleetMemberAPI, aggression);
        }
    }
}
/*
for(FleetMemberAPI memberAPI : Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedCopy()) {
            Console.showMessage(memberAPI.getFleetData().getFleet());
        }
        for(FleetMemberAPI memberAPI : Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getReservesCopy()) {
            Console.showMessage(memberAPI.getFleetData().getFleet());
        }
        for(FleetMemberAPI memberAPI : Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedCopy()) {
            Console.showMessage(memberAPI.getFleetData().getFleet());
        }
        for(FleetMemberAPI memberAPI : Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getReservesCopy()) {
            Console.showMessage(memberAPI.getFleetData().getFleet());
        }
 */