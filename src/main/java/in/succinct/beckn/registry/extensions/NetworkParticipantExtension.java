package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;

import java.util.Arrays;
import java.util.List;

public class NetworkParticipantExtension extends ParticipantExtension<NetworkParticipant> {
    static {
        registerExtension(new NetworkParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, NetworkParticipant partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            return Arrays.asList(user.getId());
        }
        return null;
    }
}
