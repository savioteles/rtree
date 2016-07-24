package serialization;

import java.util.List;
import java.util.Map;

public interface IObjectSerializer {

    public boolean isSerialObject(Object serialObject);

    public byte[] serialize(Object object);

    public <T> T unserialize(byte[] rawdata, Class<T> type);

    public <T> void unserialize(byte[] rawdata, T obj);

    public <T> T unserialize(Object serialObject, Class<?> type);

    public <T> List<T> unserializeToList(byte[] rawdata, Class<?>... itemTypes);

    public <T> void unserializeToList(byte[] rawdata, List<?> list,
            Class<?>... itemTypes);

    public <T> List<T> unserializeToList(Object serialObject,
            Class<?>... itemTypes);

    public <U, T> Map<U, T> unserializeToMap(byte[] rawdata, Class<U> keyType,
            Class<?> valueType);

    public <U, T> Map<U, T> unserializeToMap(byte[] rawdata, Class<U> keyType,
            Map<U, Class<?>> valueTypes);

    public <U, T> void unserializeToMap(byte[] rawdata, Map<U, ?> map,
            Class<U> keyType, Class<?> valueType);

    public <U, T> void unserializeToMap(byte[] rawdata, Map<U, ?> map,
            Class<U> keyType, Map<U, Class<?>> valueTypes);

    public <U, T> Map<U, T> unserializeToMap(Object serialObject,
            Class<U> keyType, Class<?> valueType);

    public <U, T> Map<U, T> unserializeToMap(Object serialObject,
            Class<U> keyType, Map<U, Class<?>> valueTypes);

}
