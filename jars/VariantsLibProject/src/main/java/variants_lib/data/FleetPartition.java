package variants_lib.data;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * represents an element of the "fleetPartitions" field in fleet jsons
 */
public class FleetPartition {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetPartition.class);
    static {
        log.setLevel(Level.ALL);
    }

    public int maxDPForPartition = 10000;
    public int maxShipsForPartition = 1000;
    public ArrayList<FleetPartitionMember> members = new ArrayList<>();
    public float partitionWeight = 10.0f;

    /**
     * Create a fleet partition
     * @param partitionData json representation of a partition
     * @param index used for log output, set it to anything
     * @param modId used for log output, set it to anything
     * @throws Exception
     */
    public FleetPartition(JSONObject partitionData, int index, String modId) throws Exception
    {
        // read "partitionWeight"
        try {
            partitionWeight = (float) partitionData.getDouble(CommonStrings.PARTITION_WEIGHT);
        } catch(Exception e) {
            throw new Exception("fleet partion " + index + " has missing or invalid \"partitionWeight\" field");
        }
        if(partitionWeight < 0) {
            throw new Exception(" fleet partion " + index + " has negative \"partitionWeight\" field");
        }

        // read "variants"
        JSONObject variants = null;
        try {
            variants = partitionData.getJSONObject(CommonStrings.VARIANTS);
        } catch(Exception e) {
            throw new Exception(" fleet partion " + index + " has missing or invalid \"variants\" field");
        }

        maxDPForPartition = JsonUtils.getInt("maxDPForPartition", Integer.MAX_VALUE, partitionData);
        maxShipsForPartition = JsonUtils.getInt("maxShipsForPartition", Integer.MAX_VALUE, partitionData);

        // construct "members field"
        float variantWeightSum = 0.0f;
        members = new ArrayList<>();
        Iterator keys = variants.keys();
        while(keys.hasNext()) {
            String key = (String) keys.next();
            if(!key.equals(CommonStrings.PARTITION_WEIGHT) && !key.equals(CommonStrings.VARIANTS)) {
                if(Global.getSettings().getVariant(key) == null && ModdedVariantsData.addVariantToStore(key, modId)) {
                    throw new Exception(" fleet partion " + index + " has unrecognised variant \"" + key + "\"");
                }

                float variantWeight = 0.0f;
                try {
                    variantWeight = (float) variants.getDouble(key);
                } catch(Exception e) {
                    throw new Exception(" fleet partion " + index + " failed to read the weight of the variant \"" + key + "\"");
                }

                if(variantWeight < 0.0f) {
                    throw new Exception(" fleet partion " + index + " the weight of the variant \"" + key + "\" is less than zero");
                }

                members.add(new FleetPartitionMember(key, variantWeight));
                variantWeightSum += variantWeight;
            }
        }
        members.trimToSize();
        
        
        for(FleetPartitionMember member : members) {
            member.makeWeightPercentage(variantWeightSum);
        }
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        try {
            json.put(CommonStrings.MAX_SHIPS_PARTITION, maxShipsForPartition);
            json.put(CommonStrings.MAX_DP_PARTITION, maxDPForPartition);
            json.put(CommonStrings.PARTITION_WEIGHT, (double) partitionWeight);
            final JSONObject membersJson = new JSONObject();
            for(FleetPartitionMember member : members) {
                membersJson.put(member.id, member.weight);
            }
            json.put(CommonStrings.VARIANTS, membersJson);
        } catch (Exception e) {
            log.info("failed to convert partition to JSON");
        }
        return json;
    }
}
