public class Symbol {

    private final String name;
    private final Type type;
    private Value value;
    private boolean valueDefined;
    private String varName;
    private String dimension = "Adimensional";
    private String unit = "Void";

    public Symbol(String name, Type type) {
        assert name != null;
        assert type != null;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Value getValue() {
        assert getValueDefined();

        return value;
    }

    public void setValue(Value value) {
        assert value != null;

        this.value = value;
    }

    public boolean getValueDefined() {
        return valueDefined;
    }

    public void setValueDefined() {
        valueDefined = true;
    }

    public String getVarName() {
        return varName;
    } 

    public void setVarName(String varName) {
        assert varName != null;

        this.varName = varName;
    }

    public String getDimension() {
        return this.dimension;
    }

    public void setDimension(String dimension) {
        assert dimension != null;

        this.dimension = dimension;
    } 

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        assert unit != null;

        this.unit = unit;
    }   

    public boolean conformsToDimension(String otherDimension) {
        if (this.name.equals(otherDimension)) {
            return true;
        }

        if (this.dimension.equals(otherDimension)) {
            return true;
        }
        return false;
    }
}

