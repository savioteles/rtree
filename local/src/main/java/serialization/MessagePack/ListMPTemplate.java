package serialization.MessagePack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.MessagePackObject;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.Template;
import org.msgpack.Templates;
import org.msgpack.Unpacker;
import org.msgpack.template.TemplateRegistry;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ListMPTemplate implements Template {

    private Template listTemplate;

    private Class[] types;

    public ListMPTemplate(Class<?>... array) {
        this.types = array;

        if (types.length == 1) {
            Template itemTemplate = Templates.TAny;

            if (!types[0].equals(Object.class))
                itemTemplate = TemplateRegistry.lookup(types[0]);

            listTemplate = Templates.tNullable(
                    Templates.tList(Templates.tNullable(itemTemplate)));
        }
    }

    @Override
    public Object convert(MessagePackObject mpobj, Object obj)
            throws MessageTypeException {

        if (listTemplate != null)
            return mpobj.convert(listTemplate, obj);

        if (mpobj == null || mpobj.isNil())
            return null;

        MessagePackObject[] array = mpobj.asArray();

        List<Object> list = getValidObject(obj);

        int i = 0;
        for (Class type : types)
            list.add(array[i++].convert(
                    Templates.tNullable(TemplateRegistry.lookup(type))));

        return list;
    }

    public List<Object> getValidObject(Object object) {
        List<Object> list = (List<Object>) object;

        if (list == null)
            list = new ArrayList<Object>();

        return list;
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        List list = (List) obj;

        if (listTemplate != null)
            packer.pack(list, listTemplate);
        else {
            int i = 0;
            for (Class type : types)
                packer.pack(list.get(i++),
                        Templates.tNullable(TemplateRegistry.lookup(type)));
        }
    }

    @Override
    public Object unpack(Unpacker unpacker, Object obj) throws IOException,
            MessageTypeException {

        if (listTemplate != null)
            return unpacker.unpack(listTemplate, obj);

        List<Object> list = getValidObject(obj);
        ArrayList<MessagePackObject> unpack = (ArrayList<MessagePackObject>) unpacker
                .unpack(Templates.tList(Templates.tNullable(Templates.TAny)));

        int i = 0;
        for (MessagePackObject mpo : unpack) {
            if (mpo == null)
                list.add(null);
            else
                list.add(mpo.convert(Templates
                        .tNullable(TemplateRegistry.lookup(types[i]))));

            i++;
        }

        return list;
    }
}
