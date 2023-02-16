package variants_lib.scripts;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import variants_lib.data.SettingsData;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.console.CommonStrings;

public class VariantsLibListener extends BaseCampaignEventListener{
    private static final Logger log = Global.getLogger(variants_lib.scripts.VariantsLibListener.class);
    static {
        log.setLevel(Level.ALL);
    }

    public VariantsLibListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportEconomyMonthEnd()
    {
        log.debug(CommonStrings.MOD_ID + ": refreshing heavy industry tracker data");
        HasHeavyIndustryTracker.refreshHasHeavyIndustry();
        HasHeavyIndustryTracker.printEntries();
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
        if(!SettingsData.personalitySetEnabled()) {
            return;
        }

        log.debug("resetting faction aggressions");
        BattleAPI battle = result.getBattle();
        for(CampaignFleetAPI fleet : battle.getNonPlayerSide()) {
            FactionAPI faction = fleet.getFaction();
            String factionId = faction.getId();
            if(UnofficeredPersonalitySetPlugin.FACTION_DEFAULT_AGGRESSION.containsKey(factionId)) {
                log.debug("resetting aggresion of " + factionId);
                faction.getDoctrine().setAggression(UnofficeredPersonalitySetPlugin.FACTION_DEFAULT_AGGRESSION.get(factionId));
            }
        }
    }
    
    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        try {
            for(FleetEditingScript script : SettingsData.universalPreModificationScripts.values()) {
                script.run(fleet);
            }
            FleetRandomizer.modify(fleet);
            for(FleetEditingScript script : SettingsData.universalPostModificationScripts.values()) {
                script.run(fleet);
            }
        } catch (Exception e) {
            log.info(variants_lib.data.CommonStrings.MOD_ID + ": error when attempting to modify fleet, if" +
                    " you're seeing this you should probably send your logs to the creator of variants lib, " +
                    "attempting to despawn affected fleet");
            log.info(e.toString());
            fleet.getFleetData().clear();
            fleet.setCommander(Global.getFactory().createPerson());
            fleet.despawn();
        }
    }
}
