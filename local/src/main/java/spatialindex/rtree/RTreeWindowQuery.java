package spatialindex.rtree;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Busca de Janela na RTree
 *
 * @author thiago.borges
 *
 */
public class RTreeWindowQuery {

    private volatile static RTreeWindowQuery rTreeWindowQuery;

    public static RTreeWindowQuery getInstance() {
        if (rTreeWindowQuery == null)
            synchronized (RTreeWindowQuery.class) {
                if (rTreeWindowQuery == null)
                    rTreeWindowQuery = new RTreeWindowQuery();
            }

        return rTreeWindowQuery;
    }

    private RTreeWindowQuery() {
    }

    private void findRecursiveInside(RTreeINode node, Geometry region,
            RTreeIRTree rtree, List<IndexObject> list) {
        // TODO: Implementar windowQuery com operador inside
    }

    private void findRecursiveIntersect(RTreeINode node, Geometry region,
            RTreeIRTree rtree, List<IndexObject> list) {

        for (RTreeIEntry entry : node.getEntries())
            if (node.isLeaf()) {
                RTreeIEntryData da = (RTreeIEntryData) entry;
                if (region == null || region.intersects(da.getPolygon()))
                    list.add(da.getObject());
            } else {
                RTreeIEntryDir di = (RTreeIEntryDir) entry;
                if (region == null || di.getBoundingBox()
                        .intersects(region.getEnvelopeInternal()))
                    findRecursiveIntersect(rtree.getNode(di.getChild()), region,
                            rtree, list);
            }
    }

    private void findRecursiveIntersectWithOrderedResults(RTreeINode node,
            Geometry region, RTreeIRTree rtree, TreeSet<IndexObject> list) {

        for (RTreeIEntry entry : node.getEntries())
            if (node.isLeaf()) {
                RTreeIEntryData da = (RTreeIEntryData) entry;
                if (region == null || region.intersects(da.getPolygon()))
                    list.add(da.getObject());
            } else {
                RTreeIEntryDir di = (RTreeIEntryDir) entry;
                if (region == null || di.getBoundingBox()
                        .intersects(region.getEnvelopeInternal()))
                    findRecursiveIntersectWithOrderedResults(
                            rtree.getNode(di.getChild()), region, rtree, list);
            }
    }

    public List<IndexObject> windowQuery(RTreeINode node, Geometry region,
            SearchOperator operator, RTreeIRTree rtree,
            boolean returnResultsOrdered) {

        List<IndexObject> list = new ArrayList<IndexObject>();

        switch (operator) {
        case soInside:
            findRecursiveInside(node, region, rtree, list);
            break;

        case soIntersect:
            if (returnResultsOrdered) {
                TreeSet<IndexObject> tree = new TreeSet<IndexObject>(
                        IndexObject.keyComparator);
                findRecursiveIntersectWithOrderedResults(node, region, rtree,
                        tree);
                list = new ArrayList<IndexObject>(tree);
            } else
                findRecursiveIntersect(node, region, rtree, list);
            break;
        }
        return list;
    }
}
