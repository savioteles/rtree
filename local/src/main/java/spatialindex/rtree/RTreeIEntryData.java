package spatialindex.rtree;

import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

public interface RTreeIEntryData extends RTreeIEntry {

    /**
     * Retorna o nome que identifica a entrada.
     *
     * @return nome que identifica a entrada.
     */
    public String getChild();

    Map<String, String> getCopyKeys();

    String getGeomKey();

    String getLayer();

    int getNumChildGeomPoints();

    IndexObject getObject();

    /**
     * Retorna o poligono que representa uma regi�o armazenada na R-Tree.
     *
     * @return poligono que representa uma regi�o armazenada na R-Tree.
     */
    public Geometry getPolygon();

    Map<String, String> getShadowGeoms();

}
