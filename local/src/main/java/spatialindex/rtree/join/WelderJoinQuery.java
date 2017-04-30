package spatialindex.rtree.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import spatialindex.rtree.RTreeIEntry;
import spatialindex.rtree.RTreeIEntryData;
import spatialindex.rtree.RTreeIEntryDir;
import spatialindex.rtree.RTreeINode;
import spatialindex.rtree.RTreeIRTree;
import spatialindex.rtree.join.JoinPredicateAnalyzer.JoinAnalyzerObject;
import spatialindex.rtree.join.RTreeJoinQuery.JoinEntryDataPar;
import spatialindex.rtree.join.RTreeJoinQuery.JoinEntryNodePar;
import spatialindex.rtree.join.RTreeJoinQuery.JoinNodePar;
import spatialindex.rtree.join.RTreeJoinQuery.JoinResultPair;
import utils.JtsFactories;
import utils.PropertiesReader;

import com.vividsolutions.jts.geom.Geometry;

public class WelderJoinQuery {
	
	private static final double errorMetersLayer1 = PropertiesReader.getInstance().getErrorInMetersLayer1();
	private static final double errorMetersLayer2 = PropertiesReader.getInstance().getErrorInMetersLayer2();
	private static final String layerName1 = PropertiesReader.getInstance().getLayer1Name();
	private static final String layerName2 = PropertiesReader.getInstance().getLayer2Name();
	
	private int maxIterations;
	private double gamma;
	private double sd;
	private static int numIterationsByStep = PropertiesReader.getInstance().getNumJoinIterationsByStep();
	private static Map<Integer, Double> cacheTc = new HashMap<Integer, Double>();

    public WelderJoinQuery(int maxIterations, double gamma, double sd) {
        this.maxIterations = maxIterations;
        this.gamma = gamma;
        this.sd = sd;
    }

    private class JoinThread implements Runnable {

        private RTreeINode nr;
        private RTreeINode nl;
        private Queue<JoinResultPair> result;

        public JoinThread(RTreeINode nr, RTreeINode nl,
                Queue<JoinResultPair> result) {
            this.nr = nr;
            this.nl = nl;
            this.result = result;
        }

        @Override
        public void run() {
            try {
                for (int j = 0; j < nl.getEntries().size(); j++) {
                    RTreeIEntryData entryNL = (RTreeIEntryData) nl.getEntries()
                            .get(j);
                    
                    for (int k = 0; k < nr.getEntries().size(); k++) {
    
                        RTreeIEntryData entryNR = (RTreeIEntryData) nr
                                .getEntries().get(k);
                        
                        int intersections = 0;
                        int iterations = 0;
                        double p = PropertiesReader.getInstance().getP();
                        double p_star = 0;
                        while(true) {
                            for(int i = 0; i < numIterationsByStep;i++) {
                                iterations++;
                                Geometry nlPolygon = ProbabilisticGeometriesService.getRandomCachedProbabilisticGeometry(entryNL.getPolygon(), layerName1, entryNL.getChild(), iterations, errorMetersLayer1);
                                Geometry nrPolygon = ProbabilisticGeometriesService.getRandomCachedProbabilisticGeometry(entryNR.getPolygon(), layerName2, entryNR.getChild(), iterations, errorMetersLayer2);
                                if(nlPolygon.intersects(nrPolygon)){
                                    intersections++;
                                }
                            }
                            
                            p_star =  (double) intersections / (double) iterations;
                            
                            Double tc = cacheTc.get(iterations);
                            if(tc == null) {
                                tc = JtsFactories.calculateTC(iterations, gamma);
                                cacheTc.put(iterations, tc);
                            }
                            
                            double marginOfError = tc * Math.sqrt(p_star * (1 - p_star) / iterations);
                            
                            double minConfidenceInterval = p_star - marginOfError;
                            double maxConfidenceInterval = p_star + marginOfError;
                            
                            if(iterations >= maxIterations || !(p >= minConfidenceInterval && p <= maxConfidenceInterval))
                                break;
                        }
                        
                        if(p_star > p) 
                            result.add(new JoinResultPair(entryNL.getChild(), entryNR.getChild(), iterations - intersections, intersections));
                    }
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

	
    /**
     * Compara as entradas de dois n�s internos de duas R-Tree diferentes para
     * verificar quais destas entradas cont�m intersec��o. Retorna uma lista que
     * cont�m os filhos das entradas com intersec��o.
     *
     * @param nleft
     *            lista de n�s da primeira R-Tree
     * @param nright
     *            lista de n�s da segunda R-Tree
     * @return uma lista contendo objetos {@link JoinNodePar}. Estes objetos s�o
     *         n�s filhos das entradas de cada R-Tree que cont�m intersec��o
     */
    private List<JoinNodePar> compareDepthNodes(List<RTreeINode> nleft,
            List<RTreeINode> nright, RTreeIRTree rtreeLeft,
            RTreeIRTree rtreeRight) {

        List<JoinNodePar> joinList = new ArrayList<JoinNodePar>();

        if (nleft.size() == nright.size())
            for (int i = 0; i < nright.size(); i++) {
                RTreeINode nl = nleft.get(i);
                RTreeINode nr = nright.get(i);

                /*
                 * Para cada entry l, r do n� a direita/esquerda -> Se
                 * l.boundbox.intersects(r.boundbox) n.add(new
                 * JoinNodePar(child_i_nl, child_i_nr));
                 */
                for (int j = 0; j < nr.getEntries().size(); j++) {
                    RTreeIEntryDir entryNR = (RTreeIEntryDir) nr
                            .getEntries().get(j);

                    for (int k = 0; k < nl.getEntries().size(); k++) {

                        RTreeIEntryDir entryNL = (RTreeIEntryDir) nl
                                .getEntries().get(k);

                        if (JtsFactories.intersects(entryNL.getBoundingBox(), entryNR.getBoundingBox(), errorMetersLayer1, errorMetersLayer2, gamma, sd)   
                                ) {
                            RTreeIEntryDir aux = entryNL;
                            joinList.add(new JoinNodePar(
                                    rtreeLeft.getNode(aux.getChild()),
                                    rtreeRight
                                            .getNode(entryNR.getChild())));
                        }

                    }
                }
            }
        else
            // TODO Lan�ar exce��o. As duas listas devem ter o mesmo tamanho.
            System.out.println("As duas listas devem ter o mesmo tamanho");
        return joinList;
    }

    /**
     * Este m�todo � invocado quando a opera��o de join chega a um ponto no qual
     * a busca em uma R-Tree chegou a folha e na outra R-Tree est� em um n�
     * interno. O m�todo recebe uma lista de entradas dos n�s-folhas da primeira
     * R-Tree que contem intersec��o com a lista de n�s internos da segunda
     * R-Tree. Este m�todo verifica quais entradas dos n�s-folhas cont�m
     * intersec��o com as entradas dos n�s internos da segunda �rvore. Retorna
     * uma lista de objetos {@link JoinEntryNodePar} resultante da intersec��o
     * da entrada do n�-folha da primeira �rvore e o n� interno da segunda
     * �rvore. Este objeto com a entrada do n�-folha da primeira �rvore com o
     * n�-filho da entrada da segunda �rvore.
     *
     * @param entries
     *            conjunto de entradas dos n�s-folhas da primeira �rvore
     * @param nIntern
     *            conjunto de n�s internos da segunda �rvore
     * @return uma lista de objetos {@link JoinEntryNodePar} resultante da
     *         intersec��o da entrada do n�-folha da primeira �rvore e o n�
     *         interno da segunda �rvore. Este objeto com a entrada do n�-folha
     *         da primeira �rvore com o n�-filho da entrada da segunda �rvore.
     */
    private List<JoinEntryNodePar> compareEntryAndNode(
            List<RTreeIEntry> entries, List<RTreeINode> nIntern,
            RTreeIRTree rtree) {

        List<JoinEntryNodePar> result = new LinkedList<JoinEntryNodePar>();

        for (int i = 0; i < entries.size(); i++) {
            RTreeINode node = nIntern.get(i);
            RTreeIEntry entry = entries.get(i);

            for (int j = 0; j < node.getEntries().size(); j++) {

                RTreeIEntryDir entryNode = (RTreeIEntryDir) node
                        .getEntry(j);

                if(JtsFactories.intersects(entry.getBoundingBox(), entryNode.getBoundingBox(), errorMetersLayer1, errorMetersLayer2, gamma, sd))
                    result.add(new JoinEntryNodePar(entry,
                            rtree.getNode(entryNode.getChild())));

            }
        }

        return result;
    }

    /**
     * Este m�todo � invocado quando duas �rvores cont�m alturas diferentes em
     * uma opera��o de join. O m�todo compara uma lista de entradas de um
     * n�-folha de uma R-Tree com uma lista de n�s-folha de outra R-Tree.
     * Verifica quais entradas tem intersec��o com as entradas do n�-folha.
     *
     * @param entries
     *            lista de entradas dos n�s-folhas da primeira �rvore
     * @param nIntern
     *            lista de n�s-folhas da segunda �rvore
     * @return uma lista de objetos {@link JoinEntryDataPar}. Este objeto cont�m
     *         duas entradas de n�s-folhas de duas R-trees diferentes que
     *         apresentam intersec��o.
     */
    private Queue<JoinResultPair> compareEntryDatasAndNode(
            List<RTreeIEntry> entries, List<RTreeINode> nIntern,
            boolean isLeftEmpty) {

        Queue<JoinResultPair> result = new ConcurrentLinkedQueue<RTreeJoinQuery.JoinResultPair>();

        for (int i = 0; i < entries.size(); i++) {
            RTreeINode node = nIntern.get(i);
            RTreeIEntryData entry = (RTreeIEntryData) entries.get(i);

            for (int j = 0; j < node.getEntries().size(); j++) {
                RTreeIEntryData entryNode = (RTreeIEntryData) node.getEntry(j);
                JoinAnalyzerObject keys = null;
                if (JtsFactories.intersects(entry.getBoundingBox()
                        ,entryNode.getBoundingBox(), errorMetersLayer1, errorMetersLayer2, gamma, sd)) {
                    if (isLeftEmpty)
                        keys = JoinPredicateAnalyzer.getChildKey(
                                entry.getCopyKeys(), entry.getChild(),
                                entryNode.getCopyKeys(),
                                entryNode.getShadowGeoms(),
                                entryNode.getChild());
                    else
                        keys = JoinPredicateAnalyzer.getChildKey(
                                entryNode.getCopyKeys(), entryNode.getChild(),
                                entry.getCopyKeys(), entry.getShadowGeoms(),
                                entry.getChild());

                    fillResult(keys, result);
                }
            }
        }

        return result;
    }

    /**
     * Compara dois n�s-folhas de �rvores diferentes e verifica quais entradas
     * destes n�s cont�m intersec��o. As entradas que cont�m intersec��o s�o
     * armazendas no objeto {@link JoinEntryDataPar}. Retorna uma lista de
     * objetos {@link JoinEntryDataPar} contendo um par de entradas que
     * apresentam intersec��o em seus poligonos.
     *
     * @param nleft
     *            n�-folha da primeira �rvore
     * @param nright
     *            n�-folha da segunda �rvore
     * @return uma lista de objetos {@link JoinEntryDataPar} contendo um par de
     *         entradas que apresentam intersec��o em seus poligonos.
     */
    private  Queue<JoinResultPair> compareNodeLeaf(
            List<RTreeINode> nleft, List<RTreeINode> nright) {

        Queue<JoinResultPair> result = new ConcurrentLinkedQueue<RTreeJoinQuery.JoinResultPair>();
        int numSystemThreads = PropertiesReader.getInstance().getNumSystemThreads();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(numSystemThreads, numSystemThreads * 2,
                10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        if (nleft.size() == nright.size())
            for (int i = 0; i < nright.size(); i++) {
                RTreeINode nl = nleft.get(i);
                RTreeINode nr = nright.get(i);
                threadPoolExecutor.execute(new JoinThread(nr, nl, result));
            }

        threadPoolExecutor.shutdown();
        try {
            threadPoolExecutor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
        }
        return result;

    }

    /**
     * Compara as entradas de dois n�s de R-Trees com alturas diferentes e
     * verifica se h� intersec��o entre elas. Compara o n� folha da primeira
     * R-Tree com um n� interno da segunda R-Tree. Retorna uma lista de objetos
     * {@link JoinEntryNodePar}. Este objeto cont�m uma entrada da R-Tree com
     * menor altura e um n� filho da entrada da R-Tree com maior altura.
     *
     * @param nLeaf
     *            n� folha da primeira R-Tree
     * @param nIntern
     *            n� interno da segunda R-Tree
     * @return Retorna uma lista de objetos {@link JoinEntryNodePar}. Este
     *         objeto cont�m uma entrada da R-Tree com menor altura e um n�
     *         filho da entrada da R-Tree com maior altura.
     */
    private List<JoinEntryNodePar> compareUnbalancedTreeNode(
            List<RTreeINode> nLeaf, List<RTreeINode> nIntern,
            RTreeIRTree rtreeLeft, RTreeIRTree rtreeRight) {

        List<JoinEntryNodePar> result = new LinkedList<JoinEntryNodePar>();

        if (nLeaf.size() == nIntern.size())
            for (int i = 0; i < nIntern.size(); i++) {
                RTreeINode nl = nLeaf.get(i);
                RTreeINode ni = nIntern.get(i);

                /*
                 * Para cada entry l, r do n� a direita/esquerda -> Se
                 * l.boundbox.intersects(r.boundbox) n.add(new
                 * JoinNodePar(child_i_nl, child_i_nr));
                 */
                for (int j = 0; j < ni.getEntries().size(); j++) {
                    RTreeIEntryDir entryNR = (RTreeIEntryDir) ni
                            .getEntries().get(j);

                    for (int k = 0; k < nl.getEntries().size(); k++) {

                        RTreeIEntry entryNL = nl.getEntries().get(k);

                        if (JtsFactories.intersects(entryNL.getBoundingBox(),
                                entryNR.getBoundingBox(), errorMetersLayer1, errorMetersLayer2, gamma, sd))
                            result.add(
                                    new JoinEntryNodePar(entryNL,
                                            rtreeRight
                                                    .getNode(entryNR
                                                            .getChild())));

                    }
                }
            }
        else
            // TODO Lan�ar exce��o. As duas listas devem ter o mesmo tamanho.
            System.out.println("As duas listas devem ter o mesmo tamanho");
        return result;
    }

    private void fillResult(JoinAnalyzerObject keys,
            Queue<JoinResultPair> result) {
        // obtem as chaves de r e s.
        String rChild = keys.getrChild();
        String sChild = keys.getsChild();

        synchronized (this) {
        	result.add(new JoinResultPair(rChild, sChild));
        }
    }

    /**
     * Realiza a opera��o de join entre duas R-Trees e retorna um objeto
     * {@link JoinEntryDataPar} contendo um entrada de cada R-Treee. Essas
     * entradas devem apresentar intersec��o entre si.
     *
     * @param left
     *            primeira R-Tree
     * @param right
     *            segunda R-Tree
     * @return objeto {@link JoinEntryDataPar} contendo duas entradas com
     *         intersec��o entre si. Uma entrada � da R-Tree left e a outra da
     *         R-Tree right.
     */
    public Queue<JoinResultPair> joinRtrees(RTreeIRTree left,
            RTreeIRTree right) {
    	
        List<RTreeINode> listl = new LinkedList<RTreeINode>();
        List<RTreeINode> listr = new LinkedList<RTreeINode>();
        List<RTreeIEntry> listEntries = new LinkedList<RTreeIEntry>();
        listl.add(left.getRoot());
        listr.add(right.getRoot());

        // Enquanto h� resultados, desce n�vel a n�vel na �rvore
        while (listl.size() > 0 && listr.size() > 0 && !listl.get(0).isLeaf()
                && !listr.get(0).isLeaf()) {

            // Nos com intersec��o neste n�vel
            List<JoinNodePar> r = compareDepthNodes(listl, listr, left, right);
            listl.clear();
            listr.clear();
            for (JoinNodePar p : r) {
                listl.add(p.left);
                listr.add(p.right);
            }
        }

        /*
         * Verifica se a busca nas duas R-Trees conteve intersec��o at� atingir
         * a folhagem.
         */
        if (listl.size() > 0 && listr.size() > 0)
            /*
             * Este bloco verifica se algumas das duas R-Trees tem altura maior
             * que a outra. Para isso, as listas listl e listr s�o analisadas
             * para saber se alguma n�o cont�m n�s-folhas. Por isso � analisado
             * o primeiro elemento de listl. Se ele for folha ent�o quer dizer
             * que o join chegou a folhagem daquela arvore, caso contr�rio, a
             * �rvore ligada a listl cont�m uma altura maior do que a �rvore
             * ligada a listr.
             */
            if (listl.get(0).isLeaf()) {

                /*
                 * Sabendo que os n�s em listl s�o folhas, o primeiro n� de
                 * listr � analisado para saber se � folha ou n�o. Se for folha,
                 * ent�o as duas �rvores tem a mesma altura. Caso contr�rio, a
                 * �rvore ligada a listr apresenta altura maior que a �rvore
                 * ligada a listl.
                 */
                if (!listr.get(0).isLeaf()) {
                    List<JoinEntryNodePar> r = compareUnbalancedTreeNode(listl,
                            listr, left, right);
                    listr.clear();
                    listl.clear();
                    listEntries.clear();
                    for (JoinEntryNodePar p : r) {
                        listr.add(p.node);
                        listEntries.add(p.entry);
                    }

                    while (listr.size() > 0 && !listr.get(0).isLeaf()) {
                        r.clear();
                        r = compareEntryAndNode(listEntries, listr, right);
                        listEntries.clear();
                        listr.clear();
                        for (JoinEntryNodePar p : r) {
                            listr.add(p.node);
                            listEntries.add(p.entry);
                        }
                    }

                }

            } else {

                List<JoinEntryNodePar> r = compareUnbalancedTreeNode(listr,
                        listl, left, right);
                listl.clear();
                listr.clear();
                listEntries.clear();
                for (JoinEntryNodePar p : r) {
                    listl.add(p.node);
                    listEntries.add(p.entry);
                }

                while (listl.size() > 0 && !listl.get(0).isLeaf()) {
                    r.clear();
                    r = compareEntryAndNode(listEntries, listl, left);
                    listEntries.clear();
                    listl.clear();
                    for (JoinEntryNodePar p : r) {
                        listl.add(p.node);
                        listEntries.add(p.entry);
                    }
                }
            }

        // N�vel das folhas nas duas �rvores - etapa de refinamento
        Queue<JoinResultPair> result;

        /*
         * Verifica se alguma �rvore tem altura maior que a outra. Se isso
         * acontecer, listEntries n�o estar� vazia. Neste caso, ser�o comparados
         * uma entrada da �rvore de menor altura com um n�-folha da �rvore de
         * maior altura. Se listEntries estiver vazia, ent�o compara-se um
         * n�-folha de cada �rvore para verifica se existe intersec��o entre
         * alguma entrada.
         */
        if (listEntries.isEmpty())
            result = compareNodeLeaf(listl, listr);
        else if (listr.isEmpty()) {
        	System.err.println("NÚMERO DE NÍVEIS ERRADO DAS ÁRVORES. POR FAVOR VERIFIQUE A CACIDADE DAS ÁRVORES");
        	System.exit(1);
            result = compareEntryDatasAndNode(listEntries, listl, false);
        } else {
        	System.err.println("NÚMERO DE NÍVEIS ERRADO DAS ÁRVORES. POR FAVOR VERIFIQUE A CACIDADE DAS ÁRVORES");
        	System.exit(1);
            result = compareEntryDatasAndNode(listEntries, listr, true);
        }

        return result;
    }
}
