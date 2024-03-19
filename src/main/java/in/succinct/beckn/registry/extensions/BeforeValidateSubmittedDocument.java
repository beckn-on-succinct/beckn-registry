package in.succinct.beckn.registry.extensions;

import in.succinct.beckn.registry.db.model.onboarding.SubmittedDocument;

public class BeforeValidateSubmittedDocument extends BeforeValidateVerifiableDocument<SubmittedDocument> {
    static {
        registerExtension(new BeforeValidateSubmittedDocument());
    }

    @Override
    public void beforeValidate(SubmittedDocument document) {
        if (document.getDocumentType() == null){
            return;
        }
        super.beforeValidate(document);
    }
}
