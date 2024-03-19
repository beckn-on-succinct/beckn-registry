package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.onboarding.ClaimRequest;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;

public class NetworkParticipantsController extends ModelController<NetworkParticipant> {
    public NetworkParticipantsController(Path path) {
        super(path);
    }
    public View claim(long id){
        NetworkParticipant participant = Database.getTable(NetworkParticipant.class).get(id);
        ClaimRequest request = participant.claim();
        return forwardTo("/claim_requests/show/" + request.getId());
    }
}
