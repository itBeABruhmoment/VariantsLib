package variants_lib.scripts;


import java.lang.reflect.Constructor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public class FleetEditingScriptTest implements FleetEditingScript{
    private static final Logger log = Global.getLogger(variants_lib.scripts.FleetEditingScriptTest.class);
    static {
        log.setLevel(Level.ALL);
    }

    public void run(CampaignFleetAPI fleet)
    {
        log.debug("this script ran :D");
    }
}
