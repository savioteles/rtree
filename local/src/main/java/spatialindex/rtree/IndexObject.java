package spatialindex.rtree;

import java.util.Comparator;

import spatialindex.serialization.HarpiaSerializableObject;
import utils.JtsFactories;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class IndexObject
        extends HarpiaSerializableObject implements Comparable<IndexObject> {

    private static final long serialVersionUID = 1L;
    private static final int ENVELOPE = 0;
    private static final int GEOMWKB = 1;
    private static final int KEY = 2;
    private static final int GEOMKEY = 3;

    public static Comparator<IndexObject> keyComparator = new Comparator<IndexObject>() {
        @Override
        public int compare(IndexObject o1, IndexObject o2) {
            if (o1.getGeomKey() != null && o2.getGeomKey() != null)
                return o1.getGeomKey().compareTo(o2.getGeomKey());
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private transient Geometry geometry;

    public IndexObject() {

    }

    public IndexObject(byte[] wkb, String key, boolean isPoint)
            throws ParseException {
        String geomKey = JtsFactories.getGeomKey(wkb);
        if (isPoint)
            setParams(null, wkb, key, geomKey);
        else
            setParams(null, null, key, geomKey);
    }

    public IndexObject(Envelope envelopeOriginal, Geometry geometry) {
        this.geometry = geometry;
        setParams(envelopeOriginal, geometry);
    }

    public IndexObject(String key, Geometry geometry) {
        this.geometry = geometry;
        String geomKey = JtsFactories.getGeomKey(geometry);
        setParams(null, JtsFactories.serialize(geometry), key, geomKey);
    }

    @Override
    public int compareTo(IndexObject o) {
        if (getGeomKey().equals(o.getGeomKey()))
            return 0;
        Double x1 = getEnvelopeOriginal().centre().x;
        Double x2 = o.getEnvelopeOriginal().centre().x;
        return x1.compareTo(x2) != 0 ? x1.compareTo(x2) : -1;
    }

    public Envelope getEnvelopeOriginal() {
        return getParam(ENVELOPE, Envelope.class);
    }

    public Geometry getGeometry() throws ParseException {
        if (geometry == null) {
            byte[] wkb = getGeomWKB();
            if (wkb != null)
                geometry = JtsFactories.unserializeGeometry(wkb);
        }
        return geometry;
    }

    public String getGeomKey() {
        return getParam(GEOMKEY, String.class);
    }

    public byte[] getGeomWKB() {
        return getParam(GEOMWKB, byte[].class);
    }

    public String getKey() {
        return getParam(KEY, String.class);
    }

    public void setEnvelopeOriginal(Envelope envelopeOriginal) {
        setParam(ENVELOPE, envelopeOriginal);
    }

    public void setGeomKey(String geomKey) {
        setParam(GEOMKEY, geomKey);
    }

    @Override
    public String toString() {
        try {
            return getGeometry().toText();
        } catch (ParseException e) {
            return null;
        }
    }
}
