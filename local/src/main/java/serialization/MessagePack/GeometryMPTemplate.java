package serialization.MessagePack;

import java.io.IOException;

import org.msgpack.MessagePackObject;
import org.msgpack.MessageTypeException;
import org.msgpack.Packer;
import org.msgpack.Template;
import org.msgpack.Unpacker;

import serialization.SerializeServiceFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class GeometryMPTemplate implements Template {

    @Override
    public Object convert(MessagePackObject mpo, Object arg1)
            throws MessageTypeException {
        try {
            return SerializeServiceFactory.getSpatialObjectSerializer()
                    .unserializeGeometry(
                            mpo.asByteArray());
        } catch (ParseException e) {
            throw new MessageTypeException(e);
        }
    }

    @Override
    public void pack(Packer packer, Object obj) throws IOException {
        packer.pack(SerializeServiceFactory.getSpatialObjectSerializer()
                .serialize((Geometry) obj));
    }

    @Override
    public Object unpack(Unpacker unpacker, Object obj) throws IOException,
            MessageTypeException {
        try {
            return SerializeServiceFactory.getSpatialObjectSerializer()
                    .unserializeGeometry(
                            unpacker.unpack(byte[].class));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
