package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.onboarding.ClaimRequest;

public class ClaimRequestsController extends ModelController<ClaimRequest> {
    public ClaimRequestsController(Path path) {
        super(path);
    }

    @SingleRecordAction(icon = "fas fa-check")
    public View verifyDomain(long id) {
        ClaimRequest claimRequest = Database.getTable(ClaimRequest.class).get(id);
        claimRequest.requestDomainVerification();
        String message = "";
        if (claimRequest.isDomainVerified()) {
            message = "Successfully verified your domain.";
        } else {
            message = "After updating your domain's txt record, wait for some time and try again .";
        }
        if (getReturnIntegrationAdaptor() == null) {
            getPath().addInfoMessage(message);
            return back();
        } else {
            return show(claimRequest);
        }
    }
}