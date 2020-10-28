
public class FloatValue extends Value {

    private double val;
    private static FloatType type = new FloatType();

    public FloatValue(double val) {
        setFloatValue(val);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public void setFloatValue(double val) {
        this.val = val;
    }

    @Override
    public double floatValue() {
        return val;
    }

    @Override
    public String toString() {
        return "" + val;
    }
}
