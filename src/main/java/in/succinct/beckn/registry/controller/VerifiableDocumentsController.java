package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.onboarding.VerifiableDocument;

public class VerifiableDocumentsController<M extends VerifiableDocument & Model> extends ModelController<M> {
    public VerifiableDocumentsController(Path path) {
        super(path);
    }

    @SingleRecordAction(icon = "fas fa-check", tooltip = "Mark Approved")
    public View approve(long id){
        M document = Database.getTable(getModelClass()).get(id);
        document.setTxnProperty("being.verified",true);
        document.setVerificationStatus(VerifiableDocument.APPROVED);
        document.save();
        return show(document);
    }

    @SingleRecordAction(icon = "fas fa-times", tooltip = "Mark Rejected")
    public View reject(long id){
        M document = Database.getTable(getModelClass()).get(id);
        document.setTxnProperty("being.verified",true);
        document.setVerificationStatus(VerifiableDocument.REJECTED);
        document.save();
        return show(document);
    }
}
