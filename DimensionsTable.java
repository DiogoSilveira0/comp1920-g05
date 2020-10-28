import java.util.*;

public class DimensionsTable {

    private HashMap<String, DimensionType> table = new HashMap<String, DimensionType>();

    public boolean exists(String name) {
        return table.containsKey(name);
    }

    public void put(String name, DimensionType dim) {
        table.put(name, dim);
    }

    public DimensionType get(String name) {
        return table.get(name);
    }

    public Collection<DimensionType> values() {
        return table.values();
    }

    public boolean existsUnit(String unit){
        for (DimensionType dimensionType : table.values()) {
            Set<String> units = dimensionType.getKeys();
            if (units.contains(unit))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String r = "";
        for (String name: table.keySet()){
            String key = name.toString();
            String value = table.get(name).toString();
            r += key + " " + value + "\n";
        }
        return r;
    }
}