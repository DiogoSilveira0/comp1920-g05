
public class BooleanValue extends Value {

    private boolean val;
    private static BooleanType type = new BooleanType();

    public BooleanValue(boolean val) {
        setBoolValue(val);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public void setBoolValue(boolean val) {
        this.val = val;
    }

    @Override
    public boolean boolValue() {
        return val;
    }

    @Override
    public String toString() {
        return "" + val;
    }
}
