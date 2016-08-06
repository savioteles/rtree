/**
 * Esta classe insere uma nova entrada na R-Tree.
 *
 * @author S�vio S. Teles de Oliveira
 * @author Thiago Borges de Oliveira
 *
 * @version 1.0
 */

package spatialindex.rtree;

import java.util.UUID;

import spatialindex.rtree.RTreeINode.NodeIsLeafException;

import com.vividsolutions.jts.geom.Envelope;

public class RTreeInsertion {

    /**
     * Este m�todo ajusta todos os ret�ngulos que est�o no caminho da inser��o
     * do novo elemento.
     *
     * @param changedINode
     *            n� em que foi inserido o novo elemento.
     * @param splitedNodes
     *            novos n�s, j� divididos
     * @throws NodeIsLeafException
     */
    public static void adjustTree(RTreeINode changedINode,
            RTreePair splitedNodes, RTreeIRTree rtree)
                    throws NodeIsLeafException {

        RTreeIEntryDir parent = changedINode.getParent();
        if (parent == null) {
            // Root Split
            RTreeNode newRoot = rtree.getNewRoot();

            // muda o nome do antigo root
            String exitingNodeName = UUID.randomUUID().toString();
            splitedNodes.existingNode.setName(exitingNodeName);
            rtree.newNode(exitingNodeName, splitedNodes.existingNode);

            newRoot.addSubNode(splitedNodes.existingNode);
            newRoot.addSubNode(splitedNodes.newNode);

            rtree.setNewRoot(newRoot);

        } else {
            RTreeINode parentOwnerNode = rtree.getNode(parent.getOwnerNode());
            if (!parentOwnerNode.isFull()) {
                changedINode
                        .substEntries(splitedNodes.existingNode.getEntries());
                parentOwnerNode.addSubNode(splitedNodes.newNode);

            } else {
                changedINode
                        .substEntries(splitedNodes.existingNode.getEntries());
                parentOwnerNode.addSubNode(splitedNodes.newNode);
                RTreePair splitedParent = RTreeOverFlowTreatment
                        .split(parentOwnerNode, rtree);
                rtree.newNode(splitedParent.getNewNode().getName(),
                        splitedParent.getNewNode());
                adjustTree(splitedParent.getExistingNode(), splitedParent,
                        rtree);
            }
        }

    }

    public static void adjustTreeMBRs(RTreeINode changedINode,
            RTreeIRTree rtree) {
        changedINode.getParent().setBoundingBox(changedINode.getBoundingBox());
        RTreeINode nodeParent = rtree
                .getNode(changedINode.getParent().getOwnerNode());
        Envelope oldMbr = nodeParent.getBoundingBox();
        Envelope newMbr = new Envelope(
                nodeParent.getEntries().get(0).getBoundingBox());

        /*
         * TODO: Imagino que exista uma forma melhor de fazer isso. Retirar a
         * entry antiga do mbr, recomputando seus limites e adicionar novamente
         * a nova Mbr. Isso evitaria o loop em todas as entradas. De O(M) para
         * O(1).
         */
        for (RTreeIEntry e : nodeParent.getEntries())
            newMbr.expandToInclude(e.getBoundingBox());

        if (!oldMbr.equals(newMbr)) {
            nodeParent.setBoundinBox(newMbr);
            if (nodeParent.getParent() != null) // somente at� a raiz
                adjustTreeMBRs(nodeParent, rtree);
        }
    }

    /**
     * Este m�todo busca o n� apropriado para inserir o novo poligono
     *
     * @param node
     *            N� raiz onde a procura come�a
     * @param polygon
     *            poligono a ser inserido
     * @return o n� onde ser� inserido o novo poligono.
     */
    public static RTreeINode chooseSubTree(RTreeINode node, Envelope newEntryBB,
            RTreeIRTree rtree) {

        RTreeIEntryDir minor;

        if (node.isLeaf())
            return node;
        else {
            assert (node.getEntries().size() > 0);

            minor = null;
            double minorloss = Double.MAX_VALUE;
            int usedcurrent = 0;

            for (RTreeIEntry entry : node.getEntries()) {
                RTreeIEntryDir entryd = (RTreeIEntryDir) entry;
                RTreeNode child = rtree.getNode(entryd.getChild());

                Envelope union = new Envelope(entry.getBoundingBox());
                union.expandToInclude(newEntryBB);
                double loss = R0_LOSS(entry.getBoundingBox(), union);

                if (loss < minorloss) {
                    minor = entryd;
                    minorloss = loss;
                    usedcurrent = child.getItemsCount();
                } else if (minorloss == loss)
                    if (usedcurrent > child.getItemsCount()) {
                        minorloss = loss;
                        usedcurrent = child.getItemsCount();
                    }
            }
        }

        return chooseSubTree(rtree.getNode(minor.getChild()), newEntryBB,
                rtree);
    }

    public static double ENV_HEIGHT(Envelope r) {
        return r.getMaxY() - r.getMinY();
    }

    /**
     * R0 Metrics
     */
    public static double ENV_WIDTH(Envelope r) {
        return r.getMaxX() - r.getMinX();
    }

    public static void insertTree(RTreeINode node, RTreeEntryData entry,
            RTreeINode nodeReinserted, RTreeIRTree rtree)
                    throws NodeIsLeafException {

        // Em qual subtree inserir
        RTreeINode subtree = chooseSubTree(node, entry.getBoundingBox(), rtree);

        if (!subtree.isFull())
            subtree.addEntry(entry);
        else {
            RTreePair splitedNodes = RTreeOverFlowTreatment
                    .overFlowTreatment(subtree, entry, nodeReinserted, rtree);

            if (splitedNodes != null) {
                rtree.newNode(splitedNodes.getNewNode().getName(),
                        splitedNodes.getNewNode());
                adjustTree(subtree, splitedNodes, rtree);
            }

        }

        /*
         * Ajusta o MBR do n� que armazena a entrada pai ap�s a inser��o de um
         * item.
         */
        if (null != subtree.getParent())
            RTreeInsertion.adjustTreeMBRs(subtree, rtree);

    }

    public static double R0_GAIN(Envelope r1, Envelope r2) {
        return (1 - (R0_QUALITY(r1) / R0_QUALITY(r2)));
    }

    public static double R0_LOSS(Envelope r1, Envelope r2) {
        return R0_GAIN(r2, r1);
    }

    public static double R0_QUALITY(Envelope r) {
        return ((1.0 / (ENV_WIDTH(r) * ENV_HEIGHT(r)))
                * Math.pow((Math.min(ENV_WIDTH(r), ENV_HEIGHT(r))
                        / Math.max(ENV_WIDTH(r), ENV_HEIGHT(r))), 0.6));
    }
}
