package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;
import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class FeatureTypeUtils implements FeatureType, Serializable {

    private static Logger logger = LogUtils.getLogger(FeatureTypeUtils.class);

    private static final long serialVersionUID = 1L;
    public static final TypeDataMaped GID = new TypeDataMaped("geom_key",
            String.class);
    public static final TypeDataMaped MULTIPOINT = new TypeDataMaped("the_geom",
            MultiPoint.class);
    public static final TypeDataMaped POLYGON = new TypeDataMaped("the_geom",
            Polygon.class);
    public static final TypeDataMaped POINT = new TypeDataMaped("the_geom",
            Point.class);
    public static final TypeDataMaped MULTILINE = new TypeDataMaped("the_geom",
            MultiLineString.class);

    public static final HarpiaFeatureType GID_FEATURETYPE = new FeatureTypeUtils(
            "gidfeaturetype", GID).toHarpiaFeatureType();
    public static final HarpiaFeatureType MULTPOINT_FEATURETYPE = new FeatureTypeUtils(
            "gidfeaturetype", GID, MULTIPOINT).toHarpiaFeatureType();
    public static final HarpiaFeatureType POLYGON_FEATURETYPE = new FeatureTypeUtils(
            "gidfeaturetype", GID, POLYGON).toHarpiaFeatureType();;
    public static final HarpiaFeatureType POINT_FEATURETYPE = new FeatureTypeUtils(
            "gidfeaturetype", GID, POINT).toHarpiaFeatureType();;
    public static final HarpiaFeatureType MULTLINE_FEATURETYPE = new FeatureTypeUtils(
            "gidfeaturetype", GID, MULTILINE).toHarpiaFeatureType();;
    public static final String GEOMETRY_FIELD_DEFAULT = "the_geom";

    private static Map<String, Integer> geomHierarchy = new LinkedHashMap<String, Integer>();

    static {
        geomHierarchy.put("Point", 0);
        geomHierarchy.put("MultiPoint", 1);
        geomHierarchy.put("LineString", 2);
        geomHierarchy.put("MultiLineString", 3);
        geomHierarchy.put("Polygon", 4);
        geomHierarchy.put("MultiPolygon", 5);
        geomHierarchy.put("com.vividsolutions.jts.geom.Point", 0);
        geomHierarchy.put("com.vividsolutions.jts.geom.MultiPoint", 1);
        geomHierarchy.put("com.vividsolutions.jts.geom.LineString", 2);
        geomHierarchy.put("com.vividsolutions.jts.geom.MultiLineString", 3);
        geomHierarchy.put("com.vividsolutions.jts.geom.Polygon", 4);
        geomHierarchy.put("com.vividsolutions.jts.geom.MultiPolygon", 5);
    }

    public static boolean checkTypeCompatibility(String name, String type,
            String newType) {
        if (name.equals(GEOMETRY_FIELD_DEFAULT))
            return geomHierarchy.get(type) >= geomHierarchy.get(newType);
        return type.equals(newType);
    }

    public static Class<? extends Geometry> getGeometryType(String geomType) {
        geomType = geomType.toLowerCase();

        if (geomType.contains("mult")) {
            if (geomType.contains("polygon"))
                return MultiPolygon.class;
            else if (geomType.contains("line"))
                return MultiLineString.class;
            else
                return MultiPoint.class;
        } else if (geomType.contains("polygon"))
            return Polygon.class;
        else if (geomType.contains("line"))
            return LineString.class;
        else
            return Point.class;
    }

    private ArrayList<TypeDataMaped> listType;

    private final Name name;

    private List<Filter> restrictions;

    private Map<Object, Object> userData;

    private boolean identified = false;

    private Collection<PropertyDescriptor> descriptors;

    public FeatureTypeUtils(String name) {
        this.name = new NameImpl(name);

        this.listType = new ArrayList<TypeDataMaped>();

        this.restrictions = new ArrayList<Filter>();

        this.userData = new HashMap<Object, Object>();
    }

    public FeatureTypeUtils(String name, TypeDataMaped... types) {
        this(name);

        for (TypeDataMaped type : types)
            addType(type);
    }

    public void addType(TypeDataMaped type) {

        if (type == GID)
            this.identified = true;

        listType.add(type);
    }

    @Override
    public Class<Collection<Property>> getBinding() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {

        try {
            return JtsFactories.sridToCRS(JtsFactories.WGS84_srid, true);
        } catch (NoSuchAuthorityCodeException e) {
            logger.error(e.getMessage(), e);
        } catch (FactoryException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
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
        if (this.descriptors != null)
            for (PropertyDescriptor prop : this.descriptors)
                if (prop.getName().toString().equals(name))
                    return prop;

        return null;
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {

        if (this.descriptors == null) {
            this.descriptors = new ArrayList<PropertyDescriptor>();

            for (TypeDataMaped value : listType) {
                PropertyDescriptor desc;

                if (value.name == null || value.name.isEmpty())
                    throw new RuntimeException(
                            "Try get invalid param descriptor of FeatureType.");

                if (value.type == null)
                    throw new RuntimeException("Nullable type in descriptor "
                            + value.name + " of FeatureType.");

                if (value.name.endsWith("the_geom")) {
                    PropertyType type = new GeometryTypeImpl(
                            new NameImpl(value.type.getSimpleName()),
                            value.type, null, false,
                            false, restrictions, null, null);
                    desc = new GeometryDescriptorImpl((GeometryType) type,
                            new NameImpl(value.name), 0, 1, true, null);
                } else {
                    PropertyType type = new AttributeTypeImpl(
                            new NameImpl(value.name),
                            value.type, false, false, restrictions, null, null);
                    desc = new AttributeDescriptorImpl(
                            (AttributeType) type, new NameImpl(value.name), 0,
                            1, true,
                            null);
                }
                descriptors.add(desc);
            }
        }

        return this.descriptors;
    }

    @Override
    public GeometryDescriptor getGeometryDescriptor() {
        return (GeometryDescriptor) getDescriptor("the_geom");
    }

    @Override
    public Name getName() {
        return this.name;
    }

    @Override
    public List<Filter> getRestrictions() {
        return this.restrictions;
    }

    @Override
    public AttributeType getSuper() {
        try {
            throw new Exception("Metodo não implementado");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Map<Object, Object> getUserData() {
        return this.userData;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return this.identified;
    }

    @Override
    public boolean isInline() {
        try {
            throw new Exception("Metodo não implementado");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public HarpiaFeatureType toHarpiaFeatureType() {
        return new HarpiaFeatureType(this);
    }
}
