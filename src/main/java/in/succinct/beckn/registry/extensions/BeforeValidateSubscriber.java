package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.Subscriber;

public class BeforeValidateSubscriber extends BeforeModelValidateExtension<Subscriber> {
    static {
        registerExtension(new BeforeValidateSubscriber());
    }
    @Override
    public void beforeValidate(Subscriber subscriber) {
        if (!ObjectUtil.isVoid(subscriber.getEncrPublicKey())){
            subscriber.setEncrPublicKey(Request.getRawEncryptionKey(subscriber.getEncrPublicKey()));
        }
        if (!ObjectUtil.isVoid(subscriber.getSigningPublicKey())){
            subscriber.setSigningPublicKey(Request.getRawSigningKey(subscriber.getSigningPublicKey()));
        }
        // Useful to store in base64encoded raw format as this is what seems to be standard.
    }



}
