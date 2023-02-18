package variants_lib.data;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class FleetPartitionMember {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetPartitionMember.class);
    static {
        log.setLevel(Level.ALL);
    }

    public String id;
    public float weight;

    public void makeWeightPercentage(double outOf)
    {
        weight /= outOf;
    }

    public FleetPartitionMember(String variantId, float spawningWeight)
    {
        id = variantId;
        weight = spawningWeight;
    }
}
