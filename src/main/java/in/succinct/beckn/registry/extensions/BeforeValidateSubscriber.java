package in.succinct.beckn.registry.extensions;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.Subscriber;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCXDHPublicKey;

import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.util.Base64;

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
