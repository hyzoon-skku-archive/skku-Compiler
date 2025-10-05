# Compiler - PA 1: Control Flow Analysis

## Objective

Build a Control Flow Graph (CFG) for each function in source code using
ANTLR tool.

## How to run

```bash
# make sure you are in the pa1 directory
# compile
make 

# build CFG for example.c
make run_cfa
```

## `CFGBuilder.java` Implementation

### 1. `BasicBlock` Class

CFG를 구성하는 기본 단위인 **노드(Node)** 에 해당

#### fields

- `String id`: BasicBlock의 고유 ID (`funcName_B{blockNum}`)
- `List<String> statements`: BasicBlock에 속한 statement들의 리스트
- `Set<BasicBlock> predecessors`: 이 BasicBlock으로 들어오는 edge를 가진 BasicBlock들의 집합 (중복 방지 위해 LinkedHashSet 사용)
- `Set<BasicBlock> successors`: 이 BasicBlock에서 나가는 edge를 가진 BasicBlock들의 집합 (중복 방지 위해 LinkedHashSet 사용)

#### methods

- `void addStatement(String stmt)`: BasicBlock에 statement 추가 (trimmed)
- `void addSuccessor(BasicBlock successor)`: successor BasicBlock 추가 및 해당 BasicBlock의 predecessor로 현재 BasicBlock 추가 (양방향 연결)
- `String getBlockNames(Collection<BasicBlock> blocks)`: 주어진 BasicBlock 컬렉션의 ID들을 쉼표로 구분된 문자열로 반환 (없으면 "-")
- `String toString()`: BasicBlock의 ID, statements, predecessors, successors를 포맷된 문자열로 반환

### 2. `Function` Class

함수 하나에 대한 정보를 관리하는 클래스로 BasicBlock들과 메타데이터를 포함

#### fields

- `String name`: 함수 이름
- `Stirng returnType`: 함수 반환 타입
- `String args`: 함수 인자
- `List<BasicBlock> blocks`: 함수에 속한 BasicBlock들의 리스트
- `BasicBlock entry`: 함수의 진입점
- `BasicBlock exit`: 함수의 종료점 (여러개의 return문이 존재하더라도 단 하나의 exit 블록으로 연결되도록 설계)

#### methods

- `void addBlock(BasicBlock block)`: 함수에 BasicBlock 추가 (중복 추가 방지)
- `void sortBlocks()`: BasicBlock들을 ID 기준으로 정렬 (entry 블록은 항상 맨 앞에 위치, exit 블록은 맨 뒤에 위치, 나머지는 블록 번호 기준 오름차순 정렬)
    - ID가 _entry로 끝나면 "00"을, _exit로 끝나면 "ZZ"를 반환하여 맨 앞과 맨 뒤로 보냄
    - 나머지 블록 ID에서는 숫자만 추출하여 6자리 문자열로 패딩(`String.format("%06d", ...)`) 하여 정렬

### 3. `CFAVisitor` Class

ANTLR가 생성한 simpleCBaseVisitor를 상속받아, 파싱 트리의 각 노드를 방문(visit)하면서 CFG를 구축.

> simpleCBaseVisitor는 simpleC.g4 문법 파일에 정의된 모든 규칙에 대해 visit[RuleName] 형태의 메서드를 제공. 기본적으로 이 메서드들은 자식 노드를 방문하는 역할만 하므로, 필요한 규칙에 대해 오버라이드하여 CFG 구축 로직을 구현.

#### fields

- `THEN_PLACEHOLDER`, `ELSE_PLACEHOLDER`, `FOLLOW_PLACEHOLDER`: 제어문을 처리할 때, 분기할 대상 블록이 아직 생성되지 않았거나 ID가 확정되지 않은 경우에 임시로 사용하는 플레이스홀더 문자열. 모든 블록 구조가 완성된 후 마지막에 실제 블록 ID로 교체.
- `List<String> GlobalDeclarations`: 전역 변수 선언들을 저장하는 리스트
- `Map<String, Function> functions`: 함수 이름을 키로 하고, 해당 함수의 `Function` 객체를 값으로 하는 LinkedHashMap
- `Function currentFunction`, `BasicBlock currentBlock`: 현재 방문 중인 함수와 BasicBlock을 추적
- `int blockCounter`: 현재 함수 내에서 생성된 BasicBlock의 수를 추적
- `Map<BasicBlock, BasicBlock> loopFollowBlocks, ifThenTargets, ifElseTargets`: 각각 루프문의 조건 블록과 해당 루프문의 종료 블록, if문의 조건 블록과 then 블록, if-else문의 조건 블록과 else 블록을 매핑하는 HashMap. 블록 간의 논리적 연결을 임시로 저장했다가, 후처리 단계인 updateLabels에서 이 정보를 사용하여 플레이스홀더를 정확한 블록 ID로 교체하기 위함.


#### helper methods

- `BasicBlock createNewBlock()`: 새로운 BasicBlock을 생성하고, 현재 함수에 추가. ID는 `funcName_B{blockCounter}` 형식으로 지정. `blockCounter`를 1 증가시킴.
- `String getFullText(ParserRuleContext ctx)`: 주어진 파서 규칙 컨텍스트의 전체 텍스트를 원본 소스 코드에서 추출하여 반환. (ANTLR 토큰 스트림 사용, `ctx.getText()` 사용해도 무방함)
- `void ensureCurrentBlock()`: currentBlock이 null일 경우 (예: return 문 처리 후) 새로운 블록을 생성하여 statement를 추가할 수 있도록 보장.
- `void PrintCFG()`: 모든 함수의 CFG를 출력. 각 함수의 BasicBlock들을 정렬한 후, 각 블록의 정보를 출력.
- `void MergeEmptyBlocks(Function func)`: 내용이 비어 있고 predecessor가 하나뿐인 블록들을 제거하여 CFG를 단순화. predecessor의 successor 목록에서 해당 블록을 제거하고, predecessor와 successor를 직접 연결.
- `void updateFollowBlockMappings(Function func)`: `mergeEmptyBlocks`로 인해 기존의 follow 블록이 제거되었을 경우, loopFollowBlocks 맵이 제거된 블록 대신 실제 실행될 다음 블록을 가리키도록 업데이트.
- `void renumberBlocks(Function func)`: BasicBlock들을 ID 기준으로 정렬한 후, 블록 번호를 0부터 재부여.
- `void updateLabels(Function func)`: 모든 플레이스홀더 문자열을 실제 블록 ID로 교체. `loopFollowBlocks`, `ifThenTargets`, `ifElseTargets` 맵을 사용하여 각 조건 블록이 가리키는 실제 follow, then, else 블록의 ID로 플레이스홀더를 대체.

#### overridden visit methods

- `void visitProgram(simpleCParser.ProgramContext ctx)`: CFG 생성의 시작점으로, 자식 노드들(전역 변수, 함수 등)을 순회하도록 visitChildren(ctx)를 호출하고, 모든 작업이 끝나면 printCFG()를 호출하여 결과를 출력.
- `Void visitFunction(simpleCParser.FunctionContext ctx)`: 하나의 함수에 대한 CFG를 생성. 
    - 함수의 메타데이터 추출 후 `Function` 객체 생성 및 `currentFunction`에 할당.
    - 함수의 진입 블록, first 블록 생성 및 `currentBlock`에 first 블록 할당.
    - `visit(ctx.compoundStmt())` 호출하여 함수 본문을 순회하며 CFG 구축.
    - successor가 없는 블록(return되지 않고 끝나는 경로)을 exit 블록으로 연결.
    - 모든 후처리 단계를 순서대로 실행하여 현재 함수의 CFG를 최종적으로 완성.
- `void visitDeclaration(simpleCParser.DeclarationContext ctx)`: 변수 선언에 대한 CFG를 생성.
- `void visitAssignStmt(simpleCParser.AssignStmtContext ctx)`: 할당문에 대한 CFG를 생성. (함수 호출 확인)
- `void visitCallStmt(simpleCParser.CallStmtContext ctx)`: 함수 호출문에 대한 CFG를 생성.
- `void visitReturnStmt(simpleCParser.ReturnStmtContext ctx)`: return문에 대한 CFG를 생성. `currentBlock` 을 `null`로 설정하고, exit 블록으로 연결.
- `void visitIfStmt(simpleCParser.IfStmtContext ctx)`: if문에 대한 CFG를 생성. 
    - `currentBlock`이 조건(condition) 블록이 되고, if (...) 문장을 플레이스홀더와 함께 추가.
    - `thenBlock`, `elseBlock`(있는 경우), 그리고 if문 전체가 끝난 후 코드가 합쳐지는 `joinBlock`을 새로 생성.
    - 조건 블록의 successor로 `thenBlock`과 (else가 있다면) `elseBlock`을, (else가 없다면) `joinBlock`을 추가.
    - `currentBlock`을 `thenBlock`으로 설정하고 then에 해당하는 본문을 visit. `thenBlock`의 successor로 `joinBlock`을 추가.
    - (else가 있다면) `currentBlock`을 `elseBlock`으로 설정하고 else에 해당하는 본문을 visit. `elseBlock`의 successor로 `joinBlock`을 추가.
    - `currentBlock`을 `joinBlock`으로 설정하여 이후 코드를 계속 작성.
- `void visitWhileStmt(simpleCParser.WhileStmtContext ctx)`: while문에 대한 CFG를 생성.
    - `currentBlock`이 조건(condition) 블록이 되고, while (...) 문장을 플레이스홀더와 함께 추가.
    - `bodyBlock`과 `followBlock`을 새로 생성.
    - 조건 블록의 successor로 `bodyBlock`과 `followBlock`을 추가.
    - `currentBlock`을 `bodyBlock`으로 설정하고 본문을 visit. `bodyBlock`의 successor로 조건 블록을 추가(루프).
    - `currentBlock`을 `followBlock`으로 설정하여 이후 코드를 계속 작성.
- `void visitForStmt(simpleCParser.ForStmtContext ctx)`: for문에 대한 CFG를 생성.
    - 초기화문을 현재 블록에 추가.
    - `currentBlock`이 조건(condition) 블록이 되고, for (...) 문장을 플레이스홀더와 함께 추가.
    - `bodyBlock`, `incBlock`, `followBlock`을 새로 생성.
    - 조건 블록의 successor로 `bodyBlock`과 `followBlock`을 추가.
    - `currentBlock`을 `bodyBlock`으로 설정하고 본문을 visit. `bodyBlock`의 successor로 `incBlock`을 추가.
    - `currentBlock`을 `incBlock`으로 설정하고 증감문을 추가. `incBlock`의 successor로 조건 블록을 추가(루프).
    - `currentBlock`을 `followBlock`으로 설정하여 이후 코드를 계속 작성.

### 4. `CFGBuilder` Class

`main` 메서드를 포함하는 진입점 클래스.

- `public static void main(String[] args)`: 
    - 명령줄 인자로부터 C 소스 파일 경로를 읽음.
    - ANTLR의 `CharStreams`와 `CommonTokenStream`을 사용하여 소스 파일을 파싱.
    - 파싱된 트리의 루트 노드인 `program` 노드를 `CFAVisitor`로 방문하여 CFG 생성 시작.