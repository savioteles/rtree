package spatialindex.rtree;

import spatialindex.serialization.HarpiaSerializableObject;

import com.vividsolutions.jts.geom.Envelope;

public class RTreeEntryDir
        extends HarpiaSerializableObject implements RTreeIEntryDir {

	private static final long serialVersionUID = 1L;
	public static final int OWNERNODE = 0;
    public static final int CHILD = 1;
    public static final int BOUNDINGBOX = 2;

    public RTreeEntryDir() {
    }

    public RTreeEntryDir(byte[] rawdata) {
        super(rawdata);
    }

    public RTreeEntryDir(String child, Envelope bb) {
        setParams(null, child, bb);
    }

    @Override
    public Envelope getBoundingBox() {
        return getParam(BOUNDINGBOX, Envelope.class);
    }

    @Override
    public String getChild() {
        return getParam(CHILD, String.class);
    }

    @Override
    public String getOwnerNode() {
        return getParam(OWNERNODE, String.class);
    }

    @Override
    public void setBoundingBox(Envelope mbr) {
        setParam(BOUNDINGBOX, mbr);
    }

    @Override
    public void setChild(String node) {
        setParam(CHILD, node);
    }

    @Override
    public void setOwnerNode(String ownerNode) {
        setParam(OWNERNODE, ownerNode);
    }

}
