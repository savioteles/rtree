package spatialindex.rtree;

import java.util.LinkedList;
import java.util.List;

import spatialindex.rtree.RTreeINode.NodeIsLeafException;

import com.vividsolutions.jts.geom.Envelope;

public class RTreeDeletion {

    private static List<RTreeINode> eliminatedNodes = new LinkedList<RTreeINode>();

    public static void condenseTree(RTreeINode node, RTreeINode root,
            RTreeIRTree rtree) throws NodeIsLeafException {
        RTreeINode n = node;
        boolean nodeIsDeleted = false;

        if (n.equals(root)) {
            for (RTreeINode nodeDeleted : eliminatedNodes)
                for (RTreeIEntry entry : nodeDeleted.getEntries())
                    if (entry instanceof RTreeIEntryDir)
                        // TODO Conversar com o Thiago. Entradas de n�s internos
                        // devem ser reinseridos ou apenas ignorados?
                        nodeDeleted = null;
                    else {
                        // TODO Consertar
                        // rTree.insert(((RTreeIEntryData) entry).getChild()
                        // ((RTreeIEntryData) entry).getPolygon(), null);
                    }
        } else {
            RTreeIEntry entryNodeParent = n.getParent();
            RTreeINode nodeParent = rtree
                    .getNode(entryNodeParent.getOwnerNode());

            if (n.getEntries().size() < n.getMinimumCapacity()) {
                nodeIsDeleted = true;
                eliminatedNodes.add(n);
                nodeParent.getEntries().remove(entryNodeParent);
            }

            if (!nodeIsDeleted) {
                entryNodeParent.setBoundingBox(new Envelope());
                n.setBoundinBox(new Envelope());

                for (RTreeIEntry entry : n.getEntries()) {
                    entryNodeParent.getBoundingBox()
                            .expandToInclude(entry.getBoundingBox());
                    n.getBoundingBox().expandToInclude(entry.getBoundingBox());
                }
            }

            condenseTree(nodeParent, root, rtree);
        }
    }

    public static boolean deleteOnRtree(RTreeIEntryData entry,
            RTreeIRTree rtree)
                    throws NodeIsLeafException {

        RTreeIEntryData entryData = (RTreeIEntryData) findLeaf(rtree.getRoot(),
                entry, rtree);

        if (null == entryData)
            return false;

        RTreeINode node = rtree.getNode(entryData.getOwnerNode());
        boolean retorno = node.getEntries().remove(entryData);

        if (retorno)
            ;
        condenseTree(node, rtree.getRoot(), rtree);

        return retorno;
    }

    public static RTreeIEntry findLeaf(RTreeINode node,
            RTreeIEntryData entryData, RTreeIRTree rtree) {

        for (RTreeIEntry entry : node.getEntries())
            if (node.isLeaf()) {
                RTreeIEntryData data1 = (RTreeIEntryData) entry;
                if (data1.getPolygon().equals(entryData.getPolygon())
                        && data1.getChild().equals(entryData.getChild()))
                    return entry;
            } else {
                RTreeIEntryDir dir = (RTreeIEntryDir) entry;

                if (dir.getBoundingBox()
                        .contains(entryData.getPolygon().getEnvelopeInternal()))
                    return findLeaf(rtree.getNode(dir.getChild()), entryData,
                            rtree);
            }

        return null;
    }
}
