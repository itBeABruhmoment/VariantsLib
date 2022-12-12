package variants_lib.data;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;

public class DefaultFleetFactory extends VariantsLibFleetFactory {
    /**
     * Creates a default fleet factory from a fleet json
     * @param fromFile The fleet json
     */
    public DefaultFleetFactory(JSONObject fromFile) throws Exception {
        super(fromFile);
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public CampaignFleetAPI makeFleet(VariantsLibFleetParams params) {
        return null;
    }

    @Override
    public void editFleet(CampaignFleetAPI original) {

    }
}
