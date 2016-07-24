package serialization;

import org.msgpack.object.NilType;

public class ParamTyped extends AbstractSerializeObject {

    /**
     * 
     */
    private static final long serialVersionUID = -6002272927787839919L;
    private static final int VALUE = 0;
    private static final int TYPE_CODE = 1;

    public ParamTyped() {
    }

    public ParamTyped(Object param) {
        if (param == null) {
            setParams(param, TypeCodec.encodeClass(NilType.class));
            return;
        }
        setParams(param, TypeCodec.encodeClass(param.getClass()));
    }

    public Object getParam() {
        Integer typeCode = getParam(TYPE_CODE, Integer.class);
        return getParam(VALUE, TypeCodec.decodeClass(typeCode));
    }
}
