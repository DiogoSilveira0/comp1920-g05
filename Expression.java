public class Expression {

    private float dep;
    private float ind;

    // y = dep*x + ind - sempre em função da unidade base (x)
    public Expression(float dep, float ind) {
        this.dep = dep;
        this.ind = ind;
    }

    // recebemos o valor na unidade base e retornamos na unidade dependente
    public String calculate(String x) {
        return "(" + dep + "*" + x + "+" + ind + ")"; 
    }

    // recebemos o valor na unidade dependente e retornamos na unidade base
    public String calculate2(String y) {
        return "((" + y + "-" + ind + ")/" + dep + ")";
    }

    public float getDep() {
        return dep;
    }

    public float getInd() {
        return ind;
    }

    @Override
    public String toString() {
        return "Expression{" +
                "dep=" + dep +
                ", ind=" + ind +
                '}';
    }
}
