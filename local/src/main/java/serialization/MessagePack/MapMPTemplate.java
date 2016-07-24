package serialization.MessagePack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.MessagePackObject;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.Template;
import org.msgpack.Templates;
import org.msgpack.Unpacker;
import org.msgpack.template.TemplateRegistry;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapMPTemplate<U> implements Template {

    private Template mapTemplate;
    private Map<U, Class<?>> valueTypes;

	public MapMPTemplate(Class<U> keyType, Class<?> valueTypes) {
        Map<Object, Class<?>> map = new HashMap<Object, Class<?>>();
        map.put("", valueTypes);
        configure(keyType, (Map<U, Class<?>>) map);
    }

    public MapMPTemplate(Class<U> keyType, Map<U, Class<?>> valueTypes) {
        configure(keyType, valueTypes);
    }

    private void configure(Class<U> keyType, Map<U, Class<?>> valueTypes) {
        this.valueTypes = valueTypes;

        Template keyTemplate = TemplateRegistry.lookup(keyType);
        Template valueTemplate = Templates.TAny;

        if (valueTypes.size() == 1
                && valueTypes.containsKey("")
                && !valueTypes.get("").equals(Object.class))
            valueTemplate = TemplateRegistry.lookup(valueTypes.get(""));

        mapTemplate = Templates.tNullable(Templates.tMap(keyTemplate,
                Templates.tNullable(valueTemplate)));
    }

    private void convert(Map map) {
        // if(map == null || valueTypes.size() == 1)
        // return;
        if (map == null)
            return;

        for (Object key : valueTypes.keySet()) {
            MessagePackObject mpo = (MessagePackObject) map.get(key);
            if (mpo != null)
                map.put(key, mpo.convert(Templates.tNullable(
                        TemplateRegistry.lookup(valueTypes.get(key)))));
        }

    }

    @Override
    public Object convert(MessagePackObject mpobj, Object obj)
            throws MessageTypeException {

        Map map = (Map) mpobj.convert(mapTemplate, obj);

        convert(map);

        return map;
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        Map map = (Map) obj;

        if (valueTypes.size() == 1)
            packer.pack(map, mapTemplate);
        else
            for (Object key : map.keySet()) {
                packer.pack(key);
                packer.pack(map.get(key));
            }
    }

    @Override
    public Object unpack(Unpacker unpacker, Object obj) throws IOException,
            MessageTypeException {

        Map map = (Map) unpacker.unpack(mapTemplate, obj);

        convert(map);

        return map;
    }
}
