import java.io.File;
import org.stringtemplate.v4.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.ParserRuleContext;

public class GeneralCompiler extends GeneralBaseVisitor<ST> {
   protected int varCount = 0;
   protected STGroup stg = null;
   protected String targetLang = "java";

   public boolean validTarget(String outputLang) {
      File stgFile = new File(outputLang + ".stg");
      return (stgFile.exists() && stgFile.isFile() && stgFile.canRead());
   }

   public boolean setTarget(String outputLang) {
      assert validTarget(outputLang);

      this.targetLang = outputLang;
      return true;
   }

   public String newVar() {
      return "v" + varCount++;
   }

   private ST getExprResult(ParserRuleContext ctx, String e1Stats, String e2Stats, String type, String var1, String op, String var2, String varOut) {
         ST result = stg.getInstanceOf("stats");
         result.add("stat", e1Stats);
         result.add("stat", e2Stats);

         ST expression = stg.getInstanceOf("expression");
         expression.add("type", type);
         expression.add("var", varOut);
         expression.add("e1", var1);
         expression.add("op", op);
         expression.add("e2", var2);

         result.add("stat", expression.render());
         return result;
    }
   
   @Override public ST visitMain(GeneralParser.MainContext ctx) {
      assert (validTarget(this.targetLang)); // Check if we have a valid stgFile
      this.stg = new STGroupFile("java.stg");

      ST resultModule = this.stg.getInstanceOf("module"); // Final result
      resultModule.add("stat", visit(ctx.statList()));

      return resultModule;
   }

   @Override public ST visitStatList(GeneralParser.StatListContext ctx) {
      ST res = stg.getInstanceOf("stats"); // Intermediate results of all stats
      for (GeneralParser.StatContext sc : ctx.stat()) {
         res.add("stat", visit(sc));
      }
      return res;
   }

   @Override public ST visitDeclaration(GeneralParser.DeclarationContext ctx) {
      ST decResult = stg.getInstanceOf("declaration");
      String id = ctx.ID().getText();

      Symbol symbol = GeneralParser.symbolTable.get(id);
      symbol.setVarName(newVar()); 
      decResult.add("type", symbol.getType().getType());
      decResult.add("var", symbol.getVarName());

      return decResult;
   }

   @Override public ST visitDecAssign(GeneralParser.DecAssignContext ctx) {
      ST result = stg.getInstanceOf("stats");
      ST decResult = visit(ctx.declaration());

      // Get the assignment
      ST assResult = stg.getInstanceOf("assign");
      String id = ctx.declaration().ID().getText();
      Symbol symbol = GeneralParser.symbolTable.get(id);
      assResult.add("stat", visit(ctx.expr()).render());
      assResult.add("var", symbol.getVarName());
      assResult.add("value", ctx.expr().varName);

      // Write both declaration and assignment in seperate lines
      result.add("stat", decResult.render());
      result.add("stat", assResult.render());
      return result;
   }

   @Override public ST visitOnlyAssign(GeneralParser.OnlyAssignContext ctx) {
      ST assignResult = stg.getInstanceOf("assign");
      String id = ctx.ID().getText();
      Symbol symbol = GeneralParser.symbolTable.get(id);
      assignResult.add("stat", visit(ctx.expr()).render());
      assignResult.add("var", symbol.getVarName());
      assignResult.add("value", ctx.expr().varName);
      return assignResult;
   }

   @Override public ST visitPrint(GeneralParser.PrintContext ctx) {
      ST printResult = this.stg.getInstanceOf("print");

      printResult.add("stat", visit(ctx.expr()));
      printResult.add("expr", ctx.expr().varName);
      if (!ctx.expr().unit.equals("Void"))
         printResult.add("unit", ctx.expr().unit);
      return printResult;
   }

   @Override public ST visitCondition(GeneralParser.ConditionContext ctx) {
      ctx.varName = newVar();

      ST a = visit(ctx.expr(0));
      ST b = visit(ctx.expr(1));

      String u1 = ctx.expr(0).unit;
      String u2 = ctx.expr(1).unit;
      Type type = ctx.expr(1).exprType;
      String e1 = ctx.expr(0).varName;
      String e2 = ctx.expr(1).varName;
      if (type.getClass().getName().equals("DimensionType")) {
         DimensionType t = (DimensionType) type;
         String baseUnit = t.getBaseUnit();
         
         if (!u1.equals(baseUnit)) {
            Expression e = t.getFormGivenUnit(u1);
            e1 = e.calculate(ctx.expr(0).varName);
         }
         System.out.println(u1 + " " + u2);
         if (!u2.equals(baseUnit)) {
            Expression e = t.getFormGivenUnit(u2);
            e2 = e.calculate(ctx.expr(1).varName);
         } 
      }
      return getExprResult(ctx, a.render(), b.render(), "boolean", e1, ctx.op.getText(), e2, ctx.varName);
   }

   @Override public ST visitAddSub(GeneralParser.AddSubContext ctx) {
      ctx.varName = newVar();

      ST a = visit(ctx.expr(0));
      ST b = visit(ctx.expr(1));

      String u1 = ctx.expr(0).unit;
      String u2 = ctx.expr(1).unit;
      Type type = ctx.expr(1).exprType;
      String e1 = ctx.expr(0).varName;
      String e2 = ctx.expr(1).varName;

      if (type.getClass().getName().equals("DimensionType")) {
         DimensionType t = (DimensionType) type;
         String baseUnit = t.getBaseUnit();
         
         if (!u1.equals(baseUnit)) {
            Expression e = t.getFormGivenUnit(u1);
            e1 = e.calculate(ctx.expr(0).varName);
         }
         
         if (!u2.equals(baseUnit)) {
            Expression e = t.getFormGivenUnit(u2);
            e2 = e.calculate(ctx.expr(1).varName);
         } 
      }
      return getExprResult(ctx, a.render(), b.render(), ctx.exprType.getType(), e1, ctx.op.getText(), e2, ctx.varName);
   }

   @Override public ST visitMultDiv(GeneralParser.MultDivContext ctx) {
      ctx.varName = newVar();
      if (ctx.exprType == null) {
         ctx.exprType = new FloatType();
      }
      return getExprResult(ctx, visit(ctx.expr(0)).render(), visit(ctx.expr(1)).render(), ctx.exprType.getType(), ctx.expr(0).varName, ctx.op.getText(), ctx.expr(1).varName, ctx.varName);
   }

   @Override public ST visitIDValue(GeneralParser.IDValueContext ctx) {
      ST result = stg.getInstanceOf("stats");
      ST idVarDecl = stg.getInstanceOf("declaration");

      String id = ctx.ID().getText();
      ctx.varName = newVar(); 
      idVarDecl.add("type", ctx.exprType.getType());
      idVarDecl.add("var", ctx.varName);
      idVarDecl.add("value", GeneralParser.symbolTable.get(id).getVarName());

      result.add("stat", idVarDecl.render());

      return result;
   }

   @Override public ST visitUnary(GeneralParser.UnaryContext ctx) {
      ctx.varName = newVar();

      return getUnary(ctx.exprType, visit(ctx.expr()).render(), ctx.expr().varName, ctx.varName,
         ctx.signal.getText());
   }

   private ST getUnary(Type type, String e1Stats, String var1, String varOut, String operator) {
      ST result = stg.getInstanceOf("stats");
      result.add("stat", e1Stats);

      ST expression = stg.getInstanceOf("unaryOperator");
      expression.add("type", type.getType());
      expression.add("var", varOut);
      expression.add("e1", var1);
      expression.add("operator", operator);

      result.add("stat", expression.render());

      return result;
   }

   @Override public ST visitNot(GeneralParser.NotContext ctx) {
      ctx.varName = newVar();
      return getExprNot(ctx, visit(ctx.expr()).render(), ctx.expr().varName, ctx.varName);
   }

   private ST getExprNot(ParserRuleContext ctx, String e1Stats, String var1, String varOut) {
      ST result = stg.getInstanceOf("stats");
      result.add("stat", e1Stats);

      ST expression = stg.getInstanceOf("expressionNot");
      expression.add("type", "boolean");
      expression.add("var", varOut);
      expression.add("e1", var1);

      result.add("stat", expression.render());
      return result;
   }
   /*
   @Override public ST visitInputValue(GeneralParser.InputValueContext ctx) {
      ST result = stg.getInstanceOf("input");
      ctx.varName = newVar();
      result.add("prompt", ctx.STRING().getText());
      result.add("var", ctx.varName);
      //if (ctx.ID() != null) {
      System.out.println("cucu");
      //}
      return result;
   }*/

   @Override public ST visitBooleanValue(GeneralParser.BooleanValueContext ctx) {
      ST result = stg.getInstanceOf("declaration");
      ctx.varName = newVar();
      result.add("type", "boolean");
      result.add("var", ctx.varName);
      result.add("value", ctx.BOOLEAN().getText());
      return result;
   }

   @Override public ST visitPow(GeneralParser.PowContext ctx) {
      ctx.varName = newVar();
      ST result = stg.getInstanceOf("stats");
      result.add("stat", visit(ctx.expr(0)).render());
      result.add("stat", visit(ctx.expr(1)).render());

      ST powResult = stg.getInstanceOf("powerExpr");
      if (ctx.exprType != null) 
         powResult.add("type", ctx.exprType.getType());
      else 
         powResult.add("type", "float");
      powResult.add("var", ctx.varName);
      powResult.add("e1", ctx.expr(0).varName);
      powResult.add("e2", ctx.expr(1).varName);

      result.add("stat", powResult.render());
      return result;
   }

   @Override public ST visitFloatValue(GeneralParser.FloatValueContext ctx) {
      ST result = stg.getInstanceOf("declaration");
      ctx.varName = newVar();
      result.add("type", "float");
      result.add("var", ctx.varName);
      result.add("value", ctx.FLOAT().getText());
      return result;
   }

   @Override public ST visitStringValue(GeneralParser.StringValueContext ctx) {
      ST result = stg.getInstanceOf("declaration");
      ctx.varName = newVar();
      result.add("type", "String");
      result.add("var", ctx.varName);
      result.add("value", ctx.STRING().getText());
      return result;
   }

   @Override public ST visitIntValue(GeneralParser.IntValueContext ctx) {
      ST result = stg.getInstanceOf("declaration");
      ctx.varName = newVar();
      result.add("type", "integer");
      result.add("var", ctx.varName);
      result.add("value", ctx.INT().getText());
      return result;
   }

   @Override public ST visitAndOr(GeneralParser.AndOrContext ctx) {
      ctx.varName = newVar();
      return getExprResult(ctx, visit(ctx.expr(0)).render(), visit(ctx.expr(1)).render(), "boolean", ctx.expr(0).varName, ctx.op.getText(), ctx.expr(1).varName, ctx.varName);
   }

   @Override public ST visitParen(GeneralParser.ParenContext ctx) {
      ST result = visit(ctx.expr());
      ctx.varName = ctx.expr().varName;
      return result;
   }

   @Override public ST visitConditional(GeneralParser.ConditionalContext ctx) {
      ST res = stg.getInstanceOf("conditional");
      res.add("stat", visit(ctx.expr()).render());
      res.add("var", ctx.expr().varName);
      res.add("ifStats", visit(ctx.statList()).render());
      res.add("elseStats", visit(ctx.elseConditon()).render());
      return res;
   }

   @Override public ST visitElseConditon(GeneralParser.ElseConditonContext ctx) {
      return visit(ctx.statList());
   }

   @Override public ST visitWhileLoop(GeneralParser.WhileLoopContext ctx) {
      ST result = stg.getInstanceOf("whileLoop");

      String exprFirst = visit(ctx.expr()).render();

      String exprNext = "";
      for (int i = 0; i < exprFirst.split("\n").length; i++) {
         exprNext += exprFirst.split("\n")[i].substring(exprFirst.split("\n")[i].indexOf(' ') + 1);
      }
      result.add("stat", exprFirst);
      result.add("var", ctx.expr().varName);
      result.add("checkCondition", "\n" + exprNext);
      result.add("trueStats", visit(ctx.statList()).render());

      return result;
   }
}
