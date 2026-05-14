# Chess Endgame Analyzer

A rule-based expert system that analyzes chess endgame positions using named theoretical principles from canonical endgame literature. Given a position in FEN notation, it classifies the endgame type, evaluates the theoretical outcome, and recommends a strategic plan with a step-by-step reasoning chain traceable to specific book references.

## Tech stack

| Layer | Technology |
|---|---|
| Rule engine | Drools 7.49.0.Final |
| Backend | Spring Boot (Java 11), Maven multi-module |
| Frontend | React 19 + Tailwind 4 (Bun) |
| Build | Maven — three modules: `model`, `kjar`, `server` |

## Features

- **Forward chaining** across 4 levels: classification -> evaluation -> recommendation -> candidate move assessment
- **Backward chaining** with the `isTheoreticalDraw` query tree decomposing into 5 sub-goals
- **Recursive king path query** (`canKingReach`) traversing a pre-loaded adjacency graph
- **Drools rule template** (`key-squares.drt`) generating 48 key-square detection rules from a data table at startup
- **Book-referenced reasoning chain** — every inference step cites de la Villa (2008) or Silman (2007)

### Supported endgame types

- King + Pawn vs. King (rule of the square, key squares, opposition, wrong-color bishop)
- Rook + Pawn vs. Rook (Lucena, Philidor)
- Bishop + Pawn vs. King (wrong-color bishop draw)
- Rook + Pawn vs. lone King
- Knight + Pawn vs. King
- Rook vs. Bishop (wrong-corner technique)
- Rook vs. Knight (edge/corner trap)
- Queen vs. 7th-rank Pawn (triangulation vs. stalemate)
- Pure Rook endgame (fortress / 7th-rank activity)

## Build and run

**Prerequisites:** Java 17, Maven 3.8+, Bun.

### Backend

Build all three modules in dependency order:

```bash
mvn install -pl model && mvn install -pl kjar && mvn install -pl server
```

Run the Spring Boot server (defaults to port 8080):

```bash
cd server && mvn spring-boot:run
```

### Frontend

```bash
cd frontend
bun install
bun run dev        # dev server on :5173
```

## API

```
POST /api/analyze
Content-Type: application/json
```

**Request**

| Field | Required | Values |
|---|---|---|
| `fen` | Yes | FEN string |
| `queryType` | Yes | `EVALUATE` or `DRAW_QUERY` |

**Example - evaluate a wrong-color bishop position:**

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fen": "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1",
    "queryType": "DRAW_QUERY"
  }'
```

**Response fields**

| Field | Description |
|---|---|
| `endgameType` | Classification string, e.g. `BISHOP_PAWN_VS_KING` |
| `sideToMove` | `WHITE` or `BLACK` |
| `derivedFacts` | Ordered reasoning chain with `level`, `type`, `explanation`, and `reference` |
| `recommendation` | `technique`, `plan`, and `bookReference` |
| `theoreticalDraw` | `true` / `false` / `null` (only populated for `DRAW_QUERY`) |

## Example FEN positions

```
# Key square occupied
8/8/8/2K1k3/8/3P4/8/8 w - - 0 1

# Pawn promotes freely (rule of the square)
8/8/1P2k3/8/8/8/8/7K w - - 0 1

# Wrong-color bishop draw
8/k7/8/P7/8/8/1K6/2B5 w - - 0 1

# Rook pawn stalemate
8/8/8/8/8/k7/p7/K7 b - - 0 1

# Philidor defense
4k3/R7/8/3KP3/8/6r1/8/8 w - - 0 1

# Lucena position
3K4/3P1k2/8/8/8/8/7r/4R3 w - - 0 1

# Queen vs. rook pawn (stalemate)
8/8/8/3K4/4Q3/8/7p/7k b - - 0 1

# Queen triangulation
8/8/8/3K4/4Q3/8/4p3/4k3 b - - 0 1
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full knowledge base documentation: all forward-chaining rules with chess-theoretical justifications, backward chaining query tree, Drools template design, and example reasoning traces.

### Module layout

```
chess-endgame-analyzer/
  pom.xml          root POM, declares three modules
  model/           Java model classes (Piece, Position)
  kjar/            Drools knowledge base (.drl rules, key-squares.drt template, kmodule.xml)
  server/          Spring Boot app (REST API, FEN parsing, KieSession management)
  frontend/        React 19 + Tailwind 4 SPA
```

### Knowledge sources

- *100 Endgames You Must Know* - Jesus de la Villa (2008) - primary source, numbered endings
- *Silman's Complete Endgame Course* - Jeremy Silman (2007) - supplementary, prerequisites and endgame classes
