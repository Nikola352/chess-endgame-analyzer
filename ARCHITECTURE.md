# Chess Endgame Analyzer - System Architecture & Knowledge Base

## 1. Project Overview

The Chess Endgame Analyzer is a rule-based expert system that reasons about chess endgame positions the way a trained human coach would -- by applying named theoretical principles from canonical endgame literature, not by searching the game tree.

Given a position in FEN notation, the system:
1. Classifies the endgame type
2. Evaluates the theoretical outcome (win / draw / complex)
3. Recommends a strategic plan with a step-by-step reasoning chain
4. Answers direct yes/no questions ("Is this a theoretical draw?") through backward chaining

Primary knowledge source: *100 Endgames You Must Know* by Jesus de la Villa (2008). Supporting source: Silman (2007).

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Rule engine | Drools 7.49.0.Final |
| Backend | Spring Boot 3.x (Java 17) |
| Frontend | React 19 + Tailwind 4 SPA |
| FEN parsing | Custom Java utility (pre-processing before Drools) |
| Build | Maven -- three modules: model, kjar, server |

**Module layout:**
- `model` -- Java classes for `Piece` and `Position` (working memory fact types)
- `kjar` -- `.drl` rule files, `.drt` template, `key-squares.csv`, `kmodule.xml`
- `server` -- Spring Boot app, REST API, `KieConfig` (KieBase builder), `ChessEndgameService`

---

## 3. System Architecture

### 3.1 Data Flow

```
User submits FEN string + query type
        |
[Spring Boot REST endpoint  POST /api/analyze]
        |
[FenParser -- Java]
  Parses FEN -> List<Piece> + one Position object
        |
[KieConfig -- Java, at application startup]
  Loads 6 static .drl files into KieHelper
  Loads key-squares.csv (18 rows) and compiles key-squares.drt template
    -> 36 template-generated rules added to KieHelper
  Builds KieBase (shared, singleton)
        |
[ChessEndgameService -- Java]
  Creates KieSession from KieBase
  Inserts: all Piece objects, the Position object,
           ~420 AdjacentSquare topology facts (directed king movement edges)
        |
[Drools KieSession -- fireAllRules()]
  Level 1 rules fire  ->  classification + basic DerivedFacts
    Template rules also fire: derive KeySquare facts from pawn positions
  Level 2 rules fire  ->  evaluation conclusions
  Level 3 rules fire  ->  Recommendation objects
        |
[Backward chaining query -- optional, user-triggered]
  isTheoreticalDraw() query fires
  Decomposes into 7 sub-queries, two of which call canKingReach() recursively
        |
[ChessEndgameService collects results]
  Reads all DerivedFact objects (ordered by level then insertion order)
  Reads Recommendation
  Reads backward chaining answer (true/false)
        |
[React frontend]
  Renders board from FEN
  Displays reasoning chain
  Shows outcome banner + strategic plan
  Shows backward chaining result if queried
```

### 3.2 Query Modes

The user selects one of two modes before submitting:

1. **Evaluate Position** -- pure forward chaining. The engine fires all rules and produces an outcome + recommendation.
2. **Draw Query** -- backward chaining. The engine first runs forward chaining to populate working memory with derived facts, then executes the `isTheoreticalDraw` query tree against those facts.

---

## 4. Input & Output

### 4.1 Input

| Field | Required | Description |
|---|---|---|
| `fen` | Yes | FEN string encoding the position |
| `queryType` | Yes | `EVALUATE` or `DRAW_QUERY` |

### 4.2 Output

| Field | Description |
|---|---|
| `fen` | The submitted FEN string |
| `sideToMove` | `WHITE` or `BLACK` |
| `endgameType` | Classification string, e.g. `KING_PAWN_ENDGAME` |
| `derivedFacts` | Ordered list of `DerivedFact` entries with level, type, side, explanation, reference |
| `recommendation` | Technique name + multi-sentence strategic plan + book reference |
| `theoreticalDraw` | `true` / `false` / `null` (null when EVALUATE mode was used) |

---

## 5. Working Memory Model

### 5.1 `Piece`
One instance per piece on the board. Populated by `FenParser`.
```
class Piece
  type   : Piece.Type    // KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
  color  : Piece.Color   // WHITE, BLACK
  square : String        // algebraic square, e.g. "e4"
```
`Piece.Type` and `Piece.Color` are Java enums declared inside the `Piece` class.

### 5.2 `Position`
Exactly one instance per session. Holds position-level metadata.
```
class Position
  fen          : String
  sideToMove   : Piece.Color   // WHITE or BLACK
  endgameType  : String        // null initially; set by Level 1 classification rules
```

### 5.3 `DerivedFact`
Produced by forward chaining rules at all three levels. Every inference step is a `DerivedFact`. Declared directly in `types.drl`.
```
declare DerivedFact
  type        : String        // symbolic constant, e.g. DIRECT_OPPOSITION, KEY_SQUARE_OCCUPIED
  side        : Piece.Color   // WHITE, BLACK, or null for position-wide facts
  explanation : String        // human-readable; shown in the reasoning chain
  level       : int           // 1, 2, or 3 -- which forward chaining level produced this
  reference   : String        // book reference, e.g. "de la Villa, Ending 3, p.33"
end
```

### 5.4 `Recommendation`
Produced by Level 3 rules. The primary strategic output of the system. Declared in `types.drl`.
```
declare Recommendation
  technique     : String   // e.g. LUCENA_BRIDGE, PHILIDOR_DEFENSE, KING_TO_KEY_SQUARE
  plan          : String   // multi-sentence natural language strategic plan
  bookReference : String   // citation
end
```

### 5.5 `AdjacentSquare`
Pre-loaded topology facts used exclusively by the `canKingReach` backward chaining query. Represents one directed king movement edge. Declared in `types.drl`.
```
declare AdjacentSquare
  from : String   // e.g. "e4"
  to   : String   // e.g. "d5"
end
```
`ChessEndgameService.insertAdjacentSquares()` pre-inserts all ~420 directed edges (each of 64 squares has up to 8 neighbors) at session initialization.

### 5.6 `KeySquare`
Derived at rule-fire time by the template-generated rules. Represents one key square for a specific pawn file and color, computed from the pawn's actual rank. Declared in `types.drl`.
```
declare KeySquare
  pawnFile  : String        // "b" through "g" (rook-pawn files excluded)
  pawnColor : Piece.Color   // WHITE or BLACK
  square    : String        // the computed key square, e.g. "d6"
end
```
`KeySquare` facts are **not** pre-loaded at session startup. They are inserted by the template-generated rules when those rules fire against actual pawn positions. Rule 15 ("Key Square Occupied") then matches these facts to detect king occupation.

---

## 6. Knowledge Base

### 6.1 Pre-loaded Facts (inserted by Java before fireAllRules)

- ~420 `AdjacentSquare` facts covering all directed king movement edges on the board (8 directions from each of the 64 squares, clipped at board edges)

`KeySquare` facts are **not** pre-loaded; they are derived at runtime by the template-generated rules.

### 6.2 Key Square Table (de la Villa, Chapter 1)

Key squares are the squares two ranks ahead of a pawn, spanning the pawn's file and one file to each side. Once the attacking king reaches any key square, pawn promotion is guaranteed. The rook-pawn case (a and h files) is excluded from the template because the drawing resources there are handled by the wrong-color bishop and rook-pawn-edge rules.

| Pawn file | Key files |
|---|---|
| b | a, b, c |
| c | b, c, d |
| d | c, d, e |
| e | d, e, f |
| f | e, f, g |
| g | f, g, h |

Black pawn key squares are the mirror image (two ranks ahead toward rank 1).

### 6.3 Drools Rule Template: Dynamic Key Square Derivation

**This is the system's usage of the Drools `template` feature.**

Key square derivation -- computing which squares are key squares for the pawn currently on the board -- is implemented as a Drools rule template (`.drt` file) compiled against a CSV data table at startup. The template separates the chess domain knowledge (which files have which key files, from de la Villa Chapter 1) from the rule logic.

#### Template file: `key-squares.drt`

The file contains **two template sections**: one for white pawns and one for black pawns.

```
template header
pawnFile
keyFile

package rules;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;

template "white-key-file-rules"

rule "Derive Key Square - WHITE @{pawnFile}-pawn @{keyFile}-file_@{row.rowNumber}"
when
    DerivedFact( type == "KING_PAWN_ENDGAME" )
    $p : Piece( type == Piece.Type.PAWN, color == Piece.Color.WHITE,
                $psq : square,
                square.substring(0, 1).equals("@{pawnFile}"),
                square.codePointAt(1) <= 53 )
    not KeySquare( pawnFile == "@{pawnFile}", pawnColor == Piece.Color.WHITE,
                   square.substring(0, 1).equals("@{keyFile}") )
then
    int pr = $psq.codePointAt(1);
    int keyRank = pr == 53 ? 54 : pr + 2;
    insert( new KeySquare("@{pawnFile}", Piece.Color.WHITE, "@{keyFile}" + (char)keyRank) );
end

end template

template "black-key-file-rules"

rule "Derive Key Square - BLACK @{pawnFile}-pawn @{keyFile}-file_@{row.rowNumber}"
when
    DerivedFact( type == "KING_PAWN_ENDGAME" )
    $p : Piece( type == Piece.Type.PAWN, color == Piece.Color.BLACK,
                $psq : square,
                square.substring(0, 1).equals("@{pawnFile}"),
                square.codePointAt(1) >= 52 )
    not KeySquare( pawnFile == "@{pawnFile}", pawnColor == Piece.Color.BLACK,
                   square.substring(0, 1).equals("@{keyFile}") )
then
    int pr = $psq.codePointAt(1);
    int keyRank = pr == 52 ? 51 : pr - 2;
    insert( new KeySquare("@{pawnFile}", Piece.Color.BLACK, "@{keyFile}" + (char)keyRank) );
end

end template
```

#### Data source: `key-squares.csv`

The template is compiled by `KieConfig.loadCsvData()` which reads this CSV at startup:

```
pawnFile,keyFile
b,a
b,b
b,c
c,b
c,c
c,d
d,c
d,d
d,e
e,d
e,e
e,f
f,e
f,f
f,g
g,f
g,g
g,h
```

18 rows total (3 key files per pawn file for files b-g).

#### What gets generated

`ObjectDataCompiler.compile(data, template)` processes each CSV row through both template sections, producing **36 concrete rules** (18 rows x 2 color templates).

**How a generated rule works:**

For the CSV row `pawnFile=e, keyFile=d`, the white template generates a rule that fires when a white pawn is on the e-file with rank <= 5 (ASCII 53) and no matching `KeySquare` exists yet. The `then` block computes `keyRank = pawnRank + 2` (capped at rank 6 when the pawn is on rank 5) and inserts `KeySquare("e", WHITE, "d" + (char)keyRank)`.

If the white e-pawn is on e4, `keyRank` = ASCII('4') + 2 = ASCII('6'), so `KeySquare("e", WHITE, "d6")` is inserted. Rule 15 then fires when the white king stands on d6.

**Key rank computation:**

For white pawns (rank char <= '5' = 53):
- Pawn on ranks 2-4: key rank = pawn rank + 2 (e.g., e4 -> key squares on rank 6)
- Pawn on rank 5: key rank = 6 (capped; prevents generating rank-7 key squares when the pawn is already advanced)

For black pawns (rank char >= '4' = 52):
- Pawn on ranks 5-7: key rank = pawn rank - 2
- Pawn on rank 4: key rank = 3 (capped)

**Why a template is the right tool here:**

The rule structure is identical for all 36 cases -- only the pawn file and key file change. This is exactly what the template mechanism is designed for: the chess domain knowledge (which files are key files for each pawn file, from de la Villa Chapter 1) lives in the CSV data table, while the computation logic and rule structure live in the template. Changing the key-file mapping requires only editing the CSV. The key rank is computed dynamically from the actual pawn position, so the derived `KeySquare` facts are always correct for any pawn rank, without hardcoding all possible pawn positions.

---

### 6.4 Endgame Principles Encoded

The following theoretical concepts are encoded as rules. Primary source: de la Villa (2008). Supplementary source: Silman (2007).

- **Rule of the square**: A defending king draws against a lone passed pawn if it can enter the promotion square. Four variants cover pawn color x side-to-move; when the defending side moves first, the effective square shrinks by one rank. *(de la Villa, Ending 1, p.27; Silman, Part Four (Class C), p.106)*
- **King In Square**: The complement of the Rule of the Square -- fires when the defending king IS inside the promotion square and the attacking king cannot escort the pawn, confirming a draw. *(de la Villa, Ending 1, p.27)*
- **Opposition** (direct): Kings on the same file or rank with exactly one square between them; the side NOT to move holds the opposition. *(de la Villa, Ending 2, p.29; Silman, Part Two (Class E), p.42)*
- **Key squares**: For a pawn on ranks 2-5 the key squares are two ranks ahead, spanning the pawn's file and one file each side. Derived dynamically by the template. *(de la Villa, Ending 3, p.33)*
- **Distant opposition**: Both kings on the same file or rank separated by 4 or 6 squares (3 or 5 squares between them). *(de la Villa, Ending 3, p.35; Silman, Part Two (Class E), p.44)*
- **Rook pawn draw**: With a rook's pawn, if the defending king reaches the promotion corner the position is drawn via stalemate. *(de la Villa, Ending 4, p.37; Silman, Part Two (Class E), p.45)*
- **Wrong-color bishop**: Bishop + rook pawn vs. lone king where the bishop does not control the pawn's promotion corner. The defending king draws by reaching that corner. *(Silman, Part Four (Class C), p.112; de la Villa, Ending 4, p.37)*
- **Zugzwang / Trebuchet**: In K+P endgames with direct opposition and an advanced pawn, the side to move is in zugzwang. *(Silman, Part Four (Class C), p.97-100)*
- **Lucena position**: Rook + pawn vs. rook. Attacking king on the 8th rank ahead of a non-rook pawn on the 7th rank. Won by bridge-building. *(de la Villa, Ending 53, p.125)*
- **Philidor position**: Rook + pawn vs. rook. Defending king in front of the pawn on the 8th rank, defending rook on rank 6. Drawn with correct continuation. *(de la Villa, Ending 52, p.124)*
- **Rook vs. Bishop draw**: K+R vs. K+B (no pawns) is almost always a theoretical draw; wrong corner exception. *(de la Villa, Endings 6-7, p.39-40)*
- **Rook vs. Knight draw**: K+R vs. K+N (no pawns) usually draws; knight on the edge or corner can be trapped. *(de la Villa, Endings 8-9, p.41-42)*
- **Queen vs. 7th-rank pawn**: Queen wins except against rook's pawn or bishop's pawn with defending king blocking. *(de la Villa, Endings 16-18, p.59-62)*

---

### 6.5 Book Reference Map

| Rule | Principle | Source |
|---|---|---|
| 1 | Endgame detection | General knowledge |
| 2 | King-pawn classification | dV Chapter 1 intro, p.27 |
| 3 | Rook+pawn vs. rook | dV Endings 52-53, p.124-125 |
| 4 | Pure rook endgame | dV Chapter 10 general |
| 5/5b | Bishop+pawn vs. king | **S Part Four (Class C), p.112** |
| 6/6b | Rook+pawn vs. lone king | dV Chapters 5-10 |
| 7/7b | Knight+pawn vs. king | dV Chapter 3, p.51 |
| 8 | Rook vs. Bishop | dV Endings 6-7, p.39-40 |
| 9 | Rook vs. Knight | dV Endings 8-9, p.41-42 |
| 10/10b | Queen vs. advanced pawn | dV Endings 16-18, p.59-62 |
| 11 | Unsupported fallback | System design |
| 12 | Direct opposition | dV Ending 2, p.29 + **S Part Two (Class E), p.42** |
| 13 | Distant opposition | dV Ending 3, p.35 + **S Part Two (Class E), p.44** |
| 14/14b/14c/14d | Rule of the square (4 variants) | dV Ending 1, p.27 + **S Part Four (Class C), p.106** |
| 14e-14h | King In Square (4 variants) | dV Ending 1, p.27 |
| 15 | Key square occupied | dV Ending 3, p.33 |
| 16/16b | Wrong-color bishop | **S Part Four (Class C), p.112** + dV Ending 4, p.37 |
| 17/17b | Lucena candidate | dV Ending 53, p.125 |
| 18/18b | Philidor candidate | dV Ending 52, p.124 |
| 19 | Knight on the edge | dV Ending 8, p.41; Ending 9, p.42 |
| 20 (White/Black) | Pawn on 6th rank | dV Ending 2, p.29 |
| 21 (White/Black) | King leads pawn | **S Part Three (Class D), p.61** |
| 22 (White/Black) | Zugzwang defender | **S Part Four (Class C), p.97-100** |
| 23 (White/Black) | Rook pawn edge | dV Ending 4, p.37 + **S Part Two (Class E), p.45** |
| 24 | Winning: key square | dV Ending 3, p.33 |
| 25 | Winning: pawn promotes freely | dV Ending 1, p.27 |
| 25b | Draw: king in square | dV Ending 1, p.27 |
| 25c | Winning: king leads pawn | dV Ending 2, p.29; **S Part Three (Class D), p.61** |
| 26 | Lucena win confirmed | dV Ending 53, p.125 |
| 27 | Philidor draw | dV Ending 52, p.124 |
| 28/28b | Wrong-color bishop draw | **S Part Four (Class C), p.112** + dV Ending 4, p.37 |
| 29a/29b/29c | Rook+pawn vs. king evaluation | dV Ending 4, p.37; Chapters 5-10 |
| 30 | Complex rook endgame | dV Chapter 10 general |
| 31 | Pure rook draw | dV Chapter 10 general |
| 32a/32b | Rook vs. Bishop evaluation | dV Endings 6-7, p.39-40 |
| 33a/33b | Rook vs. Knight evaluation | dV Endings 8-9, p.41-42 |
| 34a (3 variants) | Queen vs. pawn draw | dV Endings 17-18, p.60-62; **S Part Four (Class C)** |
| 34b | Queen vs. pawn win | dV Ending 16, p.59 |
| 35 | King to key square | dV Ending 3, p.33 |
| 35b | Advance pawn freely | dV Ending 1, p.27 |
| 35c | Escort pawn to promotion | dV Ending 2, p.29; **S Part Three (Class D), p.61** |
| 36 | Maintain opposition | dV Ending 2, p.29 + **S Part Two (Class E), p.42** |
| 37 | Lucena bridge | dV Ending 53, p.125-127 |
| 38 | Philidor maintenance | dV Ending 52, p.124-125 |
| 39 | Corner defense (wrong-color bishop) | **S Part Four (Class C), p.112** + dV Ending 4, p.37 |
| 40a/40b | Rook vs. Bishop technique | dV Endings 6-7, p.39-40 |
| 41 | Queen triangulation | dV Ending 16, p.59; **S Part Four (Class C)** |
| 42 | Generic king activation (fallback) | General endgame principle |
| 43 (White/Black) | Philidor opportunity missed | dV Ending 52, p.124 |
| Template | Key-file derivation (36 rules) | dV Ending 3, p.33 |

---

## 7. Forward Chaining Rules

Rules are organized in three levels. Each level fires on facts produced by the previous level.

**Chaining summary:**
- Level 1 facts (`KING_PAWN_ENDGAME`, `DIRECT_OPPOSITION`, `KEY_SQUARE_OCCUPIED`, etc.) trigger Level 2 rules
- Level 2 facts (`WINNING_POSITION`, `THEORETICAL_DRAW`, etc.) trigger Level 3 rules
- Level 3 rules produce `Recommendation` objects

---

### 7.1 Level 1 -- Position Classification & Basic Fact Detection

These rules fire on raw `Piece` and `Position` facts and produce the first layer of `DerivedFact` objects.

---

**Rule 1 -- Endgame Detection**
- **When**: Using `accumulate count()`, count all `Piece` objects whose type is not KING. If the total non-king piece count is 6 or fewer, the position qualifies as an endgame for analysis.
- **Then**: Insert `DerivedFact(type="ENDGAME_DETECTED", level=1)`. All subsequent rules require this fact.
- **Guard**: `not DerivedFact(type=="ENDGAME_DETECTED")` prevents re-firing.

---

**Rule 2 -- King-Pawn Endgame Classification**
- **When**: `ENDGAME_DETECTED`. Using `accumulate count()`, confirm zero pieces that are neither king nor pawn. At least one pawn exists.
- **Then**: Modify `Position.endgameType = "KING_PAWN_ENDGAME"`. Insert `DerivedFact(type="KING_PAWN_ENDGAME", level=1)`.

---

**Rule 3 -- Rook+Pawn vs. Rook Classification**
- **When**: `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. Exactly one pawn (any color). One white rook and one black rook. No other non-king pieces.
- **Then**: `Position.endgameType = "ROOK_PAWN_VS_ROOK"`. Insert `DerivedFact(type="ROOK_PAWN_VS_ROOK", side=pawnOwner, level=1)`.

---

**Rule 4 -- Pure Rook Endgame Classification**
- **When**: `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. Both sides have one rook. No pawns. No other non-king pieces.
- **Then**: `Position.endgameType = "PURE_ROOK_ENDGAME"`. Insert `DerivedFact(type="PURE_ROOK_ENDGAME", level=1)`.

---

**Rules 5 and 5b -- Bishop+Pawn vs. Lone King Classification (White and Black attacker)**
- **When** (5): `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. White has a bishop and at least one pawn and no other non-king pieces. Black has only a king (no non-king pieces).
- **When** (5b): Mirror for Black attacker.
- **Then**: `Position.endgameType = "BISHOP_PAWN_VS_KING"`. Insert `DerivedFact(type="BISHOP_PAWN_VS_KING", side=attacker, level=1)`.
- **Salience**: 1 (higher than fallback rule 11).

---

**Rules 6 and 6b -- Rook+Pawn vs. Lone King Classification (White and Black attacker)**
- **When** (6): `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. White has exactly 2 non-king pieces: one rook and one pawn. Black has only a king.
- **When** (6b): Mirror for Black attacker.
- **Then**: `Position.endgameType = "ROOK_PAWN_VS_KING"`. Insert `DerivedFact(type="ROOK_PAWN_VS_KING", side=attacker, level=1)`.
- **Salience**: 1.

---

**Rules 7 and 7b -- Knight+Pawn vs. Lone King Classification (White and Black attacker)**
- **When** (7): `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. White has exactly 2 non-king pieces: one knight and one pawn. Black has only a king.
- **When** (7b): Mirror for Black attacker.
- **Then**: `Position.endgameType = "KNIGHT_PAWN_VS_KING"`. Insert `DerivedFact(type="KNIGHT_PAWN_VS_KING", side=attacker, level=1)`.
- **Salience**: 1.

---

**Rule 8 -- Rook vs. Bishop Classification**
- **When**: `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. No pawns. Each side has exactly one non-king piece. One side has a rook; the other has a bishop (either color combination).
- **Then**: `Position.endgameType = "ROOK_VS_BISHOP"`. Insert `DerivedFact(type="ROOK_VS_BISHOP", side=null, level=1)`.

---

**Rule 9 -- Rook vs. Knight Classification**
- **When**: `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. No pawns. Each side has exactly one non-king piece. One side has a rook; the other has a knight (either color combination).
- **Then**: `Position.endgameType = "ROOK_VS_KNIGHT"`. Insert `DerivedFact(type="ROOK_VS_KNIGHT", side=null, level=1)`.

---

**Rules 10 and 10b -- Queen vs. Advanced Pawn Classification (White and Black queen)**
- **When** (10): `ENDGAME_DETECTED`. Not `KING_PAWN_ENDGAME`. White has only a queen (no other non-king pieces). Black has exactly one pawn on rank 2 (the 7th rank for a black pawn, square matches `.[2]`) and no other non-king pieces.
- **When** (10b): Black queen vs. white pawn on rank 7 (square matches `.[7]`).
- **Then**: `Position.endgameType = "QUEEN_VS_ADVANCED_PAWN"`. Insert `DerivedFact(type="QUEEN_VS_ADVANCED_PAWN", side=queenOwner, level=1)`.
- **Salience**: 1.

---

**Rule 11 -- Unsupported Endgame Fallback**
- **When**: `ENDGAME_DETECTED`. None of the classification types (KING_PAWN_ENDGAME, ROOK_PAWN_VS_ROOK, PURE_ROOK_ENDGAME, BISHOP_PAWN_VS_KING, ROOK_PAWN_VS_KING, KNIGHT_PAWN_VS_KING, ROOK_VS_BISHOP, ROOK_VS_KNIGHT, QUEEN_VS_ADVANCED_PAWN) has been inserted.
- **Then**: Insert `DerivedFact(type="UNSUPPORTED_ENDGAME", level=1)`. No further analysis rules fire.
- **Salience**: -10 (fires only as a last resort).

---

**Rule 12 -- Direct Opposition**
- **When**: `KING_PAWN_ENDGAME`. White and black kings are on the same file exactly 2 ranks apart, or on the same rank exactly 2 files apart (one square between them).
- **Then**: Insert `DerivedFact(type="DIRECT_OPPOSITION", side=[side NOT to move -- they hold opposition], level=1)`.
- **Chess basis**: de la Villa, Ending 2, p.29; Silman, Part Two (Class E), p.42.

---

**Rule 13 -- Distant Opposition**
- **When**: `KING_PAWN_ENDGAME`. White and black kings are on the same file with rank distance 4 or 6, or on the same rank with file distance 4 or 6 (3 or 5 squares between them).
- **Then**: Insert `DerivedFact(type="DISTANT_OPPOSITION", side=null, level=1)`.
- **Chess basis**: de la Villa, Ending 3, p.35; Silman, Part Two (Class E), p.44.

---

**Rules 14, 14b, 14c, 14d -- Rule of the Square (4 variants)**

Four rules cover all combinations of pawn color (White/Black) and side to move (White/Black). The key condition in each is a Chebyshev distance comparison between the defending king and the pawn's promotion rank. When the defending side moves first (black to move for a white pawn), the threshold is increased by 1 because the defender gets a free move.

- **Rule 14 -- White pawn, white to move**: Black king Chebyshev distance to rank 8 > `56 - pawnRank`. Pawn promotes freely.
- **Rule 14b -- White pawn, black to move**: Black king distance > `(56 - pawnRank) + 1`.
- **Rule 14c -- Black pawn, black to move**: White king Chebyshev distance to rank 1 > `pawnRank - 49`.
- **Rule 14d -- Black pawn, white to move**: White king distance > `(pawnRank - 49) + 1`.

- **Then** (all four): Insert `DerivedFact(type="PAWN_PROMOTES_FREELY", side=[pawn color], level=1)`.
- **Chess basis**: de la Villa, Ending 1, p.27; Silman, Part Four (Class C), p.106.

---

**Rules 14e, 14f, 14g, 14h -- King In Square (4 variants)**

These four rules are the complement of Rules 14-14d. They fire when the defending king IS inside the promotion square AND the attacking king is too far from the pawn to assist (cannot escort it to promotion).

- **Rule 14e -- White pawn, white to move**: Black king distance to rank 8 <= `56 - pawnRank` AND white king distance to rank 8 > `56 - pawnRank`.
- **Rule 14f -- White pawn, black to move**: Black king <= extended square AND white king > base square.
- **Rule 14g -- Black pawn, black to move**: White king in square AND black king too far to escort.
- **Rule 14h -- Black pawn, white to move**: White king in extended square AND black king too far.

- **Then** (all four): Insert `DerivedFact(type="KING_IN_SQUARE", side=[defending color], level=1)`.
- **Chess basis**: de la Villa, Ending 1, p.27.
- **Chain trigger**: Level 2 Rule 25b (Theoretical Draw).

---

**Rule 15 -- Key Square Occupied by Attacking King**
- **When**: `KING_PAWN_ENDGAME`. A `KeySquare` fact exists for pawn color `C`, pawn file `F`, and square `S`. The attacking king of color `C` stands on square `S`.
- **Then**: Insert `DerivedFact(type="KEY_SQUARE_OCCUPIED", side=C, level=1, explanation="Attacking king occupies key square S for the F-pawn -- promotion guaranteed regardless of defender.")`.
- **Note**: The `KeySquare` facts that this rule matches are not pre-loaded; they are inserted at runtime by the 36 template-generated rules when those rules fire against the actual pawn position.

---

**Rules 16 and 16b -- Wrong Color Bishop (White and Black attacker)**
- **When** (16): `BISHOP_PAWN_VS_KING` (side=WHITE). The pawn is a rook pawn (file a or h). The bishop's square color (file+rank sum parity) is computed in the `then` block; if it does not match the color of the promotion square (pawnFile + "8"), the fact is inserted.
- **When** (16b): Mirror for Black attacker (promotion square is pawnFile + "1").
- **Then** (if colors differ): Insert `DerivedFact(type="WRONG_COLOR_BISHOP", side=attacker, level=1)`.

---

**Rules 17 and 17b -- Lucena Position Candidate (White and Black attacker)**
- **When** (17): `ROOK_PAWN_VS_ROOK` (side=WHITE). White pawn on files b-g on rank 7 (regex `[b-g]7`). White king on rank 8.
- **When** (17b): `ROOK_PAWN_VS_ROOK` (side=BLACK). Black pawn on files b-g on rank 2. Black king on rank 1.
- **Then**: Insert `DerivedFact(type="LUCENA_CANDIDATE", side=attacker, level=1)`.

---

**Rules 18 and 18b -- Philidor Defense Candidate (White and Black attacker)**
- **When** (18): `ROOK_PAWN_VS_ROOK` (side=WHITE). White pawn on ranks 1-5 (regex `.[1-5]`). Black king on rank 8 (in front of the pawn). Black rook on rank 6.
- **When** (18b): `ROOK_PAWN_VS_ROOK` (side=BLACK). Black pawn on ranks 4-8. White king on rank 1. White rook on rank 3.
- **Then**: Insert `DerivedFact(type="PHILIDOR_CANDIDATE", side=attacker, level=1)`.

---

**Rule 19 -- Knight on the Edge**
- **When**: `ROOK_VS_KNIGHT`. The knight's square is on the a-file (codepoint 97), h-file (104), rank 1 (codepoint 49), or rank 8 (56).
- **Then**: Insert `DerivedFact(type="KNIGHT_ON_EDGE", side=knightColor, level=1)`.

---

**Rules 20 (White and Black) -- Pawn on 6th Rank**
- **When (White)**: `KING_PAWN_ENDGAME`. A white pawn exists on rank 6 (square matches `.[6]`).
- **When (Black)**: A black pawn on rank 3 (square matches `.[3]`).
- **Then**: Insert `DerivedFact(type="PAWN_ON_SIXTH_RANK", side=attacker, level=1)`.

---

**Rules 21 (White and Black) -- Attacking King Leads the Pawn**
- **When (White)**: `KING_PAWN_ENDGAME`. White king is on the same file as the white pawn, exactly one rank ahead: `kingRank - pawnRank == 1` (comparing ASCII codes of rank characters).
- **When (Black)**: Black king same file as black pawn, one rank ahead (toward rank 1): `pawnRank - kingRank == 1`.
- **Then**: Insert `DerivedFact(type="KING_LEADS_PAWN", side=attacker, level=1)`.

---

**Rules 22 (White and Black) -- Zugzwang Defender (Trebuchet)**
- **When (White attacker)**: `KING_PAWN_ENDGAME`. `DIRECT_OPPOSITION` present. `Position.sideToMove == BLACK`. White pawn on ranks 5-7 (square matches `.[5-7]`). No `KEY_SQUARE_OCCUPIED`.
- **When (Black attacker)**: Mirror: `Position.sideToMove == WHITE`. Black pawn on ranks 2-4.
- **Then**: Insert `DerivedFact(type="ZUGZWANG_DEFENDER", side=attacker, level=1)`.

---

**Rules 23 (White and Black) -- Rook Pawn Edge Detection**
- **When**: `ENDGAME_DETECTED`. A pawn of the given color exists on the a-file or h-file (square matches `[ah][1-8]`).
- **Then**: Insert `DerivedFact(type="ROOK_PAWN_EDGE", side=pawnOwner, level=1)`.

---

### 7.2 Level 2 -- Evaluation & Derived States

These rules require Level 1 `DerivedFact` objects as conditions and produce evaluation conclusions that Level 3 recommendation rules depend on.

---

**Rule 24 -- Winning Position: Key Square Occupied**
- **When**: `KING_PAWN_ENDGAME`. `KEY_SQUARE_OCCUPIED` (side S) present. No existing `WINNING_POSITION` (side S).
- **Then**: Insert `DerivedFact(type="WINNING_POSITION", side=S, level=2)`.

---

**Rule 25 -- Winning Position: Pawn Promotes Freely**
- **When**: `PAWN_PROMOTES_FREELY` (side S). No existing `WINNING_POSITION` (side S).
- **Then**: Insert `DerivedFact(type="WINNING_POSITION", side=S, level=2)`.

---

**Rule 25b -- Theoretical Draw: King In Square**
- **When**: `KING_IN_SQUARE` present. No `KEY_SQUARE_OCCUPIED`. No `THEORETICAL_DRAW`.
- **Then**: Insert `DerivedFact(type="THEORETICAL_DRAW", side=null, level=2)`.

---

**Rule 25c -- Winning Position: King Leads Pawn**
- **When**: `KING_PAWN_ENDGAME`. `KING_LEADS_PAWN` (side S). No `WINNING_POSITION` (side S). No `THEORETICAL_DRAW` (guard prevents overriding rook-pawn corner draw conclusions).
- **Then**: Insert `DerivedFact(type="WINNING_POSITION", side=S, level=2)`.

---

**Rule 26 -- Lucena Position Confirmed -- Win**
- **When**: `LUCENA_CANDIDATE` (side S). Using `accumulate count()`, both rooks are present (count of all ROOK pieces == 2). No `LUCENA_WIN`.
- **Then**: Insert `DerivedFact(type="LUCENA_WIN", side=S, level=2)` and `DerivedFact(type="WINNING_POSITION", side=S, level=2)`.
- **Drools feature**: `accumulate count()` over all ROOK pieces verifies both rooks are present before committing to the conclusion.

---

**Rule 27 -- Philidor Defense: Theoretical Draw**
- **When**: `PHILIDOR_CANDIDATE` (side S). No `PHILIDOR_DRAW`.
- **Then**: Insert `DerivedFact(type="PHILIDOR_DRAW", side=S, level=2)` and `DerivedFact(type="THEORETICAL_DRAW", side=null, level=2)`.
- **Note**: `PHILIDOR_CANDIDATE` already encodes both the king position (on 8th rank) and the rook position (on rank 6); no additional checks are needed here.

---

**Rules 28 and 28b -- Wrong Color Bishop: Theoretical Draw (White and Black attacker)**
- **When (28)**: `WRONG_COLOR_BISHOP` (side=WHITE). White pawn on file F. Black king's Chebyshev distance to the corner (F + "8") is <= 4.
- **When (28b)**: `WRONG_COLOR_BISHOP` (side=BLACK). Black pawn on file F. White king's distance to corner (F + "1") <= 4.
- **Then**: Insert `DerivedFact(type="THEORETICAL_DRAW", side=null, level=2)`.

---

**Rules 29a, 29b, 29c -- Rook+Pawn vs. Lone King: Evaluation (three cases)**
- **Rule 29a -- Winning (not an edge pawn)**: `ROOK_PAWN_VS_KING` (side S). No `ROOK_PAWN_EDGE` for side S. Insert `WINNING_POSITION`.
- **Rule 29b -- Winning (edge pawn, defending king far from corner)**: `ROOK_PAWN_VS_KING` (side S). `ROOK_PAWN_EDGE` (side S). Defending king Chebyshev distance to the pawn's promotion corner > 3. Insert `WINNING_POSITION`.
- **Rule 29c -- Draw (edge pawn, defending king near corner)**: `ROOK_PAWN_VS_KING` (side S). `ROOK_PAWN_EDGE` (side S). Defending king distance to corner <= 3. Insert `THEORETICAL_DRAW`.

---

**Rule 30 -- Complex Rook Endgame**
- **When**: `ROOK_PAWN_VS_ROOK`. Neither `LUCENA_WIN` nor `PHILIDOR_DRAW` present.
- **Then**: Insert `DerivedFact(type="COMPLEX_ROOK_ENDGAME", level=2)`.

---

**Rule 31 -- Pure Rook Endgame: Likely Draw**
- **When**: `PURE_ROOK_ENDGAME`. Using two separate `accumulate count()`: count of ROOK pieces matching `.[7]` (rank 7) == 0 AND count matching `.[2]` (rank 2) == 0.
- **Then**: Insert `DerivedFact(type="LIKELY_DRAW", level=2)`.
- **Drools feature**: Two `accumulate` blocks in sequence, each checking 7th-rank (from each side's perspective) activity.

---

**Rules 32a and 32b -- Rook vs. Bishop: Evaluation**
- **Rule 32a -- Likely Draw**: `ROOK_VS_BISHOP`. The defending king is not in a corner, or it is in a corner of the same color as the bishop (bishop can defend). The color check is performed in the `then` block using parity arithmetic. Insert `LIKELY_DRAW`.
- **Rule 32b -- Winning**: `ROOK_VS_BISHOP`. Defending king is on a corner square AND the attacking king is within 3 Chebyshev squares of that corner AND the corner's parity is opposite to the bishop's parity. Insert `WINNING_POSITION` (attacker = rook side).

---

**Rules 33a and 33b -- Rook vs. Knight: Evaluation**
- **Rule 33a -- Winning**: `ROOK_VS_KNIGHT`. `KNIGHT_ON_EDGE` present. Insert `WINNING_POSITION` (attacker = rook side).
- **Rule 33b -- Likely Draw**: `ROOK_VS_KNIGHT`. No `KNIGHT_ON_EDGE`. Insert `LIKELY_DRAW`.

---

**Rules 34a (3 variants) and 34b -- Queen vs. Advanced Pawn: Evaluation**
- **Rule 34a-1 -- Draw (rook pawn)**: `QUEEN_VS_ADVANCED_PAWN`. Pawn is on file a or h (regex `[ah][1-8]`). Insert `THEORETICAL_DRAW`.
- **Rule 34a-2 -- Draw (bishop pawn, white queen)**: `QUEEN_VS_ADVANCED_PAWN` (side=WHITE). Black pawn on file c or f (codepoint 99 or 102). Black king on the same file as the pawn with rank less than the pawn's rank (king is blocking from below). Insert `THEORETICAL_DRAW`.
- **Rule 34a-3 -- Draw (bishop pawn, black queen)**: Mirror: white pawn on c or f, white king on same file with rank greater than pawn's rank. Insert `THEORETICAL_DRAW`.
- **Rule 34b -- Winning**: `QUEEN_VS_ADVANCED_PAWN` (side S). Defending pawn on file b, d, e, or g (regex `[bdeg][1-8]`). Insert `WINNING_POSITION` (side S).

---

### 7.3 Level 3 -- Move Recommendation & Strategic Plan

These rules fire on Level 2 evaluation facts and produce `Recommendation` objects.

---

**Rule 35 -- Recommend King Approach to Key Square**
- **When**: `WINNING_POSITION` (side S). `KING_PAWN_ENDGAME`. No `KEY_SQUARE_OCCUPIED`. No `PAWN_PROMOTES_FREELY`. No `PAWN_ON_SIXTH_RANK` (side S). No `KING_LEADS_PAWN` (side S). No `Recommendation` yet.
- **Then**: Insert `Recommendation(technique="KING_TO_KEY_SQUARE", plan="Advance the attacking king toward the key squares for this pawn. Maneuver the king to one of the key squares (two ranks ahead of the pawn, spanning its file and adjacent files). Push the pawn only once the king has secured a key square -- premature pawn advances waste tempo and may allow the defending king to blockade.", bookReference="de la Villa, Ending 3, p.33")`.

---

**Rule 35b -- Recommend Advancing the Pawn (Promotes Freely)**
- **When**: `WINNING_POSITION`. `PAWN_PROMOTES_FREELY`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="ADVANCE_PAWN", plan="The defending king cannot catch the pawn -- advance the pawn directly toward promotion. No king maneuver is needed. Push the pawn one rank at a time; it queens without opposition.", bookReference="de la Villa, Ending 1, p.27")`.

---

**Rule 35c -- Recommend Escort Pawn to Promotion (King Leads Pawn)**
- **When**: `WINNING_POSITION` (side S). `KING_PAWN_ENDGAME`. `KING_LEADS_PAWN` (side S). No `Recommendation`.
- **Then**: Insert `Recommendation(technique="ESCORT_PAWN", plan="The attacking king is directly in front of its pawn. Step the king sideways or diagonally to allow the pawn to advance -- do not push the pawn while the king blocks it. Once the pawn moves forward, resume leading. The defending king cannot stop both pieces; escort the pawn rank by rank until it promotes.", bookReference="de la Villa, Ending 2, p.29; Silman, Part Three (Class D), p.61")`.
- **Salience**: 5 (fires before Rule 35 to avoid incorrect key-square recommendation when king leads pawn).

---

**Rule 36 -- Recommend Maintaining Direct Opposition**
- **When**: `DIRECT_OPPOSITION`. `WINNING_POSITION`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="MAINTAIN_OPPOSITION", plan="The attacking king holds the direct opposition. Do not step aside -- maintain it. The defending king must concede ground or allow a tempo gain. When the defender moves sideways, follow diagonally to maintain the facing relationship. Advance the pawn when the defender has no useful response.", bookReference="de la Villa, Ending 2, p.29; Silman, Part Two (Class E), p.42")`.

---

**Rule 37 -- Recommend Lucena Bridge Building**
- **When**: `LUCENA_WIN`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="LUCENA_BRIDGE", plan="Execute The Bridge technique: (1) Move the rook to the 4th rank, cutting off side checks from the defending rook. (2) Advance the attacking king out from behind the pawn using the rook as a shield -- when the defending rook checks from the side, interpose the rook one step at a time. (3) Complete the bridge rank by rank until the king escapes the checks and the pawn promotes. This is the most important winning technique in rook endgames.", bookReference="de la Villa, Ending 53, p.125-127")`.

---

**Rule 38 -- Recommend Philidor Defense Maintenance**
- **When**: `PHILIDOR_CANDIDATE`. `PHILIDOR_DRAW`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="PHILIDOR_DEFENSE", plan="Keep the defending king in front of the pawn and the rook on the 3rd rank (6th absolute for white attacker). This is the Philidor defense. The rook waits on that rank while the pawn has not advanced. CRITICAL: the moment the pawn advances to the 6th rank, switch immediately to back-rank (rear) checks. Do not move the rook away from the 3rd rank prematurely -- that concedes the draw.", bookReference="de la Villa, Ending 52, p.124-125")`.

---

**Rule 39 -- Recommend Corner Defense (Wrong Color Bishop)**
- **When**: `WRONG_COLOR_BISHOP`. `THEORETICAL_DRAW`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="WRONG_COLOR_CORNER", plan="Steer the defending king to the promotion corner (the corner the bishop cannot control). Once the defending king reaches that corner, hold it there. Any pawn push gives stalemate -- the bishop has no influence over a square of the wrong color.", bookReference="Silman, Part Four (Class C), p.112; de la Villa, Ending 4, p.37")`.

---

**Rules 40a and 40b -- Recommend Rook vs. Bishop Technique**
- **Rule 40a -- Draw side**: `ROOK_VS_BISHOP`. `LIKELY_DRAW`. No `Recommendation`. Insert `Recommendation(technique="ROOK_VS_BISHOP_CORNER_DEFENCE", plan="Keep the bishop on its long diagonal and steer the king to the corner of the SAME colour as the bishop. Never allow the king to be trapped in the opposite-colour corner -- the bishop cannot defend that corner and checkmate can be forced there.", bookReference="de la Villa, Endings 6-7, p.39-40")`.
- **Rule 40b -- Winning side**: `ROOK_VS_BISHOP`. `WINNING_POSITION`. No `Recommendation`. Insert `Recommendation(technique="ROOK_VS_BISHOP_ATTACK", plan="Drive the defending king to the corner of the OPPOSITE colour from the bishop. Use rook checks to cut off the bishop's long diagonal. Once the king is in the wrong corner and the bishop cannot defend, bring your king in to assist.", bookReference="de la Villa, Ending 6, p.39")`.

---

**Rule 41 -- Recommend Queen Triangulation**
- **When**: `QUEEN_VS_ADVANCED_PAWN`. `WINNING_POSITION`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="QUEEN_TRIANGULATION", plan="Do NOT give random checks. Use queen triangulation: (1) Give a check that forces the defending king to step in front of its own pawn, (2) During the forced pause, bring your king one step closer, (3) Repeat until the pawn is won. AVOID stalemate -- always ensure the defending king has at least one escape square before giving check.", bookReference="de la Villa, Ending 16, p.59; Silman, Part Four (Class C), 'Queen vs. Pawn on 7th Rank'")`.

---

**Rule 42 -- Recommend Generic King Activation (fallback)**
- **When**: `WINNING_POSITION`. No `Recommendation`.
- **Then**: Insert `Recommendation(technique="KING_ACTIVATION", plan="Activate the king immediately -- in endgames the king is a fighting piece, not a spectator. Centralize it toward the pawn structure and key squares. Avoid placing the king passively at the edge of the board. The side that activates the king faster typically converts the advantage.", bookReference="General endgame principle")`.
- **Salience**: -5 (fires only when no specific technique has been identified).

---

**Rules 43 (White and Black attacker) -- Philidor Opportunity Missed**
- **When (White attacker)**: `ROOK_PAWN_VS_ROOK` (side=WHITE). White pawn on ranks 1-5. Black king on rank 8 (correctly positioned in front of pawn). No black rook on rank 6 (rook is on the wrong rank).
- **When (Black attacker)**: Mirror: Black pawn on ranks 4-8. White king on rank 1. No white rook on rank 3.
- **Then**: Insert `DerivedFact(type="PHILIDOR_MISSED", side=defender, level=3)` warning that the rook must move to the correct rank immediately.
- **Salience**: 10 (fires before normal evaluation to flag the missed opportunity prominently in the reasoning chain).

---

## 8. Backward Chaining Queries

Backward chaining fires when the user selects DRAW_QUERY mode. After `fireAllRules()`, `ChessEndgameService` calls `kSession.getQueryResults("isTheoreticalDraw")` and checks whether any result rows are returned.

### 8.1 Query Tree Structure

```
isTheoreticalDraw  (root -- OR node: any one path proves the draw)
|
+-- isDirectOppositionDraw
|     Conditions:
|       DerivedFact(KING_PAWN_ENDGAME)
|       DerivedFact(DIRECT_OPPOSITION, $holderSide)
|       Piece(PAWN, $attackSide)
|       attacker does NOT hold the opposition ($attackSide != $holderSide)
|       NOT DerivedFact(KEY_SQUARE_OCCUPIED)
|
+-- isWrongColorBishopDraw
|     Conditions:
|       DerivedFact(WRONG_COLOR_BISHOP, $attackSide)
|       Piece(PAWN, attackSide, rook-pawn file [ah], $pFile)
|       $corner = $pFile + "8" (or "1" for black attacker)
|       Piece(KING, defending side, $dkSq)
|       canKingReach($dkSq, $corner, 4)  <- RECURSIVE QUERY
|
+-- isPhilidorDraw
|     Conditions:
|       DerivedFact(PHILIDOR_CANDIDATE)
|
+-- isRookPawnEdgeDraw
|     Conditions:
|       DerivedFact(ROOK_PAWN_EDGE, $attackSide)
|       NOT DerivedFact(KEY_SQUARE_OCCUPIED)
|       Piece(PAWN, attackSide, rook-pawn file [ah], $pFile)
|       $corner = $pFile + "8" (or "1" for black attacker)
|       Piece(KING, defending side, $dkSq)
|       canKingReach($dkSq, $corner, 3)  <- RECURSIVE QUERY
|
+-- isStalemateRookPawnPossible
|     Conditions:
|       DerivedFact(QUEEN_VS_ADVANCED_PAWN, $attackSide)
|       Piece(PAWN, defending side, file a or h)
|
+-- isStalemateBishopPawnPossible
|     Conditions:
|       DerivedFact(QUEEN_VS_ADVANCED_PAWN, $attackSide)
|       Piece(PAWN, defending side, file c or f, $pFile)
|       Piece(KING, defending side, same file as pawn)
|
+-- isKingInSquareDraw
      Conditions:
        DerivedFact(KING_IN_SQUARE)
        NOT DerivedFact(KEY_SQUARE_OCCUPIED)
```

### 8.2 Recursive Query: `canKingReach`

The query is defined in `backward-chaining-queries.drl` using Drools' built-in backward chaining support.

The `from` field of `AdjacentSquare` conflicts with the Drools `from` CE keyword. The DRL works around this by binding the field to `$adjFrom` inside the pattern and then comparing it with the input parameter using an `eval` constraint. The query has a base case (from equals to) and a recursive case (step to an adjacent square, depth - 1).

Parameters: `String from, String to, int depth`. Returns true if the king can reach `to` from `from` in at most `depth` steps.

**Connection to the lab example:**

| Lab (`isContainedIn`) | Chess (`canKingReach`) |
|---|---|
| `Location(x, y)` -- x directly in y | `AdjacentSquare(from, to)` -- king can step from `from` to `to` |
| `isContainedIn(x, z)` -- recursive sub-goal | `canKingReach($next, to, depth-1)` -- recursive sub-goal |
| Containment hierarchy | King path tree |
| Base case: `Location(x, y)` directly | Base case: `from == to` |

The `AdjacentSquare` facts play the same role as `Location` facts. The `depth` parameter decreases strictly at each call, guaranteeing termination. Maximum depth used: 4 (wrong-color bishop draw) and 3 (rook pawn draw).

### 8.3 Query Definitions in Plain English

**`isTheoreticalDraw()`**
The draw is proved if any one of the seven sub-queries succeeds.

**`isDirectOppositionDraw()`**
The pawn's owner does NOT hold the direct opposition (the defender holds it) and no key square is occupied. The side that must move loses the opposition advantage and cannot make progress.

**`isWrongColorBishopDraw()`**
`WRONG_COLOR_BISHOP` is present, the attacker has a rook pawn on file F, and the defending king can reach the promotion corner (F + "8" for white / F + "1" for black) within 4 king moves via `canKingReach`.

**`isPhilidorDraw()`**
`PHILIDOR_CANDIDATE` is present in working memory. That fact already encodes all required conditions (defending king on the 8th rank, defending rook on the correct rank, pawn not yet on rank 6).

**`isRookPawnEdgeDraw()`**
`ROOK_PAWN_EDGE` is present for the attacker, no key square is occupied, and the defending king can reach the promotion corner within 3 king moves via `canKingReach`.

**`isStalemateRookPawnPossible()`**
`QUEEN_VS_ADVANCED_PAWN` is present and the defending side has a pawn on file a or h. The stalemate defence in the corner is structurally available.

**`isStalemateBishopPawnPossible()`**
`QUEEN_VS_ADVANCED_PAWN` is present, the defending pawn is on file c or f, and the defending king is on the same file as the pawn. The king-blocks-pawn stalemate pattern is set up.

**`isKingInSquareDraw()`**
`KING_IN_SQUARE` is present (defending king can catch the pawn) and no `KEY_SQUARE_OCCUPIED` fact overrides it.

---

## 9. Accumulate Usage Summary

`accumulate` is used in multiple rules, satisfying the accumulate function requirement:

| Rule | What is accumulated | Purpose |
|---|---|---|
| Rule 1 | All `Piece` where type != KING -- `count(1)` | Non-king piece count <= 6 to classify as endgame |
| Rule 2 | All `Piece` where type != KING and type != PAWN -- `count(1)` | Confirm zero non-pawn non-king pieces |
| Rule 3 | `Piece(PAWN)` count == 1; `Piece(type not KING/ROOK/PAWN)` count == 0 | Exactly one pawn and no other piece types |
| Rules 6/6b | `Piece(color=attacker, type!=KING)` count == 2 | Exactly one rook + one pawn for the attacker |
| Rules 7/7b | Same pattern | Exactly one knight + one pawn |
| Rules 8/9 | Non-king pieces per color -- count == 1 each | Pure K+R vs. K+B or K+N (no extra pieces) |
| Rules 10/10b | `Piece(PAWN, defending side)` count == 1 | Exactly one defending pawn |
| Rule 26 | All `Piece(ROOK)` -- count == 2 | Both rooks present before confirming the Lucena conclusion |
| Rule 31 | `Piece(ROOK)` on rank 7 count == 0; `Piece(ROOK)` on rank 2 count == 0 | Neither rook on its 7th rank (balanced activity suggests draw) |

---

## 10. Example Reasoning Traces

### 10.1 Forward Chaining -- King-Pawn Endgame (3 levels)

**FEN**: `8/8/4k3/4P3/4K3/8/8/8 w - - 0 1`
**Position**: White king e4, White pawn e5, Black king e6. White to move.
**Query**: Evaluate position.

| Step | Level | Rule | Derived Fact / Action |
|---|---|---|---|
| 1 | 1 | Rule 1 | ENDGAME_DETECTED -- 1 non-king piece |
| 2 | 1 | Rule 2 | KING_PAWN_ENDGAME |
| 3 | 1 | Rule 12 | DIRECT_OPPOSITION -- kings on same file, 1 rank apart. Black holds it (white to move). |
| 4 | Template | white-key-file-rules | Pawn on e5 (rank char '5' = 53, capped). KeySquare("e", WHITE, "d6"), KeySquare("e", WHITE, "e6"), KeySquare("e", WHITE, "f6") inserted. |
| 5 | 1 | Rule 15 | KEY_SQUARE_OCCUPIED? -- White king on e4, no KeySquare with square == "e4". Not inserted. |
| 6 | 2 | Rule 24 | WINNING_POSITION? -- Not inserted (no KEY_SQUARE_OCCUPIED). |
| 7 | 3 | Rule 42 | Fallback: Recommendation(KING_ACTIVATION). |

---

### 10.2 Backward Chaining -- Wrong Color Bishop Draw

**FEN**: `8/k7/8/P7/8/8/1K6/2B5 w - - 0 1`
**Position**: White king b2, White bishop c1 (dark-squared), White pawn a5, Black king a7. White to move.
**Query**: "Is this a theoretical draw?"

**Forward chaining populates working memory:**
- Rule 1: ENDGAME_DETECTED
- Rule 5: BISHOP_PAWN_VS_KING (side=WHITE)
- Rule 16: WRONG_COLOR_BISHOP -- bishop on c1 is dark-squared; promotion square a8 is light-squared. Inserted.
- Rule 23: ROOK_PAWN_EDGE (side=WHITE) -- pawn on a-file

**Backward chaining query: `isTheoreticalDraw()`**

| Sub-query | Result | Reason |
|---|---|---|
| `isDirectOppositionDraw` | FAILS | No DIRECT_OPPOSITION fact |
| `isWrongColorBishopDraw` | checking | WRONG_COLOR_BISHOP present; pawn on a-file, corner = "a8" |
| -> `canKingReach("a7", "a8", 4)` | checking | Recursive query starts |
| -> -> depth=4, "a7" != "a8", AdjacentSquare("a7","a8") exists | step taken | |
| -> -> `canKingReach("a8", "a8", 3)` | TRUE | Base case: "a8" == "a8" |
| `isWrongColorBishopDraw` | SUCCEEDS | King reaches a8 in 1 move |
| `isTheoreticalDraw` | YES | Proved via isWrongColorBishopDraw |

---

## 11. Frontend Architecture (React)

**Components:**

| Component | Responsibility |
|---|---|
| `ChessBoard` | Renders board from FEN using react-chessboard (read-only) |
| `FENInput` | Text input with FEN validation + submit button |
| `QuerySelector` | Radio selection: "Evaluate" vs "Is this a draw?" |
| `ReasoningChain` | Ordered list of DerivedFact explanations, color-coded by level |
| `OutcomeBanner` | Large result display: WINNING / DRAWING / COMPLEX with side label |
| `RecommendationPanel` | Technique name + multi-sentence plan + book reference |

---

## 12. Backend Architecture (Spring Boot)

**Key classes:**

| Class | Module | Responsibility |
|---|---|---|
| `FenParser` | server | Converts FEN string to `List<Piece>` + one `Position` object |
| `KieConfig` | server | Builds the shared singleton `KieBase` at startup: loads DRL files, compiles key-squares.drt template from key-squares.csv via `ObjectDataCompiler`, verifies the KieBase |
| `ChessEndgameService` | server | Creates `KieSession`, inserts Piece/Position/AdjacentSquare facts, calls `fireAllRules()`, optionally runs `isTheoreticalDraw` query, collects `DerivedFact` and `Recommendation` results |
| `ChessEndgameController` | server | REST endpoint `POST /api/analyze` |

**REST API:**

```
POST /api/analyze
Content-Type: application/json

{
  "fen":       "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1",
  "queryType": "DRAW_QUERY"
}

Response 200 OK:
{
  "fen": "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1",
  "sideToMove": "WHITE",
  "endgameType": "BISHOP_PAWN_VS_KING",
  "derivedFacts": [
    { "level": 1, "type": "ENDGAME_DETECTED",   "explanation": "2 non-king pieces..." },
    { "level": 1, "type": "WRONG_COLOR_BISHOP",  "explanation": "The bishop does not control a8..." },
    { "level": 1, "type": "ROOK_PAWN_EDGE",      "explanation": "A rook pawn is on the board..." },
    { "level": 2, "type": "THEORETICAL_DRAW",    "explanation": "Wrong color bishop -- defending king can reach a8..." }
  ],
  "recommendation": {
    "technique": "WRONG_COLOR_CORNER",
    "plan": "Steer the defending king to the promotion corner...",
    "bookReference": "Silman, Part Four (Class C), p.112; de la Villa, Ending 4, p.37"
  },
  "theoreticalDraw": true
}
```

---

## 13. Grade Requirements Compliance

| Requirement | Implementation |
|---|---|
| Forward chaining >= 3 levels | 3 levels: classification (L1) -> evaluation (L2) -> recommendation (L3) |
| Accumulate function | Rules 1, 2, 3, 6, 7, 8, 9, 10, 26, 31 -- counting pieces of specific types and colors |
| Backward chaining | `isTheoreticalDraw` decomposes into 7 sub-queries; triggered by user's DRAW_QUERY mode |
| Recursion (tree-based) | `canKingReach(from, to, depth)` -- self-referential query traversing the AdjacentSquare king movement graph; called from `isWrongColorBishopDraw` (depth 4) and `isRookPawnEdgeDraw` (depth 3) |
| Templates | `key-squares.drt` Drools rule template compiled against 18 CSV rows x 2 color template sections = 36 concrete key-square-derivation rules; dynamically computes key square rank from actual pawn rank; the pawnFile-to-keyFile mapping (chess domain data) is fully separated from rule logic |
| Domain-relevant knowledge | Forward-chaining rules from de la Villa (2008) + Silman (2007): Endings 1-4, 6-9, 16-18, 52-53 |
| Two knowledge sources | de la Villa = primary (numbered endings, advanced theory); Silman = supplementary (Classes E-C); both cited in rules 12, 13, 14, 16, 21, 22, 23, 25c, 29c, 34a, 36 |
| Zugzwang (Trebuchet) | Rule 22 -- Silman, Part Four (Class C), p.97: "Trebuchet: whoever moves, loses" |
| R vs. B / R vs. N | Rules 8, 9, 32a/32b, 33a/33b, 40a/40b -- de la Villa Endings 6-9 |
| Q vs. advanced pawn | Rules 10/10b, 34a/34b, 41 -- de la Villa Endings 16-18 |
| Explainability | Every `DerivedFact` carries a natural-language explanation and book reference; reasoning chain is ordered by level then insertion order |
