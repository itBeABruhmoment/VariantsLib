package variants_lib.scripts;

import java.util.List;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.HashMap;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import variants_lib.data.CommonStrings;
import variants_lib.data.FleetBuildData;
import variants_lib.data.SettingsData;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.campaign.FactionAPI;

/*
for(ShipAPI ship: Global.getCombatEngine().getShips()) {
    try{
        Console.showMessage(ship.getCaptain().getPersonalityAPI().getId() + ship.getHullSpec().getHullId());
    } catch(Exception e) {

    }
}


for(FleetMemberAPI ship: Global.getCombatEngine().getContext().getOtherFleet().getMembersWithFightersCopy()) {
    try{
        Console.showMessage(ship.getFleetData().getFleet().getFullName() + " " + ship.getHullId());
    } catch(Exception e) {

    }
}
*/

public class UnofficeredPersonalitySetPlugin implements EveryFrameCombatPlugin {
    private static final Logger log = Global.getLogger(variants_lib.scripts.UnofficeredPersonalitySetPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    public static HashMap<String, Integer> FACTION_DEFAULT_AGGRESSION;

    private static final HashMap<String, Integer> AGGRESSION = new HashMap<String, Integer>() {{
        put(Personalities.CAUTIOUS, 1);     put(Personalities.TIMID, 2);    put(Personalities.STEADY, 3);
        put(Personalities.AGGRESSIVE, 4);   put(Personalities.RECKLESS, 5);
    }};

    private static final String[] AGGRESSION_TO_PERSONALITY = {null, Personalities.CAUTIOUS, Personalities.TIMID,
            Personalities.STEADY, Personalities.AGGRESSIVE, Personalities.RECKLESS};
    /*
    static void test()
    {
        for(FleetMemberAPI memberAPI : Global.getCombatEngine().getContext().getOtherFleet().getMembersWithFightersCopy()) {
            try {
                Console.showMessage(memberAPI.getHullId() + " " + memberAPI.getCaptain().getPersonalityAPI().getId());
            } catch(Exception e) {

            }
        }
    }
    */

    @Nullable
    public static String getDefaultPersonality(@NotNull String faction) {
        final Integer aggression = FACTION_DEFAULT_AGGRESSION.get(faction);
        if(aggression != null && aggression > 0 && aggression <= 5) {
            return AGGRESSION_TO_PERSONALITY[aggression];
        }
        return null;
    }

    public static void innitDefaultAggressionValues()
    {
        FACTION_DEFAULT_AGGRESSION = new HashMap<String, Integer>();
        for(FactionAPI faction : Global.getSector().getAllFactions()) {
            FACTION_DEFAULT_AGGRESSION.put(faction.getId(), faction.getDoctrine().getAggression());
        }
    }

    @Override
    public void init(CombatEngineAPI combatEngine) {
        if(!SettingsData.personalitySetEnabled()) {
            return;
        }

        CampaignFleetAPI enemyFleet = null;
        String fleetWidePersonality = null;

        if(combatEngine == null) {
            log.debug(CommonStrings.MOD_ID + ": combat engine null");
            return;
        }
        if(Global.getCurrentState() == GameState.TITLE) {
            log.debug(CommonStrings.MOD_ID + ": title");
            return;
        }
        enemyFleet = combatEngine.getContext().getOtherFleet();
        if(enemyFleet == null) {
            log.debug(CommonStrings.MOD_ID + ": enemy fleet null(huh?)");
            return;
        }

        if(enemyFleet.getMemoryWithoutUpdate().contains(CommonStrings.FLEET_VARIANT_KEY)) {
            String fleetType = enemyFleet.getMemoryWithoutUpdate().getString(CommonStrings.FLEET_VARIANT_KEY);
            fleetWidePersonality = FleetBuildData.FLEET_DATA.get(fleetType).defaultFleetWidePersonality;
            log.debug(CommonStrings.MOD_ID + ": fleet is of type " + fleetType + " with personality " + fleetWidePersonality);
        } else {
            String factionId = enemyFleet.getFaction().getId();
            if(factionId != null && FACTION_DEFAULT_AGGRESSION.containsKey(factionId)) {
                enemyFleet.getFaction().getDoctrine().setAggression(FACTION_DEFAULT_AGGRESSION.get(factionId));
            }
            log.debug(CommonStrings.MOD_ID + ": fleet has no default personality, set to default");
            return;
        }
        if(fleetWidePersonality == null) {
            String factionId = enemyFleet.getFaction().getId();
            if(factionId != null && FACTION_DEFAULT_AGGRESSION.containsKey(factionId)) {
                enemyFleet.getFaction().getDoctrine().setAggression(FACTION_DEFAULT_AGGRESSION.get(factionId));
            }
            log.debug(CommonStrings.MOD_ID + ": fleet has no default personality, set to default");
            return;
        }

        if(AGGRESSION.containsKey(fleetWidePersonality)) {
            log.debug(CommonStrings.MOD_ID + ": setting aggression to \"" + fleetWidePersonality + "\"");
            int agressionValue = AGGRESSION.get(fleetWidePersonality);
            enemyFleet.getFaction().getDoctrine().setAggression(agressionValue);
        } else {
            log.debug(CommonStrings.MOD_ID + ": combat script not run, personality \"" + fleetWidePersonality + "\" not registered");
            return;
        }
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
    
}