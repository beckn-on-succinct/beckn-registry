package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

import java.util.List;

@HAS_DESCRIPTION_FIELD("PARTICIPANT_ID")
@MENU("Admin")
public interface NetworkParticipant extends Model {
    @UNIQUE_KEY
    @Index
    public String getParticipantId();
    public void setParticipantId(String id);

    @PROTECTION(Kind.DISABLED)
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isKycComplete();
    public void setKycComplete(boolean kycComplete);

    public List<SubmittedDocument> getSubmittedDocuments();
    public List<NetworkRole> getNetworkRoles();
    public List<ParticipantKey> getParticipantKeys();


    public static NetworkParticipant find(String particpantId){
        NetworkParticipant participant = Database.getTable(NetworkParticipant.class).newRecord();
        participant.setParticipantId(particpantId);
        participant = Database.getTable(NetworkParticipant.class).getRefreshed(participant);
        return participant;
    }

    @Index
    @PARTICIPANT
    public Long getCreatorUserId();


    @IS_VIRTUAL
    @PARTICIPANT
    @HIDDEN
    public Long getAnyUserId();
    public void setAnyUserId(Long anyUserId);
    public User getAnyUser();



    @COLUMN_NAME("ID")
    @PROTECTION
    @HIDDEN
    @HOUSEKEEPING
    @PARTICIPANT
    public long getMyNetworkParticipantId();
    public void setMyNetworkParticipantId(long id);

    @IS_VIRTUAL
    public NetworkParticipant getMyNetworkParticipant();


    public ClaimRequest claim();
}
