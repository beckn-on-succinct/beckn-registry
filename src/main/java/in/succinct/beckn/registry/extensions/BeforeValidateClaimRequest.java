package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.registry.db.model.onboarding.ClaimRequest;

import java.util.UUID;

public class BeforeValidateClaimRequest extends BeforeModelValidateExtension<ClaimRequest> {
    static {
        registerExtension(new BeforeValidateClaimRequest());
    }
    @Override
    public void beforeValidate(ClaimRequest model) {


        if (model.getRawRecord().isFieldDirty("TXT_VALUE") && !model.getReflector().isVoid(model.getTxtValue())) {
            throw new RuntimeException("TXT VALUE is generated field.");
        }
        if (model.getRawRecord().isFieldDirty("DOMAIN_VERIFIED") && model.isDomainVerified()){
            throw new RuntimeException("Domain cannot be verified manually");
        }

        if (!model.isDomainVerified()){
            if (ObjectUtil.isVoid(model.getTxtValue())){
                model.setTxtValue(UUID.randomUUID().toString());
            }else if (model.isTxtRecordVerified()) {
                model.setDomainVerified(true);
                model.setTxtValue(null);
            }
        }
    }
}
