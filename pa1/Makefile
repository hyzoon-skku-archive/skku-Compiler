PROG=CFGBuilder

GRAMMAR=simpleC.g4
BUILDDIR=build

ANTLR_JAR=/usr/local/lib/antlr-complete.jar
ANTLR4=java -jar $(ANTLR_JAR)
CLASSPATH=$(BUILDDIR):$(ANTLR_JAR):.

INPUT=example.c
OUTPUT=cfg.out

TESTDIR=./test_code
TESTS=$(TESTDIR)/test1.c $(TESTDIR)/test2.c $(TESTDIR)/test3.c

.PHONY: all antlr $(PROG) run_cfa clean test1 test2 test3 test_all

# Default build target
all: $(PROG)

# Generate ANTLR parser and lexer
antlr: $(GRAMMAR)
	@mkdir -p $(BUILDDIR)
	$(ANTLR4) $(GRAMMAR) -o $(BUILDDIR) -no-listener -visitor
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(BUILDDIR)/*.java


# Compile CFGBuilder.java
$(PROG): antlr $(PROG).java
	javac -classpath $(CLASSPATH) -d $(BUILDDIR) $(PROG).java


# Run CFGBuilder on example.c
run_cfa: all
	java -classpath $(CLASSPATH) $(PROG) $(INPUT) > $(OUTPUT)
	@echo ""
	@echo "--- CFG analyzed in $(OUTPUT) ---"
	@cat $(OUTPUT)


# Run CFGBuilder on test_code/test1.c ~ test3.c
given: all
	java -classpath $(CLASSPATH) $(PROG) $(TESTDIR)/given.c > $(BUILDDIR)/given.out
	@echo ""
	@echo "--- CFG analyzed for given.c ---"
	@cat $(BUILDDIR)/given.out

test1: all
	java -classpath $(CLASSPATH) $(PROG) $(TESTDIR)/test1.c > $(BUILDDIR)/test1.out
	@echo ""
	@echo "--- CFG analyzed for test1.c ---"
	@cat $(BUILDDIR)/test1.out

test2: all
	java -classpath $(CLASSPATH) $(PROG) $(TESTDIR)/test2.c > $(BUILDDIR)/test2.out
	@echo ""
	@echo "--- CFG analyzed for test2.c ---"
	@cat $(BUILDDIR)/test2.out

test3: all
	java -classpath $(CLASSPATH) $(PROG) $(TESTDIR)/test3.c > $(BUILDDIR)/test3.out
	@echo ""
	@echo "--- CFG analyzed for test3.c ---"
	@cat $(BUILDDIR)/test3.out


# Run all test files sequentially
test_all: test1 test2 test3
	@echo ""
	@echo "--- All tests executed ---"


# Clean up build and output files
clean:
	rm -rf $(BUILDDIR)
	@if [ -f $(OUTPUT) ]; then rm $(OUTPUT); fi
