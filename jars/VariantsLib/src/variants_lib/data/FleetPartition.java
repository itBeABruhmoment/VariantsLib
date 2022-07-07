package variants_lib.data;

import com.fs.starfarer.api.Global;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class FleetPartition {
    private static final Logger log = Global.getLogger(variants_lib.data.FleetPartition.class);
    static {
        log.setLevel(Level.ALL);
    }

    public FleetPartitionMember[] members;
    public double partitionWeight; // should be percentage after processing
    public double partitionVariance; // percentage

    public void makePartitionWeightPercentage(double outOf)
    {
        partitionWeight = partitionWeight / outOf;
    }

    public void makePartitionVariancePercentage(double outOf)
    {
        partitionVariance = partitionVariance / outOf;
    }
    // loadedFileInfo and index is just data for throwing descriptive error messages
    public FleetPartition(JSONObject partitionData, String loadedFileInfo, int index) throws Exception
    {
        // read "partitionWeight"
        try {
            partitionWeight = partitionData.getDouble("partitionWeight");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has missing or invalid \"partitionWeight\" field");
        }
        if(partitionWeight < 0) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has negative \"partitionWeight\" field");
        }

        // read "partitionWeightVariance"
        try {
            partitionVariance = partitionData.getDouble("partitionWeightVariance");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has missing or invalid \"partitionWeightVariance\" field");
        }
        if(partitionVariance < 0) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has negative \"partitionWeightVariance\" field");
        }

        // read "variants"
        JSONArray partitionMembersData = null;
        try {
            partitionMembersData = partitionData.getJSONArray("variants");
        } catch(Exception e) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has missing or invalid \"variants\" field");
        }
        if(partitionMembersData.length() == 0) {
            throw new Exception(loadedFileInfo + " fleet partion " + index + " has empty \"variants\" field");
        }

        // construct "members field"
        double variantWeightSum = 0;
        members = new FleetPartitionMember[partitionMembersData.length()];
        int i = 0;
        int length = partitionMembersData.length();
        while(i < length) {
            JSONObject partitionMemberData = partitionMembersData.getJSONObject(i);
            // read and check id
            String id = null;
            try {
                id = partitionMemberData.optString("id");               
            } catch (Exception e) {
                throw new Exception(loadedFileInfo + " fleet partion " + index + " variant " + i + " could not have its \"id\" field read");
            }
            if(id.length() == 0) {
                throw new Exception(loadedFileInfo + " fleet partion " + index + " variant " + i + " could not have its \"id\" field read");
            }
            if(!Global.getSettings().doesVariantExist(id)) {
                throw new Exception(loadedFileInfo + " fleet partion " + index + " variant " + i + ": " + id + " is not a recognized variant");
            }

            // read and check weight
            double weight = 0;
            try {
                weight = partitionMemberData.getDouble("weight");
            } catch(Exception e) {
                throw new Exception(loadedFileInfo + " fleet partion " + index + " variant " + i + " could not have its \"weight\" field read");
            }
            if(weight <= 0) {
                throw new Exception(loadedFileInfo + " fleet partion " + index + " variant " + i + " has \"weight\" field that is less than or equal to 0");
            }
            variantWeightSum += weight;
            members[i] = new FleetPartitionMember(id, weight);
            i++;
            length = partitionMembersData.length();

        }
        
        for(FleetPartitionMember mem : members) {
            mem.makeWeightPercentage(variantWeightSum);
        }
    }
}
