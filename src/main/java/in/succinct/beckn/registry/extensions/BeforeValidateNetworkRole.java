package in.succinct.beckn.registry.extensions;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;

public class BeforeValidateNetworkRole extends BeforeModelValidateExtension<NetworkRole> {
    static {
        registerExtension(new BeforeValidateNetworkRole());
    }
    @Override
    public void beforeValidate(NetworkRole model) {
        if (ObjectUtil.isVoid(model.getSubscriberId())){
            String domain = "";
            if (model.getNetworkDomain() != null){
                domain = model.getNetworkDomain().getName();
            }
            String participantId = "";
            if (model.getNetworkParticipantId() != null){
                participantId = model.getNetworkParticipant().getParticipantId();
            }
            model.setSubscriberId(String.format("%s.%s.%s",
                    StringUtil.valueOf(participantId),
                    StringUtil.valueOf(domain),
                    StringUtil.valueOf(model.getType())));
        }
    }
}
