import java.util.*;
import java.util.stream.Collectors;

/**
 * BasicBlock: Node in the Control Flow Graph (CFG).
 */
public class BasicBlock {
    String id;
    List<String> statements = new ArrayList<>();
    Set<BasicBlock> predecessors = new LinkedHashSet<>();
    Set<BasicBlock> successors = new LinkedHashSet<>();

    Set<String> use = new TreeSet<>();
    Set<String> def = new TreeSet<>();


    BasicBlock(String id) {
        this.id = id;
    }

    void addStatement(String stmt) {
        statements.add(stmt.trim());
    }

    void addSuccessor(BasicBlock successor) {
        this.successors.add(successor);
        successor.predecessors.add(this);
    }

    private String getBlockNames(Collection<BasicBlock> blocks) {
        if (blocks.isEmpty()) return "-";
        return blocks.stream().map(b -> b.id)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(id).append("\n{\n");
        for (String stmt : statements) sb.append("    ").append(stmt.replace("\n", "\n    ")).append("\n");
        sb.append("}\n");
        sb.append("Predecessors: ").append(getBlockNames(predecessors)).append("\n");
        sb.append("Successors: ").append(getBlockNames(successors)).append("\n");

        // print out use/def set for debugging
        // sb.append("USE: ").append(use).append("\n");
        // sb.append("DEF: ").append(def).append("\n");
        return sb.toString();
    }
}