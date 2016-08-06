package serialization;

public class ListTyped extends AbstractSerializeObject {

    /**
     * 
     */
    private static final long serialVersionUID = -4702560484129879674L;
    private static final int LISTOFTYPES = 0;
    private transient Class<?>[] arrayOfClass = null;

    public ListTyped() {
    }

    public ListTyped(Class<?>[] classes) {

        int[] array = new int[classes.length];
        int i = 0;
        for (Class<?> clazz : classes) {
            int value = TypeCodec.encodeClass(clazz);
            try {
                TypeCodec.decodeClass(value);
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Class " + clazz.getName() + " not maped.", e);
            }
            array[i++] = value;
        }

        setParams(array);
    }

    public Class<?>[] getTypes() {
        if (arrayOfClass == null) {

            int[] param = getParam(LISTOFTYPES, int[].class);
            arrayOfClass = new Class[param.length];
            int i = 0;
            for (int value : param)
                arrayOfClass[i++] = TypeCodec.decodeClass(value);
        }

        return arrayOfClass;
    }
}
