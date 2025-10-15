import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

/**
 * BasicBlock: Node in the Control Flow Graph (CFG).
 */
class BasicBlock {
    String id;                                              // Unique identifier for the basic block
    List<String> statements = new ArrayList<>();            // List of statements within this block (CFAVistor add statements here)
    Set<BasicBlock> predecessors = new LinkedHashSet<>();   // Set of blocks that can branch to this block
    Set<BasicBlock> successors   = new LinkedHashSet<>();   // Set of blocks this block can branch to

    // constructor
    BasicBlock(String id) { this.id = id; }

    /**
     * Adds a statement to this block's statements list.
     * Trim whitespace for cleaner output.
     * @param stmt The statement to add.
     */
    void addStatement(String stmt) { statements.add(stmt.trim()); }


    /**
     * Adds a successor to this block and sets this block as a predecessor of the successor.
     * @param successor The basic block that follows this one.
     */
    void addSuccessor(BasicBlock successor) {
        this.successors.add(successor);
        successor.predecessors.add(this);
    }

    /**
     * Helper to get a comma-separated string of block IDs from a predecessor/successor collection.
     * @param blocks The collection of basic blocks. (predecessors or successors).
     * @return Comma-separated string of block IDs, or "-" if empty.
     */
    private String getBlockNames(Collection<BasicBlock> blocks) {
        if (blocks.isEmpty()) return "-";
        return blocks.stream().map(b -> b.id).sorted().collect(Collectors.joining(", "));
    }

    /**
     * String representation of the basic block, including its ID, statements, predecessors, and successors.
     * @return Formatted string of the basic block.
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(id).append("\n{\n");
        for (String stmt : statements) sb.append("    ").append(stmt.replace("\n", "\n    ")).append("\n");
        sb.append("}\n");
        sb.append("Predecessors: ").append(getBlockNames(predecessors)).append("\n");
        sb.append("Successors: ").append(getBlockNames(successors)).append("\n");
        return sb.toString();
    }
}

/**
 * Function: Represents a function in the program, containing its CFG.
 */
class Function {
    String name, returnType, args;                  // Function metadata
    List<BasicBlock> blocks = new ArrayList<>();    // All basic blocks belonging to this function
    BasicBlock entry, exit;                         // Special entry and exit blocks for the function's CFG

    // constructor
    Function(String name, String returnType, String args) {
        this.name = name; this.returnType = returnType; this.args = args;
        this.entry = new BasicBlock(name + "_entry");
        this.exit  = new BasicBlock(name + "_exit");
        addBlock(entry);
    }

    /**
     * Adds a basic block to the function's block list if not already present.
     * @param block The basic block to add.
     */
    void addBlock(BasicBlock block) { if (!blocks.contains(block)) blocks.add(block); }

    /**
     * Sorts the blocks in a logical order:
     * - Entry block first
     * - Exit block last
     * - Other blocks in numerical order based on their IDs
     */
    void sortBlocks() {
        blocks.sort(Comparator.comparing(b -> {
            if (b.id.endsWith("_entry")) return "00";
            if (b.id.endsWith("_exit"))  return "ZZ";
            String d = b.id.replaceAll("[^0-9]", "");
            if (d.isEmpty()) return "999999";
            return String.format("%06d", Integer.parseInt(d));
        }));
    }
}
/**
 * CFAVisitor: ANTLR visitor to build the Control Flow Graph (CFG) from the parse tree.
 */
class CFAVisitor extends simpleCBaseVisitor<Void> {
    // Placeholders are used in statements because the target block IDs are not known at creation time.
    // They are replaced with actual block IDs in a post-processing step.
    private static final String THEN_PLACEHOLDER   = "@THEN_BLOCK@";
    private static final String ELSE_PLACEHOLDER   = "@ELSE_BLOCK@";
    private static final String FOLLOW_PLACEHOLDER = "@FOLLOW_BLOCK@";

    private final List<String> globalDeclarations = new ArrayList<>();      // Stores global variable declarations
    private final Map<String, Function> functions = new LinkedHashMap<>();  // Map of function names to Function objects

    private Function  currentFunction = null;   // The function currently being processed
    private BasicBlock currentBlock   = null;   // The basic block currently being built
    private int blockCounter = 0;               // Counter for generating unique block IDs within a function

    // Maps to store relationships between blocks for post-processing label updates.
    private final Map<BasicBlock, BasicBlock> loopFollowBlocks = new HashMap<>(); // Maps a loop condition block to its follow block (the block after the loop)
    private final Map<BasicBlock, BasicBlock> ifThenTargets    = new HashMap<>(); // Maps an if-condition block to its 'then' block
    private final Map<BasicBlock, BasicBlock> ifElseTargets    = new HashMap<>(); // Maps an if-condition block to its 'else' block

    /**
     * (helper) Creates a new basic block with a unique ID and adds it to the current function.
     * @return The newly created basic block.
     */
    private BasicBlock createNewBlock() {
        String id = currentFunction.name + "_B" + blockCounter++;
        BasicBlock b = new BasicBlock(id);
        currentFunction.addBlock(b);
        return b;
    }

    /**
     * (helper) Extracts the full text of a parse tree node, including all whitespace and formatting.
     * @param ctx The parse tree node.
     * @return The full text represented by the node.
     */
    private String getFullText(ParserRuleContext ctx) {
        if (ctx.start == null || ctx.stop == null ||
            ctx.start.getStartIndex() < 0 || ctx.stop.getStopIndex() < 0) {
            return ctx.getText();
        }
        return ctx.start.getInputStream().getText(
            // Get the full text from the start to the stop token
            new org.antlr.v4.runtime.misc.Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
    }

    /**
     * (helper) Ensures that there is a current block to add statements to. If not, creates a new block.
     */
    private void ensureCurrentBlock() {
        if (currentBlock == null) currentBlock = createNewBlock();
    }

    /**
     * (helper) Prints the entire CFG in the specified format.
     */
    private void printCFG() {
        System.out.println("# Control Flow Graph\n");
        
        // Print global declarations
        System.out.println("@globals {");
        for (String g : globalDeclarations) System.out.println("    " + g);
        System.out.println("}");
        System.out.println("Predecessors: -");
        System.out.println("Successors: -\n");
        
        // Print each function's CFG
        for (Function f : functions.values()) {
            f.sortBlocks();
            for (BasicBlock b : f.blocks) {

                // Print function metadata at the entry block
                if (b == f.entry) { 
                    System.out.println("@" + f.entry.id + " {");
                    System.out.println("    name: " + f.name);
                    System.out.println("    ret_type: " + f.returnType);
                    System.out.println("    args: " + f.args);
                    System.out.println("}");
                    String succ = "-";
                    if (!f.entry.successors.isEmpty()) succ = f.entry.successors.iterator().next().id;
                    System.out.println("Predecessors: -");
                    System.out.println("Successors: " + succ + "\n");
                } 
                
                // Print Regular blocks
                else {
                    System.out.println(b);
                }
            }
        }
    }

    /**
     * (helper) Post-processing step to merge empty blocks that have a single successor.
     * @param func The function whose CFG is to be simplified.
     */
    private void mergeEmptyBlocks(Function func) {
        boolean changed;
        do {
            changed = false;
            List<BasicBlock> toRemove = new ArrayList<>();
            for (BasicBlock block : new ArrayList<>(func.blocks)) {
                if (block == func.entry || block == func.exit) continue;

                // Merge condition: empty, has predecessors, and has exactly one successor
                if (block.statements.isEmpty() && !block.predecessors.isEmpty() && block.successors.size() == 1) {
                    BasicBlock successor = block.successors.iterator().next();
                    
                    // Re-wire predecessors to point to this block's successor
                    for (BasicBlock pred : new ArrayList<>(block.predecessors)) {
                        pred.successors.remove(block);
                        pred.addSuccessor(successor);
                    }
                    
                    // Re-wire successor to remove this block from its predecessors
                    successor.predecessors.remove(block);

                    toRemove.add(block);
                    changed = true;
                }
            }
            if (!toRemove.isEmpty()) {
                func.blocks.removeAll(toRemove);
            }
        } while (changed);
    }
    
    /**
     * (helper) After merging, the original target of a placeholder (e.g., a follow block) might have been removed.
     * This method updates all target mappings (for if, else, loop_end) to point to the correct, non-empty successor block.
     * @param func The function being processed.
     */
    private void updateTargetMappings(Function func) {
        // Helper to traverse down a chain of empty blocks to find the first non-empty successor
        java.util.function.Function<BasicBlock, BasicBlock> findActualTarget = (startBlock) -> {
            BasicBlock current = startBlock;
            while (current != func.entry && current != func.exit &&
                   current.statements.isEmpty() && current.successors.size() == 1) {
                current = current.successors.iterator().next();
            }
            return current;
        };

        // Update loopFollowBlocks
        Map<BasicBlock, BasicBlock> updatedLoopFollows = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : loopFollowBlocks.entrySet()) {
            updatedLoopFollows.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        loopFollowBlocks.clear();
        loopFollowBlocks.putAll(updatedLoopFollows);

        // Update ifThenTargets
        Map<BasicBlock, BasicBlock> updatedIfThens = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : ifThenTargets.entrySet()) {
            updatedIfThens.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        ifThenTargets.clear();
        ifThenTargets.putAll(updatedIfThens);
        
        // Update ifElseTargets
        Map<BasicBlock, BasicBlock> updatedIfElses = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : ifElseTargets.entrySet()) {
            updatedIfElses.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        ifElseTargets.clear();
        ifElseTargets.putAll(updatedIfElses);
    }

    /**
     * (helper) Post-processing step to remove dead/unreachable blocks from the CFG.
     * Performs a graph traversal starting from the entry block to find all live blocks.
     * @param func The function whose CFG is to be pruned.
     */
    private void removeDeadBlocks(Function func) {
        Set<BasicBlock> reachable = new HashSet<>();
        Queue<BasicBlock> worklist = new LinkedList<>();

        if (func.entry != null) {
            reachable.add(func.entry);
            worklist.add(func.entry);
        }

        while (!worklist.isEmpty()) {
            BasicBlock current = worklist.poll();
            for (BasicBlock succ : current.successors) {
                if (reachable.add(succ)) {
                    worklist.add(succ);
                }
            }
        }
        
        // The exit block is reachable if any of its predecessors are reachable.
        if (func.exit != null && func.exit.predecessors.stream().anyMatch(reachable::contains)) {
            reachable.add(func.exit);
        }

        func.blocks.retainAll(reachable);
        
        // Clean up predecessor lists of remaining blocks
        for (BasicBlock block : func.blocks) {
            block.predecessors.retainAll(reachable);
        }
    }


    /**
     * (helper) Post-processing step to renumber the basic blocks sequentially after all merges and structuring.
     * @param func The function whose blocks are to be renumbered.
     */
    private void renumberBlocks(Function func) {
        func.sortBlocks();
        int c = 0;
        for (BasicBlock b : func.blocks) {
            if (b != func.entry && b != func.exit) b.id = func.name + "_B" + c++;
        }
    }

    /**
     * (helper) Final post-processing step to replace all placeholders (e.g., @FOLLOW_BLOCK@)
     * in the statements with the final, renumbered IDs of their target blocks.
     * @param func The function whose labels are to be updated.
     */
    private void updateLabels(Function func) {
        for (Map.Entry<BasicBlock, BasicBlock> e : loopFollowBlocks.entrySet()) {
            BasicBlock cond = e.getKey();
            BasicBlock follow = e.getValue();
            for (int i = 0; i < cond.statements.size(); i++) {
                String stmt = cond.statements.get(i);
                if (stmt.contains(FOLLOW_PLACEHOLDER)) {
                    cond.statements.set(i, stmt.replace(FOLLOW_PLACEHOLDER, follow.id));
                }
            }
        }
        for (Map.Entry<BasicBlock, BasicBlock> e : ifThenTargets.entrySet()) {
            BasicBlock cond = e.getKey();
            BasicBlock thenBlock = e.getValue();
            BasicBlock elseBlock = ifElseTargets.get(cond);
            for (int i = 0; i < cond.statements.size(); i++) {
                String stmt = cond.statements.get(i);
                if (thenBlock != null && stmt.contains(THEN_PLACEHOLDER)) {
                    stmt = stmt.replace(THEN_PLACEHOLDER, thenBlock.id);
                }
                if (elseBlock != null && stmt.contains(ELSE_PLACEHOLDER)) {
                    stmt = stmt.replace(ELSE_PLACEHOLDER, elseBlock.id);
                }
                cond.statements.set(i, stmt);
            }
        }
    }

    /**
     * Recursively collects all function call names within a parse tree node.
     * @param node The parse tree node to search within.
     * @param out A list to store the names of called functions.
     */
    private void collectCallees(ParseTree node, List<String> out) {
        if (node == null) return;
        if (node instanceof simpleCParser.CallContext) {
            out.add(((simpleCParser.CallContext) node).ID().getText());
        }
        for (int i = 0; i < node.getChildCount(); i++) collectCallees(node.getChild(i), out);
    }

    // --------------- Overridden visitor methods ----------------

    /**
     * Visits the entire program, starting the CFG construction process.
     * @param ctx The program context.
     * @return null.
     */
    @Override
    public Void visitProgram(simpleCParser.ProgramContext ctx) {
        visitChildren(ctx);
        printCFG();
        return null;
    }

    /**
     * Visits a function definition, creating its CFG.
     * Resets state, extracts function metadata, and processes the function body.
     * After visiting, performs post-processing to clean up and finalize the CFG.
     * @param ctx The function context.
     * @return null.
     */
    @Override
    public Void visitFunction(simpleCParser.FunctionContext ctx) {
        // Reset state for the new function
        loopFollowBlocks.clear();
        ifThenTargets.clear();
        ifElseTargets.clear();

        // Extract function metadata
        String fn   = ctx.ID().getText();
        String rt   = ctx.type().getText();
        String args = (ctx.paramList() != null) ? getFullText(ctx.paramList()) : "";

        // Create and register the new function
        currentFunction = new Function(fn, rt, args);
        functions.put(fn, currentFunction);
        blockCounter = 0;

        // Start building the CFG from the entry block
        BasicBlock first = createNewBlock();
        currentFunction.entry.addSuccessor(first);
        currentBlock = first;

        // Visit the function body
        visit(ctx.compoundStmt());

        // any block without successors (not counting exit) should go to exit
        currentFunction.blocks.stream()
            .filter(b -> b != currentFunction.exit && b.successors.isEmpty() && !b.predecessors.isEmpty())
            .forEach(b -> b.addSuccessor(currentFunction.exit));
        currentFunction.addBlock(currentFunction.exit);

        // post-processing steps
        mergeEmptyBlocks(currentFunction);
        updateTargetMappings(currentFunction);
        removeDeadBlocks(currentFunction); 
        renumberBlocks(currentFunction);
        updateLabels(currentFunction);

        // Reset state
        currentFunction = null;
        return null;
    }
    
    /**
     * Visits a variable declaration. Adds it to global declarations if outside a function,
     * otherwise adds it as a statement in the current block.
     * @param ctx The declaration context.
     * @return null.
     */
    @Override
    public Void visitDeclaration(simpleCParser.DeclarationContext ctx) {
        if (currentFunction == null) globalDeclarations.add(getFullText(ctx));
        else { ensureCurrentBlock(); currentBlock.addStatement(getFullText(ctx)); }
        return null;
    }

    /**
     * Visits an assignment statement, adding it to the current block.
     * @param ctx The assignment statement context.
     * @return null.
     */
    @Override
    public Void visitAssignStmt(simpleCParser.AssignStmtContext ctx) {
        ensureCurrentBlock();
        String stmtText = getFullText(ctx);
        
        List<String> callees = new ArrayList<>();
        collectCallees(ctx.assign().expr(), callees);

        StringBuilder comments = new StringBuilder();
        for (String callee : callees) {
            comments.append(" # call in expr: ").append(callee).append(" -> ").append(callee).append("_entry");
        }
        
        currentBlock.addStatement(stmtText + comments.toString());
        return null;
    }

    /**
     * Visits a function call statement, adding it to the current block.
     * @param ctx The call statement context.
     * @return null.
     */
    @Override
    public Void visitCallStmt(simpleCParser.CallStmtContext ctx) {
        ensureCurrentBlock();
        String stmtText = getFullText(ctx);
        String callee = ctx.call().ID().getText();
        String comment = " # call: " + callee + " -> " + callee + "_entry";
        currentBlock.addStatement(stmtText + comment);
        return null;
    }

    /**
     * Visits a return statement, adding it to the current block.
     * Connects the current block to the function's exit block and terminates the current block.
     * @param ctx The return statement context.
     * @return null.
     */
    @Override
    public Void visitRetStmt(simpleCParser.RetStmtContext ctx) {
        ensureCurrentBlock();
        String stmtText = getFullText(ctx);
        StringBuilder comments = new StringBuilder();

        if (ctx.expr() != null) {
            List<String> callees = new ArrayList<>();
            collectCallees(ctx.expr(), callees);
            for (String callee : callees) {
                comments.append(" # call in return: ").append(callee).append(" -> ").append(callee).append("_entry");
            }
        }
        
        currentBlock.addStatement(stmtText + comments.toString());
        currentBlock.addSuccessor(currentFunction.exit);
        currentBlock = null; // This block is now terminated
        return null;
    }

    /**
     * Visits an if statement, creating the branching structure in the CFG.
     * Handles both 'then' and optional 'else' branches, connecting them to a join block.
     * @param ctx The if statement context.
     * @return null.
     */
    @Override
    public Void visitIfStmt(simpleCParser.IfStmtContext ctx) {
        ensureCurrentBlock();
        BasicBlock condBlock = currentBlock;

        // Create blocks for the 'then', 'else' (optional), and 'join' (follow) paths.
        boolean hasElse = (ctx.stmt(1) != null);
        StringBuilder line = new StringBuilder();
        String ifCondition = "if (" + getFullText(ctx.expr()) + ")";
        line.append(ifCondition)
            .append(" # then: ").append(THEN_PLACEHOLDER);
        if (hasElse) {
            String padding = " ".repeat(ifCondition.length());
            line.append("\n").append(padding)
                .append(" # else: ").append(ELSE_PLACEHOLDER);
        }
        condBlock.addStatement(line.toString());

        BasicBlock thenBlock = createNewBlock();
        condBlock.addSuccessor(thenBlock);
        ifThenTargets.put(condBlock, thenBlock);
        
        BasicBlock joinBlock = createNewBlock();

        // Visit the 'then' branch
        currentBlock = thenBlock;
        visit(ctx.stmt(0));
        if (currentBlock != null) currentBlock.addSuccessor(joinBlock); // Connect end of 'then' to 'join'

        if (hasElse) {
            // Visit the 'else' branch
            BasicBlock elseBlock = createNewBlock();
            condBlock.addSuccessor(elseBlock);
            ifElseTargets.put(condBlock, elseBlock);
            
            currentBlock = elseBlock;
            visit(ctx.stmt(1));
            if (currentBlock != null) currentBlock.addSuccessor(joinBlock); // Connect end of 'else' to 'join'
            
        } else {
            // If no 'else', the condition block can also branch directly to the join block
            condBlock.addSuccessor(joinBlock);
        }

        // Continue building from the join block
        currentBlock = joinBlock; 
        return null;
    }

    /**
     * Visits a while statement, creating the loop structure in the CFG.
     * Connects the loop body back to the condition and the condition to the follow block.
     * @param ctx The while statement context.
     * @return null.
     */
    @Override
    public Void visitWhileStmt(simpleCParser.WhileStmtContext ctx) {
        BasicBlock prev = currentBlock;         // Block before the loop
        BasicBlock cond = createNewBlock();     // Block for the loop condition
        if (prev != null) prev.addSuccessor(cond);

        cond.addStatement("while (" + getFullText(ctx.expr()) + ") # loop_end: " + FOLLOW_PLACEHOLDER);

        BasicBlock body   = createNewBlock();       // Block for the loop body
        BasicBlock follow = createNewBlock();       // Block for after the loop
        cond.addSuccessor(body);                    // If condition is true, go to body
        cond.addSuccessor(follow);                  // If condition is false, go to follow
        loopFollowBlocks.put(cond, follow);         // Map condition to its follow block

        // Visit the loop body
        currentBlock = body;
        visit(ctx.stmt());
        
        // End of body jumps back to condition
        if (currentBlock != null) currentBlock.addSuccessor(cond);
        
        // Continue building from the follow block
        currentBlock = follow; 
        return null;
    }

    /**
     * Visits a for statement, creating the loop structure in the CFG.
     * Handles initialization, condition, increment, body, and follow blocks.
     * @param ctx The for statement context.
     * @return null.
     */
    @Override
    public Void visitForStmt(simpleCParser.ForStmtContext ctx) {
        ensureCurrentBlock();
        
        // initialization statement
        currentBlock.addStatement(getFullText(ctx.assign(0)) + ";");

        BasicBlock cond = createNewBlock(); // Block for the loop condition
        currentBlock.addSuccessor(cond);
        cond.addStatement("for (" + getFullText(ctx.expr()) + ") # loop_end: " + FOLLOW_PLACEHOLDER);

        BasicBlock body   = createNewBlock();       // Block for the loop body
        BasicBlock inc    = createNewBlock();       // Block for the increment expression
        BasicBlock follow = createNewBlock();       // Block for after the loop
        cond.addSuccessor(body);                    // If condition true, go to body
        cond.addSuccessor(follow);                  // If condition false, go to follow
        loopFollowBlocks.put(cond, follow);

        // Visit the loop body
        currentBlock = body;
        visit(ctx.stmt());

        // End of body goes to increment
        if (currentBlock != null) currentBlock.addSuccessor(inc);

        // The increment block goes back to the condition
        currentBlock = inc;
        currentBlock.addStatement(getFullText(ctx.assign(1)) + ";");
        currentBlock.addSuccessor(cond);
        
        // Continue building from the follow block
        currentBlock = follow; 
        return null;
    }
}

/**
 * Main class to run the CFGBuilder.
 * Reads a C source file, parses it, and builds the CFG using CFAVisitor.
 */
public class CFGBuilder {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java CFGBuilder <input-file.c>");
            return;
        }
        CharStream input = CharStreams.fromFileName(args[0]);
        simpleCLexer lexer = new simpleCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        simpleCParser parser = new simpleCParser(tokens);
        ParseTree tree = parser.program();

        // Create the CFG by visiting the parse tree
        new CFAVisitor().visit(tree);
    }
}



