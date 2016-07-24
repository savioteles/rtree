package spatialindex.rtree;

import java.util.HashMap;
import java.util.Map;

import spatialindex.rtree.RTreeINode.NodeIsLeafException;
import spatialindex.serialization.HarpiaSerializableObject;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public abstract class RStar extends HarpiaSerializableObject implements RTreeIRTree {

	private static final long serialVersionUID = 1L;

	private static final String rootName = "root";

    public static final int CAPACITY = 0;
    public static final int NAME = 1;
    public static final int SIZE = 2;
    public static final int NODES = 3;
    public static final int ISPOINT = 4;

    public RStar() {
    }

    public RStar(byte[] rawdata) {
        super(rawdata);
    }

    public RStar(int capacity, String name, boolean isPoint) {
        RTreeNode root = new RTreeNode(capacity, true, rootName);

        Map<String, RTreeNode> map = new HashMap<String, RTreeNode>();
        map.put(rootName, root);

        setParams(capacity, name, 0, map, isPoint);
    }

    public void decrSize() {
        setParam(SIZE, getSize() - 1);
    }

    @Override
    public int getCapacity() {
        return getParam(CAPACITY, Integer.class);
    }

    @Override
    public String getName() {
        return getParam(NAME, String.class);
    }

    @Override
    public RTreeNode getNewRoot() {
        return new RTreeNode(getCapacity(), false, rootName);
    }

    @Override
    public RTreeNode getNode(String name) {
        return getMapParam(NODES, String.class, RTreeNode.class).get(name);
    }

    private Map<String, RTreeNode> getNodes() {
        return getMapParam(NODES, String.class, RTreeNode.class);
    }

    @Override
    public RTreeNode getRoot() {
        return getNode(rootName);
    }

    @Override
    public int getSize() {
        return getParam(SIZE, Integer.class);
    }

    public void incrSize() {
        setParam(SIZE, getSize() + 1);
    }

    @Override
    public synchronized void insert(Envelope bb, IndexObject object,
            RTreeINode nodeReinserted) throws NodeIsLeafException {
        incrSize();
        RTreeInsertion.insertTree(getRoot(), new RTreeEntryData(bb, object),
                nodeReinserted, this);
    }

    public boolean isPoint() {
        return getParam(ISPOINT, Boolean.class);
    }

    @Override
    public void newNode(String name, RTreeINode node) {

        Map<String, RTreeNode> map = getNodes();
        map.put(name, (RTreeNode) node);
        setNodes(map);
    }

    @Override
    public void remove(String uri, Geometry polygon)
            throws NodeIsLeafException {
        decrSize();
        RTreeDeletion.deleteOnRtree(new RTreeEntryData(uri, polygon), this);
    }

    @Override
    public synchronized void setNewRoot(RTreeNode node) {
        Map<String, RTreeNode> map = getNodes();
        map.put(rootName, node);
        setParam(NODES, map);
    }

    private void setNodes(Map<String, RTreeNode> map) {
        setParam(NODES, map);
    }
}
