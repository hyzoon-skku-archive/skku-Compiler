import java.util.Set;
import java.util.TreeSet;

/**
 * ANTLR Visitor class for collecting 'use' vars
 */
public class VariableVisitor extends simpleCBaseVisitor<Set<String>> {

    @Override
    protected Set<String> defaultResult() {
        return new TreeSet<>();
    }

    @Override
    protected Set<String> aggregateResult(Set<String> aggregate, Set<String> nextResult) {
        aggregate.addAll(nextResult);
        return aggregate;
    }

    // if atom node = var(id), add to 'use' set
    @Override
    public Set<String> visitAtom(simpleCParser.AtomContext ctx) {
        Set<String> used = defaultResult();
        if (ctx.ID() != null) {
            used.add(ctx.ID().getText());
        }

        // visit child node
        used.addAll(visitChildren(ctx));
        return used;
    }

    // arguments of function call = 'use' set
    @Override
    public Set<String> visitCall(simpleCParser.CallContext ctx) {
        Set<String> used = defaultResult();
        if (ctx.argList() != null) {
            used.addAll(visit(ctx.argList()));
        }
        return used;
    }
}