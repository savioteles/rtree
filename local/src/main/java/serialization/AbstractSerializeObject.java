package serialization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.type.NullType;

import org.msgpack.object.NilType;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractSerializeObject implements Serializable {

	private static final long serialVersionUID = 1L;

	public static <T> Object createTypedOfList(List<T> objList) {
        Class[] list = new Class[objList.size()];

        for (int i = 0; i < objList.size(); i++) {
            T object = objList.get(i);
            if (object != null)
                list[i] = object.getClass();
            else
                list[i] = NullType.class;
        }

        return new ListTyped(list);
    }

	public static Object createTypedOfMap(Map<String, Object> objMap) {
        Map<String, Class<?>> map = new LinkedHashMap<String, Class<?>>();

        for (Entry<String, Object> entry : objMap.entrySet()) {
            Object object = entry.getValue();
            if (object == null)
                map.put(entry.getKey(), NullType.class);
            else
                map.put(entry.getKey(), object.getClass());
        }

        return new MapTyped(map);
    }

    public static <T extends AbstractSerializeObject> T unserialize(
            byte[] rawdata, Class<T> type) {
        if (rawdata == null || type == null || rawdata.length == 0)
            return null;

        return SerializeServiceFactory.getObjectSerializer()
                .unserialize(rawdata, type);
    }

    protected static <T extends AbstractSerializeObject> void unserialize(
            byte[] rawdata, T obj) {
        if (rawdata == null || obj == null || rawdata.length == 0)
            return;

        SerializeServiceFactory.getObjectSerializer().unserialize(rawdata, obj);
    }

    private Object[] params;

    boolean newThreadRequired = false;

    private String requestId = null;

    public AbstractSerializeObject() {

    }

    public AbstractSerializeObject(byte[] rawdata) {
        unserialize(rawdata, this);
    }

    public AbstractSerializeObject(List<Object> params) {
        setParams(params.toArray());
    }

    private void addParams(List<Object> newParams) {
        List<Object> temp = new ArrayList<Object>();

        if (params != null)
            temp.addAll(Arrays.asList(params));

        temp.addAll(newParams);
        setParams(temp.toArray());
    }

    /**
     * Função quadratica, utilize passando todos os atributos de uma vez ao
     * invez de um por um. Para um número muito grande de atributos pode
     * degradar o desempenho.
     *
     * @param newParams
     */
    public void addParams(Object... newParams) {
        addParams(Arrays.asList(newParams));
    }

    public void copyParams(AbstractSerializeObject obj) {
        setParams(Arrays.asList(obj.params).toArray());
    }

    public ParamTyped createParamTyped(Object param) {
        return new ParamTyped(param);
    }

    public <U> List<U> getListParam(int i, Class... itemTypes) {
        if (params == null || params[i] == null)
            return null;

        List<U> obj = SerializeServiceFactory.getObjectSerializer()
                .unserializeToList(params[i],
                        itemTypes);
        params[i] = obj;
        return obj;
    }

    public <U> void getListParam(int i, List<U> list, Class... itemTypes) {
        if (params == null || params[i] == null)
            return;

        List<U> obj = SerializeServiceFactory.getObjectSerializer()
                .unserializeToList(params[i],
                        itemTypes);
        list.addAll(obj);
        params[i] = list;
    }

    public <U, T> Map<U, T> getListParamAsMap(int i, Class<U> keyType,
            U[] keyValues,
            Class<?>... valueTypes) {
        List<T> list = getListParam(i, valueTypes);

        if (list == null)
            return null;

        Map<U, T> obj = new HashMap<U, T>();

        int count = 0;
        for (U key : keyValues)
            obj.put(key, list.get(count++));

        return obj;
    }

    public <T> List<T> getListTyped(int indexType, int indexValue) {
        if (!isSerialObject(indexValue))
            return (List<T>) getParam(indexValue);

        ListTyped listTyped = getParam(indexType, ListTyped.class);
        Class[] classes = listTyped.getTypes();
        return (List<T>) getListParam(indexValue, classes);
    }

    public <T> List<T> getListTyped(int indexType, int indexValue,
            List<T> list) {
        if (!isSerialObject(indexValue))
            return (List<T>) getParam(indexValue);

        ListTyped listTyped = getParam(indexType, ListTyped.class);
        Class[] classes = listTyped.getTypes();
        getListParam(indexValue, list, classes);
        return list;
    }

    public <U, T> Map<U, T> getMapParam(int i, Class<U> keyType,
            Class<T> valueType) {
        if (params == null || params[i] == null)
            return null;

        Map<U, T> obj = SerializeServiceFactory.getObjectSerializer()
                .unserializeToMap(params[i],
                        keyType, valueType);
        params[i] = obj;
        return obj;
    }

    public <U, T> Map<U, T> getMapParam(int i, Class<U> keyType,
            Map<U, Class<?>> valueTypes) {
        if (params == null || params[i] == null)
            return null;

        Map<U, T> obj = SerializeServiceFactory.getObjectSerializer()
                .unserializeToMap(params[i],
                        keyType, valueTypes);
        params[i] = obj;
        return obj;
    }

    public Map<String, Object> getMapTyped(int indexType, int indexValue) {
        if (!isSerialObject(indexValue))
            return (Map<String, Object>) getParam(indexValue);

        MapTyped mapTyped = getParam(indexType, MapTyped.class);
        Map<String, Class<?>> classes = mapTyped.getTypes();
        return getMapParam(indexValue, String.class, classes);
    }

    public Object getParam(int i) {
        if (params == null || params[i] instanceof NilType)
            return null;

        return params[i];
    }

    public <T> T getParam(int i, Class<T> type) {
        if (getSizeParams() <= i)
            throw new RuntimeException("Illegal index to access param.");

        if (params == null || params[i] == null)
            return null;

        T obj = (T) SerializeServiceFactory.getObjectSerializer()
                .unserialize(params[i], type);
        params[i] = obj;
        return obj;
    }

    public Object getParamTyped(int index) {
        ParamTyped paramTyped = getParam(index, ParamTyped.class);
        return paramTyped.getParam();
    }

    public int getPriority() {
        return 2;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getSizeParams() {
        if (params == null)
            return 0;

        return params.length;
    }

    public boolean isNewThreadRequerid() {
        return newThreadRequired;
    }

    public boolean isSerialObject(int i) {
        return SerializeServiceFactory.getObjectSerializer()
                .isSerialObject(params[i]);
    }

    public void notifyUnserialize() {

    }

    public byte[] serialize() {
        return SerializeServiceFactory.getObjectSerializer().serialize(this);
    }

    public void setNewThreadIsRequerid() {
        newThreadRequired = true;
    }

    public void setParam(int i, Object param) {
        this.params[i] = param;
    }

    public void setParams(Object... params) {
        this.params = params;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.params);
    }
}
