package spatialindex.rtree;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import spatialindex.rtree.RTreeINode.NodeIsLeafException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class RTreeOverFlowTreatment {

    public static class Distribution {

        int numEntriesGroup1;
        double minOverlapValue;
        double minAreaValue;

        public Distribution(int numEntriesGroup1) {
            this.numEntriesGroup1 = numEntriesGroup1;
        }
    }

    static class GenericPair<O1, O2> {
        O1 obj1;
        O2 obj2;
    }

    static class OrderingEntriesByCenter implements Comparator<RTreeIEntry> {

        Coordinate centerPointNode;

        public OrderingEntriesByCenter(Coordinate centerPointNode) {
            this.centerPointNode = centerPointNode;
        }

        @Override
        public int compare(RTreeIEntry e1, RTreeIEntry e2) {
            return sortComparer(
                    e1.getBoundingBox().centre().distance(centerPointNode)
                            - e2.getBoundingBox().centre()
                                    .distance(centerPointNode));

        }

    }

    public static class OrderingEntriesMaxX implements Comparator<RTreeIEntry> {

        @Override
        public int compare(RTreeIEntry e1, RTreeIEntry e2) {
            return sortComparer(e1.getBoundingBox().getMaxX()
                    - e2.getBoundingBox().getMaxX());
        }

    }

    public static class OrderingEntriesMaxY implements Comparator<RTreeIEntry> {

        @Override
        public int compare(RTreeIEntry e1, RTreeIEntry e2) {
            return sortComparer(e1.getBoundingBox().getMaxY()
                    - e2.getBoundingBox().getMaxY());
        }

    }

    public static class OrderingEntriesMinX implements Comparator<RTreeIEntry> {

        @Override
        public int compare(RTreeIEntry e1, RTreeIEntry e2) {
            return sortComparer(e1.getBoundingBox().getMinX()
                    - e2.getBoundingBox().getMinX());
        }

    }

    public static class OrderingEntriesMinY implements Comparator<RTreeIEntry> {

        @Override
        public int compare(RTreeIEntry e1, RTreeIEntry e2) {
            return sortComparer(e1.getBoundingBox().getMinY()
                    - e2.getBoundingBox().getMinY());
        }

    }

    private static int count = 0;

    private static GenericPair<Vector<RTreeIEntry>, Distribution> chooseSplitIndex(
            Vector<Distribution> distributions,
            Vector<RTreeIEntry> entriesMinX, Vector<RTreeIEntry> entriesMaxX,
            Vector<RTreeIEntry> entriesMinY, Vector<RTreeIEntry> entriesMaxY) {

        Distribution cdMinX = chooseSplitOneAxis(distributions, entriesMinX);
        Distribution cdMaxX = chooseSplitOneAxis(distributions, entriesMaxX);
        Distribution cdMinY = chooseSplitOneAxis(distributions, entriesMinY);
        Distribution cdMaxY = chooseSplitOneAxis(distributions, entriesMaxY);

        Distribution cd = cdMinX;
        Vector<RTreeIEntry> entries = entriesMinX;

        if (cdMaxX.minOverlapValue < cd.minOverlapValue ||
                (cdMaxX.minOverlapValue == cd.minOverlapValue
                        && cdMaxX.minAreaValue < cd.minAreaValue)) {
            cd = cdMaxX;
            entries = entriesMaxX;
        }
        if (cdMaxY.minOverlapValue < cd.minOverlapValue ||
                (cdMaxY.minOverlapValue == cd.minOverlapValue
                        && cdMaxY.minAreaValue < cd.minAreaValue)) {
            cd = cdMaxY;
            entries = entriesMaxY;
        }
        if (cdMinY.minOverlapValue < cd.minOverlapValue ||
                (cdMinY.minOverlapValue == cd.minOverlapValue
                        && cdMinY.minAreaValue < cd.minAreaValue)) {
            cd = cdMinY;
            entries = entriesMinY;
        }

        GenericPair<Vector<RTreeIEntry>, Distribution> result = new GenericPair<Vector<RTreeIEntry>, Distribution>();
        result.obj1 = entries;
        result.obj2 = cd;
        return result;
    }

    private static Distribution chooseSplitOneAxis(
            Vector<Distribution> distributions, Vector<RTreeIEntry> entries) {

        double minOverlapValue = Double.MAX_VALUE;
        double minAreaValue = Double.MAX_VALUE;
        Distribution choosedDistribution = null;

        for (int i = 0; i < distributions.size(); i++) {
            Envelope firstDistribution = new Envelope();
            Envelope secondDistribution = new Envelope();

            Distribution distribution = distributions.get(i);

            int j = 0;
            for (RTreeIEntry e : entries) {
                if (j < distribution.numEntriesGroup1)
                    firstDistribution.expandToInclude(e.getBoundingBox());
                else
                    secondDistribution.expandToInclude(e.getBoundingBox());
                j++;
            }

            double overlap = firstDistribution.intersection(secondDistribution)
                    .getArea();
            double area = (firstDistribution.getArea()
                    + secondDistribution.getArea());

            /*
             * Verifica a intersecão dos retângulos desta distribuição é a menor
             * até esse momento
             */
            if ((overlap < minOverlapValue)
                    || (overlap == minOverlapValue && area < minAreaValue)) {
                minOverlapValue = overlap;
                minAreaValue = area;
                choosedDistribution = new Distribution(
                        distribution.numEntriesGroup1);
                choosedDistribution.minOverlapValue = overlap;
                choosedDistribution.minAreaValue = area;
            }
        }
        return choosedDistribution;
    }

    private static Vector<Distribution> defineDistributions(
            int num_distribuitions, int numMinEntries) {

        Vector<Distribution> distributions = new Vector<Distribution>();

        for (int i = 0; i < num_distribuitions; i++)
            distributions.add(new Distribution(numMinEntries + i));

        return distributions;
    }

    public static String getNodeName() {
        return count++ + "";
    }

    /**
     * M�todo que recebe um n� com a capacidade excedida e realiza um tratamento
     * de overflow. Primeiramente o algoritmo tenta reinserir algumas entradas
     * do n�. Se estas entradas forem alocadas em um n� diferente, ent�o n�o �
     * preciso realizar a divis�o. Caso contr�rio, o n� � divido em dois grupos.
     *
     * @param node
     *            n� cheio, onde a entry deve ser adicionada
     * @param entry
     *            entrada a ser adicionada
     * @return um par com os dois n�s criados ap�s a divis�o. Se ocorrer uma
     *         reinser��o de entradas, ent�o � retornado null.
     * @throws URISyntaxException
     */
    public static RTreePair overFlowTreatment(RTreeINode node,
            RTreeIEntryData entry, RTreeINode nodeReinserted,
            RTreeIRTree rtree) {

        if (null != nodeReinserted || null == node.getParent()
                || node.getEntry(0) instanceof RTreeIEntryDir) {
            node.addEntry(entry);
            return split(node, rtree);

        } else {
            reinsertEntries(node, rtree);

            // Ap�s reinser��o, o n� continua cheio, ent�o split
            if (node.isFull()) {
                node.addEntry(entry);
                return split(node, rtree);
            } else {
                node.addEntry(entry);
                return null;
            }
        }
    }

    private static void reinsertEntries(RTreeINode node, RTreeIRTree rtree) {
        Envelope rectangleNode = node.getBoundingBox();
        Coordinate centerPointNode = new Coordinate(rectangleNode.getMinX(),
                rectangleNode.getMinY());
        List<RTreeIEntryData> entriesReinserted = new LinkedList<RTreeIEntryData>();

        Collections.sort(node.getEntries(),
                new OrderingEntriesByCenter(centerPointNode));

        /*
         * Primeiro remove as entradas da �rvore
         */
        for (int i = 0; i < node.getNumEntriesReinsert(); i++) {
            // RTreeTestUtils.countLeaves(rtree.getRoot());

            entriesReinserted.add((RTreeIEntryData) node.getEntries().get(0));
            node.delEntry(0);
        }

        /* Ajusta MBRs da estrutura antes de reinserir itens */
        RTreeInsertion.adjustTreeMBRs(node, rtree);

        /*
         * Depois insere as entradas removidas novamente.
         */
        for (RTreeIEntryData entry : entriesReinserted)
            try {
                rtree.insert(entry.getBoundingBox(), entry.getObject(), node);
            } catch (NodeIsLeafException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

    }

    public static int sortComparer(double i) {
        if (i < 0.0)
            return -1;
        if (i > 0.0)
            return 1;
        return 0;
    }

    public static RTreePair split(RTreeINode node, RTreeIRTree rtree) {

        RTreePair pair = null;

        int num_distribuitions = rtree.getCapacity()
                - 2 * node.getMinimumCapacity() + 2;
        int numMinEntries = node.getMinimumCapacity();

        Vector<RTreeIEntry> entriesMinX = new Vector<RTreeIEntry>();
        Vector<RTreeIEntry> entriesMaxX = new Vector<RTreeIEntry>();
        Vector<RTreeIEntry> entriesMinY = new Vector<RTreeIEntry>();
        Vector<RTreeIEntry> entriesMaxY = new Vector<RTreeIEntry>();
        entriesMinX.addAll(node.getEntries());
        entriesMaxX.addAll(node.getEntries());
        entriesMinY.addAll(node.getEntries());
        entriesMaxY.addAll(node.getEntries());

        Collections.sort(entriesMinX, new OrderingEntriesMinX());
        Collections.sort(entriesMaxX, new OrderingEntriesMaxX());
        Collections.sort(entriesMinY, new OrderingEntriesMinY());
        Collections.sort(entriesMaxY, new OrderingEntriesMaxY());

        Vector<Distribution> distributions = defineDistributions(
                num_distribuitions, numMinEntries);

        GenericPair<Vector<RTreeIEntry>, Distribution> result = chooseSplitIndex(
                distributions, entriesMinX, entriesMaxX, entriesMinY,
                entriesMaxY);

        RTreeINode node2 = new RTreeNode(node.getCapacity(),
                node.isLeaf(), UUID.randomUUID().toString());

        node.getEntries().clear();
        node.setBoundinBox(new Envelope());

        int j = 0;
        Vector<RTreeIEntry> entries = result.obj1;
        for (RTreeIEntry e : entries) {
            if (j < result.obj2.numEntriesGroup1)
                node.addEntry(e);
            else
                node2.addEntry(e);
            j++;
        }

        pair = new RTreePair(node, node2);

        return pair;
    }
}
