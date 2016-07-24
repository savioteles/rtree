package serialization.MessagePack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.MessagePackObject;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.Template;
import org.msgpack.Unpacker;

import serialization.AbstractSerializeObject;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class AbstractSerializeObjectMPTemplate implements Template {

    private Class type;

    public <T extends AbstractSerializeObject> AbstractSerializeObjectMPTemplate(
            Class<T> type) {
        this.type = type;
    }

    @Override
    public Object convert(MessagePackObject mpobj, Object obj)
            throws MessageTypeException {
        AbstractSerializeObject hmo = getValidObject(obj);
        hmo.setParams(((List<Object>) mpobj.convert(List.class)).toArray());
        hmo.notifyUnserialize();
        return hmo;
    }

    public AbstractSerializeObject getValidObject(Object obj)
            throws MessageTypeException {
        AbstractSerializeObject hmo = (AbstractSerializeObject) obj;

        if (hmo == null)
            try {
                hmo = (AbstractSerializeObject) type.newInstance();
            } catch (Exception e) {
                throw new MessageTypeException("Class " + type.getName()
                        + " not contains default constructor acessible.", e);
            }

        return hmo;
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        AbstractSerializeObject aso = (AbstractSerializeObject) obj;
        List<Object> list = new ArrayList<Object>();

        for (int i = 0; i < aso.getSizeParams(); i++) {
            Object item;

            if (aso.isSerialObject(i))
                item = aso.getParam(i);
            else {
                item = aso.getParam(i, Object.class);
                MessagePackSerializer.verifyRegisterObj(item);
            }

            list.add(item);
        }

        packer.pack(list);
    }

    @Override
    public Object unpack(Unpacker unpacker, Object obj)
            throws IOException, MessageTypeException {
        AbstractSerializeObject hmo = getValidObject(obj);
        hmo.setParams(((List<Object>) unpacker.unpack(List.class)).toArray());
        hmo.notifyUnserialize();
        return hmo;
    }
}
