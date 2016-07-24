package spatialindex.rtree;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Envelope;

public interface RTreeIEntry extends Serializable {

    /**
     * Retorna o ret�ngulo de uma entrada de um n� interno.
     *
     * @return ret�ngulo de uma entrada de um n� interno.
     */
    public Envelope getBoundingBox();

    /**
     * Retorna o n� que cont�m a entrada.
     *
     * @return n� que cont�m a entrada.
     */
    public String getOwnerNode();

    /**
     * Altera o bounding box da entrada.
     *
     * @param mbr
     *            novo bounding box
     *
     */
    public void setBoundingBox(Envelope mbr);

    /**
     * Altera o n� dono da entrada
     *
     * @param node
     *            novo n� dono da entrada
     */
    public void setOwnerNode(String node);
}
