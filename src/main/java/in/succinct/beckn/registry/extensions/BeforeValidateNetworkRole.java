package in.succinct.beckn.registry.extensions;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;

public class BeforeValidateNetworkRole extends BeforeModelValidateExtension<NetworkRole> {
    static {
        registerExtension(new BeforeValidateNetworkRole());
    }
    @Override
    public void beforeValidate(NetworkRole model) {
        model.setSubscriberId(String.format("%s.%s.%s",
                StringUtil.valueOf(model.getNetworkParticipant().getParticipantId()),
                StringUtil.valueOf(model.getNetworkDomain().getName()),
                StringUtil.valueOf(model.getType())));
    }
}
