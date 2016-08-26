package spatialindex.rtree.join;

import java.util.Map;

public class JoinPredicateAnalyzer {

    public static class JoinAnalyzerObject {
        String rChild;
        String sChild;
        Object rChildInfo;
        Object sChildInfo;

        public JoinAnalyzerObject(String rChild, String sChild) {
            this.rChild = rChild;
            this.sChild = sChild;
        }

        public JoinAnalyzerObject(String rChild, String sChild,
                Object rChildInfo, Object sChildInfo) {
            this.rChild = rChild;
            this.sChild = sChild;
            this.rChildInfo = rChildInfo;
            this.sChildInfo = sChildInfo;
        }

        public String getrChild() {
            return rChild;
        }

        public Object getrChildInfo() {
            return rChildInfo;
        }

        public String getsChild() {
            return sChild;
        }

        public Object getsChildInfo() {
            return sChildInfo;
        }

    }

    /**
     * Retorna as chave dos filhos da entrada base e remota. No vetor, a
     * primeira posição é o filho da entrada base e a segunda o filho da entrada
     * remota
     */
    public static JoinAnalyzerObject getChildKey(
            Map<String, String> localCopiesKeys, String localChild,
            Map<String, String> remoteCopiesKeys,
            Map<String, String> remoteShadowsKeys, String remoteChild) {

        if (localCopiesKeys == null && remoteCopiesKeys == null
                && remoteShadowsKeys == null)
            return new JoinAnalyzerObject(localChild, remoteChild);

        // Procura um tile onde as copias de 'local' e 'remote' estão para que
        // não haja transferência na rede. A chave do mapa é o id do tile.
        // Se uma chave está presente nos dois mapas, então os objetos 'local' e
        // 'remote'
        // estão no mesmo tile.
        for (String localKey : localCopiesKeys.keySet())
            if (remoteCopiesKeys.containsKey(localKey)) {
                String localChildAux = localCopiesKeys.get(localKey);

                // Quando o valor de um item no mapa é nulo, então o ponteiro
                // para o filho
                // é o valor de child.
                if (localChildAux == null)
                    localChildAux = localChild;

                String remoteChildAux = remoteCopiesKeys.get(localKey);

                if (remoteChildAux == null)
                    remoteChildAux = remoteChild;

                return new JoinAnalyzerObject(localChildAux, remoteChildAux);
            }

        /*
         * Esse loop busca uma chave de uma sombra de 'remote' que está no mesmo
         * tile que uma cópia de 'local'
         */
        for (String localKey : localCopiesKeys.keySet())
            if (remoteShadowsKeys.containsKey(localKey)) {
                String localChildAux = localCopiesKeys.get(localKey);

                // Quando o valor de um item no mapa é nulo, então o ponteiro
                // para o filho
                // é o valor de child.
                if (localChildAux == null)
                    localChildAux = localChild;

                String remoteChildAux = remoteShadowsKeys.get(localKey);

                return new JoinAnalyzerObject(localChild, remoteChild, null,
                        remoteChildAux);
            }

        return new JoinAnalyzerObject(localChild, remoteChild);
    }

    
}
