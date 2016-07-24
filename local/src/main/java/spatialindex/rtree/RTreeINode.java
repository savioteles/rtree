package spatialindex.rtree;

import java.io.Serializable;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

public interface RTreeINode extends Serializable {

    class NodeIsLeafException extends Exception {
		private static final long serialVersionUID = 1L;
    }

    /**
     * Adiciona uma nova entrada no n�.
     *
     * @param entry
     *            entrada a ser adicionada
     */
    public void addEntry(RTreeIEntry entry);

    /**
     *
     * Adiciona um INode filho
     *
     * @param subNode
     *            subNode a ser inserido
     */
    public void addSubNode(RTreeINode subNode) throws NodeIsLeafException;

    /**
     * Remove uma nova entrada no n�.
     *
     * @param entry
     *            entrada a ser removida
     */
    public void delEntry(int entry);

    /**
     * Retorna o ret�ngulo que cobre todos os ret�ngulos das entradas do n�.
     *
     * @return o ret�ngulo que cobre todos os ret�ngulos das entradas do n�.
     */
    public Envelope getBoundingBox();

    /**
     * Retorna a capacidade do INode, ou seja, o M da RTree
     *
     * @return capacidade do INode
     */
    public int getCapacity();

    /**
     * Retorna as entradas do n� atual.
     *
     * @return entradas do n� atual.
     */
    public List<RTreeIEntry> getEntries();

    /**
     * Retorna a i-�sima entrada do n�
     *
     * @param i
     *            �ndice da entrada
     * @return i-�sima entrada
     * @throws IndexOutOfBoundsException
     */
    public RTreeIEntry getEntry(int i) throws IndexOutOfBoundsException;

    /**
     * Retorna a quantidade de itens preenchidos no INode
     *
     * @return quantidade de itens no vetor
     */
    public int getItemsCount();

    public int getMinimumCapacity();

    public String getName();

    /**
     * Retorna um n�mero de entradas padr�o que deve serem reinseridas se
     * ocorrer overflow no n�
     *
     * @return o n�mero de entradas a serem reinseridas
     */
    public long getNumEntriesReinsert();

    /**
     * Retorna o pai do n� atual.
     *
     * @return pai do n� atual.
     */
    public RTreeIEntryDir getParent();

    /**
     * Retorna se o n� est� cheio, observando a capacidade (M) da RTree
     *
     * @return True se n�mero de entradas = M
     */
    public boolean isFull();

    /**
     * Verifica se o n� � folha ou n�o.
     *
     * @return true se o n� for folha, caso contr�rio retorna false.
     */
    public boolean isLeaf();

    /**
     * Informa se j� foram reinseridas entradas neste n� no escopo de um mesmo
     * tratamento de overflow.
     *
     * @return true se tiver ocorrido reinser��o de entradas, caso contr�rio
     *         retorne false.
     */
    public boolean reinsertedEntries();

    /**
     * Substitui o MBR quando os Entries foram alterados
     *
     * @param boundingBox
     *            Novo bounding box que encobre as Entries
     */
    public void setBoundinBox(Envelope boundingBox);

    void setName(String newName);

    /**
     * Preenche o pai do n�
     *
     * @param parent
     *            IEntryDir que � pai do n� atual
     */
    public void setParent(RTreeEntryDir parent);

    /**
     * Altera o estado indicativo do n� de ocorrencia ou n�o de tratamento de
     * overflow
     *
     * @param reinsertedEntries
     *            novo estado
     */
    public void setReinsertedEntries(boolean reinsertedEntries);

    /**
     * Substitui as entradas de um Entry por outras. Utilizado no AjustaArvore
     * para trocar o n� antigo pelos dois novos ap�s a divis�o
     */
    public void substEntries(List<RTreeIEntry> newEntries);
}
