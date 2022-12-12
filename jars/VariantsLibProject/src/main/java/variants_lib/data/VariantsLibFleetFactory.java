package variants_lib.data;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import org.json.JSONObject;

import java.util.Random;

public abstract class VariantsLibFleetFactory {
    /**
     * Should create the factory from a fleet json
     * @param fromFile The fleet json
     */
    public VariantsLibFleetFactory(JSONObject fromFile) {

    }

    /**
     * Should return an unique id
     * @return An unique id
     */
    public abstract String getId();

    /**
     * Should make a fleet from parameters
     * @param params Parameters to make fleet with
     * @return
     */
    public abstract CampaignFleetAPI makeFleet(VariantsLibFleetParams params);

    /**
     * Should edit a fleet
     * @param original The fleet to edit
     */
    public abstract void editFleet(CampaignFleetAPI original);

}
