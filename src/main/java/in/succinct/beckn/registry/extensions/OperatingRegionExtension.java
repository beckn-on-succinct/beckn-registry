package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.OperatingRegion;

import java.util.Collections;
import java.util.List;

public class OperatingRegionExtension extends ParticipantExtension<OperatingRegion> {
    static {
        registerExtension(new OperatingRegionExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, OperatingRegion partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            if (partiallyFilledModel.getRawRecord().isNewRecord()) {
                return Collections.singletonList(user.getId());
            }
        }
        return null;
    }
}
