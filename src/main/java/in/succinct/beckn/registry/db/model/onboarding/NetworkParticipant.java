package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.attachment.db.model.Attachment;

import java.util.List;

@HAS_DESCRIPTION_FIELD("PARTICIPANT_ID")
@MENU("Admin")
public interface NetworkParticipant extends Model {
    @UNIQUE_KEY
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
}
