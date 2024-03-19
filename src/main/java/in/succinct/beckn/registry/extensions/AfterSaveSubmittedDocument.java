package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.registry.db.model.onboarding.DocumentPurpose;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.SubmittedDocument;
import in.succinct.beckn.registry.db.model.onboarding.VerifiableDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AfterSaveSubmittedDocument extends AfterModelSaveExtension<SubmittedDocument> {
    static {
        registerExtension(new AfterSaveSubmittedDocument());
    }
    @Override
    public void afterSave(SubmittedDocument model) {
        TaskManager.instance().executeAsync(new KycInspector(model.getNetworkParticipant()),false);
    }

    public static class KycInspector implements Task {
        NetworkParticipant participant ;
        public KycInspector(NetworkParticipant participant){
            this.participant = participant;
        }

        @Override
        public int hashCode() {
            return (NetworkParticipant.class.getName() + ":" + participant.getId()).hashCode();
        }

        @Override
        public void execute() {
            Select select = new Select().from(DocumentPurpose.class);
            select.where(new Expression(select.getPool(),"REQUIRED_FOR_KYC", Operator.EQ,true));
            List<DocumentPurpose> documentPurposes = select.execute();
            List<SubmittedDocument> submittedDocuments = participant.getSubmittedDocuments();
            Map<Long,Boolean> kycRequirementCompletionMap = new HashMap<>();
            documentPurposes.forEach(p->{
                kycRequirementCompletionMap.put(p.getId(),false);
            });
            submittedDocuments.forEach(sd->{
                if (kycRequirementCompletionMap.containsKey(sd.getDocumentTypeId())){
                    if (!sd.isExpired() && ObjectUtil.equals(sd.getVerificationStatus(), VerifiableDocument.APPROVED)){
                        kycRequirementCompletionMap.remove(sd.getDocumentTypeId());
                    }
                }
            });
            if (kycRequirementCompletionMap.isEmpty()){
                participant.setTxnProperty("kyc.complete",true);
                participant.setKycComplete(true);
                participant.save();
            }
        }
    }
}
