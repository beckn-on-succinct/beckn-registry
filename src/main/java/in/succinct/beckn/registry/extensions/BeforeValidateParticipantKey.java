package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;

public class BeforeValidateParticipantKey extends BeforeModelValidateExtension<ParticipantKey> {
    static {
        registerExtension(new BeforeValidateParticipantKey());
    }
    @Override
    public void beforeValidate(ParticipantKey participantKey) {
        if (!ObjectUtil.isVoid(participantKey.getEncrPublicKey())){
            participantKey.setEncrPublicKey(Request.getRawEncryptionKey(participantKey.getEncrPublicKey()));
        }
        if (!ObjectUtil.isVoid(participantKey.getSigningPublicKey())){
            participantKey.setSigningPublicKey(Request.getRawSigningKey(participantKey.getSigningPublicKey()));
        }
        // Useful to store in base64encoded raw format as this is what seems to be standard.
    }



}
