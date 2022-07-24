package variants_lib.scripts;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

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
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
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
        FleetRandomizer.modify(fleet);
    }
}