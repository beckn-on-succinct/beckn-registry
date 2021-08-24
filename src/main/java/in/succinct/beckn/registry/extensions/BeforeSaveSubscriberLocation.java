package in.succinct.beckn.registry.extensions;

import com.venky.geo.GeoCoordinate;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.plugins.collab.util.BoundingBox;
import in.succinct.beckn.registry.db.model.SubscriberLocation;

public class BeforeSaveSubscriberLocation extends BeforeModelSaveExtension<SubscriberLocation> {
    static {
        registerExtension(new BeforeSaveSubscriberLocation());
    }
    @Override
    public void beforeSave(SubscriberLocation model) {
        if (model.getLat() != null && model.getLng() != null && model.getRadius() > 0 ) {
            BoundingBox bb = new BoundingBox(new GeoCoordinate(model),0, model.getRadius());
            model.setMinLat(bb.getMin().getLat());
            model.setMinLng(bb.getMin().getLng());
            model.setMaxLat(bb.getMax().getLat());
            model.setMaxLng(bb.getMax().getLng());
        }
    }
}
