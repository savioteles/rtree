package spatialindex.rtree;

import java.util.ArrayList;
import java.util.List;

import spatialindex.serialization.HarpiaSerializableObject;

import com.vividsolutions.jts.geom.Envelope;

public class RTreeNode extends HarpiaSerializableObject implements RTreeINode {

	private static final long serialVersionUID = 1L;
	private static double minimumCapacityFactor = 0.40;
    private static double reinsertNodesFactor = 0.30;

    public static final int ENTRIES = 0;
    public static final int ISLEAF = 1;
    public static final int REINSERTEDENTRIES = 2;
    public static final int BOUNDINGBOX = 3;
    public static final int PARENT = 4;
    public static final int CAPACITY = 5;
    public static final int ENTRIESTYPE = 6;
    public static final int NAME = 7;

    public RTreeNode() {
    }

    public RTreeNode(byte[] rawdata) {
        super(rawdata);
    }

    public RTreeNode(int capacity, boolean isLeaf, String name) {
        List<RTreeIEntry> entries = new ArrayList<RTreeIEntry>();
        Envelope boundingBox = new Envelope();
        setParams(entries, isLeaf, false, boundingBox, null, capacity,
                createTypedOfList(entries), name);
    }

    @Override
    public void addEntry(RTreeIEntry entry) {

        // Altera o ownerNode da entrada
        entry.setOwnerNode(getName());
        // Ajusta o boundingBox para cobrir a nova g
        if (getBoundingBox().isNull())
            setBoundinBox(entry.getBoundingBox());
        else {
            Envelope box = getBoundingBox();
            box.expandToInclude(entry.getBoundingBox());
            setBoundinBox(box);
        }

        List<RTreeIEntry> entries = getEntries();
        entries.add(entry);
        setParam(ENTRIES, entries);
        setParam(ENTRIESTYPE, createTypedOfList(entries));
    }

    @Override
    public void addSubNode(RTreeINode subNode) throws NodeIsLeafException {
        if (isLeaf())
            throw new NodeIsLeafException();
        else {
            RTreeEntryDir e = new RTreeEntryDir(subNode.getName(),
                    subNode.getBoundingBox());
            subNode.setParent(e);
            addEntry(e);
        }

    }

    @Override
    public void delEntry(int entry) {
        List<RTreeIEntry> entries = getEntries();
        entries.remove(entry);

        if (entries.size() > 0) {
            Envelope boundingBox = entries.get(0).getBoundingBox();
            for (RTreeIEntry e : entries)
                boundingBox.expandToInclude(e.getBoundingBox());
            setParam(BOUNDINGBOX, boundingBox);
        } else
            setParam(BOUNDINGBOX, null);

    }

    @Override
    public Envelope getBoundingBox() {
        return getParam(BOUNDINGBOX, Envelope.class);
    }

    @Override
    public int getCapacity() {
        return getParam(CAPACITY, Integer.class);
    }

    @Override
    public List<RTreeIEntry> getEntries() {
        if (!isSerialObject(ENTRIES))
            return this.<RTreeIEntry> getListTyped(ENTRIESTYPE, ENTRIES);
        else {
            List<RTreeIEntry> entries = new ArrayList<RTreeIEntry>(
                    getCapacity() + 1);
            getListTyped(ENTRIESTYPE, ENTRIES, entries);
            return entries;
        }
    }

    @Override
    public RTreeIEntry getEntry(int i) throws IndexOutOfBoundsException {
        if (i >= getEntries().size())
            throw new IndexOutOfBoundsException();
        return getEntries().get(i);
    }

    @Override
    public int getItemsCount() {
        return getEntries().size();
    }

    @Override
    public int getMinimumCapacity() {
        int minimumCapacity = (int) (minimumCapacityFactor * getCapacity());
        return minimumCapacity;
    }

    @Override
    public String getName() {
        return getParam(NAME, String.class);
    }

    @Override
    public long getNumEntriesReinsert() {
        long numEntries = Math.round(reinsertNodesFactor * getCapacity());
        return numEntries;
    }

    @Override
    public RTreeEntryDir getParent() {
        return getParam(PARENT, RTreeEntryDir.class);
    }

    @Override
    public boolean isFull() {
        return getCapacity() == getEntries().size();
    }

    @Override
    public boolean isLeaf() {
        return getParam(ISLEAF, Boolean.class);
    }

    @Override
    public boolean reinsertedEntries() {
        return getParam(REINSERTEDENTRIES, Boolean.class);
    }

    @Override
    public void setBoundinBox(Envelope boundingBox) {
        setParam(BOUNDINGBOX, boundingBox);
    }

    public void setLeaf(boolean isLeaf) {
        setParam(ISLEAF, isLeaf);
    }

    @Override
    public void setName(String newName) {
        setParam(NAME, newName);
        for (RTreeIEntry e : getEntries())
            e.setOwnerNode(newName);
    }

    @Override
    public void setParent(RTreeEntryDir parent) {
        setParam(PARENT, parent);
    }

    @Override
    public void setReinsertedEntries(boolean reinsertedEntries) {
        setParam(REINSERTEDENTRIES, reinsertedEntries);
    }

    @Override
    public void substEntries(List<RTreeIEntry> newEntries) {
        setParam(ENTRIES, newEntries);
        setParam(ENTRIESTYPE, createTypedOfList(newEntries));

        Envelope boundingBox = newEntries.get(0).getBoundingBox();
        for (RTreeIEntry e : newEntries)
            boundingBox.expandToInclude(e.getBoundingBox());
        setParam(BOUNDINGBOX, boundingBox);
    }

}