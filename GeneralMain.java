import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.stringtemplate.v4.*;

public class GeneralMain {
   public static void main(String[] args) throws Exception {
      // create a CharStream that reads from standard input:
      CharStream input = CharStreams.fromStream(System.in);
      // create a lexer that feeds off of input CharStream:
      GeneralLexer lexer = new GeneralLexer(input);
      // create a buffer of tokens pulled from the lexer:
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      // create a parser that feeds off the tokens buffer:
      GeneralParser parser = new GeneralParser(tokens);
      // replace error listener:
      //parser.removeErrorListeners(); // remove ConsoleErrorListener
      //parser.addErrorListener(new ErrorHandlingListener());
      // begin parsing at main rule:
      ParseTree tree = parser.main();
      if (parser.getNumberOfSyntaxErrors() == 0) {
         SemanticCheckGeneral semCheck = new SemanticCheckGeneral();
	      GeneralCompiler compiler = new GeneralCompiler();
         
         semCheck.visit(tree);

         if (!ErrorHandling.error()) {
            String outputLang = "java";

            if (!compiler.validTarget(outputLang)) {
               ErrorHandling.printError("Can't find template group file for " + outputLang);
               System.exit(1);
            }

            compiler.setTarget(outputLang);
            ST code = compiler.visit(tree);

            String outputFileName = args[0].split("\\.")[0];

            String outputFileExtension =  "." + outputLang;

            String outputFile = outputFileName + outputFileExtension;

            try {
               code.add("name", outputFileName);
               PrintWriter pw = new PrintWriter(new File(outputFile));
               pw.print(code.render());
               pw.close();

            } catch (FileNotFoundException e) {
               System.err.println("Failed to write code file");
               System.exit(1);
            }
         }
      }
   }
}
