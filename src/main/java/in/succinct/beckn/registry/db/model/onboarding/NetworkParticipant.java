package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

import java.util.List;

@HAS_DESCRIPTION_FIELD("PARTICIPANT_ID")
@MENU("Admin")
public interface NetworkParticipant extends Model {
    @UNIQUE_KEY
    @Index
    public String getParticipantId();
    public void setParticipantId(String id);

    public List<Attachment> getAttachments();
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

}
