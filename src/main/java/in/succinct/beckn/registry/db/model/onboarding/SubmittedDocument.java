package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;



public interface SubmittedDocument extends Model,VerifiableDocument {

    @PARTICIPANT
    @HIDDEN
    @IS_NULLABLE(false)
    public Long getNetworkParticipantId();
    public void setNetworkParticipantId(Long id);
    public NetworkParticipant getNetworkParticipant();

    @IS_NULLABLE(false)
    public Long getDocumentTypeId();
    public void setDocumentTypeId(Long id);
    public DocumentPurpose getDocumentType();

}
