import java.util.Scanner;
public class While {  
    public static void main(String[] args){
        double v0;
        int v1 = 5;
        v0 = v1;
        double v2;
        int v3 = 10;
        v2 = v3;
        double v5 = v0;
        double v6 = v2;
        boolean v4 = (0.001*v5+0.0) != (0.001*v6+0.0);
        while (v4) {
            String v7 = "bla bla bla";
            System.out.println(v7);

            v5 = v0;v6 = v2;v4 = (0.001*v5+0.0) != (0.001*v6+0.0); 
        }
    }
}