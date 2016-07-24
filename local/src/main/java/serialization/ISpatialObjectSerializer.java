package serialization;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public interface ISpatialObjectSerializer {

    public byte[] serialize(Envelope geom);

    public byte[] serialize(Geometry geom);

    public Envelope unserializeEnvelope(byte[] rawdata) throws ParseException;

    public Geometry unserializeGeometry(byte[] rawdata) throws ParseException;
}
