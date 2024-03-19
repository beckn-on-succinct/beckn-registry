package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;

public class NetworkParticipantImpl extends ModelImpl<NetworkParticipant> {
    public NetworkParticipantImpl(NetworkParticipant participant){
        super(participant);
    }
    public Long getAnyUserId() {
        return null;
    }
    public void setAnyUserId(Long id){

    }
    public Long getAnyUser(){
        return null;
    }


    public NetworkParticipant getMyNetworkParticipant(){
        return getProxy();
    }

    public ClaimRequest claim(){
        ClaimRequest claimRequest = Database.getTable(ClaimRequest.class).newRecord();
        claimRequest.setNetworkParticipantId(getProxy().getId());
        claimRequest.setCreatorUserId(Database.getInstance().getCurrentUser().getId());
        claimRequest = Database.getTable(ClaimRequest.class).getRefreshed(claimRequest);
        claimRequest.save();
        return claimRequest;
    }

}
