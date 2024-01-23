package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantDomain;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;

import java.util.Collections;
import java.util.List;

public class ParticipantKeyExtension extends ParticipantExtension<ParticipantKey> {
    static {
        registerExtension(new ParticipantKeyExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, ParticipantKey partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            if (partiallyFilledModel.getRawRecord().isNewRecord()) {
                return Collections.singletonList(user.getId());
            }
        }
        return null;
    }
}
