import java.util.*;
import java.lang.Math;

public class SemantickCheckDimensions extends DimensionsBaseVisitor<String> {

   @Override public String visitMain(DimensionsParser.MainContext ctx) {
      Iterator<DimensionsParser.StatContext> iter = ctx.stat().iterator();
      while (iter.hasNext()) {
         visit(iter.next());
      }
      System.out.println(DimensionsParser.dimensionsTable.toString());
      return "SUCCESS";
   }

   @Override public String visitStatDimension(DimensionsParser.StatDimensionContext ctx) {
      visit(ctx.dimension());
      return "SUCCESS";
   }

   @Override public String visitStatAddUnit(DimensionsParser.StatAddUnitContext ctx) {
      visit(ctx.addUnit());
      return "SUCCESS";
   }

   @Override public String visitSimpleDimension(DimensionsParser.SimpleDimensionContext ctx) {
      String dim = ctx.ID(0).getText();
      String value = ctx.value().getText();
      String unit = ctx.ID(1).getText();
      Type type = null;

      if (value.equals("int"))
         type = new IntegerType();
      else 
         type = new FloatType();

      if (DimensionsParser.dimensionsTable.exists(dim)) {
         ErrorHandling.printError(ctx, "Dimension \"" + dim + "\" already declared!");
         System.exit(1);
      } 
      else if (DimensionsParser.dimensionsTable.existsUnit(unit)) {
         ErrorHandling.printError(ctx, "Unit \"" + unit + "\" already in use!");
         System.exit(1);
      }
      else {
         DimensionsParser.dimensionsTable.put(dim, new DimensionType(dim, type, unit));
      }
      return "SUCCESS";
   }

   @Override public String visitComposeDimension(DimensionsParser.ComposeDimensionContext ctx) {
      String dim = ctx.ID(0).getText();
      String compose = visit(ctx.expr());
      String unit = "";
      if (ctx.ID(1) != null)
         unit = ctx.ID(1).getText();

      LinkedHashMap<DimensionType, Integer> form = new LinkedHashMap<>();
      DimensionType newDim;
      Integer pow;
      String [] auxCompose = compose.split("\\*");
      String [] aux;
      
      // when relation is pow
      Integer powGeral = 1;
      if (compose.split(":").length > 3) {
         powGeral = Integer.parseInt(compose.split(":")[3]);
      }

      for (int i = 0; i < auxCompose.length; i++) {
         aux = auxCompose[i].split(":");
         if (!DimensionsParser.dimensionsTable.exists(aux[0])) {
            ErrorHandling.printError(ctx, "Dimension \"" + aux[0] + "\" not declared yet!");
            System.exit(1);
         }

         newDim = DimensionsParser.dimensionsTable.get(aux[0]);
         pow = Integer.parseInt(aux[1])*powGeral;
         form.put(newDim, pow);
      }
      
      if (unit.equals("")) {
         for (DimensionType d : form.keySet()) {
            if (form.get(d) < 0) {
               unit = unit.substring(0, unit.length()-1);
               unit += "/";
            }

            unit += d.getBaseUnit();

            if (form.get(d) != 1 && form.get(d) != -1 ) {
               unit += Math.abs(form.get(d));
            }

            unit += "*";
         }
         unit = unit.substring(0, unit.length() - 1);
      }

      if (DimensionsParser.dimensionsTable.existsUnit(unit)) {
         ErrorHandling.printError(ctx, "Unit \"" + unit + "\" already in use!");
         System.exit(1);
      }

      DimensionsParser.dimensionsTable.put(dim, new DimensionType(dim, form, unit));

      return "SUCCESS";
   }

   @Override public String visitIntValue(DimensionsParser.IntValueContext ctx) {
      return "int";
   }

   @Override public String visitFloatValue(DimensionsParser.FloatValueContext ctx) {
      return "float";
   }

   @Override public String visitAddUnit(DimensionsParser.AddUnitContext ctx) {
      String newUnit = ctx.ID(0).getText();
      String dim = ctx.ID(1).getText();
      String relation = visit(ctx.expr());
      float dep = 0;
      float ind = 0;

      DimensionType dimension = DimensionsParser.dimensionsTable.get(dim);
      
      if (relation.contains("+")) {
         String [] aux = relation.split("\\+");
         relation = aux[0];
         ind = Float.parseFloat(aux[1]);
      }

      // relation depending on relUnit
      String relUnit = "";

      // control if relation only have one dependent unit
      int numRelUnits = 0;

      String [] aux = relation.split("\\*");

      
      for (int i=0; i<aux.length; i++) {
         if (aux[i].matches(".*[a-zA-Z].*")) {
            numRelUnits++;
            if (numRelUnits > 1) {
               ErrorHandling.printError(ctx, "Relation should be ax+b format!");
               System.exit(1);
            }

            if (aux[i].split(":").length!=2) {
               ErrorHandling.printError(ctx, "Relation should be ax+b format!");
               System.exit(1);
            }

            relUnit = aux[i].split(":")[0];
            
            if (!dimension.getBaseUnit().equals(relUnit)) {
               ErrorHandling.printError(ctx, "\"" + relUnit + "\" is not a base unit!");
               System.exit(1);
            }
         }
         else { 
            String [] rel = aux[i].split(":");
            float e1 = Float.parseFloat(rel[0]);
            float e2 = Float.parseFloat(rel[1]);
            dep = (float) Math.pow(e1, e2);    
         } 
      }    
      
      dimension.addUnit(newUnit, dep, ind);

      return "SUCCESS";
   }

   @Override public String visitMulDivExpr(DimensionsParser.MulDivExprContext ctx) {
      String arg1 = visit(ctx.expr(0));
      String arg2 = visit(ctx.expr(1));
      String op = ctx.op.getText();
      
      if (!arg1.contains(":")) {
         arg1 += ":1";
      }
      
      if (!arg2.contains(":") && op.equals("/"))
         arg2 += ":-1";
      else if (!arg2.contains(":") && op.equals("*")) 
         arg2 += ":1";
      else if (arg2.contains(":") && op.equals("/")) {
         String [] argAux = arg2.split(":");
         arg2 = argAux[0] + ":-" + argAux[1];
      }

      return arg1 + "*" + arg2;
   }

   @Override public String visitIdExpr(DimensionsParser.IdExprContext ctx) {
      return ctx.ID().getText();
   }

   @Override public String visitNumberExpr(DimensionsParser.NumberExprContext ctx) {
      return "" + ctx.NUMBER().getText();
   }

   @Override public String visitPowExpr(DimensionsParser.PowExprContext ctx) {
      String base = visit(ctx.expr(0));
      String exp = visit(ctx.expr(1));

      try {
         Integer.parseInt(exp);
      } catch (NumberFormatException ex) {
         ErrorHandling.printError(ctx, "Exponent must be an integer!");
         System.exit(1);
      }
      return base + ":" + exp;
   }

   @Override public String visitParenExpr(DimensionsParser.ParenExprContext ctx) {
      return visit(ctx.expr()) ;
   }

   @Override public String visitAddSubExpr(DimensionsParser.AddSubExprContext ctx) {
      String arg1 = visit(ctx.expr(0));
      String arg2 = visit(ctx.expr(1));
      String op = ctx.op.getText();

      try {
         Float.parseFloat(arg2);
      } catch (NumberFormatException ex) {
         ErrorHandling.printError(ctx, "Independent value should have no unit! Relation should be ax+b format!");
         System.exit(1);
      }

      if (op.equals("-")) {
         if (arg2.contains("-"))
            arg2 = arg2.substring(1, arg2.length());
         else
            arg2 = "-" + arg2;
      }
      return arg1 + "+" + arg2;
   }

}
