package in.succinct.beckn.registry.db.model.onboarding;

public interface Attachment extends com.venky.swf.plugins.attachment.db.model.Attachment {
    public Long getNetworkParticipantId();
    public void setNetworkParticipantId(Long id);
    public NetworkParticipant getNetworkParticipant();
}
