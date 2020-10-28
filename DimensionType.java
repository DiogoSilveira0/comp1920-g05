import java.util.*;

public class DimensionType extends Type {
    private Type type;
    private String baseUnit;

    // save compose dimensions
    private HashMap<DimensionType, Integer> form;

    private HashMap<String, Expression> units = new HashMap<String, Expression>();

    protected DimensionType(String name, Type type, String unit) {
        super(name);
        assert type != null;
        assert unit != null;

        this.type = type;
        this.baseUnit = unit;
        this.units.put(unit, new Expression(1,0));
    }

    public DimensionType(String name, HashMap<DimensionType, Integer> form, String unit) {
        super(name);
        assert form != null;
        assert unit != null;

        this.form = form;
        this.baseUnit = unit;
        this.units.put(unit, new Expression(1,0));
    }

    public void addUnit(String newUnit, float dep, float ind) {
        assert newUnit != null;
        this.units.put(newUnit, new Expression(dep,ind));
    }

    public Set<String> getKeys() {
        return units.keySet();
    }

    public String getBaseUnit() {
        return this.baseUnit;
    }

    public Expression getFormGivenUnit(String unit) {
        return this.units.get(unit);
    }

    public boolean checkUnit(String unit) {
        if (this.units.keySet().contains(unit)) {
            return true;
        }
        return false;
    }

    @Override
    public String getType() {
        if (type == null) 
            return "float";
        else if (type.equals("integer"))
            return "Integer";
        else 
            return "float";
   }

    @Override 
    public String toString() {
        return "DimensionType{" +
                "type=" + type +
                ", baseUnit='" + baseUnit + '\'' +
                '}';
    }
}