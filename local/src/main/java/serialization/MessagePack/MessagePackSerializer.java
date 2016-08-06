package serialization.MessagePack;

import java.util.List;
import java.util.Map;

import org.msgpack.MessagePack;
import org.msgpack.MessagePackObject;
import org.msgpack.Template;
import org.msgpack.Templates;
import org.msgpack.template.DefaultTemplate;
import org.msgpack.template.TemplateRegistry;

import serialization.AbstractSerializeObject;
import serialization.IObjectSerializer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessagePackSerializer implements IObjectSerializer {

    protected static void verifyRegisterObj(Object obj) {
        if (obj == null)
            return;

        if (obj instanceof Object[])
            for (Object item : (Object[]) obj)
                verifyRegisterObj(item);
        else if (obj instanceof List)
            for (Object item : (List) obj)
                verifyRegisterObj(item);
        else if (obj instanceof Map)
            for (Object key : ((Map) obj).keySet()) {
                verifyRegisterObj(key);
                verifyRegisterObj(((Map) obj).get(key));
            }
        else
            verifyRegisterType(obj.getClass());
    }

    protected static void verifyRegisterType(Class... types) {
        // registro de tipos dinamicos
        for (Class type : types)
            if (type != null
                    && AbstractSerializeObject.class.isAssignableFrom(type)
                    && (TemplateRegistry.tryLookup(type) == null
                            || TemplateRegistry
                                    .tryLookup(
                                            type) instanceof DefaultTemplate))
                MessagePack.register(type,
                        new AbstractSerializeObjectMPTemplate(type));
        // if (type instanceof Class<?>)
        // System.err.println("erro");
    }

    public MessagePackSerializer() {
        // Registro de tipos especificos
        MessagePack.register(Geometry.class, new GeometryMPTemplate());
        MessagePack.register(Envelope.class, new EnvelopeMPTemplate());
        MessagePack.register(Throwable.class, new ThrowableMPTemplate());
    }

    private <T> T convertByClass(byte[] rawdata, Class<T> type) {
        verifyRegisterType(type);
        return MessagePack.unpack(rawdata, type);
    }

    private <T> T convertByClass(Object serialObject, Class<?> type) {
        if (!isSerialObject(serialObject))
            return (T) serialObject;

        verifyRegisterType(type);

        return (T) ((MessagePackObject) serialObject).convert(type);
    }

    private <T> T convertByObject(byte[] rawdata, T object) {
        verifyRegisterType(object.getClass());
        return MessagePack.unpack(rawdata, object);
    }

    private <T> T convertByObject(byte[] rawdata, T object, Template template,
            Class type, Class... types) {
        verifyRegisterType(type);
        verifyRegisterType(types);
        return MessagePack.unpack(rawdata, template, object);
    }

    private <T> T convertByTemplate(byte[] rawdata, Template template,
            Class type, Class... types) {
        verifyRegisterType(type);
        verifyRegisterType(types);
        return (T) MessagePack.unpack(rawdata, template);
    }

    private <T> T convertByTemplate(Object serialObject, Template template,
            Class type, Class... types) {
        if (!isSerialObject(serialObject))
            return (T) serialObject;

        verifyRegisterType(type);
        verifyRegisterType(types);

        return (T) ((MessagePackObject) serialObject).convert(template);
    }

    @Override
    public boolean isSerialObject(Object object) {
        return object instanceof MessagePackObject;
    }

    @Override
    public byte[] serialize(Object object) {
        verifyRegisterObj(object);
        return MessagePack.pack(object, Templates.tNullable(Templates.TAny));
    }

    @Override
    public <T> T unserialize(byte[] rawdata, Class<T> type) {
        return convertByClass(rawdata, type);
    }

    @Override
    public <T> void unserialize(byte[] rawdata, T object) {
        convertByObject(rawdata, object);
    }

    @Override
    public <T> T unserialize(Object object, Class<?> type) {
        return (T) convertByClass(object, type);
    }

    @Override
    public <T> List<T> unserializeToList(byte[] rawdata,
            Class<?>... itemTypes) {
        return convertByTemplate(rawdata, new ListMPTemplate(itemTypes), null,
                itemTypes);
    }

    @Override
    public <T> void unserializeToList(byte[] rawdata, List<?> list,
            Class<?>... itemTypes) {
        convertByObject(rawdata, list, new ListMPTemplate(itemTypes), null,
                itemTypes);
    }

    @Override
    public <T> List<T> unserializeToList(Object object, Class<?>... itemTypes) {
        if (!isSerialObject(object))
            return (List<T>) object;
        return convertByTemplate(object, new ListMPTemplate(itemTypes), null,
                itemTypes);
    }

    @Override
    public <U, T> Map<U, T> unserializeToMap(byte[] rawdata, Class<U> keyType,
            Class<?> valueType) {
        return convertByTemplate(rawdata,
                new MapMPTemplate(keyType, valueType), keyType, valueType);
    }

    @Override
    public <U, T> Map<U, T> unserializeToMap(byte[] rawdata, Class<U> keyType,
            Map<U, Class<?>> valueTypes) {
        Class<?>[] array = new Class<?>[valueTypes.size()];
        valueTypes.values().toArray(array);
        return convertByTemplate(rawdata,
                new MapMPTemplate(keyType, valueTypes), keyType, array);
    }

    @Override
    public <U, T> void unserializeToMap(byte[] rawdata, Map<U, ?> map,
            Class<U> keyType, Class<?> valueType) {
        convertByObject(rawdata, map, new MapMPTemplate(keyType, valueType),
                keyType, valueType);
    }

    @Override
    public <U, T> void unserializeToMap(byte[] rawdata, Map<U, ?> map,
            Class<U> keyType, Map<U, Class<?>> valueTypes) {
        Class<?>[] array = new Class<?>[valueTypes.size()];
        valueTypes.values().toArray(array);
        convertByObject(rawdata, map, new MapMPTemplate(keyType, valueTypes),
                keyType, array);
    }

    @Override
    public <U, T> Map<U, T> unserializeToMap(Object object, Class<U> keyType,
            Class<?> valueType) {
        if (!isSerialObject(object))
            return (Map<U, T>) object;
        return convertByTemplate(object, new MapMPTemplate(keyType, valueType),
                keyType, valueType);
    }

    @Override
    public <U, T> Map<U, T> unserializeToMap(Object object, Class<U> keyType,
            Map<U, Class<?>> valueTypes) {
        if (!isSerialObject(object))
            return (Map<U, T>) object;
        Class<?>[] array = new Class<?>[valueTypes.size()];
        valueTypes.values().toArray(array);
        return convertByTemplate(object,
                new MapMPTemplate(keyType, valueTypes), keyType, array);
    }
}
