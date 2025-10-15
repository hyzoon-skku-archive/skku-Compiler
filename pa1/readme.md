# Compiler - PA 1: Control Flow Analysis

## Objective

Build a Control Flow Graph (CFG) for each function in source code using
the ANTLR tool.

---

## How to run

```bash
# make sure you are in the pa1 directory
# compile
make 

# build CFG for example.c
make run_cfa

# test output for ./test_code/test#.c
make test1
make test2
make test3
make test_all # run all test cases

# clean up
make clean
```

---

## `CFGBuilder.java` Implementation

### 1. `BasicBlock` Class

CFG를 구성하는 기본 단위인 **노드(Node)** 에 해당

#### fields

* `String id`: BasicBlock의 고유 ID (`funcName_B{blockNum}` 또는 `funcName_entry`, `funcName_exit`)
* `List<String> statements`: BasicBlock에 속한 statement들의 리스트
* `Set<BasicBlock> predecessors`: 이 BasicBlock으로 들어오는 edge를 가진 BasicBlock들의 집합 (중복 방지를 위해 `LinkedHashSet` 사용)
* `Set<BasicBlock> successors`: 이 BasicBlock에서 나가는 edge를 가진 BasicBlock들의 집합 (중복 방지를 위해 `LinkedHashSet` 사용)

#### methods

* `void addStatement(String stmt)`: BasicBlock에 statement 추가 (trim 처리 후 저장)
* `void addSuccessor(BasicBlock successor)`: successor BasicBlock을 추가하고, 해당 BasicBlock의 predecessor로 현재 BasicBlock을 추가 (양방향 연결)
* `String getBlockNames(Collection<BasicBlock> blocks)`: 주어진 BasicBlock 컬렉션의 ID들을 쉼표로 구분된 문자열로 반환 (없으면 `"-"`)
* `String toString()`: BasicBlock의 ID, statements, predecessors, successors를 포맷된 문자열로 반환

---

### 2. `Function` Class

함수 하나에 대한 정보를 관리하는 클래스로 BasicBlock들과 메타데이터를 포함

#### fields

* `String name`: 함수 이름
* `String returnType`: 함수 반환 타입
* `String args`: 함수 인자
* `List<BasicBlock> blocks`: 함수에 속한 BasicBlock들의 리스트
* `BasicBlock entry`: 함수의 진입점
* `BasicBlock exit`: 함수의 종료점 (여러 개의 return문이 존재하더라도 단 하나의 exit 블록으로 연결되도록 설계)

#### methods

* `void addBlock(BasicBlock block)`: 함수에 BasicBlock 추가 (중복 추가 방지)
* `void sortBlocks()`: BasicBlock들을 ID 기준으로 정렬

  * `_entry` 블록은 항상 맨 앞, `_exit` 블록은 항상 맨 뒤
  * 나머지는 블록 ID 내 숫자 부분을 추출하여 6자리 문자열로 패딩(`String.format("%06d", ...)`) 후 오름차순 정렬
  * CFG의 논리적 구조에는 영향을 주지 않으며, **출력 가독성**을 위한 정렬 단계

---

### 3. `CFAVisitor` Class

ANTLR가 생성한 `simpleCBaseVisitor`를 상속받아, 파싱 트리의 각 노드를 방문(visit)하면서 CFG를 구축.

> `simpleCBaseVisitor`는 `simpleC.g4` 문법 파일에 정의된 모든 규칙에 대해 `visit[RuleName]` 형태의 메서드를 제공.
> 기본적으로 자식 노드를 방문하는 역할만 하므로, 필요한 규칙에 대해 오버라이드하여 CFG 구축 로직을 구현.

#### fields

* `THEN_PLACEHOLDER`, `ELSE_PLACEHOLDER`, `FOLLOW_PLACEHOLDER`: 제어문을 처리할 때 분기할 대상 블록의 ID가 확정되지 않은 경우 임시로 사용하는 플레이스홀더 문자열. 모든 블록 구조가 완성된 후 후처리 단계에서 실제 블록 ID로 교체.
* `List<String> globalDeclarations`: 전역 변수 선언들을 저장하는 리스트
* `Map<String, Function> functions`: 함수 이름을 키로, `Function` 객체를 값으로 저장하는 `LinkedHashMap`
* `Function currentFunction`, `BasicBlock currentBlock`: 현재 방문 중인 함수와 블록을 추적
* `int blockCounter`: 현재 함수 내에서 생성된 BasicBlock의 수를 추적
* `Map<BasicBlock, BasicBlock> loopFollowBlocks`, `ifThenTargets`, `ifElseTargets`:
  각각 루프문의 조건 블록과 루프 이후(follow) 블록, if문의 조건 블록과 then 블록, if-else문의 조건 블록과 else 블록을 매핑
  (`updateLabels` 단계에서 placeholder를 실제 블록 ID로 교체하기 위해 사용)


#### helper methods

* `BasicBlock createNewBlock()`: 새로운 BasicBlock을 생성하고, 현재 함수에 추가.
  ID는 `funcName_B{blockCounter}` 형식으로 지정하며 `blockCounter`를 1 증가시킴.
* `String getFullText(ParserRuleContext ctx)`: 주어진 파서 규칙 컨텍스트의 전체 텍스트를 원본 소스 코드에서 추출하여 반환.
  (ANTLR 토큰 스트림 사용)
* `void ensureCurrentBlock()`: `currentBlock`이 `null`일 경우 새로운 블록을 생성하여 statement를 추가할 수 있도록 보장.
* `void printCFG()`: 모든 함수의 CFG를 출력.
  각 함수의 블록을 `sortBlocks()`로 정렬한 후, 각 블록의 statements 및 predecessor/successor를 함께 출력.
* `void mergeEmptyBlocks(Function func)`: 내용이 비어 있고 **후속 블록(successor)이 하나뿐인** 블록을 제거하여 CFG를 단순화.
  predecessor의 successor 목록에서 해당 블록을 제거하고, predecessor와 successor를 직접 연결.
* `void updateTargetMappings(Function func)`: `mergeEmptyBlocks` 수행 이후 삭제된 블록이 있을 경우,
  `if-then-else` 및 `loop` 문의 target 매핑(`ifThenTargets`, `ifElseTargets`, `loopFollowBlocks`)이
  실제 유효 블록을 가리키도록 갱신.
* `void removeDeadBlocks(Function func)`: 함수의 entry 블록으로부터 도달할 수 없는 블록(dead node)을 제거.
  BFS 탐색을 통해 reachable한 블록만 남기고, 나머지를 삭제하여 CFG를 최적화.
  (exit 블록은 예외적으로 유지)
* `void renumberBlocks(Function func)`: BasicBlock들을 정렬(`sortBlocks()`)한 뒤, entry/exit를 제외한 블록에 대해
  ID를 0부터 순차적으로 재부여 (`funcName_B0`, `funcName_B1`, … 형식).
* `void updateLabels(Function func)`: 모든 placeholder(`@THEN_BLOCK@`, `@ELSE_BLOCK@`, `@FOLLOW_BLOCK@`)를
  해당 블록 매핑(`ifThenTargets`, `ifElseTargets`, `loopFollowBlocks`)의 실제 ID로 교체.


#### overridden visit methods

* `void visitProgram(simpleCParser.ProgramContext ctx)`:
  CFG 생성의 시작점으로, 자식 노드들(전역 변수, 함수 등)을 순회(`visitChildren(ctx)`) 후 `printCFG()` 호출.
* `Void visitFunction(simpleCParser.FunctionContext ctx)`:
  하나의 함수에 대한 CFG를 생성.

  * 함수 메타데이터 추출 후 `Function` 객체 생성 및 `currentFunction`에 할당
  * entry → first 블록 연결, 이후 본문(`compoundStmt`) 방문
  * successor가 없는 블록(return 없이 끝나는 경로)을 exit 블록으로 연결
  * 후처리 단계 실행
    (**`mergeEmptyBlocks → updateTargetMappings → removeDeadBlocks → renumberBlocks → updateLabels`**)
  * 함수별 CFG 구조 최적화 후 종료
* `void visitDeclaration(simpleCParser.DeclarationContext ctx)`:
  함수 외부라면 전역 변수로 저장, 내부라면 현재 블록에 statement로 추가.
* `void visitAssignStmt(simpleCParser.AssignStmtContext ctx)`:
  할당문을 현재 블록에 추가. 표현식 내 함수 호출이 존재할 경우 “`# call in expr:`” 주석을 추가.
* `void visitCallStmt(simpleCParser.CallStmtContext ctx)`:
  독립적인 함수 호출문을 추가. “`# call:`” 주석으로 호출 정보를 함께 기록.
* `void visitRetStmt(simpleCParser.RetStmtContext ctx)`:
  return 문을 현재 블록에 추가하고 exit 블록으로 연결.
  표현식 내 함수 호출이 있을 경우 “`# call in return:`” 주석을 추가.
  이후 `currentBlock`을 `null`로 설정(해당 블록은 터미널 블록).
* `void visitIfStmt(simpleCParser.IfStmtContext ctx)`:
  if-else 제어 흐름을 생성.

  * 조건 블록(`condBlock`)에 if문을 추가하고 placeholder(`@THEN_BLOCK@`, `@ELSE_BLOCK@`)를 삽입
  * thenBlock, elseBlock(선택), joinBlock(follow)을 생성
  * 조건 블록의 successor로 thenBlock 및 elseBlock(또는 joinBlock)을 연결
  * then/else 블록을 각각 방문 후 joinBlock으로 합류
  * `currentBlock`을 joinBlock으로 설정하여 이후 코드 작성
* `void visitWhileStmt(simpleCParser.WhileStmtContext ctx)`:
  while문 제어 흐름 생성.

  * 조건(cond), body, follow 블록을 생성
  * 조건 블록에 loop_end placeholder(`@FOLLOW_BLOCK@`)를 포함한 while문 추가
  * cond → body, cond → follow 연결
  * body 끝에서 cond로 back-edge 연결
  * follow 블록으로 제어 이동
* `void visitForStmt(simpleCParser.ForStmtContext ctx)`:
  for문 제어 흐름 생성.

  * 초기화문을 현재 블록에 추가
  * 조건(cond), body, inc, follow 블록 생성
  * cond → body / cond → follow 연결
  * body 끝에서 inc로, inc에서 cond로 back-edge 연결
  * follow 블록으로 제어 이동

---

### 후처리 단계 요약

| 단계                     | 목적                          |
| ---------------------- | --------------------------- |
| `mergeEmptyBlocks`     | 비어 있고 successor가 하나뿐인 블록 제거 |
| `updateTargetMappings` | 삭제된 블록 참조 복구                |
| `removeDeadBlocks`     | 도달 불가능 블록 제거                |
| `renumberBlocks`       | entry/exit 제외한 블록 ID 재할당    |
| `updateLabels`         | placeholder를 실제 블록 ID로 교체   |

