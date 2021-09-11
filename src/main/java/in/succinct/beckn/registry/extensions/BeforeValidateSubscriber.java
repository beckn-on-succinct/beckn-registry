package in.succinct.beckn.registry.extensions;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.Subscriber;
import org.bouncycastle.cert.ocsp.Req;

public class BeforeValidateSubscriber extends BeforeModelValidateExtension<Subscriber> {
    static {
        registerExtension(new BeforeValidateSubscriber());
    }
    @Override
    public void beforeValidate(Subscriber subscriber) {
        if (!ObjectUtil.isVoid(subscriber.getEncrPublicKey())){
            Crypt.getInstance().getPublicKey(Request.ENCRYPTION_ALGO,subscriber.getEncrPublicKey());
        }
        if (!ObjectUtil.isVoid(subscriber.getSigningPublicKey())){
            Crypt.getInstance().getPublicKey(Request.SIGNATURE_ALGO,subscriber.getSigningPublicKey());
        }
    }
}
