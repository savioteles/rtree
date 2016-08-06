package utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.feature.type.Types;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Classe para serializar e desserializar o Schema (FeatureType) dos shapes.
 *
 * @author Roberto Rodrigues Junior
 *
 */
public class HarpiaFeatureType implements FeatureType, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ATTR_GEOMETRY_NAME = "the_geom";
    public static final String ATTR_BOUND_NAME = "bound";
    /**
     * Se o retorno da consulta for um json
     */
    public static final String JSON_NAME = "json";

    public static String createStringType(HarpiaFeatureType hft) {
        int i = 0;
        String stringType = "";
        for (PropertyDescriptor pd : hft.getDescriptors()) {
            String geomPrefix = "";
            String geomSuffix = "";
            // a inserção do asterisco no inicio indica para o construtor
            // de featuretype que este atributo é a geometria default
            // a inserção do srid indica a projeção utilizada
            if (pd.getName().toString().equals(ATTR_GEOMETRY_NAME)) {
                geomPrefix = "*";
                geomSuffix = ":srid=" + JtsFactories
                        .CRSToSrid(hft.getCoordinateReferenceSystem());
            }
            if (i == 0)
                stringType = geomPrefix + pd.getName() + ":"
                        + pd.getType().getBinding().getName() + geomSuffix;
            else
                stringType += "," + geomPrefix + pd.getName() + ":"
                        + pd.getType().getBinding().getName() + geomSuffix;

            i++;
        }

        return stringType;
    }

    public static SimpleFeatureType toSimpleFeatureType(String identification,
            HarpiaFeatureType hft)
                    throws IOException {

        String stringType = createStringType(hft);

        try {
            SimpleFeatureType type = DataUtilities.createType(identification,
                    stringType);

            CoordinateReferenceSystem crs = hft.getCoordinateReferenceSystem();

            return DataUtilities.createSubType(type, null, crs);

        } catch (SchemaException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private transient Map<String, Class<?>> mapNameTypes;

    private transient Map<String, PropertyDescriptor> mapDescriptors;

    private transient List<String> attrStringNames;
    private Name name;
    private List<Name> attrTypeNames;

    private List<Class<?>> bindings;
    private List<Name> attrNames;
    private Class<Collection<Property>> binding;

    private List<Filter> restrictions;
    private Map<Object, Object> userData;
    private int min;
    private int max;
    private boolean nillable;

    private boolean isAbstract;

    private boolean identified;

    private int index_attr_geometry;

    private long version = -1;

    // // FeatureType methods

    protected HarpiaFeatureType() {
        // inicializa variaveis com valores default
        this.attrTypeNames = new LinkedList<Name>();
        this.bindings = new LinkedList<Class<?>>();
        this.attrNames = new LinkedList<Name>();
        index_attr_geometry = -1;
        setMin(0);
        setMax(1);
        setNillable(false);
        setIdentified(false);
    }

    public HarpiaFeatureType(FeatureType schema) {
        this();

        if (schema == null)
            throw new IllegalArgumentException("Esquema não pode ser nulo.");

        List<String> nameAttrs = new LinkedList<String>();
        List<String> typeNames = new LinkedList<String>();
        List<Class<?>> types = new LinkedList<Class<?>>();

        decomposeDescriptors(schema.getDescriptors(), nameAttrs, typeNames,
                types);

        setFeatureTypeParameters(schema.getName().toString(),
                schema.getGeometryDescriptor() != null
                        ? schema.getGeometryDescriptor().getName()
                                .toString()
                        : null,
                schema.getGeometryDescriptor() != null ? schema
                        .getGeometryDescriptor().getMinOccurs() : 0,
                schema.getGeometryDescriptor() != null
                        ? schema.getGeometryDescriptor()
                                .getMaxOccurs()
                        : 1,
                schema.getGeometryDescriptor() != null ? schema
                        .getGeometryDescriptor().isNillable() : false,
                nameAttrs, typeNames, types,
                schema.getBinding(), schema.getRestrictions(),
                schema.isAbstract(),
                schema.getUserData());
    }

    public synchronized boolean addNewAttribute(Class<?> bind, String name) {
        name = name.toLowerCase();

        if (getDescriptor(name) != null)
            return false;

        attrNames.add(new NameImpl(name));
        bindings.add(bind);

        if (Geometry.class.isAssignableFrom(bind))
            attrTypeNames.add(new NameImpl(bind.getSimpleName()));
        else
            attrTypeNames.add(new NameImpl(name));

        // incrementa a versão deste feature type
        incrementVersion();
        cleanDataContext();

        return true;
    }

    public synchronized boolean alterNameOfAttribute(String oldName,
            String newName) {
        oldName = oldName.toLowerCase();
        newName = newName.toLowerCase();

        if (getDescriptor(oldName) == null)
            return false;

        if (oldName.equals(newName))
            return true;

        int index = getNameOfAttributes().indexOf(oldName);

        attrTypeNames.remove(index);
        attrNames.remove(index);

        attrTypeNames.add(index, new NameImpl(newName));
        attrNames.add(index, new NameImpl(newName));

        // incrementa a versão deste feature type
        incrementVersion();
        cleanDataContext();

        return true;
    }

    /*
     * Limpa o contexto temporario do objeto
     */
    private void cleanDataContext() {
        mapNameTypes = null;
        mapDescriptors = null;
        attrStringNames = null;
    }

    public boolean compatible(HarpiaFeatureType schema) {
        if (schema == null)
            return false;

        FeatureType ft = schema;
        Collection<PropertyDescriptor> descriptors = ft.getDescriptors();
        Collection<PropertyDescriptor> localFtDescriptors = this
                .getDescriptors();

        if (descriptors.size() != localFtDescriptors.size())
            return false;

        Iterator<PropertyDescriptor> iterator = descriptors.iterator();
        Iterator<PropertyDescriptor> localFtiterator = localFtDescriptors
                .iterator();
        for (int i = 0; i < descriptors.size(); i++) {
            PropertyDescriptor p = iterator.next();
            String name = p.getName().toString();
            String type = p.getType().getBinding().getSimpleName().toString();

            PropertyDescriptor p1 = localFtiterator.next();
            String localFtName = p1.getName().toString();
            String localFtType = p1.getType().getBinding().getSimpleName()
                    .toString();

            if (!name.equals(localFtName) || !FeatureTypeUtils
                    .checkTypeCompatibility(name, type, localFtType))
                return false;
        }
        return true;
    }

    private void decomposeDescriptors(
            Collection<PropertyDescriptor> descriptors,
            List<String> nameAttrs, List<String> typeNames,
            List<Class<?>> types) {

        for (PropertyDescriptor prop : descriptors) {
            nameAttrs.add(prop.getName().toString());
            typeNames.add(prop.getType().getName().toString());

            types.add(prop.getType().getBinding());
        }

    }

    public synchronized boolean deleteNameOfAttributes(String name) {
        name = name.toLowerCase();

        if (getDescriptor(name) == null)
            return false;

        int index = getNameOfAttributes().indexOf(name);

        attrTypeNames.remove(index);
        attrNames.remove(index);
        bindings.remove(index);

        // incrementa a versão deste feature type
        incrementVersion();
        cleanDataContext();

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        FeatureType ft = (FeatureType) obj;
        Collection<PropertyDescriptor> descriptors = ft.getDescriptors();
        Collection<PropertyDescriptor> localFtDescriptors = this
                .getDescriptors();

        if (descriptors.size() != localFtDescriptors.size())
            return false;

        Iterator<PropertyDescriptor> iterator = descriptors.iterator();
        Iterator<PropertyDescriptor> localFtiterator = localFtDescriptors
                .iterator();
        for (int i = 0; i < descriptors.size(); i++) {
            PropertyDescriptor p = iterator.next();
            String name = p.getName().toString();
            String type = p.getType().getBinding().toString();

            PropertyDescriptor p1 = localFtiterator.next();
            String localFtName = p1.getName().toString();
            String localFtType = p1.getType().getBinding().toString();

            if (!name.equals(localFtName) || !type.equals(localFtType))
                return false;
        }
        return true;
    }

    @Override
    public Class<Collection<Property>> getBinding() {
        return binding;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        try {
            return JtsFactories.sridToCRS(JtsFactories.WGS84_srid, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InternationalString getDescription() {
        return null;
    }

    @Override
    public PropertyDescriptor getDescriptor(Name name) {
        return getDescriptor(name.toString());
    }

    @Override
    public PropertyDescriptor getDescriptor(String name) {
        return getMapDescriptors().get(name.toLowerCase());
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {
        return getMapDescriptors().values();
    }

    @Override
    public GeometryDescriptor getGeometryDescriptor() {
        return (GeometryDescriptor) getMapDescriptors().get(ATTR_GEOMETRY_NAME);
    }

    public synchronized int getGeometryIndex() {
        return index_attr_geometry;
    }

    // //// Encapsuled fields

    /*
     * Gera mapa de propertyDescriptor. O mapa garante a ordenação dos itens.
     */
    private synchronized Map<String, PropertyDescriptor> getMapDescriptors() {
        if (mapDescriptors == null) {
            mapDescriptors = new LinkedHashMap<String, PropertyDescriptor>();
            for (int i = 0; i < attrTypeNames.size(); i++) {
                PropertyDescriptor desc;

                if (Geometry.class.isAssignableFrom(bindings.get(i))) {
                    PropertyType type = new GeometryTypeImpl(
                            attrTypeNames.get(i), bindings.get(i),
                            null, false, false, restrictions, null, null);
                    desc = new GeometryDescriptorImpl((GeometryType) type,
                            attrNames.get(i),
                            getMin(), getMax(), isNillable(), null);
                } else {
                    PropertyType type = new AttributeTypeImpl(
                            attrTypeNames.get(i),
                            bindings.get(i), false, false, restrictions, null,
                            null);
                    desc = new AttributeDescriptorImpl((AttributeType) type,
                            attrNames.get(i),
                            getMin(), getMax(), true, null);
                }
                mapDescriptors.put(attrNames.get(i).toString(), desc);
            }
        }

        return mapDescriptors;
    }

    public synchronized Map<String, Class<?>> getMapNameTypes() {
        if (mapNameTypes == null) {
            mapNameTypes = new LinkedHashMap<String, Class<?>>();

            for (String name : getNameOfAttributes())
                mapNameTypes.put(name,
                        getMapDescriptors().get(name).getType().getBinding());
        }

        return mapNameTypes;
    }

    private int getMax() {
        return max;
    }

    private int getMin() {
        return min;
    }

    @Override
    public Name getName() {
        return this.name;
    }

    public synchronized List<String> getNameOfAttributes() {
        if (attrStringNames == null)
            attrStringNames = new ArrayList<String>(
                    getMapDescriptors().keySet());

        return attrStringNames;
    }

    @Override
    public List<Filter> getRestrictions() {
        return this.restrictions;
    }

    @Override
    public AttributeType getSuper() {
        return null;
    }

    @Override
    public Map<Object, Object> getUserData() {
        return this.userData;
    }

    // // auxiliar methods

    public long getVersion() {
        return version;
    }

    private void incrementVersion() {
        version++;
    }

    @Override
    public boolean isAbstract() {
        return this.isAbstract;
    }

    @Override
    public boolean isIdentified() {
        return this.identified;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    private boolean isNillable() {
        return nillable;
    }

    public Map<String, Object> mountFeatureMap(String layerName,
            Map<String, Object> values) throws Exception {

        if (values.keySet().size() != getNameOfAttributes().size())
            throw new ArrayIndexOutOfBoundsException(
                    "The number of values sent is not equal to schema. The schema defined "
                            + getNameOfAttributes().size()
                            + " attributes but was sent "
                            + values.keySet().size() + " values.");

        String errorMessage = "";

        for (String name : values.keySet())
            try {
                AttributeDescriptor descriptor = (AttributeDescriptor) getDescriptor(
                        name);

                if (name == null)
                    throw new IllegalArgumentException(
                            "The attribute with name " + name
                                    + " doesn't exists.");

                Types.validate(descriptor, values.get(name));
            } catch (IllegalArgumentException e) {
                try {
                    AttributeDescriptor descriptor = (AttributeDescriptor) getDescriptor(
                            name);
                    Class<?> clazz = descriptor.getType().getBinding();
                    if (!clazz.equals(String.class)) {
                        Object obj = values.get(name);
                        Object value = DataLoaderUtils.getObjectFromString(name,
                                String.valueOf(obj), clazz);
                        values.put(name, value);
                    } else
                        errorMessage += "The attribute with name " + name
                                + " did not meet the schema requirements.\n";
                } catch (Exception e1) {
                    errorMessage += "The attribute with name " + name
                            + " did not meet the schema requirements.\n";
                }
            } catch (Exception e) {
                errorMessage += "Error to validate the attribute with name "
                        + name + "\n";
            }

        if (!errorMessage.isEmpty())
            throw new Exception(errorMessage);

        return values;
    }

    public synchronized boolean replaceAttribute(Class<?> bind,
            int indexOfObj) {

        bindings.remove(indexOfObj);
        bindings.add(indexOfObj, bind);

        if (Geometry.class.isAssignableFrom(bind)) {
            attrTypeNames.remove(indexOfObj);
            attrTypeNames.add(indexOfObj, new NameImpl(bind.getSimpleName()));
        }

        // incrementa a versão deste feature type
        incrementVersion();
        cleanDataContext();

        return true;
    }

    protected void setFeatureTypeParameters(String name,
            String nameGeometryDescriptor, int min,
            int max, boolean nillable, List<String> nameAttrs,
            List<String> typeNames,
            List<Class<?>> types, Class<Collection<Property>> binding,
            List<Filter> restrictions,
            boolean isAbstract, Map<Object, Object> userData) {

        // ajusta dados especificos do featureType
        this.name = new NameImpl(name);
        this.binding = binding;
        this.restrictions = restrictions;
        this.isAbstract = isAbstract;
        this.userData = userData;

        // obtem informações dos atributos
        for (int i = 0; i < nameAttrs.size(); i++) {
            String nameAttr = nameAttrs.get(i).toLowerCase();
            String typeName = typeNames.get(i).toLowerCase();
            Class<?> type = types.get(i);

            // converte o nome do geometryDescriptor para ATTR_GEOMETRY_NAME
            if (nameGeometryDescriptor != null
                    && nameGeometryDescriptor.toLowerCase().equals(nameAttr)) {
                nameAttr = ATTR_GEOMETRY_NAME;

                // ajusta dados especificos da geometria
                setMin(min);
                setMax(max);
                setNillable(nillable);

                index_attr_geometry = i;
            }

            // verifica se o nom ATTR_GEOMETRY_NAME já está sendo utilizado por
            // outro atributo
            if (nameAttr.equals(ATTR_GEOMETRY_NAME)
                    && attrNames.contains(ATTR_GEOMETRY_NAME))
                throw new RuntimeException(
                        "FeatureType not convertable to HarpiaFeatureType. Atribute \""
                                + ATTR_GEOMETRY_NAME
                                + "\" exist and not is a GeometryDescriptor.");

            attrNames.add(new NameImpl(nameAttr));
            attrTypeNames.add(new NameImpl(typeName));
            bindings.add(type);
            setIdentified(true);
        }

        incrementVersion();
    }

    private void setIdentified(boolean value) {
        this.identified = value;
    }

    private void setMax(int max) {
        this.max = max;
    }

    private void setMin(int min) {
        this.min = min;
    }

    public void setName(String newName) {
        name = new NameImpl(newName);
        incrementVersion();
    }

    private void setNillable(boolean isNillable) {
        this.nillable = isNillable;
    }
}
