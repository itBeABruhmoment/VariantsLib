package variants_lib.scripts;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;

import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class NoAutofitFleetInflater extends DefaultFleetInflater 
{
    private static final Logger log = Global.getLogger(variants_lib.scripts.NoAutofitFleetInflater.class);
    static {
        log.setLevel(Level.ALL);
    }

    private Random rand;

    NoAutofitFleetInflater(DefaultFleetInflaterParams params) {
        super(params);
    }

    NoAutofitFleetInflater(DefaultFleetInflater defaultInflator, Random Rand) {
        super((DefaultFleetInflaterParams)defaultInflator.getParams());
        rand = Rand;
    }

    @Override
    public void inflate(CampaignFleetAPI fleet)
    {
        float quality = getQuality();
        quality = quality + (0.05f * quality); // noticed an abnormal amount dmods in factions such as diktat
        for(FleetMemberAPI memberAPI : fleet.getMembersWithFightersCopy()) {
            if(!memberAPI.isFighterWing() && !memberAPI.isMothballed() && !memberAPI.isStation()) {
                int numExistingDmods = DModManager.getNumDMods(memberAPI.getVariant());
                if(quality <= 0.0f) {
                    int numDmods = 5 - numExistingDmods;
                    if(numDmods > 0) {
                        DModManager.addDMods(memberAPI, true, numDmods, rand);
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
                        DModManager.addDMods(memberAPI, true, numDmods, rand);
                    }
                } // otherwise apply no dmods
            }
        }
    }
    
    
}
