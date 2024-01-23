package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NetworkRoleExtension extends ParticipantExtension<NetworkRole> {
    static {
        registerExtension(new NetworkRoleExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, NetworkRole partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            if (partiallyFilledModel.getRawRecord().isNewRecord()) {
                return Collections.singletonList(user.getId());
            }
        }
        return null;
    }
}
