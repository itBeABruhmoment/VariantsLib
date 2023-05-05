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
public class NoAutofitInflater extends DefaultFleetInflater
{
    public float averageSmods = 0.0f;
    public float quality = 0.0f;
    private static final Logger log = Global.getLogger(NoAutofitInflater.class);
    static {
        log.setLevel(Level.ALL);
    }

    private Random rand = new Random();

    NoAutofitInflater(DefaultFleetInflaterParams params) {
        super(params);
    }

    NoAutofitInflater(DefaultFleetInflater defaultInflator, Random Rand) {
        super((DefaultFleetInflaterParams)defaultInflator.getParams());
        rand = Rand;
    }

    @Override
    public void inflate(CampaignFleetAPI fleet)
    {
        FleetBuildingUtils.addDMods(fleet, rand, quality);
        FleetBuildingUtils.addSMods(fleet, rand, averageSmods);
    }


}
