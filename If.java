import java.util.Scanner;
public class If {  
    public static void main(String[] args){
        int v2 = 1;
        int v3 = 4;
        boolean v1 = v2 < v3;
        int v5 = 9;
        int v6 = 4;
        boolean v4 = v5 > v6;
        boolean v0 = v1 && v4;
        if (v0) {
           String v7 = "ola";
           System.out.println(v7);
        }
        else {
           String v8 = "adeus";
           System.out.println(v8);
        }
    }
}