# SimpleChessEngine

A fully functional chess engine written in Java, tuned to approximately **1200 ELO** strength. Supports the UCI protocol (plug into any chess GUI), interactive console play, and perft testing for move generation verification.

---

## Features

- **Complete move generation** — all pieces, castling (kingside & queenside), en passant, and pawn promotion
- **Alpha-beta pruning** with iterative deepening for efficient game tree search
- **Quiescence search** to resolve capture sequences and avoid the horizon effect
- **MVV-LVA move ordering** (Most Valuable Victim – Least Valuable Aggressor) for better pruning efficiency
- **Piece-square tables** for human-like positional evaluation (pawn center control, knight centralization, king safety, rook on 7th rank, bishop pair bonus)
- **Transposition table** with Zobrist hashing to avoid re-evaluating repeated positions
- **Repetition and 50-move rule** draw detection
- **1200 ELO tuning** — controlled randomness (±25 centipawns) among near-equal moves for human-like play
- **UCI protocol support** — works with Arena, Cute Chess, or any UCI-compatible GUI
- **Interactive console mode** with commands to play, evaluate, and inspect positions
- **Perft testing** — verified against standard test positions (starting position, Kiwipete, CPW endgame, castling edge cases, en passant)

---

## Getting Started

### Requirements
- Java 8 or higher

### Compile
```bash
javac SimpleChessEngine.java
```

### Run — Console Mode (play against the engine)
```bash
java SimpleChessEngine console
```

### Run — UCI Mode (for chess GUIs like Arena or Cute Chess)
```bash
java SimpleChessEngine
```

### Run — Perft Tests (verify move generation)
```bash
java SimpleChessEngine perft
```

---

## Console Commands

| Command | Description |
|---|---|
| `d` / `display` | Display the current board |
| `go` | Let the engine make a move |
| `move e2e4` | Make a move in UCI notation |
| `list` | Show all legal moves |
| `eval` | Show position evaluation in centipawns |
| `perft <depth>` | Count all positions to a given depth |
| `divide <depth>` | Perft with per-move breakdown |
| `tests` | Run all standard perft test positions |
| `new` | Start a new game |
| `depth <n>` | Set engine search depth (default: 5) |
| `fen <string>` | Load a position from a FEN string |
| `help` | Show all commands |
| `quit` | Exit |

**Move format:** UCI notation — `e2e4`, `g1f3`, `e7e8q` (for promotion)

---

## How It Works

### Search
The engine uses **iterative deepening alpha-beta** search. It starts at depth 1 and deepens each iteration, always keeping the best move found so far. At leaf nodes, **quiescence search** continues until no captures remain, preventing the engine from making blunders at the edge of its search horizon.

**Alpha-beta complexity:**
| Case | Time Complexity |
|---|---|
| Best case (perfect ordering) | O(b^(d/2)) |
| Average case | O(b^(3d/4)) |
| Worst case (no pruning) | O(b^d) |

Where `b` ≈ 35 (branching factor) and `d` = search depth.

### Move Ordering
Moves are ordered using **MVV-LVA** — captures of high-value pieces by low-value pieces are searched first. The transposition table's stored best move gets top priority. This maximizes the number of alpha-beta cutoffs.

### Evaluation
The evaluation function scans the board once (O(1)) and considers:
- Material balance using standard centipawn values (P=100, N=320, B=330, R=500, Q=900)
- Piece-square tables for positional bonuses
- Endgame detection (switches king tables when total material drops below threshold)
- Mobility bonus (legal move count difference)
- Bishop pair bonus (+30 centipawns)

### Hashing
Zobrist hashing generates a unique 64-bit key per position factoring in piece placement, side to move, castling rights, and en passant file. Used for the transposition table and repetition detection.

---

## Perft Results (Move Generation Verification)

| Position | Depth | Expected | 
|---|---|---|
| Starting Position | 4 | 197,281 |
| Kiwipete | 3 | 97,862 |
| CPW Endgame | 4 | 43,238 |
| Castling Test | 2 | 568 |

---

## Project Structure

```
SimpleChessEngine.java   — Single-file engine (1891 lines)
  ├── Board representation (8x8 int array, FEN parsing)
  ├── Move generation (pseudo-legal + legality filter)
  ├── Make/Unmake move
  ├── Attack detection & check detection
  ├── Evaluation function + piece-square tables
  ├── Alpha-beta search + quiescence
  ├── Zobrist hashing + transposition table
  ├── Perft testing
  ├── UCI protocol handler
  └── Console mode
```

---

## Tech

- **Language:** Java 21
- **Single file, zero dependencies**
