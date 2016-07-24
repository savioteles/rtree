package serialization.wkb;

import serialization.ISpatialObjectSerializer;
import utils.JtsFactories;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class WKBSpatialObjectSerializer implements ISpatialObjectSerializer {

    @Override
    public byte[] serialize(Envelope envelope) {
        return JtsFactories.serialize(envelope);
    }

    @Override
    public byte[] serialize(Geometry geom) {
        return JtsFactories.serialize(geom);
    }

    @Override
    public Envelope unserializeEnvelope(byte[] rawdata) throws ParseException {
        return JtsFactories.unserializeEnvelope(rawdata);
    }

    @Override
    public Geometry unserializeGeometry(byte[] rawdata) throws ParseException {
        return JtsFactories.unserializeGeometry(rawdata);
    }

}
