package spatialindex.rtree;

/**
 * Classe com um par de INode's utilizada no processo de Split/Ajuste de �rvore
 *
 * @author savio
 *
 */

public class RTreePair {

    /*
     * TODO: � necess�rio dar um nome que faz mais sentido a estas vari�veis.
     * Aguardar todos os poss�veis usos e depois alterar.
     */
    public RTreeINode existingNode;
    public RTreeINode newNode;

    public RTreePair(RTreeINode existingNode, RTreeINode newNode) {
        this.existingNode = existingNode;
        this.newNode = newNode;
    }

    public RTreeINode getExistingNode() {
        return existingNode;
    }

    public RTreeINode getNewNode() {
        return newNode;
    }

    public void setExistingNode(RTreeINode existingNode) {
        this.existingNode = existingNode;
    }

    public void setNewNode(RTreeINode newNode) {
        this.newNode = newNode;
    }
}
