# Compiler - PA 2: Data Flow Analysis

## Objective

Builds precise control-flow graphs (CFGs), and runs a backwards data-flow analysis to compute liveness information
using the ANTLR tools.

## How to run

```bash
# make sure you are in the pa2 directory
# compile
make

# build CFG for example.c
make run_cfa

# build liveness information for example.c
make run_dfa

# clean up
make clean
```

### Sample Outputs

After `make run_cfa`, `cfg.out` starts with the global declarations and blocks similar to:

```
# Control Flow Graph
@func_entry {
    name: func
    ret_type: int
    args: int x
}
```

After `make run_dfa`, `liveness.out` groups results per function:

```
### Function: func
B0-IN: ;
B0-OUT: n, x
```

Use these artifacts during grading or when debugging new optimizations.

## Notes

- Review the grammar in [`simpleC.g4`](simpleC.g4) to understand which language constructs are supported.
- Start from [`example.c`](example.c) to craft additional test cases and see how statements map to blocks.
- Inspect the analyzer implementations (`CFAVisitor.java`, `DFAAnalyzer.java`, `VariableVisitor.java`) for implementation details.
