package variants_lib.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Random;

public class VariantsLibFleetInflater extends DefaultFleetInflater {
    public float averageSmods = 0.0f;
    public float quality = 1.0f;
    private static final Logger log = Global.getLogger(NoAutofitInflater.class);
    static {
        log.setLevel(Level.ALL);
    }

    VariantsLibFleetInflater(DefaultFleetInflaterParams params) {
        super(params);
    }

    VariantsLibFleetInflater(DefaultFleetInflaterParams params, float quality, float averageSmods) {
        super(params);
        this.quality = quality;
        this.averageSmods = averageSmods;
    }

    @Override
    public void inflate(CampaignFleetAPI fleet)
    {
        final Random rand = new Random(getSeed());
        FleetBuildingUtils.addDMods(fleet, rand, quality);
        FleetBuildingUtils.addSMods(fleet, rand, averageSmods);
    }
}
