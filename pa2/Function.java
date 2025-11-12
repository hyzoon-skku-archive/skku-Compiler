import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Function: Represents a function in the program, containing its CFG.
 */
public class Function {
    String name, returnType, args;
    List<BasicBlock> blocks = new ArrayList<>();
    BasicBlock entry, exit;

    Function(String name, String returnType, String args) {
        this.name = name;
        this.returnType = returnType;
        this.args = args;
        this.entry = new BasicBlock(name + "_entry");
        this.exit = new BasicBlock(name + "_exit");
        addBlock(entry);
    }

    void addBlock(BasicBlock block) {
        if (!blocks.contains(block)) blocks.add(block);
    }

    void sortBlocks() {
        blocks.sort(Comparator.comparing(b -> {
            if (b.id.endsWith("_entry")) return "00";
            if (b.id.endsWith("_exit")) return "ZZ";
            String d = b.id.replaceAll("[^0-9]", "");
            if (d.isEmpty()) return "999999";
            return String.format("%06d", Integer.parseInt(d));
        }));
    }
}