package utils;

import java.util.ArrayList;
import java.util.List;

import spatialindex.serialization.HarpiaSerializableObject;

public class HarpiaList extends HarpiaSerializableObject {
    private static final long serialVersionUID = 1L;
    private static final int LIST = 0;
    private static final int AUXINFOLIST = 1;

    public HarpiaList() {
        super();
        setParams(new ArrayList<String>(), new ArrayList<String>());
    }

    public HarpiaList(byte[] rawdata) {
        super(rawdata);
    }

    public void addItem(String key, String infoKey) {
        getList().add(key);
        getAuxList().add(infoKey);

    }

    public List<String> getAuxList() {
        return getListParam(AUXINFOLIST, String.class);
    }

    public List<String> getList() {
        return getListParam(LIST, String.class);
    }

    @Override
    public String toString() {
        return getList().toString();
    }

}