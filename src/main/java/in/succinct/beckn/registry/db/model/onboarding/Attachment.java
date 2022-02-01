package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface Attachment extends com.venky.swf.plugins.attachment.db.model.Attachment {
    @PARTICIPANT(redundant = true)
    public Long getNetworkParticipantId();
    public void setNetworkParticipantId(Long id);
    public NetworkParticipant getNetworkParticipant();
}
