package utils;

@SuppressWarnings({ "rawtypes" })
public class TypeDataMaped {
    public String name;
    public Class type;

    public TypeDataMaped(String name, Class type) {
        this.name = name;
        this.type = type;
    }
}