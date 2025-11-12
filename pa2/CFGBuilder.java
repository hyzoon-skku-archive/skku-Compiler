import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

/**
 * Main class to run the CFGBuilder (PA1).
 */
public class CFGBuilder {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java CFGBuilder <input-file.c>");
            return;
        }

        String inputFile = args[0];
        CharStream input = CharStreams.fromFileName(inputFile);
        simpleCLexer lexer = new simpleCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        simpleCParser parser = new simpleCParser(tokens);
        ParseTree tree = parser.program();

        // 1. Create the CFG by visiting the parse tree
        CFAVisitor visitor = new CFAVisitor();
        visitor.visit(tree);

        // 2. Print the CFG to standard output (for 'run_cfa')
        visitor.printCFG();
    }
}