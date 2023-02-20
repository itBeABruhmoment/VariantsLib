package variants_lib.test;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import variants_lib.data.CommonStrings;
import variants_lib.data.VariantsLibFleetFactory;
import variants_lib.data.VariantsLibFleetParams;

public class TestFleetFactory extends VariantsLibFleetFactory {
    protected static final Logger log = Global.getLogger(TestFleetFactory.class);
    static {
        log.setLevel(Level.ALL);
    }

    public TestFleetFactory() {
        super();
    }

    /**
     * Create a VariantsLibFleetFactory
     *
     * @param fleetJson       The fleet json
     * @param fleetJsonCsvRow The fleet json's row in fleets.csv
     * @param modOfOrigin     The mod the fleet json is from
     * @throws Exception An exception containing some message on fields that are set to invalid values or failed to load
     */
    public TestFleetFactory(@NotNull JSONObject fleetJson, @NotNull JSONObject fleetJsonCsvRow, @NotNull String modOfOrigin) throws Exception {
        super(fleetJson, fleetJsonCsvRow, modOfOrigin);
    }

    @Override
    public CampaignFleetAPI createFleet(@NotNull VariantsLibFleetParams params) {
        log.debug(CommonStrings.MOD_ID + ": test that custom fleet factories are working as intended");
        return super.createFleet(params);
    }
}
