import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/**
 * CFAVisitor: ANTLR visitor to build the Control Flow Graph (CFG).
 */
public class CFAVisitor extends simpleCBaseVisitor<Void> {

    private static final String THEN_PLACEHOLDER = "@THEN_BLOCK@";
    private static final String ELSE_PLACEHOLDER = "@ELSE_BLOCK@";
    private static final String FOLLOW_PLACEHOLDER = "@FOLLOW_BLOCK@";

    private final VariableVisitor varVisitor = new VariableVisitor();
    private final List<String> globalDeclarations = new ArrayList<>();
    private final Map<String, Function> functions = new LinkedHashMap<>();
    private final Map<BasicBlock, BasicBlock> loopFollowBlocks = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> ifThenTargets = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> ifElseTargets = new HashMap<>();
    private Function currentFunction = null;
    private BasicBlock currentBlock = null;
    private int blockCounter = 0;


    private BasicBlock createNewBlock() {
        String id = currentFunction.name + "_B" + blockCounter++;
        BasicBlock b = new BasicBlock(id);
        currentFunction.addBlock(b);
        return b;
    }

    private String getFullText(ParserRuleContext ctx) {
        if (ctx == null || ctx.start == null || ctx.stop == null ||
                ctx.start.getStartIndex() < 0 || ctx.stop.getStopIndex() < 0) {
            return (ctx != null) ? ctx.getText() : "";
        }
        return ctx.start.getInputStream().getText(
                new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
    }

    private void ensureCurrentBlock() {
        if (currentBlock == null) currentBlock = createNewBlock();
    }

    public void printCFG() {
        System.out.println("# Control Flow Graph\n");
        System.out.println("@globals {");
        for (String g : globalDeclarations) System.out.println("    " + g);
        System.out.println("}");
        System.out.println("Predecessors: -");
        System.out.println("Successors: -\n");

        for (Function f : functions.values()) {
            f.sortBlocks();
            for (BasicBlock b : f.blocks) {
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
                } else {
                    System.out.println(b);
                }
            }
        }
    }

    public Map<String, Function> getFunctions() {
        return this.functions;
    }

    // Post-processing methods
    private void mergeEmptyBlocks(Function func) {
        boolean changed;
        do {
            changed = false;
            List<BasicBlock> toRemove = new ArrayList<>();
            for (BasicBlock block : new ArrayList<>(func.blocks)) {
                if (block == func.entry || block == func.exit) continue;
                if (block.statements.isEmpty() && !block.predecessors.isEmpty() && block.successors.size() == 1) {
                    BasicBlock successor = block.successors.iterator().next();
                    for (BasicBlock pred : new ArrayList<>(block.predecessors)) {
                        pred.successors.remove(block);
                        pred.addSuccessor(successor);
                    }
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

    private void updateTargetMappings(Function func) {
        java.util.function.Function<BasicBlock, BasicBlock> findActualTarget = (startBlock) -> {
            BasicBlock current = startBlock;
            while (current != func.entry && current != func.exit &&
                    current.statements.isEmpty() && current.successors.size() == 1) {
                current = current.successors.iterator().next();
            }
            return current;
        };
        Map<BasicBlock, BasicBlock> updatedLoopFollows = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : loopFollowBlocks.entrySet()) {
            updatedLoopFollows.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        loopFollowBlocks.clear();
        loopFollowBlocks.putAll(updatedLoopFollows);
        Map<BasicBlock, BasicBlock> updatedIfThens = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : ifThenTargets.entrySet()) {
            updatedIfThens.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        ifThenTargets.clear();
        ifThenTargets.putAll(updatedIfThens);
        Map<BasicBlock, BasicBlock> updatedIfElses = new HashMap<>();
        for (Map.Entry<BasicBlock, BasicBlock> entry : ifElseTargets.entrySet()) {
            updatedIfElses.put(entry.getKey(), findActualTarget.apply(entry.getValue()));
        }
        ifElseTargets.clear();
        ifElseTargets.putAll(updatedIfElses);
    }

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
        if (func.exit != null && func.exit.predecessors.stream().anyMatch(reachable::contains)) {
            reachable.add(func.exit);
        }
        func.blocks.retainAll(reachable);
        for (BasicBlock block : func.blocks) {
            block.predecessors.retainAll(reachable);
        }
    }

    private void renumberBlocks(Function func) {
        func.sortBlocks();
        int c = 0;
        for (BasicBlock b : func.blocks) {
            if (b != func.entry && b != func.exit) b.id = func.name + "_B" + c++;
        }
    }

    private void updateLabels() {
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

    // Overridden visitor methods
    @Override
    public Void visitProgram(simpleCParser.ProgramContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitFunction(simpleCParser.FunctionContext ctx) {
        loopFollowBlocks.clear();
        ifThenTargets.clear();
        ifElseTargets.clear();

        String fn = ctx.ID().getText();
        String rt = ctx.type().getText();
        String args = (ctx.paramList() != null) ? getFullText(ctx.paramList()) : "";

        currentFunction = new Function(fn, rt, args);
        functions.put(fn, currentFunction);
        blockCounter = 0;

        BasicBlock first = createNewBlock();
        currentFunction.entry.addSuccessor(first);
        currentBlock = first;

        // function parameters become 'def' in entry block
        if (ctx.paramList() != null) {
            for (simpleCParser.IdentifierContext idCtx : ctx.paramList().identifier()) {
                currentFunction.entry.def.add(idCtx.ID().getText());
            }
        }

        visit(ctx.compoundStmt());

        currentFunction.blocks.stream()
                .filter(b -> b != currentFunction.exit && b.successors.isEmpty() && !b.predecessors.isEmpty())
                .forEach(b -> b.addSuccessor(currentFunction.exit));
        currentFunction.addBlock(currentFunction.exit);

        // post-processing logic
        mergeEmptyBlocks(currentFunction);
        updateTargetMappings(currentFunction);
        removeDeadBlocks(currentFunction);
        renumberBlocks(currentFunction);
        updateLabels();

        currentFunction = null;
        return null;
    }

    @Override
    public Void visitDeclaration(simpleCParser.DeclarationContext ctx) {
        String stmtText = getFullText(ctx);
        if (currentFunction == null) {
            globalDeclarations.add(stmtText);
        } else {
            ensureCurrentBlock();
            currentBlock.addStatement(stmtText);

            // calc 'def & 'use' set in declare stmt
            for (simpleCParser.IdentifierContext idCtx : ctx.identList().identifier()) {
                String varName = idCtx.ID().getText();
                currentBlock.def.add(varName);

                // in simpleC.g4 grammar, when declaration, we cannot init vars
                // so don't have to calc 'use' set in declaration
                // e.g. int x = 10;, int x = y; impossible
            }
        }
        return null;
    }

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
        currentBlock.addStatement(stmtText + comments);

        // calc 'def & 'use' set in assign stmt
        // 1. LHS = 'def'
        String defVar = ctx.assign().ID().getText();
        currentBlock.def.add(defVar);

        // 2. RHS = 'use'
        Set<String> usedVars = varVisitor.visit(ctx.assign().expr());
        currentBlock.use.addAll(usedVars);

        return null;
    }

    // collectCallees helper
    private void collectCallees(ParseTree node, List<String> out) {
        if (node == null) return;
        if (node instanceof simpleCParser.CallContext) {
            out.add(((simpleCParser.CallContext) node).ID().getText());
        }
        for (int i = 0; i < node.getChildCount(); i++) collectCallees(node.getChild(i), out);
    }

    @Override
    public Void visitCallStmt(simpleCParser.CallStmtContext ctx) {
        ensureCurrentBlock();
        String stmtText = getFullText(ctx);
        String callee = ctx.call().ID().getText();
        String comment = " # call: " + callee + " -> " + callee + "_entry";
        currentBlock.addStatement(stmtText + comment);

        // calc 'use' set in call stmt
        // argument = 'use'
        if (ctx.call().argList() != null) {
            currentBlock.use.addAll(varVisitor.visit(ctx.call().argList()));
        }

        return null;
    }

    @Override
    public Void visitRetStmt(simpleCParser.RetStmtContext ctx) {
        ensureCurrentBlock();
        String stmtText = getFullText(ctx);
        StringBuilder comments = new StringBuilder();

        // calc 'use' set in return stmt
        if (ctx.expr() != null) {
            currentBlock.use.addAll(varVisitor.visit(ctx.expr()));

            List<String> callees = new ArrayList<>();
            collectCallees(ctx.expr(), callees);
            for (String callee : callees) {
                comments.append(" # call in return: ").append(callee).append(" -> ").append(callee).append("_entry");
            }
        }

        currentBlock.addStatement(stmtText + comments);
        currentBlock.addSuccessor(currentFunction.exit);
        currentBlock = null; // block is terminated in this return line
        return null;
    }

    @Override
    public Void visitIfStmt(simpleCParser.IfStmtContext ctx) {
        ensureCurrentBlock();
        BasicBlock condBlock = currentBlock;

        // calc 'use' set in if stmt
        currentBlock.use.addAll(varVisitor.visit(ctx.expr()));

        // else block
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

        // then block
        BasicBlock thenBlock = createNewBlock();
        condBlock.addSuccessor(thenBlock);
        ifThenTargets.put(condBlock, thenBlock);
        BasicBlock joinBlock = createNewBlock();

        currentBlock = thenBlock;
        visit(ctx.stmt(0));
        if (currentBlock != null) currentBlock.addSuccessor(joinBlock);

        if (hasElse) {
            BasicBlock elseBlock = createNewBlock();
            condBlock.addSuccessor(elseBlock);
            ifElseTargets.put(condBlock, elseBlock);
            currentBlock = elseBlock;
            visit(ctx.stmt(1));
            if (currentBlock != null) currentBlock.addSuccessor(joinBlock);
        } else {
            condBlock.addSuccessor(joinBlock);
        }
        currentBlock = joinBlock;
        return null;
    }

    @Override
    public Void visitWhileStmt(simpleCParser.WhileStmtContext ctx) {
        BasicBlock prev = currentBlock;
        BasicBlock cond = createNewBlock();
        if (prev != null) prev.addSuccessor(cond);

        // calc 'use' set in while stmt
        cond.use.addAll(varVisitor.visit(ctx.expr()));

        cond.addStatement("while (" + getFullText(ctx.expr()) + ") # loop_end: " + FOLLOW_PLACEHOLDER);

        BasicBlock body = createNewBlock();
        BasicBlock follow = createNewBlock();
        cond.addSuccessor(body);
        cond.addSuccessor(follow);
        loopFollowBlocks.put(cond, follow);

        currentBlock = body;
        visit(ctx.stmt());
        if (currentBlock != null) currentBlock.addSuccessor(cond);

        currentBlock = follow;
        return null;
    }

    @Override
    public Void visitForStmt(simpleCParser.ForStmtContext ctx) {
        ensureCurrentBlock();

        String initStmt = getFullText(ctx.assign(0)) + ";";
        currentBlock.addStatement(initStmt);

        // calc 'def' & 'use' set in for stmt
        // 1. init = 'def'
        String initDef = ctx.assign(0).ID().getText();
        currentBlock.def.add(initDef);

        // 2. initUse = 'use'
        Set<String> initUse = varVisitor.visit(ctx.assign(0).expr());
        currentBlock.use.addAll(initUse);

        BasicBlock cond = createNewBlock();
        currentBlock.addSuccessor(cond);

        cond.addStatement("for (" + getFullText(ctx.expr()) + ") # loop_end: " + FOLLOW_PLACEHOLDER);

        // 3. cond = 'use'
        cond.use.addAll(varVisitor.visit(ctx.expr()));

        BasicBlock bodyAndInc = createNewBlock();
        BasicBlock follow = createNewBlock();
        cond.addSuccessor(bodyAndInc);
        cond.addSuccessor(follow);
        loopFollowBlocks.put(cond, follow);

        currentBlock = bodyAndInc;
        visit(ctx.stmt());

        if (currentBlock != null) {
            currentBlock.addStatement(getFullText(ctx.assign(1)) + ";");

            // 4. incr = 'def' & 'use'
            String incDef = ctx.assign(1).ID().getText();
            bodyAndInc.def.add(incDef);
            Set<String> incUse = varVisitor.visit(ctx.assign(1).expr());
            bodyAndInc.use.addAll(incUse);

            currentBlock.addSuccessor(cond);
        }

        currentBlock = follow;
        return null;
    }
}