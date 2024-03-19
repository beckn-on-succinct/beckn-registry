package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.exceptions.AccessDeniedException;
import in.succinct.beckn.registry.db.model.onboarding.VerifiableDocument;

public class BeforeValidateVerifiableDocument<M extends VerifiableDocument & Model> extends BeforeModelValidateExtension<M> {
    @Override
    public void beforeValidate(M document) {
        if (!ObjectUtil.equals(true,document.getTxnProperty("being.verified")) && document.getRawRecord().isFieldDirty("VERIFICATION_STATUS") && !document.getVerificationStatus().equals(VerifiableDocument.PENDING)){
             throw new AccessDeniedException();
        }
    }
}
