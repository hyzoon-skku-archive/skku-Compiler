import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Main class to do Liveness Analysis (Data Flow Analyzer)
 */
public class DFAAnalyzer {

    private final Map<String, Function> functions;
    private final Map<BasicBlock, Set<String>> inSet = new HashMap<>();
    private final Map<BasicBlock, Set<String>> outSet = new HashMap<>();

    public DFAAnalyzer(Map<String, Function> functions) {
        this.functions = functions;
        for (Function f : functions.values()) {
            for (BasicBlock b : f.blocks) {
                inSet.put(b, new TreeSet<>());
                outSet.put(b, new TreeSet<>());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java DFAAnalyzer <input-file.c>");
            return;
        }

        // 1. ANTLR parsing
        CharStream input = CharStreams.fromFileName(args[0]);
        simpleCLexer lexer = new simpleCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        simpleCParser parser = new simpleCParser(tokens);
        ParseTree tree = parser.program();

        // 2. build CFG
        CFAVisitor cfaVisitor = new CFAVisitor();
        cfaVisitor.visit(tree);
        Map<String, Function> allFunctions = cfaVisitor.getFunctions();

        // 3. Liveness Analysis
        DFAAnalyzer dfa = new DFAAnalyzer(allFunctions);
        dfa.run();

        // 4. print out to file
        dfa.printResults("liveness.out");
    }

    public void run() {
        for (Function f : functions.values()) {
            runOnFunction(f);
        }
    }

    private void runOnFunction(Function func) {
        // postorder CFG list
        List<BasicBlock> postorder = getPostorder(func);

        // 2. generate priority queue (worklist) w/ postorder idx
        // TODO: should this be reverse post order (dfs) ??
        PriorityQueue<BasicBlock> worklist = new PriorityQueue<>(
                Comparator.comparingInt(postorder::indexOf)
        );

        // 3. add all blocks (except entry, exit) in work list
        // TODO: should entry block also be in the work list ?? (we added parameter in the entry block def set)
        for (BasicBlock b : func.blocks) {
            if (b != func.entry && b != func.exit) {
                worklist.add(b);
            }
        }

        // 4. iterate w/ worklist algorithm
        while (!worklist.isEmpty()) {
            BasicBlock B = worklist.poll();

            // 4-1. OUT[B] = UNION(IN[S]) for all successors S of B
            Set<String> newOut = new TreeSet<>();
            for (BasicBlock succ : B.successors) {
                newOut.addAll(inSet.get(succ));
            }
            outSet.put(B, newOut);

            // 4-2. IN[B] = USE[B] + (OUT[B] - DEF[B])
            Set<String> newIn = new TreeSet<>(B.use);
            Set<String> outMinusDef = new TreeSet<>(newOut);
            outMinusDef.removeAll(B.def);
            newIn.addAll(outMinusDef);

            // 4-3. check if IN[B] changed
            if (!inSet.get(B).equals(newIn)) {
                inSet.put(B, newIn);

                // 4-4. if so, put all predecessors of B to the worklist
                for (BasicBlock pred : B.predecessors) {
                    if (pred != func.entry && !worklist.contains(pred)) {
                        worklist.add(pred);
                    }
                }
            }
        }
    }

    private List<BasicBlock> getPostorder(Function func) {
        List<BasicBlock> order = new ArrayList<>();
        Set<BasicBlock> visited = new HashSet<>();

        dfsPostorder(func.entry, visited, order);

        // extra check for reachability
        if (!visited.contains(func.exit)) {
            dfsPostorder(func.exit, visited, order);
        }
        return order;
    }

    private void dfsPostorder(BasicBlock b, Set<BasicBlock> visited, List<BasicBlock> order) {
        if (b == null || visited.contains(b)) {
            return;
        }
        visited.add(b);
        // TODO: i traversed successor but same reason, is this ok ??
        for (BasicBlock succ : b.successors) {
            dfsPostorder(succ, visited, order);
        }
        order.add(b);
    }

    public void printResults(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Function f : functions.values()) {
                f.sortBlocks();
                for (BasicBlock b : f.blocks) {
                    // ignore entry / exit block
                    if (b == f.entry || b == f.exit) continue;

                    // TODO: in the pa2 spec., the function name is not presented but how could we distinguish several functions ??
                    String blockName = b.id.substring(f.name.length() + 1);
                    writer.println(blockName + "-IN: " + formatSet(inSet.get(b)));
                    writer.println(blockName + "-OUT: " + formatSet(outSet.get(b)));
                }
            }
        }
    }

    // formatSet helper
    private String formatSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return ";";
        }
        return String.join(", ", set);
    }
}