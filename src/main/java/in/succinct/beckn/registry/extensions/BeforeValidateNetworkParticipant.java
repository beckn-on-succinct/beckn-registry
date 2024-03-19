package in.succinct.beckn.registry.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.exceptions.AccessDeniedException;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;

public class BeforeValidateNetworkParticipant extends BeforeModelValidateExtension<NetworkParticipant> {
    static {
        registerExtension(new BeforeValidateNetworkParticipant());
    }
    @Override
    public void beforeValidate(NetworkParticipant model) {
        if (model.isKycComplete() && model.getRawRecord().isFieldDirty("KYC_COMPLETE")){
            if (!model.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(model.getTxnProperty("kyc.complete"))){
                throw new AccessDeniedException();
            }
        }
    }
}
