package spatialindex.rtree;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

public class RTreeDiagnostic {

    static public void printEntriesMBRsRecursive(RStar rstar, RTreeIEntry e,
            int nivel) throws Exception {
        Envelope mbr = e.getBoundingBox();
        System.out.printf(
                "{'type': 'Feature', 'geometry': {'type': 'Polygon', 'coordinates': [[[%f, %f], [%f, %f], [%f, %f], [%f, %f]]]}, 'properties': {'nivel': '%d'}},",
                mbr.getMinX(), mbr.getMinY(), mbr.getMaxX(), mbr.getMinY(),
                mbr.getMaxX(), mbr.getMaxY(), mbr.getMinX(), mbr.getMaxY(),
                nivel);

        if (e instanceof RTreeIEntryDir) {
            RTreeIEntryDir ed = (RTreeIEntryDir) e;
            RTreeNode rtn = rstar.getNode(ed.getChild());
            for (RTreeIEntry ech : rtn.getEntries())
                printEntriesMBRsRecursive(rstar, ech, nivel + 1);
        }
    }

    static public double printOverlapRecursive(RStar rstar, RTreeIEntry e) {
        double overlap = 0.0;
        if (e instanceof RTreeIEntryDir) {
            RTreeIEntryDir ed = (RTreeIEntryDir) e;
            RTreeNode rtn = rstar.getNode(ed.getChild());
            for (RTreeIEntry ech : rtn.getEntries())
                overlap += printOverlapRecursive(rstar, ech);

            List<RTreeIEntry> entries = rtn.getEntries();
            for (int i = 0; i < entries.size(); i++)
                for (int j = i + 1; j < entries.size(); j++) {
                    Envelope imbr = entries.get(i).getBoundingBox();
                    Envelope jmbr = entries.get(j).getBoundingBox();
                    overlap += imbr.intersection(jmbr).getArea();
                }
            return overlap;
        } else
            return 0.0;
    }
}
