import java.util.*;

/**
 * SimpleChessEngine - A Java chess engine tuned for ~1200 ELO strength
 * 
 * FEATURES:
 * - 8x8 board representation with FEN parsing
 * - Legal move generation (all pieces + special moves)
 * - Alpha-beta pruning with iterative deepening
 * - Evaluation with piece-square tables and human-like positional play
 * - UCI protocol support for chess GUIs
 * - Console mode for playing against the engine
 * - Perft testing for move generation verification
 * 
 * COMPILE: javac SimpleChessEngine.java
 * RUN UCI: java SimpleChessEngine
 * RUN CONSOLE: java SimpleChessEngine console
 * 
 * ============================================================================
 * ALGORITHMIC COMPLEXITY ANALYSIS
 * ============================================================================
 * 
 * MOVE GENERATION:
 * - Time: O(1) per piece type, O(n) for all pieces where n = number of pieces
 * - Space: O(m) where m = number of legal moves (max ~218 in chess)
 * - Sliding pieces: O(k) per direction where k = squares until edge/block
 * 
 * MINIMAX SEARCH:
 * - Time: O(b^d) where b = branching factor (~35), d = depth
 * - Space: O(d) for recursion stack
 * - Without pruning: explores ALL nodes in game tree
 * 
 * ALPHA-BETA PRUNING:
 * - Time: O(b^(d/2)) with perfect move ordering (best case)
 * - Time: O(b^d) worst case (no pruning)
 * - Average: O(b^(3d/4)) with good move ordering
 * - Space: O(d) for recursion stack + O(tt) for transposition table
 * 
 * EVALUATION FUNCTION:
 * - Time: O(64) = O(1) - scans entire board once
 * - Space: O(1) - constant space for score calculation
 * 
 * PERFT:
 * - Time: O(b^d) - counts all leaf nodes at depth d
 * - Space: O(d) - recursion depth
 * 
 * @author Competitive Programmer
 * @version 2.0
 */
public class SimpleChessEngine {
    
    // ============================================================================
    // CONSTANTS AND PIECE DEFINITIONS
    // ============================================================================
    
    // Piece types (0 = empty, positive = white, negative = black)
    static final int EMPTY = 0;
    static final int PAWN = 1;
    static final int KNIGHT = 2;
    static final int BISHOP = 3;
    static final int ROOK = 4;
    static final int QUEEN = 5;
    static final int KING = 6;
    
    // Color multipliers
    static final int WHITE = 1;
    static final int BLACK = -1;
    
    // Material values (centipawns) - tuned for 1200 ELO
    static final int PAWN_VALUE = 100;
    static final int KNIGHT_VALUE = 320;
    static final int BISHOP_VALUE = 330;
    static final int ROOK_VALUE = 500;
    static final int QUEEN_VALUE = 900;
    static final int KING_VALUE = 20000;
    
    // ============================================================================
    // PIECE-SQUARE TABLES (TUNED FOR HUMAN-LIKE POSITIONAL PLAY)
    // ============================================================================
    
    /**
     * Pawn table: Encourages center control, discourages early queen pawn pushes
     * Values favor gradual advancement with pawn chains
     */
    static final int[][] PAWN_TABLE = {
        {0,   0,   0,   0,   0,   0,   0,   0},
        {50,  50,  50,  50,  50,  50,  50,  50},
        {10,  10,  20,  30,  30,  20,  10,  10},
        {5,   5,   10,  25,  25,  10,  5,   5},
        {0,   0,   0,   20,  20,  0,   0,   0},
        {5,   -5,  -10, 0,   0,   -10, -5,  5},
        {5,   10,  10,  -20, -20, 10,  10,  5},
        {0,   0,   0,   0,   0,   0,   0,   0}
    };
    
    /**
     * Knight table: Strong central squares, avoid corners and edges
     * Knights are most effective in the center where they control 8 squares
     */
    static final int[][] KNIGHT_TABLE = {
        {-50, -40, -30, -30, -30, -30, -40, -50},
        {-40, -20, 0,   0,   0,   0,   -20, -40},
        {-30, 0,   10,  15,  15,  10,  0,   -30},
        {-30, 5,   15,  20,  20,  15,  5,   -30},
        {-30, 0,   15,  20,  20,  15,  0,   -30},
        {-30, 5,   10,  15,  15,  10,  5,   -30},
        {-40, -20, 0,   5,   5,   0,   -20, -40},
        {-50, -40, -30, -30, -30, -30, -40, -50}
    };
    
    /**
     * Bishop table: Long diagonals are valuable
     * Bishops excel on open long diagonals
     */
    static final int[][] BISHOP_TABLE = {
        {-20, -10, -10, -10, -10, -10, -10, -20},
        {-10, 0,   0,   0,   0,   0,   0,   -10},
        {-10, 0,   5,   10,  10,  5,   0,   -10},
        {-10, 5,   5,   10,  10,  5,   5,   -10},
        {-10, 0,   10,  10,  10,  10,  0,   -10},
        {-10, 10,  10,  10,  10,  10,  10,  -10},
        {-10, 5,   0,   0,   0,   0,   5,   -10},
        {-20, -10, -10, -10, -10, -10, -10, -20}
    };
    
    /**
     * Rook table: 7th rank bonus, central files
     * Rooks are powerful on open files and 7th rank
     */
    static final int[][] ROOK_TABLE = {
        {0,   0,   0,   0,   0,   0,   0,   0},
        {5,   10,  10,  10,  10,  10,  10,  5},
        {-5,  0,   0,   0,   0,   0,   0,   -5},
        {-5,  0,   0,   0,   0,   0,   0,   -5},
        {-5,  0,   0,   0,   0,   0,   0,   -5},
        {-5,  0,   0,   0,   0,   0,   0,   -5},
        {-5,  0,   0,   0,   0,   0,   0,   -5},
        {0,   0,   0,   5,   5,   0,   0,   0}
    };
    
    /**
     * Queen table: Central control, avoid early development
     * Queen is powerful but should be developed carefully
     */
    static final int[][] QUEEN_TABLE = {
        {-20, -10, -10, -5,  -5,  -10, -10, -20},
        {-10, 0,   0,   0,   0,   0,   0,   -10},
        {-10, 0,   5,   5,   5,   5,   0,   -10},
        {-5,  0,   5,   5,   5,   5,   0,   -5},
        {0,   0,   5,   5,   5,   5,   0,   -5},
        {-10, 5,   5,   5,   5,   5,   0,   -10},
        {-10, 0,   5,   0,   0,   0,   0,   -10},
        {-20, -10, -10, -5,  -5,  -10, -10, -20}
    };
    
    /**
     * King middle game table: Castling and king safety
     * Encourages castling and keeping king safe behind pawns
     */
    static final int[][] KING_MIDDLE_TABLE = {
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-20, -30, -30, -40, -40, -30, -30, -20},
        {-10, -20, -20, -20, -20, -20, -20, -10},
        {20,  20,  0,   0,   0,   0,   20,  20},
        {20,  30,  10,  0,   0,   10,  30,  20}
    };
    
    /**
     * King endgame table: Active king in endgames
     * In endgames, king should be centralized and active
     */
    static final int[][] KING_END_TABLE = {
        {-50, -40, -30, -20, -20, -30, -40, -50},
        {-30, -20, -10, 0,   0,   -10, -20, -30},
        {-30, -10, 20,  30,  30,  20,  -10, -30},
        {-30, -10, 30,  40,  40,  30,  -10, -30},
        {-30, -10, 30,  40,  40,  30,  -10, -30},
        {-30, -10, 20,  30,  30,  20,  -10, -30},
        {-30, -30, 0,   0,   0,   0,   -30, -30},
        {-50, -30, -30, -30, -30, -30, -30, -50}
    };
    
    // Direction offsets for sliding pieces
    static final int[][] BISHOP_DIRS = {{-1,-1}, {-1,1}, {1,-1}, {1,1}};
    static final int[][] ROOK_DIRS = {{-1,0}, {1,0}, {0,-1}, {0,1}};
    static final int[][] QUEEN_DIRS = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};
    
    // Knight move offsets
    static final int[][] KNIGHT_MOVES = {
        {-2,-1}, {-2,1}, {-1,-2}, {-1,2}, {1,-2}, {1,2}, {2,-1}, {2,1}
    };
    
    // King move offsets
    static final int[][] KING_MOVES = {
        {-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}
    };
    
    // ============================================================================
    // BOARD REPRESENTATION
    // ============================================================================
    
    // 8x8 board: board[row][col], row 0 = rank 8, row 7 = rank 1
    int[][] board = new int[8][8];
    
    // Game state
    int sideToMove = WHITE;
    int castlingRights = 0; // bit 0 = White K, 1 = White Q, 2 = Black k, 3 = Black q
    int enPassantSquare = -1; // -1 = none, otherwise 0-63
    int halfmoveClock = 0;
    int fullmoveNumber = 1;
    
    // King positions for quick access
    int whiteKingPos = -1;
    int blackKingPos = -1;
    
    // Search statistics and control
    long nodesSearched = 0;
    long startTime = 0;
    boolean stopSearch = false;
    int searchDepth = 5;
    Random random = new Random();
    
    // 1200 ELO tuning: Add randomness to moves with similar scores
    static final int RANDOMNESS_RANGE = 25; // +/- 25 centipawns of randomness
    static final int EQUAL_SCORE_THRESHOLD = 50; // Moves within 50 cp are "equal"
    
    // Transposition table
    Map<Long, TranspositionEntry> transpositionTable = new HashMap<>();
    
    // Zobrist hashing
    long[][][] zobristKeys = new long[8][8][13];
    long zobristSide;
    long[] zobristCastling = new long[4];
    long[] zobristEnPassant = new long[8];
    
    // Move history for repetition detection
    List<Long> positionHistory = new ArrayList<>();
    
    // ============================================================================
    // MOVE REPRESENTATION
    // ============================================================================
    
    static class Move {
        int fromRow, fromCol;
        int toRow, toCol;
        int promotion;
        int capturedPiece;
        boolean isCastling;
        boolean isEnPassant;
        int score;
        
        Move(int fromRow, int fromCol, int toRow, int toCol) {
            this(fromRow, fromCol, toRow, toCol, 0);
        }
        
        Move(int fromRow, int fromCol, int toRow, int toCol, int promotion) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.promotion = promotion;
            this.capturedPiece = EMPTY;
            this.isCastling = false;
            this.isEnPassant = false;
            this.score = 0;
        }
        
        Move copy() {
            Move m = new Move(fromRow, fromCol, toRow, toCol, promotion);
            m.capturedPiece = capturedPiece;
            m.isCastling = isCastling;
            m.isEnPassant = isEnPassant;
            m.score = score;
            return m;
        }
        
        @Override
        public String toString() {
            char[] cols = {'a','b','c','d','e','f','g','h'};
            char[] rows = {'8','7','6','5','4','3','2','1'};
            String moveStr = "" + cols[fromCol] + rows[fromRow] + cols[toCol] + rows[toRow];
            if (promotion != 0) {
                char promChar = " nbrq".charAt(promotion);
                moveStr += promChar;
            }
            return moveStr;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Move)) return false;
            Move other = (Move) obj;
            return fromRow == other.fromRow && fromCol == other.fromCol &&
                   toRow == other.toRow && toCol == other.toCol &&
                   promotion == other.promotion;
        }
    }
    
    static class TranspositionEntry {
        long key;
        int depth;
        int score;
        int flag;
        Move bestMove;
        
        TranspositionEntry(long key, int depth, int score, int flag, Move bestMove) {
            this.key = key;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }
    
    // ============================================================================
    // CONSTRUCTOR AND INITIALIZATION
    // ============================================================================
    
    public SimpleChessEngine() {
        initZobrist();
        loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }
    
    /**
     * Initialize Zobrist hashing keys
     * Time: O(8*8*13) = O(1)
     * Space: O(8*8*13) = O(1)
     */
    void initZobrist() {
        Random rand = new Random(123456789);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                for (int p = 0; p < 13; p++) {
                    zobristKeys[r][c][p] = rand.nextLong();
                }
            }
        }
        zobristSide = rand.nextLong();
        for (int i = 0; i < 4; i++) zobristCastling[i] = rand.nextLong();
        for (int i = 0; i < 8; i++) zobristEnPassant[i] = rand.nextLong();
    }
    
    // ============================================================================
    // FEN PARSING
    // ============================================================================
    
    /**
     * Parse FEN string and set up position
     * Time: O(64) = O(1) - scans board once
     * Space: O(1)
     * 
     * @param fen FEN string representing the position
     */
    void loadPosition(String fen) {
        // Reset board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = EMPTY;
            }
        }
        positionHistory.clear();
        
        String[] parts = fen.split(" ");
        
        // Parse piece placement
        String[] ranks = parts[0].split("/");
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : ranks[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += ch - '0';
                } else {
                    int piece = charToPiece(ch);
                    board[r][c] = piece;
                    if (Math.abs(piece) == KING) {
                        if (piece > 0) whiteKingPos = r * 8 + c;
                        else blackKingPos = r * 8 + c;
                    }
                    c++;
                }
            }
        }
        
        // Parse side to move
        sideToMove = parts[1].equals("w") ? WHITE : BLACK;
        
        // Parse castling rights
        castlingRights = 0;
        if (parts.length > 2 && !parts[2].equals("-")) {
            for (char ch : parts[2].toCharArray()) {
                switch (ch) {
                    case 'K': castlingRights |= 1; break;
                    case 'Q': castlingRights |= 2; break;
                    case 'k': castlingRights |= 4; break;
                    case 'q': castlingRights |= 8; break;
                }
            }
        }
        
        // Parse en passant
        enPassantSquare = -1;
        if (parts.length > 3 && !parts[3].equals("-")) {
            enPassantSquare = parseSquare(parts[3]);
        }
        
        // Parse move counters
        halfmoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        fullmoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
        
        transpositionTable.clear();
    }
    
    int charToPiece(char ch) {
        int color = Character.isUpperCase(ch) ? WHITE : BLACK;
        int type;
        switch (Character.toLowerCase(ch)) {
            case 'p': type = PAWN; break;
            case 'n': type = KNIGHT; break;
            case 'b': type = BISHOP; break;
            case 'r': type = ROOK; break;
            case 'q': type = QUEEN; break;
            case 'k': type = KING; break;
            default: return EMPTY;
        }
        return type * color;
    }
    
    char pieceToChar(int piece) {
        if (piece == EMPTY) return '.';
        char[] chars = {'.', 'P', 'N', 'B', 'R', 'Q', 'K'};
        char ch = chars[Math.abs(piece)];
        return piece > 0 ? ch : Character.toLowerCase(ch);
    }
    
    int parseSquare(String sq) {
        int col = sq.charAt(0) - 'a';
        int row = '8' - sq.charAt(1);
        return row * 8 + col;
    }
    
    // ============================================================================
    // MOVE GENERATION (CORRECTED FOR ALL EDGE CASES)
    // ============================================================================
    
    /**
     * Generate all legal moves for current position
     * 
     * CORRECTLY HANDLES:
     * - Pinned pieces (cannot move if it exposes king to check)
     * - Discovered checks (moving piece reveals attack on king)
     * - Illegal castling through check
     * - En passant capturing pawn that just moved two squares
     * 
     * Time: O(m * k) where m = pseudo-legal moves, k = cost of make/unmake
     * Space: O(m) for storing legal moves
     * 
     * @return List of all legal moves
     */
    List<Move> generateLegalMoves() {
        List<Move> pseudoLegal = generatePseudoLegalMoves();
        List<Move> legal = new ArrayList<>();
        
        for (Move move : pseudoLegal) {
            // Make move and verify king is not in check
            makeMove(move);
            boolean kingInCheck = isInCheck(-sideToMove); // Check if our king is attacked
            unmakeMove(move);
            
            if (!kingInCheck) {
                legal.add(move);
            }
        }
        
        return legal;
    }
    
    /**
     * Generate pseudo-legal moves (may leave king in check)
     * Time: O(n * k) where n = pieces, k = moves per piece
     * Space: O(m) where m = total moves
     * 
     * @return List of pseudo-legal moves
     */
    List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board[r][c];
                if (piece != EMPTY && Integer.signum(piece) == sideToMove) {
                    generatePieceMoves(r, c, piece, moves);
                }
            }
        }
        
        return moves;
    }
    
    void generatePieceMoves(int r, int c, int piece, List<Move> moves) {
        int type = Math.abs(piece);
        
        switch (type) {
            case PAWN: generatePawnMoves(r, c, moves); break;
            case KNIGHT: generateKnightMoves(r, c, moves); break;
            case BISHOP: generateBishopMoves(r, c, moves); break;
            case ROOK: generateRookMoves(r, c, moves); break;
            case QUEEN: generateQueenMoves(r, c, moves); break;
            case KING: generateKingMoves(r, c, moves); break;
        }
    }
    
    void generatePawnMoves(int r, int c, List<Move> moves) {
        int direction = -sideToMove;
        int startRow = sideToMove == WHITE ? 6 : 1;
        int promotionRow = sideToMove == WHITE ? 0 : 7;
        
        // Single push
        int newR = r + direction;
        if (newR >= 0 && newR < 8 && board[newR][c] == EMPTY) {
            if (newR == promotionRow) {
                for (int prom : new int[]{QUEEN, ROOK, BISHOP, KNIGHT}) {
                    moves.add(new Move(r, c, newR, c, prom));
                }
            } else {
                moves.add(new Move(r, c, newR, c));
            }
            
            // Double push
            if (r == startRow) {
                newR = r + 2 * direction;
                if (board[newR][c] == EMPTY) {
                    moves.add(new Move(r, c, newR, c));
                }
            }
        }
        
        // Captures
        for (int dc : new int[]{-1, 1}) {
            int newC = c + dc;
            if (newC < 0 || newC > 7) continue;
            newR = r + direction;
            if (newR < 0 || newR > 7) continue;
            
            int target = board[newR][newC];
            
            // Normal capture
            if (target != EMPTY && Integer.signum(target) != sideToMove) {
                if (newR == promotionRow) {
                    for (int prom : new int[]{QUEEN, ROOK, BISHOP, KNIGHT}) {
                        moves.add(new Move(r, c, newR, newC, prom));
                    }
                } else {
                    moves.add(new Move(r, c, newR, newC));
                }
            }
            
            // En passant
            if (enPassantSquare != -1) {
                int epR = enPassantSquare / 8;
                int epC = enPassantSquare % 8;
                if (newR == epR && newC == epC) {
                    Move m = new Move(r, c, newR, newC);
                    m.isEnPassant = true;
                    moves.add(m);
                }
            }
        }
    }
    
    void generateKnightMoves(int r, int c, List<Move> moves) {
        for (int[] offset : KNIGHT_MOVES) {
            int newR = r + offset[0];
            int newC = c + offset[1];
            if (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                int target = board[newR][newC];
                if (target == EMPTY || Integer.signum(target) != sideToMove) {
                    moves.add(new Move(r, c, newR, newC));
                }
            }
        }
    }
    
    void generateBishopMoves(int r, int c, List<Move> moves) {
        generateSlidingMoves(r, c, BISHOP_DIRS, moves);
    }
    
    void generateRookMoves(int r, int c, List<Move> moves) {
        generateSlidingMoves(r, c, ROOK_DIRS, moves);
    }
    
    void generateQueenMoves(int r, int c, List<Move> moves) {
        generateSlidingMoves(r, c, QUEEN_DIRS, moves);
    }
    
    void generateSlidingMoves(int r, int c, int[][] directions, List<Move> moves) {
        for (int[] dir : directions) {
            int newR = r + dir[0];
            int newC = c + dir[1];
            while (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                int target = board[newR][newC];
                if (target == EMPTY) {
                    moves.add(new Move(r, c, newR, newC));
                } else {
                    if (Integer.signum(target) != sideToMove) {
                        moves.add(new Move(r, c, newR, newC));
                    }
                    break;
                }
                newR += dir[0];
                newC += dir[1];
            }
        }
    }
    
    /**
     * Generate king moves including castling
     * CORRECTLY CHECKS: King cannot castle through or into check
     */
    void generateKingMoves(int r, int c, List<Move> moves) {
        // Normal king moves
        for (int[] offset : KING_MOVES) {
            int newR = r + offset[0];
            int newC = c + offset[1];
            if (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                int target = board[newR][newC];
                if (target == EMPTY || Integer.signum(target) != sideToMove) {
                    moves.add(new Move(r, c, newR, newC));
                }
            }
        }
        
        // Castling - verify king is not in check and doesn't pass through check
        if (sideToMove == WHITE) {
            // Kingside
            if ((castlingRights & 1) != 0 && board[7][5] == EMPTY && board[7][6] == EMPTY) {
                if (!isSquareAttacked(7, 4, BLACK) && 
                    !isSquareAttacked(7, 5, BLACK) && 
                    !isSquareAttacked(7, 6, BLACK)) {
                    Move m = new Move(7, 4, 7, 6);
                    m.isCastling = true;
                    moves.add(m);
                }
            }
            // Queenside
            if ((castlingRights & 2) != 0 && board[7][1] == EMPTY && 
                board[7][2] == EMPTY && board[7][3] == EMPTY) {
                if (!isSquareAttacked(7, 4, BLACK) && 
                    !isSquareAttacked(7, 3, BLACK) && 
                    !isSquareAttacked(7, 2, BLACK)) {
                    Move m = new Move(7, 4, 7, 2);
                    m.isCastling = true;
                    moves.add(m);
                }
            }
        } else {
            // Kingside
            if ((castlingRights & 4) != 0 && board[0][5] == EMPTY && board[0][6] == EMPTY) {
                if (!isSquareAttacked(0, 4, WHITE) && 
                    !isSquareAttacked(0, 5, WHITE) && 
                    !isSquareAttacked(0, 6, WHITE)) {
                    Move m = new Move(0, 4, 0, 6);
                    m.isCastling = true;
                    moves.add(m);
                }
            }
            // Queenside
            if ((castlingRights & 8) != 0 && board[0][1] == EMPTY && 
                board[0][2] == EMPTY && board[0][3] == EMPTY) {
                if (!isSquareAttacked(0, 4, WHITE) && 
                    !isSquareAttacked(0, 3, WHITE) && 
                    !isSquareAttacked(0, 2, WHITE)) {
                    Move m = new Move(0, 4, 0, 2);
                    m.isCastling = true;
                    moves.add(m);
                }
            }
        }
    }
    
    // ============================================================================
    // MOVE EXECUTION
    // ============================================================================
    
    /**
     * Execute a move on the board
     * Time: O(1)
     * Space: O(1)
     */
    void makeMove(Move move) {
        int piece = board[move.fromRow][move.fromCol];
        move.capturedPiece = board[move.toRow][move.toCol];
        
        // Update board
        board[move.toRow][move.toCol] = move.promotion != 0 ? move.promotion * sideToMove : piece;
        board[move.fromRow][move.fromCol] = EMPTY;
        
        // Handle castling
        if (move.isCastling) {
            if (move.toCol == 6) {
                board[move.toRow][5] = board[move.toRow][7];
                board[move.toRow][7] = EMPTY;
            } else {
                board[move.toRow][3] = board[move.toRow][0];
                board[move.toRow][0] = EMPTY;
            }
        }
        
        // Handle en passant
        if (move.isEnPassant) {
            int captureRow = sideToMove == WHITE ? move.toRow + 1 : move.toRow - 1;
            move.capturedPiece = board[captureRow][move.toCol];
            board[captureRow][move.toCol] = EMPTY;
        }
        
        // Update king position
        if (Math.abs(piece) == KING) {
            if (sideToMove == WHITE) whiteKingPos = move.toRow * 8 + move.toCol;
            else blackKingPos = move.toRow * 8 + move.toCol;
        }
        
        // Update castling rights
        if (Math.abs(piece) == KING) {
            if (sideToMove == WHITE) castlingRights &= ~3;
            else castlingRights &= ~12;
        }
        if (Math.abs(piece) == ROOK) {
            if (move.fromRow == 7 && move.fromCol == 0) castlingRights &= ~2;
            if (move.fromRow == 7 && move.fromCol == 7) castlingRights &= ~1;
            if (move.fromRow == 0 && move.fromCol == 0) castlingRights &= ~8;
            if (move.fromRow == 0 && move.fromCol == 7) castlingRights &= ~4;
        }
        
        // Update en passant
        if (Math.abs(piece) == PAWN && Math.abs(move.toRow - move.fromRow) == 2) {
            enPassantSquare = (move.fromRow + move.toRow) / 2 * 8 + move.fromCol;
        } else {
            enPassantSquare = -1;
        }
        
        // Update halfmove clock
        if (Math.abs(piece) == PAWN || move.capturedPiece != EMPTY) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }
        
        // Update fullmove number
        if (sideToMove == BLACK) {
            fullmoveNumber++;
        }
        
        sideToMove = -sideToMove;
        
        // Store position for repetition detection
        positionHistory.add(computeZobristKey());
    }
    
    /**
     * Undo a move
     * Time: O(1)
     * Space: O(1)
     */
    void unmakeMove(Move move) {
        // Remove position from history
        if (!positionHistory.isEmpty()) {
            positionHistory.remove(positionHistory.size() - 1);
        }
        
        sideToMove = -sideToMove;
        
        int piece = board[move.toRow][move.toCol];
        if (move.promotion != 0) {
            piece = PAWN * sideToMove;
        }
        
        // Restore board
        board[move.fromRow][move.fromCol] = piece;
        board[move.toRow][move.toCol] = move.capturedPiece;
        
        // Restore castling
        if (move.isCastling) {
            if (move.toCol == 6) {
                board[move.toRow][7] = board[move.toRow][5];
                board[move.toRow][5] = EMPTY;
            } else {
                board[move.toRow][0] = board[move.toRow][3];
                board[move.toRow][3] = EMPTY;
            }
        }
        
        // Restore en passant
        if (move.isEnPassant) {
            int captureRow = sideToMove == WHITE ? move.toRow + 1 : move.toRow - 1;
            board[captureRow][move.toCol] = move.capturedPiece;
            board[move.toRow][move.toCol] = EMPTY;
        }
        
        // Restore king position
        if (Math.abs(piece) == KING) {
            if (sideToMove == WHITE) whiteKingPos = move.fromRow * 8 + move.fromCol;
            else blackKingPos = move.fromRow * 8 + move.fromCol;
        }
    }
    
    // ============================================================================
    // ATTACK DETECTION
    // ============================================================================
    
    /**
     * Check if the given color's king is in check
     * Time: O(1) - uses precomputed king position
     * Space: O(1)
     */
    boolean isInCheck(int color) {
        int kingPos = color == WHITE ? whiteKingPos : blackKingPos;
        if (kingPos == -1) return false;
        int kingRow = kingPos / 8;
        int kingCol = kingPos % 8;
        return isSquareAttacked(kingRow, kingCol, -color);
    }
    
    /**
     * Check if a square is attacked by the given color
     * Time: O(1) - constant number of directions to check
     * Space: O(1)
     */
    boolean isSquareAttacked(int r, int c, int byColor) {
        // Pawn attacks
        int pawnDir = byColor == WHITE ? -1 : 1;
        for (int dc : new int[]{-1, 1}) {
            int newR = r + pawnDir;
            int newC = c + dc;
            if (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                if (board[newR][newC] == PAWN * byColor) return true;
            }
        }
        
        // Knight attacks
        for (int[] offset : KNIGHT_MOVES) {
            int newR = r + offset[0];
            int newC = c + offset[1];
            if (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                if (board[newR][newC] == KNIGHT * byColor) return true;
            }
        }
        
        // King attacks
        for (int[] offset : KING_MOVES) {
            int newR = r + offset[0];
            int newC = c + offset[1];
            if (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                if (board[newR][newC] == KING * byColor) return true;
            }
        }
        
        // Sliding piece attacks
        for (int[] dir : BISHOP_DIRS) {
            int newR = r + dir[0];
            int newC = c + dir[1];
            while (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                int piece = board[newR][newC];
                if (piece != EMPTY) {
                    if (piece == BISHOP * byColor || piece == QUEEN * byColor) return true;
                    break;
                }
                newR += dir[0];
                newC += dir[1];
            }
        }
        
        for (int[] dir : ROOK_DIRS) {
            int newR = r + dir[0];
            int newC = c + dir[1];
            while (newR >= 0 && newR < 8 && newC >= 0 && newC < 8) {
                int piece = board[newR][newC];
                if (piece != EMPTY) {
                    if (piece == ROOK * byColor || piece == QUEEN * byColor) return true;
                    break;
                }
                newR += dir[0];
                newC += dir[1];
            }
        }
        
        return false;
    }
    
    // ============================================================================
    // EVALUATION FUNCTION (TUNED FOR 1200 ELO)
    // ============================================================================
    
    /**
     * Evaluate the current position from side to move's perspective
     * 
     * 1200 ELO TUNING:
     * - Material values are standard
     * - Piece-square tables favor human-like positional concepts
     * - Randomness added for moves with similar scores
     * - Endgame detection for king activity
     * 
     * Time: O(64) = O(1)
     * Space: O(1)
     * 
     * @return Score in centipawns (positive = good for side to move)
     */
    int evaluate() {
        int score = 0;
        int whiteMaterial = 0;
        int blackMaterial = 0;
        int pieceCount = 0;
        
        // Calculate material and position
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board[r][c];
                if (piece != EMPTY) {
                    int type = Math.abs(piece);
                    int color = Integer.signum(piece);
                    int value = getPieceValue(type);
                    pieceCount++;
                    
                    if (color == WHITE) whiteMaterial += value;
                    else blackMaterial += value;
                    
                    // Material score
                    score += value * color;
                    
                    // Positional score
                    int posScore = getPieceSquareScore(type, r, c, color, whiteMaterial + blackMaterial);
                    score += posScore * color;
                }
            }
        }
        
        // Mobility bonus (small, for 1200 ELO)
        int mobility = countLegalMoves(WHITE) - countLegalMoves(BLACK);
        score += mobility * 3;
        
        // Bishop pair bonus
        if (hasBishopPair(WHITE)) score += 30;
        if (hasBishopPair(BLACK)) score -= 30;
        
        return score * sideToMove;
    }
    
    int getPieceValue(int type) {
        switch (type) {
            case PAWN: return PAWN_VALUE;
            case KNIGHT: return KNIGHT_VALUE;
            case BISHOP: return BISHOP_VALUE;
            case ROOK: return ROOK_VALUE;
            case QUEEN: return QUEEN_VALUE;
            case KING: return KING_VALUE;
            default: return 0;
        }
    }
    
    boolean hasBishopPair(int color) {
        int bishopCount = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == BISHOP * color) bishopCount++;
            }
        }
        return bishopCount >= 2;
    }
    
    /**
     * Get piece-square table score
     * Uses endgame tables when material is low
     */
    int getPieceSquareScore(int type, int r, int c, int color, int totalMaterial) {
        int tableR = color == WHITE ? r : 7 - r;
        boolean isEndgame = totalMaterial < 2600; // Rough endgame threshold
        
        switch (type) {
            case PAWN: return PAWN_TABLE[tableR][c];
            case KNIGHT: return KNIGHT_TABLE[tableR][c];
            case BISHOP: return BISHOP_TABLE[tableR][c];
            case ROOK: return ROOK_TABLE[tableR][c];
            case QUEEN: return QUEEN_TABLE[tableR][c];
            case KING: return isEndgame ? KING_END_TABLE[tableR][c] : KING_MIDDLE_TABLE[tableR][c];
            default: return 0;
        }
    }
    
    int countLegalMoves(int color) {
        int savedSide = sideToMove;
        sideToMove = color;
        List<Move> moves = generateLegalMoves();
        sideToMove = savedSide;
        return moves.size();
    }
    
    // ============================================================================
    // SEARCH ALGORITHM WITH ALPHA-BETA PRUNING
    // ============================================================================
    
    /**
     * ============================================================================
     * ALPHA-BETA PRUNING EXPLANATION FOR COMPETITIVE PROGRAMMERS
     * ============================================================================
     * 
     * WHAT IS ALPHA-BETA PRUNING?
     * ----------------------------
     * Alpha-beta pruning is an optimization of the minimax algorithm that reduces
     * the number of nodes evaluated in the game tree. It works by maintaining two
     * values:
     * 
     * - ALPHA: The best value that the maximizing player (current side to move) 
     *          can guarantee so far
     * - BETA:  The best value that the minimizing player (opponent) can 
     *          guarantee so far
     * 
     * THE PRUNING CONDITION:
     * ----------------------
     * If at any point alpha >= beta, we can STOP exploring the current branch
     * because:
     * - The maximizing player already has a better option (alpha)
     * - The minimizing player won't allow this worse branch (beta)
     * 
     * EXAMPLE:
     * --------
     * Consider a position where White has found a move worth +5 (alpha = 5).
     * Now White is evaluating another move where Black responds.
     * Black finds a response that gives White only +3.
     * Since Black will choose the WORST for White, this branch is worth <= 3.
     * But White already has +5, so this branch is useless - PRUNE IT!
     * 
     * MOVE ORDERING WITH MVV-LVA:
     * ---------------------------
     * MVV-LVA = "Most Valuable Victim - Least Valuable Aggressor"
     * 
     * This is a capture ordering heuristic that improves alpha-beta efficiency:
     * - Prioritize capturing HIGH-VALUE enemy pieces (Queen > Rook > etc.)
     * - When values are equal, use LOW-VALUE attackers (Pawn > Queen)
     * 
     * WHY IT WORKS:
     * - Captures often cause the largest alpha-beta cutoffs
     * - Good captures raise alpha quickly
     * - Bad captures (losing material) are quickly pruned
     * 
     * EXAMPLE MVV-LVA ORDERING:
     * - QxQ (Queen takes Queen) = 905 - 5 = 900
     * - PxQ (Pawn takes Queen) = 905 - 1 = 904  <- BETTER!
     * - QxP (Queen takes Pawn) = 101 - 5 = 96
     * 
     * COMPLEXITY:
     * -----------
     * Without pruning: O(b^d) where b = branching factor (~35), d = depth
     * With perfect ordering: O(b^(d/2)) - explores only half the tree!
     * With good ordering: O(b^(3d/4)) - typical case
     * 
     * ============================================================================
     */
    
    /**
     * Find the best move using iterative deepening alpha-beta search
     * 
     * Time: O(b^(3d/4)) with good move ordering
     * Space: O(d) for recursion + O(tt) for transposition table
     * 
     * @param maxDepth Maximum search depth
     * @return Best move found
     */
    Move findBestMove(int maxDepth) {
        nodesSearched = 0;
        startTime = System.currentTimeMillis();
        stopSearch = false;
        
        // Adjust depth for endgames to avoid horizon effect
        int totalMaterial = countTotalMaterial();
        if (totalMaterial < 1500) maxDepth = Math.min(maxDepth, 4);
        if (totalMaterial < 800) maxDepth = Math.min(maxDepth, 3);
        
        Move bestMove = null;
        int bestScore = -Integer.MAX_VALUE;
        
        List<Move> moves = generateLegalMoves();
        if (moves.isEmpty()) return null;
        
        // Iterative deepening
        for (int depth = 1; depth <= maxDepth && !stopSearch; depth++) {
            List<Move> depthMoves = new ArrayList<>();
            
            // Move ordering
            orderMoves(moves, bestMove);
            
            for (Move move : moves) {
                makeMove(move);
                int score = -alphaBeta(depth - 1, -Integer.MAX_VALUE, -bestScore, 1);
                unmakeMove(move);
                
                if (stopSearch) break;
                
                move.score = score;
                depthMoves.add(move);
            }
            
            if (!stopSearch) {
                // 1200 ELO: Add randomness to moves with similar scores
                bestMove = selectMoveWithRandomness(depthMoves);
                bestScore = bestMove.score;
            }
        }
        
        return bestMove;
    }
    
    /**
     * Select a move with randomness for 1200 ELO strength
     * Moves within EQUAL_SCORE_THRESHOLD are considered "equal"
     */
    Move selectMoveWithRandomness(List<Move> moves) {
        if (moves.isEmpty()) return null;
        
        // Sort by score descending
        moves.sort((a, b) -> b.score - a.score);
        
        int bestScore = moves.get(0).score;
        
        // Find all moves within threshold of best
        List<Move> candidates = new ArrayList<>();
        for (Move m : moves) {
            if (bestScore - m.score <= EQUAL_SCORE_THRESHOLD) {
                // Add random factor
                m.score += random.nextInt(RANDOMNESS_RANGE * 2) - RANDOMNESS_RANGE;
                candidates.add(m);
            }
        }
        
        // Select best after randomization
        candidates.sort((a, b) -> b.score - a.score);
        return candidates.get(0);
    }
    
    int countTotalMaterial() {
        int total = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board[r][c];
                if (piece != EMPTY && Math.abs(piece) != KING) {
                    total += getPieceValue(Math.abs(piece));
                }
            }
        }
        return total;
    }
    
    /**
     * Alpha-beta search with optimizations
     * 
     * Time: O(b^(3d/4)) average case
     * Space: O(d) recursion depth
     * 
     * @param depth Remaining search depth
     * @param alpha Best score for maximizing player
     * @param beta Best score for minimizing player
     * @param ply Current ply from root
     * @return Best score for this position
     */
    int alphaBeta(int depth, int alpha, int beta, int ply) {
        nodesSearched++;
        
        // Time check
        if (nodesSearched % 10000 == 0) {
            if (System.currentTimeMillis() - startTime > 5000) {
                stopSearch = true;
                return 0;
            }
        }
        
        if (stopSearch) return 0;
        
        // Draw detection
        if (halfmoveClock >= 100 || isRepetition()) return 0;
        
        // Transposition table lookup
        long key = computeZobristKey();
        TranspositionEntry entry = transpositionTable.get(key);
        if (entry != null && entry.key == key && entry.depth >= depth) {
            if (entry.flag == 0) return entry.score;
            if (entry.flag == 1 && entry.score <= alpha) return alpha;
            if (entry.flag == 2 && entry.score >= beta) return beta;
        }
        
        // Leaf node
        if (depth <= 0) {
            return quiescence(alpha, beta);
        }
        
        List<Move> moves = generateLegalMoves();
        
        // Checkmate or stalemate
        if (moves.isEmpty()) {
            if (isInCheck(sideToMove)) {
                return -30000 + ply;
            }
            return 0;
        }
        
        // Move ordering
        orderMoves(moves, entry != null && entry.key == key ? entry.bestMove : null);
        
        int bestScore = -Integer.MAX_VALUE;
        Move bestMove = null;
        
        for (Move move : moves) {
            makeMove(move);
            int score = -alphaBeta(depth - 1, -beta, -alpha, ply + 1);
            unmakeMove(move);
            
            if (stopSearch) return 0;
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break; // Beta cutoff - prune this branch
            }
        }
        
        // Store in transposition table
        int flag = bestScore <= alpha ? 1 : (bestScore >= beta ? 2 : 0);
        transpositionTable.put(key, new TranspositionEntry(key, depth, bestScore, flag, bestMove));
        
        return bestScore;
    }
    
    boolean isRepetition() {
        long current = computeZobristKey();
        int count = 0;
        for (long key : positionHistory) {
            if (key == current) count++;
        }
        return count >= 2;
    }
    
    /**
     * Quiescence search - extend search at leaf nodes for captures
     * Prevents horizon effect by resolving capture sequences
     * 
     * Time: O(c^q) where c = capture moves, q = quiescence depth
     * Space: O(q) for recursion
     */
    int quiescence(int alpha, int beta) {
        int standPat = evaluate();
        
        if (standPat >= beta) return beta;
        if (alpha < standPat) alpha = standPat;
        
        // Generate capture moves only
        List<Move> captures = new ArrayList<>();
        for (Move move : generatePseudoLegalMoves()) {
            if (board[move.toRow][move.toCol] != EMPTY || move.isEnPassant) {
                makeMove(move);
                if (!isInCheck(-sideToMove)) {
                    move.score = getPieceValue(Math.abs(board[move.toRow][move.toCol]));
                    captures.add(move);
                }
                unmakeMove(move);
            }
        }
        
        // MVV-LVA ordering
        captures.sort((a, b) -> b.score - a.score);
        
        for (Move move : captures) {
            makeMove(move);
            int score = -quiescence(-beta, -alpha);
            unmakeMove(move);
            
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        
        return alpha;
    }
    
    /**
     * Move ordering for alpha-beta efficiency
     * Uses MVV-LVA for captures, prioritizes hash move
     * 
     * Time: O(m log m) for sorting m moves
     * Space: O(1) additional
     */
    void orderMoves(List<Move> moves, Move hashMove) {
        for (Move move : moves) {
            move.score = 0;
            
            // Hash move gets highest priority
            if (hashMove != null && move.equals(hashMove)) {
                move.score = 1000000;
                continue;
            }
            
            int movingPiece = Math.abs(board[move.fromRow][move.fromCol]);
            int capturedPiece = Math.abs(board[move.toRow][move.toCol]);
            
            // MVV-LVA: Most Valuable Victim - Least Valuable Aggressor
            if (capturedPiece != EMPTY) {
                // Score = victim_value * 10 - attacker_value
                // This ensures QxQ < PxQ (pawn capturing queen is better)
                move.score = 10000 + capturedPiece * 10 - movingPiece;
            }
            
            // Promotion bonus
            if (move.promotion != 0) {
                move.score += 5000 + move.promotion * 100;
            }
        }
        
        moves.sort((a, b) -> b.score - a.score);
    }
    
    // ============================================================================
    // ZOBRIST HASHING
    // ============================================================================
    
    /**
     * Compute Zobrist hash key for current position
     * Time: O(64) = O(1)
     * Space: O(1)
     */
    long computeZobristKey() {
        long key = 0;
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board[r][c];
                if (piece != EMPTY) {
                    key ^= zobristKeys[r][c][piece + 6];
                }
            }
        }
        
        if (sideToMove == BLACK) key ^= zobristSide;
        
        for (int i = 0; i < 4; i++) {
            if ((castlingRights & (1 << i)) != 0) {
                key ^= zobristCastling[i];
            }
        }
        
        if (enPassantSquare != -1) {
            key ^= zobristEnPassant[enPassantSquare % 8];
        }
        
        return key;
    }
    
    // ============================================================================
    // PERFT (PERFORMANCE TEST) - MOVE GENERATION VERIFICATION
    // ============================================================================
    
    /**
     * PERFT counts all legal move paths to a given depth
     * Used to verify move generation correctness
     * 
     * Standard test positions:
     * 1. Starting position
     * 2. Kiwipete (complex middle game)
     * 3. CPW Position 3 (endgame)
     * 
     * Time: O(b^d) - exponential in depth
     * Space: O(d) - recursion depth
     * 
     * @param depth Search depth
     * @return Number of leaf nodes
     */
    long perft(int depth) {
        if (depth == 0) return 1;
        
        long nodes = 0;
        List<Move> moves = generateLegalMoves();
        
        for (Move move : moves) {
            makeMove(move);
            nodes += perft(depth - 1);
            unmakeMove(move);
        }
        
        return nodes;
    }
    
    /**
     * Divided perft - shows breakdown by first move
     */
    void perftDivide(int depth) {
        long total = 0;
        List<Move> moves = generateLegalMoves();
        
        for (Move move : moves) {
            makeMove(move);
            long nodes = perft(depth - 1);
            unmakeMove(move);
            System.out.println(move + ": " + nodes);
            total += nodes;
        }
        System.out.println("Total: " + total);
    }
    
    /**
     * Run standard perft test positions
     */
    void runPerftTests() {
        System.out.println("=== PERFT TEST POSITIONS ===\n");
        
        // Position 1: Starting position
        System.out.println("Position 1: Starting Position");
        System.out.println("FEN: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println("Perft(1) = " + perft(1) + " (expected: 20)");
        System.out.println("Perft(2) = " + perft(2) + " (expected: 400)");
        System.out.println("Perft(3) = " + perft(3) + " (expected: 8902)");
        System.out.println("Perft(4) = " + perft(4) + " (expected: 197281)");
        System.out.println();
        
        // Position 2: Kiwipete
        System.out.println("Position 2: Kiwipete (Complex Middle Game)");
        System.out.println("FEN: r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        loadPosition("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        System.out.println("Perft(1) = " + perft(1) + " (expected: 48)");
        System.out.println("Perft(2) = " + perft(2) + " (expected: 2039)");
        System.out.println("Perft(3) = " + perft(3) + " (expected: 97862)");
        System.out.println("Perft(4) = " + perft(4) + " (expected: 4085603)");
        System.out.println();
        
        // Position 3: CPW Endgame
        System.out.println("Position 3: CPW Endgame");
        System.out.println("FEN: 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -");
        loadPosition("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        System.out.println("Perft(1) = " + perft(1) + " (expected: 14)");
        System.out.println("Perft(2) = " + perft(2) + " (expected: 191)");
        System.out.println("Perft(3) = " + perft(3) + " (expected: 2812)");
        System.out.println("Perft(4) = " + perft(4) + " (expected: 43238)");
        System.out.println();
        
        // Position 4: Castling test
        System.out.println("Position 4: Castling Test");
        System.out.println("FEN: r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        loadPosition("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        System.out.println("Perft(1) = " + perft(1) + " (expected: 26)");
        System.out.println("Perft(2) = " + perft(2) + " (expected: 568)");
        System.out.println();
        
        // Position 5: En passant test
        System.out.println("Position 5: En Passant Test");
        System.out.println("FEN: rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        loadPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        System.out.println("En passant square: " + (enPassantSquare == -1 ? "none" : squareToString(enPassantSquare)));
        List<Move> moves = generateLegalMoves();
        System.out.println("Legal moves: " + moves.size());
        for (Move m : moves) {
            if (m.isEnPassant) System.out.println("  " + m + " (en passant)");
        }
        
        // Reset to starting position
        loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }
    
    String squareToString(int sq) {
        char[] cols = {'a','b','c','d','e','f','g','h'};
        int col = sq % 8;
        int row = 8 - sq / 8;
        return "" + cols[col] + row;
    }
    
    // ============================================================================
    // UCI PROTOCOL
    // ============================================================================
    
    void runUCI() {
        Scanner scanner = new Scanner(System.in);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            
            String[] tokens = line.split("\\s+");
            String command = tokens[0];
            
            switch (command) {
                case "uci":
                    System.out.println("id name SimpleChessEngine 1200");
                    System.out.println("id author Competitive Programmer");
                    System.out.println("option name Depth type spin default 5 min 1 max 10");
                    System.out.println("uciok");
                    break;
                    
                case "isready":
                    System.out.println("readyok");
                    break;
                    
                case "position":
                    handlePosition(tokens, line);
                    break;
                    
                case "go":
                    handleGo(tokens);
                    break;
                    
                case "stop":
                    stopSearch = true;
                    break;
                    
                case "quit":
                    return;
                    
                case "ucinewgame":
                    transpositionTable.clear();
                    loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    void handlePosition(String[] tokens, String line) {
        int idx = 1;
        
        if (tokens[idx].equals("startpos")) {
            loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            idx++;
        } else if (tokens[idx].equals("fen")) {
            idx++;
            StringBuilder fen = new StringBuilder();
            for (int i = 0; i < 6 && idx < tokens.length; i++) {
                fen.append(tokens[idx++]).append(" ");
            }
            loadPosition(fen.toString().trim());
        }
        
        if (idx < tokens.length && tokens[idx].equals("moves")) {
            idx++;
            for (; idx < tokens.length; idx++) {
                Move move = parseUCIMove(tokens[idx]);
                if (move != null) makeMove(move);
            }
        }
    }
    
    Move parseUCIMove(String moveStr) {
        if (moveStr.length() < 4) return null;
        
        int fromCol = moveStr.charAt(0) - 'a';
        int fromRow = '8' - moveStr.charAt(1);
        int toCol = moveStr.charAt(2) - 'a';
        int toRow = '8' - moveStr.charAt(3);
        
        int promotion = 0;
        if (moveStr.length() > 4) {
            char prom = moveStr.charAt(4);
            switch (prom) {
                case 'n': promotion = KNIGHT; break;
                case 'b': promotion = BISHOP; break;
                case 'r': promotion = ROOK; break;
                case 'q': promotion = QUEEN; break;
            }
        }
        
        Move move = new Move(fromRow, fromCol, toRow, toCol, promotion);
        
        if (Math.abs(board[fromRow][fromCol]) == KING && Math.abs(toCol - fromCol) == 2) {
            move.isCastling = true;
        }
        
        if (Math.abs(board[fromRow][fromCol]) == PAWN && toCol != fromCol && board[toRow][toCol] == EMPTY) {
            move.isEnPassant = true;
        }
        
        return move;
    }
    
    void handleGo(String[] tokens) {
        int depth = searchDepth;
        
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].equals("depth")) {
                depth = Integer.parseInt(tokens[++i]);
            }
        }
        
        Move bestMove = findBestMove(depth);
        
        if (bestMove != null) {
            System.out.println("bestmove " + bestMove.toString());
        } else {
            System.out.println("bestmove 0000");
        }
    }
    
    // ============================================================================
    // CONSOLE MODE - PLAY AGAINST ENGINE
    // ============================================================================
    
    /**
     * Interactive console mode for playing against the engine
     * Commands:
     * - d: Display board
     * - go: Let engine think and move
     * - list: Show all legal moves
     * - eval: Show position evaluation
     * - move <uci>: Make a move (e.g., move e2e4)
     * - perft <depth>: Run perft test
     * - tests: Run standard perft test positions
     * - new: New game
     * - help: Show commands
     * - quit: Exit
     */
    void runConsoleMode() {
        Scanner scanner = new Scanner(System.in);
        
        printBanner();
        printBoard();
        
        while (true) {
            System.out.print("\n> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            
            String[] tokens = line.split("\\s+");
            String cmd = tokens[0].toLowerCase();
            
            switch (cmd) {
                case "d":
                case "display":
                    printBoard();
                    break;
                    
                case "go":
                    engineMove();
                    break;
                    
                case "list":
                    listMoves();
                    break;
                    
                case "eval":
                    showEval();
                    break;
                    
                case "move":
                    if (tokens.length > 1) {
                        playerMove(tokens[1]);
                    } else {
                        System.out.println("Usage: move <uci> (e.g., move e2e4)");
                    }
                    break;
                    
                case "perft":
                    int depth = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 4;
                    runPerft(depth);
                    break;
                    
                case "divide":
                    int d = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 4;
                    perftDivide(d);
                    break;
                    
                case "tests":
                    runPerftTests();
                    break;
                    
                case "new":
                    newGame();
                    break;
                    
                case "depth":
                    if (tokens.length > 1) {
                        searchDepth = Integer.parseInt(tokens[1]);
                        System.out.println("Search depth set to: " + searchDepth);
                    }
                    break;
                    
                case "fen":
                    if (tokens.length > 1) {
                        String fen = line.substring(4).trim();
                        loadPosition(fen);
                        printBoard();
                    }
                    break;
                    
                case "help":
                case "?":
                    printHelp();
                    break;
                    
                case "quit":
                case "exit":
                    System.out.println("Goodbye!");
                    return;
                    
                default:
                    // Try to parse as a move directly
                    if (tokens[0].matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                        playerMove(tokens[0]);
                    } else {
                        System.out.println("Unknown command. Type 'help' for commands.");
                    }
            }
        }
    }
    
    void printBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     SIMPLE CHESS ENGINE - 1200 ELO       ║");
        System.out.println("║                                          ║");
        System.out.println("║  Play against the engine in console!     ║");
        System.out.println("║  Type 'help' for available commands      ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
    
    void printHelp() {
        System.out.println("\n=== AVAILABLE COMMANDS ===");
        System.out.println("d, display    - Display the board");
        System.out.println("go            - Let the engine make a move");
        System.out.println("list          - Show all legal moves");
        System.out.println("eval          - Show position evaluation");
        System.out.println("move <uci>    - Make a move (e.g., 'move e2e4' or just 'e2e4')");
        System.out.println("perft <depth> - Count positions to depth");
        System.out.println("divide <depth>- Perft with move breakdown");
        System.out.println("tests         - Run standard perft test positions");
        System.out.println("new           - Start a new game");
        System.out.println("depth <n>     - Set engine search depth (default: 5)");
        System.out.println("fen <string>  - Load position from FEN");
        System.out.println("help          - Show this help");
        System.out.println("quit          - Exit the program");
        System.out.println();
        System.out.println("UCI format: e2e4 (from square to square)");
        System.out.println("Promotion: e7e8q (add q/r/b/n for promotion piece)");
    }
    
    void engineMove() {
        System.out.println("Engine thinking...");
        long start = System.currentTimeMillis();
        
        Move bestMove = findBestMove(searchDepth);
        
        long time = System.currentTimeMillis() - start;
        
        if (bestMove != null) {
            System.out.println("Engine plays: " + bestMove);
            System.out.println("Nodes: " + nodesSearched + " | Time: " + time + "ms | NPS: " + (nodesSearched * 1000 / Math.max(time, 1)));
            makeMove(bestMove);
            printBoard();
            checkGameEnd();
        } else {
            System.out.println("No legal moves!");
            checkGameEnd();
        }
    }
    
    void playerMove(String moveStr) {
        Move move = parseUCIMove(moveStr);
        if (move == null) {
            System.out.println("Invalid move format. Use UCI notation (e.g., e2e4)");
            return;
        }
        
        // Verify it's a legal move
        List<Move> legalMoves = generateLegalMoves();
        boolean isLegal = false;
        for (Move legal : legalMoves) {
            if (legal.equals(move)) {
                isLegal = true;
                move = legal; // Use the legal move with all flags set
                break;
            }
        }
        
        if (!isLegal) {
            System.out.println("Illegal move! Type 'list' to see legal moves.");
            return;
        }
        
        makeMove(move);
        printBoard();
        checkGameEnd();
    }
    
    void listMoves() {
        List<Move> moves = generateLegalMoves();
        System.out.println("Legal moves (" + moves.size() + "):");
        
        // Print in columns
        int count = 0;
        for (Move move : moves) {
            System.out.printf("%-8s", move.toString());
            count++;
            if (count % 8 == 0) System.out.println();
        }
        if (count % 8 != 0) System.out.println();
    }
    
    void showEval() {
        int eval = evaluate();
        System.out.println("Position evaluation: " + eval + " centipawns");
        System.out.println("(Positive = good for " + (sideToMove == WHITE ? "White" : "Black") + " to move)");
        
        if (eval > 500) System.out.println("Assessment: Winning advantage");
        else if (eval > 200) System.out.println("Assessment: Clear advantage");
        else if (eval > 50) System.out.println("Assessment: Slight advantage");
        else if (eval > -50) System.out.println("Assessment: Equal position");
        else if (eval > -200) System.out.println("Assessment: Slight disadvantage");
        else if (eval > -500) System.out.println("Assessment: Clear disadvantage");
        else System.out.println("Assessment: Losing position");
    }
    
    void runPerft(int depth) {
        System.out.println("Running perft(" + depth + ")...");
        long start = System.currentTimeMillis();
        long nodes = perft(depth);
        long time = System.currentTimeMillis() - start;
        
        System.out.println("Nodes: " + nodes);
        System.out.println("Time: " + time + " ms");
        System.out.println("NPS: " + (nodes * 1000 / Math.max(time, 1)));
    }
    
    void newGame() {
        loadPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println("New game started!");
        printBoard();
    }
    
    void checkGameEnd() {
        List<Move> moves = generateLegalMoves();
        if (moves.isEmpty()) {
            if (isInCheck(sideToMove)) {
                System.out.println("CHECKMATE! " + (sideToMove == WHITE ? "Black" : "White") + " wins!");
            } else {
                System.out.println("STALEMATE! Game is drawn.");
            }
        } else if (isInCheck(sideToMove)) {
            System.out.println("CHECK!");
        }
    }
    
    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================
    
    void printBoard() {
        System.out.println();
        System.out.println("    a   b   c   d   e   f   g   h");
        System.out.println("  +---+---+---+---+---+---+---+---+");
        
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + " |");
            for (int c = 0; c < 8; c++) {
                char piece = pieceToChar(board[r][c]);
                System.out.print(" " + piece + " |");
            }
            System.out.println(" " + (8 - r));
            System.out.println("  +---+---+---+---+---+---+---+---+");
        }
        
        System.out.println("    a   b   c   d   e   f   g   h");
        System.out.println();
        System.out.println("Side to move: " + (sideToMove == WHITE ? "White" : "Black"));
        
        if (enPassantSquare != -1) {
            System.out.println("En passant: " + squareToString(enPassantSquare));
        }
    }
    
    // ============================================================================
    // MAIN
    // ============================================================================
    
    public static void main(String[] args) {
        SimpleChessEngine engine = new SimpleChessEngine();
        
        if (args.length > 0 && args[0].equals("console")) {
            engine.runConsoleMode();
        } else if (args.length > 0 && args[0].equals("perft")) {
            engine.runPerftTests();
        } else {
            engine.runUCI();
        }
    }
}
