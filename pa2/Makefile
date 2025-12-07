# Makefile for PA1 (CFA) and PA2 (DFA)

# --- Variables ---
GRAMMAR=simpleC.g4
BUILDDIR=build
ANTLR_JAR=/usr/local/lib/antlr-complete.jar
ANTLR4=java -jar $(ANTLR_JAR)
CLASSPATH=$(BUILDDIR):$(ANTLR_JAR):.

INPUT=example.c
OUTPUT_CFA=cfg.out
OUTPUT_DFA=liveness.out

PROG_CFA=CFGBuilder
PROG_DFA=DFAAnalyzer

.PHONY: all antlr run_cfa run_dfa clean

# Default build target (Builds parser, CFA, and DFA)
all: $(PROG_CFA) $(PROG_DFA)

# Generate ANTLR parser and lexer
antlr: $(GRAMMAR)
	@mkdir -p $(BUILDDIR)
	$(ANTLR4) $(GRAMMAR) -o $(BUILDDIR) -no-listener -visitor
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(BUILDDIR)/*.java

# Compile PA1 (CFGBuilder) and its dependencies
$(PROG_CFA): antlr BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_CFA).java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_CFA).java

# Compile PA2 (DFAAnalyzer) and its dependencies
$(PROG_DFA): antlr BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_DFA).java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_DFA).java


# === (Execution Targets) ===

# Run PA1 (CFGBuilder) on example.c
# Output is redirected to cfg.out
run_cfa: $(PROG_CFA)
	java -classpath $(CLASSPATH) $(PROG_CFA) $(INPUT) > $(OUTPUT_CFA)
	@echo ""
	@echo "--- CFG analyzed in $(OUTPUT_CFA) (PA1) ---"
	@cat $(OUTPUT_CFA)

# Run PA2 (DFAAnalyzer) on example.c
# The Java program writes directly to liveness.out, so no redirection needed here
run_dfa: $(PROG_DFA)
	java -classpath $(CLASSPATH) $(PROG_DFA) $(INPUT)
	@echo ""
	@echo "--- Liveness analysis in $(OUTPUT_DFA) (PA2) ---"
	@cat $(OUTPUT_DFA)

# Clean up build and output files
clean:
	rm -rf $(BUILDDIR)
	@if [ -f $(OUTPUT_CFA) ]; then rm $(OUTPUT_CFA); fi
	@if [ -f $(OUTPUT_DFA) ]; then rm $(OUTPUT_DFA); fi