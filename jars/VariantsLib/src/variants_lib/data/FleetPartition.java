package variants_lib.data;

import com.fs.starfarer.api.Global;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class FleetPartition {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetPartition.class);
    static {
        log.setLevel(Level.ALL);
    }

    public Vector<FleetPartitionMember> members;
    public float partitionWeight; // should be percentage after processing

    private static final String PARTITION_WEIGHT = "partitionWeight";
    private static final String VARIANTS = "variants";

    public void makePartitionWeightPercentage(float outOf)
    {
        partitionWeight = partitionWeight / outOf;
    }

    // loadedFileInfo and index is just data for throwing descriptive error messages
    public FleetPartition(JSONObject partitionData, String loadedFileInfo, int index) throws Exception
    {
        // read "partitionWeight"
        try {
            partitionWeight = (float) partitionData.getDouble(PARTITION_WEIGHT);
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has missing or invalid \"partitionWeight\" field");
        }
        if(partitionWeight < 0) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has negative \"partitionWeight\" field");
        }

        // read "variants"
        JSONObject variants = null;
        try {
            variants = partitionData.getJSONObject(VARIANTS);
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has missing or invalid \"variants\" field");
        }

        // construct "members field"
        float variantWeightSum = 0.0f;
        members = new Vector<FleetPartitionMember>();
        Iterator keys = partitionData.keys();
        while(keys.hasNext()) {
            String key = (String) keys.next();
            if(!key.equals(PARTITION_WEIGHT) && !key.equals(VARIANTS)) {
                if(Global.getSettings().getVariant(key) == null) {
                    throw new Exception(loadedFileInfo + " fleet partion " + index + " has unrecognised variant \"" + key + "\"");
                }

                float variantWeight = 0.0f;
                try {
                    variantWeight = (float) partitionData.getDouble(key);
                } catch(Exception e) {
                    throw new Exception(loadedFileInfo + " fleet partion " + index + " failed to read the weight of the variant \"" + key + "\"");
                }

                if(variantWeight < 0.0f) {
                    throw new Exception(loadedFileInfo + " fleet partion " + index + " the weight of the variant \"" + key + "\" is less than zero");
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
}
