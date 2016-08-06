package utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reflections.Reflections;

public class DataLoaderUtils {

	private static final String METADATA_DATE_FORMAT = "date_format";
	 
	public static Object getObjectFromString(String ColumnName, String value,
            Class<?> clazz)
                    throws NoSuchMethodException, SecurityException,
                    IOException, ParseException {
        return getObjectFromString(ColumnName, value, clazz, null);
    }

    public static Object getObjectFromString(String ColumnName, String value,
            Class<?> clazz, JSONObject dataFormats)
                    throws IOException {
        Constructor<?> constructor = null;

        try {
            if (clazz.equals(Date.class)) {
                String format = null;

                if (dataFormats != null
                        && dataFormats.has(METADATA_DATE_FORMAT))
                    format = getDateFormatFromMetadata(dataFormats, ColumnName);

                if (format != null)
                    return DateUtil.parse(value, format);
                else
                    return DateUtil.parse(value);
            }

            constructor = clazz.getConstructor(String.class);

            return constructor.newInstance(value);
        } catch (InstantiationException | IllegalAccessException
                | ExceptionInInitializerError | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IOException(
                    "Error to get instance of Value Object on getObjectFromString operation."
                            +
                            " The parameters values at error time was: ColumnName = "
                            + ColumnName + "; value = " + value + "; clazz = " +
                            clazz.toString()
                            + (dataFormats == null ? "."
                                    : "; dataFormats = " + dataFormats + "."),
                    e);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IOException(
                    "Error to get String constructor from clazz on getObjectFromString operation."
                            +
                            " The parameters values at error time was: ColumnName = "
                            + ColumnName + "; value = " + value + "; clazz = " +
                            clazz.toString()
                            + (dataFormats == null ? "."
                                    : "; dataFormats = " + dataFormats + "."),
                    e);
        } catch (ParseException e) {
            throw new IOException(
                    "Error to parse date value on getObjectFromString operation."
                            +
                            " The parameters values at error time was: ColumnName = "
                            + ColumnName + "; value = " + value + "; clazz = " +
                            clazz.toString()
                            + (dataFormats == null ? "."
                                    : "; dataFormats = " + dataFormats + "."),
                    e);
        } catch (JSONException e) {
            throw new IOException(
                    "Error to get DateFormat from metadata on getObjectFromString operation."
                            +
                            " The parameters values at error time was: ColumnName = "
                            + ColumnName + "; value = " + value + "; clazz = " +
                            clazz.toString()
                            + (dataFormats == null ? "."
                                    : "; dataFormats = " + dataFormats + "."),
                    e);
        }
    }
    
    private static String getDateFormatFromMetadata(JSONObject dataFormats,
            String dateColumnName) throws JSONException {
        JSONArray aux = null;

        aux = dataFormats.getJSONArray(METADATA_DATE_FORMAT);

        if (aux != null)
            for (int i = 0; i < aux.length(); i++) {
                JSONArray aux2 = aux.getJSONArray(i);
                if (aux2.getString(0).equals(dateColumnName))
                    return aux2.getString(1);
            }

        return null;
    }
    
    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Class<?>[] getClassesOnPackage(String packageName)
            throws ClassNotFoundException {
        return getClassesOnPackage(packageName, Object.class);
    }

    public static <T> Class<T>[] getClassesOnPackage(String packageName,
            Class<T> restriction) throws ClassNotFoundException {

        Reflections reflections = new Reflections(packageName);

        Set<Class<? extends T>> allClasses = reflections
                .getSubTypesOf(restriction);

        @SuppressWarnings("unchecked")
        Class<T>[] classes = new Class[allClasses.size()];

        return allClasses.toArray(classes);
    }

}