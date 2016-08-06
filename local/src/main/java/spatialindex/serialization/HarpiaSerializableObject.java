package spatialindex.serialization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import serialization.AbstractSerializeObject;
import utils.HarpiaFeatureType;

public abstract class HarpiaSerializableObject
        extends AbstractSerializeObject {

    private static final long serialVersionUID = 1L;

    public static <T> List<T> attributesToList(Map<String, T> mapAttr,
            HarpiaFeatureType featureType) {
        if (featureType == null)
            return new ArrayList<T>(mapAttr.values());

        Map<String, Class<?>> mapTypes = featureType.getMapNameTypes();

        if (mapTypes == null)
            return null;

        List<T> list = new ArrayList<T>();

        for (String key : mapTypes.keySet())
            list.add(mapAttr.get(key));

        return list;
    }

    private transient long timeCreated = -1;

    private boolean commitChanges = true;
    private String requestId = null;

    private String taskId = null;

    public HarpiaSerializableObject() {
    }

    public HarpiaSerializableObject(byte[] rawdata) {
        super(rawdata);
    }

    /**
     * Obtem a lista de atributos usando apenas os atributos na lista dada
     *
     * @param i
     * @param featureType
     *            Contem o esquema de colunas da lista de atributos
     * @param featureSubset
     *            Atributos que devem ser retornados
     * @return
     */
    public <T> List<T> getAttributeFromListParam(int i,
            HarpiaFeatureType featureType, HarpiaFeatureType featureSubset) {

        List<String> featureList = featureType.getNameOfAttributes();
        Map<String, Class<?>> typeMap = featureType.getMapNameTypes();

        List<String> attrList = featureSubset.getNameOfAttributes();
        Map<String, Integer> indexMap = new LinkedHashMap<String, Integer>();
        // Nao pega o the_geom
        Class<?>[] attributeTypes = new Class<?>[featureList.size() - 1];

        int index = 0;
        for (String feature : featureList) {
            if (feature.equals("the_geom"))
                continue;

            indexMap.put(feature, index);
            attributeTypes[index] = typeMap.get(feature);
            index++;
        }

        List<T> allAttributes = getListParam(i, attributeTypes);
        List<T> subsetAttributes = new ArrayList<T>();

        // Coloca os attributos do subconjunto na ordem em que eles estao no
        // Featuretype
        for (String attr : attrList) {
            if (attr == "the_geom")
                continue;

            Integer idx = indexMap.get(attr);
            if (idx != null)
                subsetAttributes.add(allAttributes.get(idx));
            else
                subsetAttributes.add(null);
        }

        return subsetAttributes;
    }

    /**
     * Obtem a lista de atributos ignorando os atributos da lista dada
     *
     * @param i
     * @param featureType
     *            Contem o esquema de colunas da lista de atributos
     * @param attributesToIgnore
     *            Atributos que serao ignorados
     * @return
     */
    public <T> List<T> getAttributeIgnoreListParam(int i,
            HarpiaFeatureType featureType, List<String> attributesToIgnore) {

        Map<String, Class<?>> mapTypes = new LinkedHashMap<String, Class<?>>(
                featureType.getMapNameTypes());

        if (attributesToIgnore != null)
            for (String attr : attributesToIgnore)
                mapTypes.remove(attr);

        String[] keyValues = new String[mapTypes.size()];
        Class<?>[] valueTypes = new Class[mapTypes.size()];

        mapTypes.keySet().toArray(keyValues);

        int count = 0;
        for (String key : mapTypes.keySet())
            valueTypes[count++] = mapTypes.get(key);

        return getListParam(i, valueTypes);
    }

    public <T> Map<String, T> getAttributeParam(int i,
            HarpiaFeatureType featureType) {

        Map<String, Class<?>> mapTypes = featureType.getMapNameTypes();

        String[] keyValues = new String[mapTypes.size()];
        Class<?>[] valueTypes = new Class[mapTypes.size()];

        mapTypes.keySet().toArray(keyValues);

        int count = 0;
        for (String key : mapTypes.keySet())
            valueTypes[count++] = mapTypes.get(key);

        return getListParamAsMap(i, String.class, keyValues, valueTypes);
    }

    public long getLatencyToProcess() {
        return (System.currentTimeMillis() - getTimeCreated());
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public boolean isToCommitChanges() {
        return this.commitChanges;
    }

    public void notCommitChanges() {
        this.commitChanges = false;
    }

    @Override
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setTimeCreated(long time) {
        timeCreated = time;
    }
}
