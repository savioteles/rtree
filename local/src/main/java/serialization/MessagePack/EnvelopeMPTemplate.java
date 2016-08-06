package serialization.MessagePack;

import java.io.IOException;

import org.msgpack.MessagePackObject;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.Template;
import org.msgpack.Unpacker;

import serialization.SerializeServiceFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.ParseException;

public class EnvelopeMPTemplate implements Template {

    @Override
    public Object convert(MessagePackObject mpo, Object obj)
            throws MessageTypeException {
        try {
            return SerializeServiceFactory.getSpatialObjectSerializer()
                    .unserializeEnvelope(
                            mpo.asByteArray());
        } catch (ParseException e) {
            throw new MessageTypeException(e);
        }
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        packer.pack(SerializeServiceFactory.getSpatialObjectSerializer()
                .serialize((Envelope) obj));
    }

    @Override
    public Object unpack(Unpacker unpacker, Object obj) throws IOException,
            MessageTypeException {
        try {
            return SerializeServiceFactory.getSpatialObjectSerializer()
                    .unserializeEnvelope(
                            unpacker.unpack(byte[].class));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
