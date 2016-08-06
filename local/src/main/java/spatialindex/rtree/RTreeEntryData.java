package spatialindex.rtree;

import java.util.Map;

import spatialindex.serialization.HarpiaSerializableObject;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class RTreeEntryData
        extends HarpiaSerializableObject implements RTreeIEntryData {

	private static final long serialVersionUID = 1L;
	public static final int BOUND = 0;
    public static final int OBJECT = 1;
    public static final int OWNERNODE = 2;

    public RTreeEntryData() {
    }

    public RTreeEntryData(byte[] rawdata) {
        super(rawdata);
    }

    public RTreeEntryData(Envelope bb, IndexObject obj) {
        setParams(bb, obj, null);
    }

    @Override
    public Envelope getBoundingBox() {
        return getParam(BOUND, Envelope.class);
    }

    @Override
    public String getChild() {
        return getObject().getKey();
    }

    @Override
    public Map<String, String> getCopyKeys() {
        return null;
    }

    @Override
    public String getGeomKey() {
        return getObject().getGeomKey();
    }

    @Override
    public String getLayer() {
        return null;
    }

    @Override
    public int getNumChildGeomPoints() {
        return getPolygon().getNumPoints();
    }

    @Override
    public IndexObject getObject() {
        return getParam(OBJECT, IndexObject.class);
    }

    @Override
    public String getOwnerNode() {
        return getParam(OWNERNODE, String.class);
    }

    @Override
    public Geometry getPolygon() {
        try {
            return getObject().getGeometry();
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getShadowGeoms() {
        return null;
    }

    @Override
    public void setBoundingBox(Envelope mbr) {
        setParam(BOUND, mbr);
    }

    @Override
    public void setOwnerNode(String node) {
        setParam(OWNERNODE, node);
    }
}
