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

public class ThrowableMPTemplate implements Template {

    private Exception convert(List<MessagePackObject> unpack) {
        Exception exception = null;
        Exception current = null;

        MessagePackObject mpo;

        int i = 0;
        while (i < unpack.size()) {
            mpo = unpack.get(i++);
            if (mpo == null || mpo.isNil())
                break;

            MessagePackObject[] dto = mpo.asArray();
            int w = 0;
            List<Exception> listthrows = null;

            mpo = dto[w++];
            if (mpo != null && !mpo.isNil()) {
                MessagePackObject[] asArray = mpo.asArray();
                listthrows = new ArrayList<Exception>();
                for (MessagePackObject aggrobj : asArray)
                    listthrows.add(convert(aggrobj.asList()));
            }

            String msg = dto[w++].asString();
            Exception aux = new Exception(msg);

            int j = dto[w++].asInt();

            StackTraceElement array[] = new StackTraceElement[j];
            for (int k = 0; k < j; k++) {
                mpo = dto[w++];
                MessagePackObject[] stearray = mpo.asArray();
                array[k] = new StackTraceElement(stearray[0].asString(),
                        stearray[1].asString(),
                        stearray[2].asString(),
                        stearray[3].asInt());
            }
            aux.setStackTrace(array);

            if (current == null)
                exception = current = aux;
            else {
                current.initCause(aux);
                current = aux;
            }
        }

        return exception;
    }

    @Override
    public Object convert(MessagePackObject mpo, Object obj)
            throws MessageTypeException {

        List<MessagePackObject> unpack = mpo.asList();

        return convert(unpack);
    }

    private List<List<Object>> generate(Throwable t) {
        List<List<Object>> list = new ArrayList<List<Object>>();

        while (t != null) {
            List<Object> dto = new ArrayList<Object>();

            String type = null;
            String devmsg = null;
            List<List<List<Object>>> throwslist = null;

            dto.add(type);
            dto.add(devmsg);

            dto.add(throwslist);

            dto.add("[" + t.getClass().getName() + "]: " + t.getMessage());
            dto.add(t.getStackTrace().length);

            for (StackTraceElement ste : t.getStackTrace()) {
                List<Object> listste = new ArrayList<Object>();
                listste.add(ste.getClassName());
                listste.add(ste.getMethodName());
                listste.add(ste.getFileName());
                listste.add(ste.getLineNumber());
                dto.add(listste);
            }

            list.add(dto);
            t = t.getCause();
        }

        return list;
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        Throwable t = (Throwable) obj;

        List<List<Object>> list = generate(t);

        packer.pack(list, Templates.tNullable(
                Templates.tList(Templates.tNullable(Templates.TAny))));
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object unpack(Unpacker unpacker, Object obj) throws IOException,
            MessageTypeException {

        List<MessagePackObject> unpack = (List<MessagePackObject>) unpacker
                .unpack(Templates.tNullable(
                        Templates.tList(Templates.tNullable(Templates.TAny))));

        return convert(unpack);
    }

}
