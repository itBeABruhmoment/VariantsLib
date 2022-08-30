package variants_lib.scripts;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionIntel;

import variants_lib.data.SettingsData;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
            if(UnofficeredPersonalitySetPlugin.FACTION_DEFAULT_AGRESSION.containsKey(factionId)) {
                log.debug("resetting aggresion of " + factionId);
                faction.getDoctrine().setAggression(UnofficeredPersonalitySetPlugin.FACTION_DEFAULT_AGRESSION.get(factionId));
            }
        }
    }
    
    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        for(FleetEditingScript script : SettingsData.universalPreModificationScripts.values()) {
            script.run(fleet);
        }
        FleetRandomizer.modify(fleet);
        for(FleetEditingScript script : SettingsData.universalPostModificationScripts.values()) {
            script.run(fleet);
        }
    }
}
