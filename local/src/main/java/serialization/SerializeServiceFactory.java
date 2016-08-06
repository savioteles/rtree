package serialization;

import serialization.MessagePack.MessagePackSerializer;
import serialization.wkb.WKBSpatialObjectSerializer;

public class SerializeServiceFactory {

    private static IObjectSerializer objectSerializer = new MessagePackSerializer();
    // private static IObjectSerializer objectSerializer = new JavaSerializer();
    private static ISpatialObjectSerializer spatialObjectSerializer = new WKBSpatialObjectSerializer();

    public static IObjectSerializer getObjectSerializer() {
        return objectSerializer;
    }

    public static ISpatialObjectSerializer getSpatialObjectSerializer() {
        return spatialObjectSerializer;
    }
}
