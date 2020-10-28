import java.io.*;
import java.text.CharacterIterator;
import java.util.Iterator;

import javax.print.DocFlavor.STRING;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class SemanticCheckGeneral extends GeneralBaseVisitor<Boolean> {
   private final FloatType floatType = new FloatType();
   private final IntegerType integerType = new IntegerType();
   private final BooleanType booleanType = new BooleanType();
   private final StringType stringType = new StringType();

   @Override public Boolean visitMain(GeneralParser.MainContext ctx) {
      Boolean res = true;
      if (ctx.importDimensions() != null) {
         Iterator<GeneralParser.ImportDimensionsContext> iter = ctx.importDimensions().iterator();
         while (iter.hasNext())
            res = visit(iter.next());
      }
      if (!ErrorHandling.error()) {
         res = visit(ctx.statList());
      }
      return res;
   }

   @Override public Boolean visitImportDimensions(GeneralParser.ImportDimensionsContext ctx) {
      Boolean res = true;
      String fileName = ctx.STRING().getText();
      fileName = fileName.substring(1, fileName.length()-1) + ".txt";
      InputStream in_stream = null;
      CharStream input = null;
      try {
         in_stream = new FileInputStream(new File(fileName));
         input = CharStreams.fromStream(in_stream);
      } catch (IOException e) {
         ErrorHandling.printError(ctx, "ERROR: Reading file!");
         System.exit(1);
      }

      if (res) {
         DimensionsLexer lexer = new DimensionsLexer(input);
         CommonTokenStream tokens = new CommonTokenStream(lexer);
         DimensionsParser parser = new DimensionsParser(tokens);
         ParseTree tree = parser.main();

         if (parser.getNumberOfSyntaxErrors() == 0) {
            SemanticCheckDimensions visitor0 = new SemanticCheckDimensions();
            visitor0.visit(tree);
         }

      }
      return res;
   }

   @Override public Boolean visitDeclaration(GeneralParser.DeclarationContext ctx) {
      String typeStr = ctx.type().getText();
      String id = ctx.ID().getText();

      if (GeneralParser.symbolTable.exists(id)) {
         ErrorHandling.printError(ctx, "Variable \"" + id + "\" already declared!");
         System.exit(1);
      } 
      else {
         visit(ctx.type());
         Type type = ctx.type().res;
         Symbol s = new Symbol(typeStr, type);
         s.setValueDefined();
         GeneralParser.symbolTable.put(id, s);
      }
      
      return true;
   }

   @Override public Boolean visitIntegerType(GeneralParser.IntegerTypeContext ctx) {
      ctx.res = integerType;
      return true;
   }

   @Override public Boolean visitFloatType(GeneralParser.FloatTypeContext ctx) {
      ctx.res = floatType;
      return true;
   }

   @Override public Boolean visitBooleanType(GeneralParser.BooleanTypeContext ctx) {
      ctx.res = booleanType;
      return true;
   }

   @Override public Boolean visitStringType(GeneralParser.StringTypeContext ctx) {
      ctx.res = stringType;
      return true;
   }

   @Override public Boolean visitDimensionType(GeneralParser.DimensionTypeContext ctx) {
      String dim = ctx.ID().getText();
      if (!DimensionsParser.dimensionsTable.exists(dim)) {
         ErrorHandling.printError(ctx, "Dimension \"" + dim + "\" does not exist!");
         System.exit(1);
      }
      DimensionType d = DimensionsParser.dimensionsTable.get(dim);
      ctx.res = d;
      return true;
   }

   @Override public Boolean visitDecAssign(GeneralParser.DecAssignContext ctx) {
      Boolean res = visit(ctx.expr());
      String id = ctx.declaration().ID().getText(), typeStr = ctx.declaration().type().getText();
      
      if (GeneralParser.symbolTable.exists(id)) {
         ErrorHandling.printError(ctx, "Variable \"" + id + "\" already declared!");
         System.exit(1);
      } 
      else {
         res = visit(ctx.declaration().type());

         Type type = ctx.declaration().type().res;

         if (type.getClass().getName().equals("DimensionType")) {
            if (ctx.expr().unit != null) {
               String unit = ctx.expr().unit;
               DimensionType temp = (DimensionType) type;
               if (!temp.checkUnit(unit)) {
                  ErrorHandling.printError(ctx, "Unit \"" + unit + "\" not declared for dimension " + temp.name());
                  System.exit(1);
               }
            }
            else {
               ErrorHandling.printError(ctx, "Variable referenced before assignment!");
               System.exit(1);
            }
         }

         Symbol sym = new Symbol(typeStr, type);

         if (!sym.conformsToDimension(ctx.expr().dimension)) {
            ErrorHandling.printError(ctx, "Value type does not conform to variable \"" + id + "\" type!");
            System.exit(1);
         }
         else {
            if (type.getClass().getName().equals("DimensionType")) {
               sym.setDimension(typeStr);
               sym.setUnit(ctx.expr().unit);
            }
            sym.setValueDefined();
            GeneralParser.symbolTable.put(id, sym);
         } 
      }
      return res;
   }

   @Override public Boolean visitOnlyAssign(GeneralParser.OnlyAssignContext ctx) {
      String id = ctx.ID().getText();
      Boolean res = visit(ctx.expr());
      
      if (!GeneralParser.symbolTable.exists(id)) {
         ErrorHandling.printError(ctx, "Variable \"" + id + "\" referenced before declaration!");
         System.exit(1);
      }
      else {
         Symbol sym = GeneralParser.symbolTable.get(id);
         if (sym.getType().getClass().getName().equals("DimensionType")) {
            if (ctx.expr().unit != null) {
               String unit = ctx.expr().unit;
               DimensionType temp = (DimensionType) sym.getType();
               if (!temp.checkUnit(unit)) {
                  ErrorHandling.printError(ctx, "Unit \"" + unit + "\" not declared for dimension " + sym.getName());
                  System.exit(1);
               }
            }
            else {
               ErrorHandling.printError(ctx, "Variable referenced before assignment!");
               System.exit(1);
            }
         }
         
         if (!sym.conformsToDimension(ctx.expr().dimension)) {
            ErrorHandling.printError(ctx, "Value type does not conform to variable \"" + id + "\" type!");
            System.exit(1);
         } 
         else {
            if (sym.getType().getClass().getName().equals("DimensionType")) {
               sym.setDimension(ctx.expr().dimension);
               sym.setUnit(ctx.expr().unit);
            }
            sym.setValueDefined();
         }
         
      }
      return res;
   }

   @Override public Boolean visitCondition(GeneralParser.ConditionContext ctx) {
      Boolean res1 = visit(ctx.expr(0));
      Boolean res2 = visit(ctx.expr(1));

      if (!ctx.expr(0).dimension.equals(ctx.expr(1).dimension)) {
         ErrorHandling.printError(ctx, "Cannot compare expressions of diferent dimensions!");
         System.exit(1);
      }
      else {
         if (ctx.expr(0).exprType.toString().equals("boolean") || ctx.expr(1).exprType.toString().equals("boolean") || ctx.expr(0).exprType.toString().equals("String") || ctx.expr(1).exprType.toString().equals("String")) {
            ErrorHandling.printError(ctx, "Numeric operator applied to a non-numeric operand!");
            System.exit(1); 
         }
         ctx.exprType = booleanType;
         ctx.unit = "Void";
         ctx.dimension = "Adimensional";
      }
      return true;      
   }

   @Override public Boolean visitAddSub(GeneralParser.AddSubContext ctx) {
      Boolean res1 = visit(ctx.expr(0));
      Boolean res2 = visit(ctx.expr(1));

      if (!ctx.expr(0).dimension.equals(ctx.expr(1).dimension)) {
         ErrorHandling.printError(ctx, "Cannot add expressions of diferent dimensions!");
         System.exit(1);
      }
      else {
         if (ctx.expr(0).exprType.toString().equals("boolean") || ctx.expr(1).exprType.toString().equals("boolean") || ctx.expr(0).exprType.toString().equals("String") || ctx.expr(1).exprType.toString().equals("String")) {
            ErrorHandling.printError(ctx, "Numeric operator applied to a non-numeric operand!");
            System.exit(1); 
         }
         ctx.exprType = ctx.expr(0).exprType;
         if (ctx.exprType.getClass().getName().equals("DimensionType")) {
            DimensionType t = (DimensionType) ctx.exprType;
            ctx.unit = t.getBaseUnit();
         }
         else
            ctx.unit = ctx.expr(0).unit;
         ctx.dimension = ctx.expr(0).dimension;
      }
      return true;
   }

   @Override
   public Boolean visitParen(GeneralParser.ParenContext ctx) {
      visit(ctx.expr());
      ctx.exprType = ctx.expr().exprType;
      ctx.unit = ctx.expr().unit;
      ctx.dimension = ctx.expr().dimension;
      return true;
   }

   @Override public Boolean visitIDValue(GeneralParser.IDValueContext ctx) {
      String id = ctx.ID().getText();

      if (!GeneralParser.symbolTable.exists(id)) {
         ErrorHandling.printError(ctx, "Variable \"" + id + "\" not declared!");
         System.exit(1);
      } 
      else {
         Symbol sym = GeneralParser.symbolTable.get(id);
         if (!sym.getValueDefined()) {
            ErrorHandling.printError(ctx, "Variable \"" + id + "\" not defined!");
            System.exit(1);
         } else {
            ctx.exprType = sym.getType();
            ctx.dimension = sym.getDimension();
            ctx.unit = sym.getUnit();
         }
      }
      return true;
   }

   @Override public Boolean visitUnary(GeneralParser.UnaryContext ctx) {
      Boolean res = visit(ctx.expr()) && ctx.expr().dimension.equals("Adimensional");
      if (res) {
         ctx.unit = ctx.expr().unit;
         ctx.dimension = ctx.expr().dimension;
         ctx.exprType = ctx.expr().exprType;
      } else {
         ErrorHandling.printError(ctx, "Numeric operator applied to a non-numeric operand!");
         System.exit(1);
      }
      return res;
   }

   @Override public Boolean visitMultDiv(GeneralParser.MultDivContext ctx) {
      visit(ctx.expr(0));
      visit(ctx.expr(1));
      String op = ctx.op.getText();

      if (ctx.expr(0).exprType.toString().equals("boolean") || ctx.expr(1).exprType.toString().equals("boolean") || ctx.expr(0).exprType.toString().equals("String") || ctx.expr(1).exprType.toString().equals("String")) {
         ErrorHandling.printError(ctx, "Numeric operator applied to a non-numeric operand!");
         System.exit(1);
      }
      else {
         
         if (ctx.expr(0).dimension.equals("Adimensional") && ctx.expr(1).dimension.equals("Adimensional")) {
            ctx.unit = ctx.expr(0).unit;
            ctx.dimension = ctx.expr(0).dimension;
            if (ctx.expr(0).exprType.conformsTo(floatType) || ctx.expr(1).exprType.conformsTo(floatType))
               ctx.exprType = floatType;
            else
               ctx.exprType = integerType;
         } 
         else if (ctx.expr(1).dimension.equals("Adimensional")) {
            ctx.unit = ctx.expr(0).unit;
            ctx.dimension = ctx.expr(0).dimension;
            ctx.exprType = ctx.expr(0).exprType;
         } 
         else if (ctx.expr(0).dimension.equals("Adimensional")) {
            if (op.equals("/")) {
               ErrorHandling.printError(ctx, "Invalid division: Numerator cannot be void when denominator is not!");
               System.exit(1);   
            }
            ctx.unit = ctx.expr(1).unit;
            ctx.dimension = ctx.expr(1).dimension;
            ctx.exprType = ctx.expr(1).exprType;
         } 
         else {
            String d1 = ctx.expr(0).dimension;
            String d2 = ctx.expr(1).dimension;
            if (d1.equals(d2) && op.equals("/")) {
               ctx.unit = "Void";
               ctx.dimension = "Adimensional";
               ctx.exprType = floatType;
            } 
            else {
               boolean control = false;
               String aux = ctx.expr(0).unit + op + ctx.expr(1).unit;
               for (DimensionType d : DimensionsParser.dimensionsTable.values()) {
                  if (d.checkUnit(aux)) {
                     ctx.dimension = d.name();
                     ctx.unit = aux;
                     control = true;
                  }
               }
               if (control == false) {
                  ErrorHandling.printError(ctx, "Invalid unit: \"" + aux + "\"");
                  System.exit(1);
               }
            }
         }
      }
      return true;
   }

   @Override public Boolean visitNot(GeneralParser.NotContext ctx) {
      Boolean res = visit(ctx.expr());

      if (!ctx.expr().exprType.conformsTo(booleanType)) {
         ErrorHandling.printError(ctx, "Bad operand type for operator \"not\"");
         System.exit(1);
      }
      ctx.exprType = booleanType;
      ctx.unit = "Void";
      ctx.dimension = "Adimensional";
      
      return res;
   }
   /*
   @Override public Boolean visitInputValue(GeneralParser.InputValueContext ctx) {
      if (ctx.ID() != null) {
         Boolean check = false;
         String unit = ctx.ID().getText();
         for (DimensionType d : DimensionsParser.dimensionsTable.values()) {
            if (d.checkUnit(unit)) {
               ctx.dimension = d.name();
               ctx.unit = unit;
               check = true;
            }
         }
         if (!check) {
            ErrorHandling.printError(ctx, "Invalid unit: \"" + ctx.ID().getText() + "\"");
            System.exit(1);
         }
      } 
      return true;
   }*/

   @Override public Boolean visitBooleanValue(GeneralParser.BooleanValueContext ctx) {
      ctx.exprType = booleanType;
      ctx.dimension = "Adimensional";
      ctx.unit = "Void";
      return true;
   }

   @Override public Boolean visitPow(GeneralParser.PowContext ctx) {
      visit(ctx.expr(0));
      visit(ctx.expr(1));
      if (!ctx.expr(1).dimension.equals("Adimensional")) {
         ErrorHandling.printError(ctx, "Bad operand types for exponent \"" + ctx.expr(1).getText() + "\"");
         System.exit(1);
      }
      if (ctx.expr(0).exprType == booleanType || ctx.expr(1).exprType == booleanType || ctx.expr(0).exprType == stringType || ctx.expr(1).exprType == stringType) {
         ErrorHandling.printError(ctx, "Numeric operator applied to a non-numeric operand!");
         System.exit(1); 
      }

      if (ctx.expr(1).getText().equals("1") || ctx.expr(0).unit.equals("Void")) {
         ctx.unit = ctx.expr(0).unit;
         ctx.dimension = ctx.expr(0).dimension;
         ctx.exprType = ctx.expr(0).exprType; 
      }
      else {
         boolean control = false;
         String unit = ctx.expr(0).unit + ctx.expr(1).getText();
         for (DimensionType d : DimensionsParser.dimensionsTable.values()) {
            if (d.checkUnit(unit)) {
               ctx.dimension = d.name();
               ctx.unit = unit;
               control = true;
            }
         }
         if (control == false) {
            ErrorHandling.printError(ctx, "Invalid unit: \"" + unit + "\"");
            System.exit(1);
         }
      }
      
      return true;
   }

   @Override public Boolean visitFloatValue(GeneralParser.FloatValueContext ctx) {
      ctx.exprType = floatType;
      if (ctx.ID() != null) {
         Boolean check = false;
         String unit = ctx.ID().getText();
         for (DimensionType d : DimensionsParser.dimensionsTable.values()) {
            if (d.checkUnit(unit)) {
               ctx.dimension = d.name();
               ctx.unit = unit;
               check = true;
            }
         }
         if (!check) {
            ErrorHandling.printError(ctx, "Invalid unit: \"" + ctx.ID().getText() + "\"");
            System.exit(1);
         }
      } 
      else {
         ctx.dimension = "Adimensional";
         ctx.unit = "Void";
      }
      return true;
   }

   @Override public Boolean visitStringValue(GeneralParser.StringValueContext ctx) {
      ctx.exprType = stringType;
      ctx.dimension = "Adimensional";
      ctx.unit = "Void";
      return true;
   }

   @Override public Boolean visitIntValue(GeneralParser.IntValueContext ctx) {
      ctx.exprType = integerType;
      if (ctx.ID() != null) {
         Boolean check = false;
         String unit = ctx.ID().getText();
         for (DimensionType d : DimensionsParser.dimensionsTable.values()) {
            if (d.checkUnit(unit)) {
               ctx.dimension = d.name();
               ctx.unit = unit;
               check = true;
            }
         }
         if (!check) {
            ErrorHandling.printError(ctx, "Invalid unit: \"" + ctx.ID().getText() + "\"");
            System.exit(1);
         }
      } 
      else {
         ctx.dimension = "Adimensional";
         ctx.unit = "Void";
      }
      return true;
   }

   @Override public Boolean visitAndOr(GeneralParser.AndOrContext ctx) {
      Boolean res = visit(ctx.expr(0)) && visit(ctx.expr(1));

      if (!ctx.expr(0).exprType.conformsTo(booleanType) || !ctx.expr(1).exprType.conformsTo(booleanType)) {
         ErrorHandling.printError(ctx, "Bad operand types for operator \"" + ctx.op.getText() + "\"");
         System.exit(1);
      }
      ctx.exprType = booleanType;
      ctx.unit = "Void";
      ctx.dimension = "Adimensional";
      
      return res;
   }

   @Override public Boolean visitConditional(GeneralParser.ConditionalContext ctx) {
      Boolean res = visit(ctx.expr());

      if (!ctx.expr().exprType.conformsTo(booleanType)) {
         ErrorHandling.printError(ctx, "Condition expression should be BooleanType!");
         System.exit(1);
      }
      res = visit(ctx.statList());

      /*if (ctx.elifCondition() != null) {
         Iterator<GeneralParser.ElifConditionContext> iter = ctx.elifCondition().iterator();
         while (iter.hasNext())
            res = visit(iter.next());
      } */

      if (ctx.elseConditon() != null)
         res = visit(ctx.elseConditon());
      
      return res;
   }
   /*
   @Override public Boolean visitElifCondition(GeneralParser.ElifConditionContext ctx) {
      Boolean res = visit(ctx.expr());

      if (!ctx.expr().exprType.conformsTo(booleanType)) {
         ErrorHandling.printError(ctx, "Condition expression should be BooleanType!");
         System.exit(1);
      }
      res = visit(ctx.statList());
      
      return res;
   }*/

   @Override public Boolean visitWhileLoop(GeneralParser.WhileLoopContext ctx) {
      Boolean res = visit(ctx.expr());

      if (!ctx.expr().exprType.conformsTo(booleanType)) {
         ErrorHandling.printError(ctx, "While expression should be BooleanType!");
         System.exit(1);
      }
      res = visit(ctx.statList());
      
      return res;
   }

   // @Override public Boolean visitForLoop(GeneralParser.ForLoopContext ctx) {
   //    Boolean res = visit(ctx.assign()) && visit(ctx.expr(0)) && visit(ctx.expr(1)) && visit(ctx.statList());

   //    if (!ctx.expr(0).exprType.conformsTo(booleanType)) {
   //       ErrorHandling.printError(ctx, "Bad break conditional expression for \"for\" statement");
   //       System.exit(1);
   //    }
   //    ctx.expr(0).exprType = booleanType;

   //    if (!ctx.expr(1).exprType.isNumeric()) {
   //       ErrorHandling.printError(ctx, "Bad increment conditional expression for \"for\" statement");
   //       System.exit(1);
   //    }
   //    ctx.expr(1).exprType = booleanType;
      
   //    return res;
   // }
}
