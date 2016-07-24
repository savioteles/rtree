package spatialindex.rtree;

import java.io.Serializable;

import spatialindex.rtree.RTreeINode.NodeIsLeafException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public interface RTreeIRTree extends Serializable {

    /**
     * Capacidade padr�o de uma RTree. Valor de M.
     */
    int defaultCapacity = 50;

    /**
     * Retorna a capacidade da RTree
     *
     * @return Capacidade dos n�s da RTree instanciada.
     */
    public int getCapacity();

    /**
     * Retorna o nome da RTree atual
     *
     * @return nome da RTree
     */
    public String getName();

    /**
     * Entrega uma nova inst�ncia de INode. � chamado quando a raiz for
     * dividida.
     *
     * @return Nova INode raiz
     */
    public RTreeNode getNewRoot();

    public RTreeNode getNode(String name);

    /**
     * Retorna o n� raiz da �rvore.
     *
     * @return N� raiz
     */
    public RTreeNode getRoot();

    int getSize();

    /**
     * Insere uma nova regi�o na R-Tree. Esta regi�o � representado por um
     * poligono.
     *
     * @param uri
     *            identificador desta regi�o.
     * @param polygon
     *            poligono que representa a regi�o a ser inserida
     * @throws NodeIsLeafException
     */
    public void insert(Envelope bb, IndexObject object,
            RTreeINode nodeReinserted) throws NodeIsLeafException;

    public void newNode(String name, RTreeINode node);

    /**
     * Remove uma entrada da RTree
     *
     * @param uri
     *            uri da entrada.
     * @param polygon
     *            poligono da entrada.
     * @throws NodeIsLeafException
     */
    public void remove(String uri, Geometry polygon) throws NodeIsLeafException;

    /**
     * Notifica a estrutura que o n� raiz foi dividido.
     *
     * @param node
     *            Novo n� raiz
     */
    public void setNewRoot(RTreeNode node);

}