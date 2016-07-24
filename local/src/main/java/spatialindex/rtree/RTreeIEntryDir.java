package spatialindex.rtree;

public interface RTreeIEntryDir extends RTreeIEntry {

    /**
     * Retorna o n� filho do n� atual que a entrada est� apontando.
     *
     * @return o n� filho do n� atual que a entrada est� apontando.
     */
    public String getChild();

    /**
     * Altera o n�-filho do n� atual que a entrada est� apontando.
     *
     * @param node
     *            novo n�-filho
     */
    public void setChild(String node);
}
