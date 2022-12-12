package variants_lib.scripts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

public class HasHeavyIndustryTracker {
    private static final Logger log = Global.getLogger(variants_lib.scripts.HasHeavyIndustryTracker.class);
    static {
        log.setLevel(Level.ALL);
    }

    private static HashSet<String> HAS_HEAVY_INDUSTRY = new HashSet<>();

    // returns true if a faction has heavy industry, false otherwise
    public static boolean hasHeavyIndustry(String factionId) {
        return HAS_HEAVY_INDUSTRY.contains(factionId);
    }

    public static void refreshHasHeavyIndustry()
    {
        HAS_HEAVY_INDUSTRY.clear();

        for(FactionAPI faction : Global.getSector().getAllFactions()) {
            String factionId = faction.getId();
            for(MarketAPI market : Misc.getFactionMarkets(factionId)) {
                boolean hasHeavyIndustry = false;
                try {
                    hasHeavyIndustry = market.hasIndustry("heavyindustry") || market.hasIndustry("orbitalworks");
                } catch(Exception e) {
                    hasHeavyIndustry = false;
                }

                if(hasHeavyIndustry) {
                    HAS_HEAVY_INDUSTRY.add(factionId);
                }
            }
        }
    }

    public static void printEntries()
    {
        String out = "";
        for(String str : HAS_HEAVY_INDUSTRY) {
            out += str + " ";
        }
        log.debug(out);
    }
}
// runcode variants_lib.scripts.HasHeavyIndustryTracker.refreshHasHeavyIndustry();
// runcode variants_lib.scripts.HasHeavyIndustryTracker.printEntries();