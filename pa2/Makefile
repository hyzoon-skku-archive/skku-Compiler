# Makefile for PA1 (CFA) and PA2 (DFA)

# --- (PA1 Variables) ---
GRAMMAR=simpleC.g4
BUILDDIR=build
ANTLR_JAR=/usr/local/lib/antlr-complete.jar
ANTLR4=java -jar $(ANTLR_JAR)
CLASSPATH=$(BUILDDIR):$(ANTLR_JAR):.

INPUT=example.c
OUTPUT=cfg.out

# --- (PA2 Variables) ---
PROG_CFA=CFGBuilder
PROG_DFA=DFAAnalyzer
OUTPUT_DFA=liveness.out

# --- (PA1 Test Files) ---
TESTDIR=./test_code
TESTS=$(TESTDIR)/test1.c $(TESTDIR)/test2.c $(TESTDIR)/test3.c

.PHONY: all antlr $(PROG_CFA) $(PROG_DFA) run_cfa run_dfa clean test1 test2 test3 test_all

# Default build target (Builds both CFA and DFA)
all: $(PROG_CFA) $(PROG_DFA)

# Generate ANTLR parser and lexer
antlr: $(GRAMMAR)
	@mkdir -p $(BUILDDIR)
	$(ANTLR4) $(GRAMMAR) -o $(BUILDDIR) -no-listener -visitor
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(BUILDDIR)/*.java

# Compile PA1 (CFGBuilder) and its dependencies
$(PROG_CFA): antlr BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_CFA).java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) BasicBlock.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) Function.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) VariableVisitor.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) CFAVisitor.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(PROG_CFA).java

# Compile PA2 (DFAAnalyzer) and its dependencies
$(PROG_DFA): antlr BasicBlock.java Function.java VariableVisitor.java CFAVisitor.java $(PROG_DFA).java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) BasicBlock.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) Function.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) VariableVisitor.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) CFAVisitor.java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(PROG_DFA).java


# === (Execution Targets) ===

# Run PA1 (CFGBuilder) on example.c
run_cfa: all
	java -classpath $(CLASSPATH) $(PROG_CFA) $(INPUT) > $(OUTPUT)
	@echo ""
	@echo "--- CFG analyzed in $(OUTPUT) (PA1) ---"
	@cat $(OUTPUT)

# Run PA2 (DFAAnalyzer) on example.c
run_dfa: all
	java -classpath $(CLASSPATH) $(PROG_DFA) $(INPUT)
	@echo ""
	@echo "--- Liveness analysis in $(OUTPUT_DFA) (PA2) ---"
	@cat $(OUTPUT_DFA)


# === (Test Targets - PA1) ===

test1: all
	java -classpath $(CLASSPATH) $(PROG_CFA) $(TESTDIR)/test1.c > $(BUILDDIR)/test1.out
	@echo ""
	@echo "--- CFG analyzed for test1.c ---"
	@cat $(BUILDDIR)/test1.out

test2: all
	java -classpath $(CLASSPATH) $(PROG_CFA) $(TESTDIR)/test2.c > $(BUILDDIR)/test2.out
	@echo ""
	@echo "--- CFG analyzed for test2.c ---"
	@cat $(BUILDDIR)/test2.out

test3: all
	java -classpath $(CLASSPATH) $(PROG_CFA) $(TESTDIR)/test3.c > $(BUILDDIR)/test3.out
	@echo ""
	@echo "--- CFG analyzed for test3.c ---"
	@cat $(BUILDDIR)/test3.out

test_all: test1 test2 test3
	@echo ""
	@echo "--- All PA1 tests executed ---"


# Clean up build and output files
clean:
	rm -rf $(BUILDDIR)
	@if [ -f $(OUTPUT) ]; then rm $(OUTPUT); fi
	@if [ -f $(OUTPUT_DFA) ]; then rm $(OUTPUT_DFA); fi