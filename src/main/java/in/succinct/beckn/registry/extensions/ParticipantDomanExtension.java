package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.registry.db.model.onboarding.OperatingRegion;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantDomain;

import java.util.Collections;
import java.util.List;

public class ParticipantDomanExtension extends ParticipantExtension<ParticipantDomain> {
    static {
        registerExtension(new ParticipantDomanExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, ParticipantDomain partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            if (partiallyFilledModel.getRawRecord().isNewRecord()) {
                return Collections.singletonList(user.getId());
            }
        }
        return null;
    }
}
