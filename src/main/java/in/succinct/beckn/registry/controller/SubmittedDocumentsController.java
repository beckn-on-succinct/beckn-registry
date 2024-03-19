package in.succinct.beckn.registry.controller;

import com.venky.swf.path.Path;
import in.succinct.beckn.registry.db.model.onboarding.SubmittedDocument;

public class SubmittedDocumentsController extends VerifiableDocumentsController<SubmittedDocument> {
    public SubmittedDocumentsController(Path path) {
        super(path);
    }

}
