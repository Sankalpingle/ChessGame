import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChessGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessGame game = new ChessGame();
            game.setVisible(true);
        });
    }

    // Model
    enum ColorSide { WHITE, BLACK }
    enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

    static class Piece {
        final PieceType type;
        final ColorSide color;

        Piece(PieceType type, ColorSide color) {
            this.type = type;
            this.color = color;
        }
    }

    static class Move {
        final int fromR, fromC, toR, toC;
        final PieceType promotion; // null unless a promotion
        final boolean enPassant;
        final boolean castling;

        Move(int fromR, int fromC, int toR, int toC) {
            this(fromR, fromC, toR, toC, null, false, false);
        }
        Move(int fromR, int fromC, int toR, int toC, PieceType promotion) {
            this(fromR, fromC, toR, toC, promotion, false, false);
        }
        Move(int fromR, int fromC, int toR, int toC, boolean enPassant, boolean castling) {
            this(fromR, fromC, toR, toC, null, enPassant, castling);
        }
        Move(int fromR, int fromC, int toR, int toC, PieceType promotion, boolean enPassant, boolean castling) {
            this.fromR = fromR; this.fromC = fromC; this.toR = toR; this.toC = toC;
            this.promotion = promotion; this.enPassant = enPassant; this.castling = castling;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Move)) return false;
            Move move = (Move) o;
            return fromR == move.fromR && fromC == move.fromC && toR == move.toR && toC == move.toC &&
                    promotion == move.promotion && enPassant == move.enPassant && castling == move.castling;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromR, fromC, toR, toC, promotion, enPassant, castling);
        }
    }

    // GUI
    private final JButton[][] squares = new JButton[8][8];
    private final JPanel boardPanel = new JPanel(new GridLayout(8, 8));
    private final JLabel status = new JLabel("White to move");
    private final JButton resetBtn = new JButton("Reset");

    // Game state
    private Piece[][] board = new Piece[8][8];
    private ColorSide sideToMove = ColorSide.WHITE;

    private Integer selR = null, selC = null; // selected square
    private List<Move> currentLegalMoves = new ArrayList<>();

    // Castling rights
    private boolean whiteKingMoved = false;
    private boolean whiteRookAMoved = false; // a1
    private boolean whiteRookHMoved = false; // h1
    private boolean blackKingMoved = false;
    private boolean blackRookAMoved = false; // a8
    private boolean blackRookHMoved = false; // h8

    // En passant target square (landing square). -1 means none.
    private int epTargetR = -1, epTargetC = -1;

    // Styles
    private final Color lightColor = new Color(240, 217, 181);
    private final Color darkColor = new Color(181, 136, 99);
    private final Border highlightBorder = BorderFactory.createLineBorder(Color.YELLOW, 3);
    private final Border selectBorder = BorderFactory.createLineBorder(new Color(0, 200, 0), 3);
    private final Border emptyBorder = BorderFactory.createEmptyBorder();

    public ChessGame() {
        super("Java Swing Chess");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(640, 720);
        setLocationRelativeTo(null);

        setupBoardPieces();
        setupUI();
        refreshBoardUI();
        updateStatus();
    }

    private void setupUI() {
        JPanel container = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        top.add(status, BorderLayout.WEST);

        resetBtn.addActionListener((ActionEvent e) -> {
            setupBoardPieces();
            sideToMove = ColorSide.WHITE;
            clearSelection();
            refreshBoardUI();
            updateStatus();
        });
        top.add(resetBtn, BorderLayout.EAST);

        container.add(top, BorderLayout.NORTH);

        initBoardButtons();
        container.add(boardPanel, BorderLayout.CENTER);

        setContentPane(container);
    }

    private void initBoardButtons() {
        boardPanel.removeAll();
        Font f = new Font("SansSerif", Font.PLAIN, 32);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton b = new JButton();
                b.setFocusPainted(false);
                b.setFont(f);
                b.setOpaque(true);
                b.setBorder(emptyBorder);
                final int rr = r, cc = c;
                b.addActionListener(e -> handleClick(rr, cc));
                squares[r][c] = b;
                boardPanel.add(b);
            }
        }
    }

    private void setupBoardPieces() {
        board = new Piece[8][8];
        // Black pieces
        board[0][0] = new Piece(PieceType.ROOK, ColorSide.BLACK);
        board[0][1] = new Piece(PieceType.KNIGHT, ColorSide.BLACK);
        board[0][2] = new Piece(PieceType.BISHOP, ColorSide.BLACK);
        board[0][3] = new Piece(PieceType.QUEEN, ColorSide.BLACK);
        board[0][4] = new Piece(PieceType.KING, ColorSide.BLACK);
        board[0][5] = new Piece(PieceType.BISHOP, ColorSide.BLACK);
        board[0][6] = new Piece(PieceType.KNIGHT, ColorSide.BLACK);
        board[0][7] = new Piece(PieceType.ROOK, ColorSide.BLACK);
        for (int c = 0; c < 8; c++) board[1][c] = new Piece(PieceType.PAWN, ColorSide.BLACK);
        // White pieces
        for (int c = 0; c < 8; c++) board[6][c] = new Piece(PieceType.PAWN, ColorSide.WHITE);
        board[7][0] = new Piece(PieceType.ROOK, ColorSide.WHITE);
        board[7][1] = new Piece(PieceType.KNIGHT, ColorSide.WHITE);
        board[7][2] = new Piece(PieceType.BISHOP, ColorSide.WHITE);
        board[7][3] = new Piece(PieceType.QUEEN, ColorSide.WHITE);
        board[7][4] = new Piece(PieceType.KING, ColorSide.WHITE);
        board[7][5] = new Piece(PieceType.BISHOP, ColorSide.WHITE);
        board[7][6] = new Piece(PieceType.KNIGHT, ColorSide.WHITE);
        board[7][7] = new Piece(PieceType.ROOK, ColorSide.WHITE);

        // reset castling and en passant
        whiteKingMoved = false; whiteRookAMoved = false; whiteRookHMoved = false;
        blackKingMoved = false; blackRookAMoved = false; blackRookHMoved = false;
        epTargetR = -1; epTargetC = -1;
    }

    private void refreshBoardUI() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton b = squares[r][c];
                b.setBackground(((r + c) % 2 == 0) ? lightColor : darkColor);
                b.setBorder(emptyBorder);
                Piece p = board[r][c];
                b.setText(p == null ? "" : unicodeFor(p));
                b.setForeground(Color.BLACK);
            }
        }
        // Re-apply highlights if selection exists
        if (selR != null && selC != null) {
            squares[selR][selC].setBorder(selectBorder);
            for (Move m : currentLegalMoves) {
                squares[m.toR][m.toC].setBorder(highlightBorder);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private String unicodeFor(Piece p) {
        switch (p.type) {
            case KING: return p.color == ColorSide.WHITE ? "♔" : "♚";
            case QUEEN: return p.color == ColorSide.WHITE ? "♕" : "♛";
            case ROOK: return p.color == ColorSide.WHITE ? "♖" : "♜";
            case BISHOP: return p.color == ColorSide.WHITE ? "♗" : "♝";
            case KNIGHT: return p.color == ColorSide.WHITE ? "♘" : "♞";
            case PAWN: return p.color == ColorSide.WHITE ? "♙" : "♟";
        }
        return "?";
    }

    private void handleClick(int r, int c) {
        Piece clicked = board[r][c];

        // If a move is available to this square, perform it
        if (selR != null && selC != null) {
            for (Move m : currentLegalMoves) {
                if (m.toR == r && m.toC == c) {
                    makeMove(m);
                    clearSelection();
                    refreshBoardUI();
                    postMoveStateCheck();
                    return;
                }
            }
        }

        // Otherwise, new selection if it's the current side's piece
        if (clicked != null && clicked.color == sideToMove) {
            selR = r; selC = c;
            currentLegalMoves = legalMovesForSquare(board, r, c, sideToMove);
        } else {
            clearSelection();
        }

        refreshBoardUI();
        updateStatus();
    }

    private void clearSelection() {
        selR = selC = null;
        currentLegalMoves.clear();
    }

    private void makeMove(Move m) {
        Piece moving = board[m.fromR][m.fromC];

        // Update castling rights if capturing a rook on its original square
        Piece target = board[m.toR][m.toC];
        if (target != null && target.type == PieceType.ROOK) {
            if (m.toR == 7 && m.toC == 0) whiteRookAMoved = true;
            if (m.toR == 7 && m.toC == 7) whiteRookHMoved = true;
            if (m.toR == 0 && m.toC == 0) blackRookAMoved = true;
            if (m.toR == 0 && m.toC == 7) blackRookHMoved = true;
        }

        // Clear en passant target unless set by a double pawn move
        int nextEpR = -1, nextEpC = -1;

        // Execute move
        board[m.fromR][m.fromC] = null;

        // En passant capture
        if (m.enPassant && moving.type == PieceType.PAWN) {
            board[m.toR][m.toC] = moving;
            // Remove the pawn that moved two last turn
            int capturedR = moving.color == ColorSide.WHITE ? m.toR + 1 : m.toR - 1;
            board[capturedR][m.toC] = null;
        } else if (moving.type == PieceType.PAWN && (m.toR == 0 || m.toR == 7)) {
            // Pawn promotion (auto-queen)
            board[m.toR][m.toC] = new Piece(PieceType.QUEEN, moving.color);
        } else if (m.promotion != null) {
            board[m.toR][m.toC] = new Piece(m.promotion, moving.color);
        } else {
            board[m.toR][m.toC] = moving;
        }

        // Handle castling rook movement and update move rights for king/rooks
        if (moving.type == PieceType.KING) {
            if (moving.color == ColorSide.WHITE) whiteKingMoved = true; else blackKingMoved = true;
            // King-side castling
            if (m.castling && m.fromC == 4 && m.toC == 6) {
                if (m.toR == 7) { // White
                    if (board[7][7] != null) { board[7][5] = board[7][7]; board[7][7] = null; }
                    whiteRookHMoved = true;
                } else if (m.toR == 0) { // Black
                    if (board[0][7] != null) { board[0][5] = board[0][7]; board[0][7] = null; }
                    blackRookHMoved = true;
                }
            }
            // Queen-side castling
            if (m.castling && m.fromC == 4 && m.toC == 2) {
                if (m.toR == 7) { // White
                    if (board[7][0] != null) { board[7][3] = board[7][0]; board[7][0] = null; }
                    whiteRookAMoved = true;
                } else if (m.toR == 0) { // Black
                    if (board[0][0] != null) { board[0][3] = board[0][0]; board[0][0] = null; }
                    blackRookAMoved = true;
                }
            }
        } else if (moving.type == PieceType.ROOK) {
            // If a rook moves from its original square, mark it as moved
            if (moving.color == ColorSide.WHITE) {
                if (m.fromR == 7 && m.fromC == 0) whiteRookAMoved = true;
                if (m.fromR == 7 && m.fromC == 7) whiteRookHMoved = true;
            } else {
                if (m.fromR == 0 && m.fromC == 0) blackRookAMoved = true;
                if (m.fromR == 0 && m.fromC == 7) blackRookHMoved = true;
            }
        } else if (moving.type == PieceType.PAWN) {
            // Set en passant target if double move
            int delta = m.toR - m.fromR;
            if (Math.abs(delta) == 2) {
                nextEpR = (m.fromR + m.toR) / 2;
                nextEpC = m.fromC;
            }
        }

        // Update en passant target for next move
        epTargetR = nextEpR; epTargetC = nextEpC;

        sideToMove = (sideToMove == ColorSide.WHITE) ? ColorSide.BLACK : ColorSide.WHITE;
    }

    private void postMoveStateCheck() {
        ColorSide current = sideToMove;
        boolean inCheck = isInCheck(board, current);
        boolean hasMove = hasAnyLegalMove(board, current);

        if (hasMove) {
            updateStatus();
        } else {
            if (inCheck) {
                status.setText((current == ColorSide.WHITE ? "White" : "Black") + " is in checkmate. " +
                        (current == ColorSide.WHITE ? "Black" : "White") + " wins!");
                disableBoard();
            } else {
                status.setText("Stalemate. Draw.");
                disableBoard();
            }
        }
    }

    private void disableBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                squares[r][c].setEnabled(false);
        resetBtn.setEnabled(true);
    }

    private void enableBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                squares[r][c].setEnabled(true);
    }

    private void updateStatus() {
        boolean inCheck = isInCheck(board, sideToMove);
        String side = sideToMove == ColorSide.WHITE ? "White" : "Black";
        status.setText(side + " to move" + (inCheck ? " (Check)" : ""));
        enableBoard();
    }

    // Move generation and rules

    private List<Move> legalMovesForSquare(Piece[][] pos, int r, int c, ColorSide side) {
        Piece p = pos[r][c];
        if (p == null || p.color != side) return new ArrayList<>();
        List<Move> pseudo = pseudoLegalMoves(pos, r, c);
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            if (isMoveLegal(pos, m, side)) {
                legal.add(m);
            }
        }
        return legal;
    }

    private boolean isMoveLegal(Piece[][] pos, Move m, ColorSide side) {
        Piece[][] copy = deepCopy(pos);
        applyMove(copy, m);
        return !isInCheck(copy, side);
    }

    private boolean isInCheck(Piece[][] pos, ColorSide side) {
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = pos[r][c];
                if (p != null && p.type == PieceType.KING && p.color == side) {
                    kr = r; kc = c;
                    break;
                }
            }
        if (kr == -1) return true; // no king found = consider in check
        ColorSide enemy = (side == ColorSide.WHITE) ? ColorSide.BLACK : ColorSide.WHITE;
        return isSquareAttacked(pos, kr, kc, enemy);
    }

    private boolean hasAnyLegalMove(Piece[][] pos, ColorSide side) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = pos[r][c];
                if (p != null && p.color == side) {
                    List<Move> moves = pseudoLegalMoves(pos, r, c);
                    for (Move m : moves) {
                        if (isMoveLegal(pos, m, side)) return true;
                    }
                }
            }
        return false;
    }

    private List<Move> pseudoLegalMoves(Piece[][] pos, int r, int c) {
        Piece p = pos[r][c];
        List<Move> out = new ArrayList<>();
        if (p == null) return out;
        switch (p.type) {
            case PAWN:
                genPawnMoves(pos, r, c, p.color, out);
                break;
            case KNIGHT:
                genKnightMoves(pos, r, c, p.color, out);
                break;
            case BISHOP:
                genSlidingMoves(pos, r, c, p.color, out, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
                break;
            case ROOK:
                genSlidingMoves(pos, r, c, p.color, out, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
                break;
            case QUEEN:
                genSlidingMoves(pos, r, c, p.color, out, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}});
                break;
            case KING:
                genKingMoves(pos, r, c, p.color, out);
                break;
        }
        return out;
    }

    private void genPawnMoves(Piece[][] pos, int r, int c, ColorSide side, List<Move> out) {
        int dir = (side == ColorSide.WHITE) ? -1 : 1;
        int startRow = (side == ColorSide.WHITE) ? 6 : 1;

        // forward 1
        int nr = r + dir;
        if (inBounds(nr, c) && pos[nr][c] == null) {
            addPawnMoveWithPromotion(out, r, c, nr, c, side);
            // forward 2 from start
            int nr2 = r + 2 * dir;
            if (r == startRow && pos[nr2][c] == null) {
                out.add(new Move(r, c, nr2, c));
            }
        }
        // captures
        int[] dc = {-1, 1};
        for (int d : dc) {
            int nc = c + d;
            if (inBounds(nr, nc) && pos[nr][nc] != null && pos[nr][nc].color != side) {
                addPawnMoveWithPromotion(out, r, c, nr, nc, side);
            }
        }
        // En passant
        if (epTargetR != -1) {
            for (int d : dc) {
                int tr = r + dir;
                int tc = c + d;
                if (tr == epTargetR && tc == epTargetC) {
                    // Must have enemy pawn adjacent on same rank
                    int enemyR = r;
                    int enemyC = tc;
                    if (inBounds(enemyR, enemyC)) {
                        Piece adj = pos[enemyR][enemyC];
                        if (adj != null && adj.type == PieceType.PAWN && adj.color != side && pos[tr][tc] == null) {
                            out.add(new Move(r, c, tr, tc, true, false)); // en passant move
                        }
                    }
                }
            }
        }
    }

    private void addPawnMoveWithPromotion(List<Move> out, int fromR, int fromC, int toR, int toC, ColorSide side) {
        if ((side == ColorSide.WHITE && toR == 0) || (side == ColorSide.BLACK && toR == 7)) {
            out.add(new Move(fromR, fromC, toR, toC, PieceType.QUEEN)); // auto-queen
        } else {
            out.add(new Move(fromR, fromC, toR, toC));
        }
    }

    private void genKnightMoves(Piece[][] pos, int r, int c, ColorSide side, List<Move> out) {
        int[][] d = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] mv : d) {
            int nr = r + mv[0], nc = c + mv[1];
            if (!inBounds(nr, nc)) continue;
            if (pos[nr][nc] == null || pos[nr][nc].color != side) {
                out.add(new Move(r, c, nr, nc));
            }
        }
    }

    private void genSlidingMoves(Piece[][] pos, int r, int c, ColorSide side, List<Move> out, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBounds(nr, nc)) {
                if (pos[nr][nc] == null) {
                    out.add(new Move(r, c, nr, nc));
                } else {
                    if (pos[nr][nc].color != side) {
                        out.add(new Move(r, c, nr, nc));
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
    }

    private void genKingMoves(Piece[][] pos, int r, int c, ColorSide side, List<Move> out) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (!inBounds(nr, nc)) continue;
                if (pos[nr][nc] == null || pos[nr][nc].color != side) {
                    out.add(new Move(r, c, nr, nc));
                }
            }
        }
        // Castling (if king not currently in check)
        if (!isInCheck(pos, side)) {
            if (side == ColorSide.WHITE && r == 7 && c == 4 && !whiteKingMoved) {
                // King-side: f1(7,5), g1(7,6)
                if (!whiteRookHMoved && pos[7][5] == null && pos[7][6] == null &&
                        !isSquareAttacked(pos, 7, 5, ColorSide.BLACK) &&
                        !isSquareAttacked(pos, 7, 6, ColorSide.BLACK) &&
                        pos[7][7] != null && pos[7][7].type == PieceType.ROOK && pos[7][7].color == ColorSide.WHITE) {
                    out.add(new Move(7, 4, 7, 6, false, true));
                }
                // Queen-side: d1(7,3), c1(7,2), and b1(7,1) empty
                if (!whiteRookAMoved && pos[7][3] == null && pos[7][2] == null && pos[7][1] == null &&
                        !isSquareAttacked(pos, 7, 3, ColorSide.BLACK) &&
                        !isSquareAttacked(pos, 7, 2, ColorSide.BLACK) &&
                        pos[7][0] != null && pos[7][0].type == PieceType.ROOK && pos[7][0].color == ColorSide.WHITE) {
                    out.add(new Move(7, 4, 7, 2, false, true));
                }
            } else if (side == ColorSide.BLACK && r == 0 && c == 4 && !blackKingMoved) {
                // King-side: f8(0,5), g8(0,6)
                if (!blackRookHMoved && pos[0][5] == null && pos[0][6] == null &&
                        !isSquareAttacked(pos, 0, 5, ColorSide.WHITE) &&
                        !isSquareAttacked(pos, 0, 6, ColorSide.WHITE) &&
                        pos[0][7] != null && pos[0][7].type == PieceType.ROOK && pos[0][7].color == ColorSide.BLACK) {
                    out.add(new Move(0, 4, 0, 6, false, true));
                }
                // Queen-side: d8(0,3), c8(0,2), b8(0,1) empty
                if (!blackRookAMoved && pos[0][3] == null && pos[0][2] == null && pos[0][1] == null &&
                        !isSquareAttacked(pos, 0, 3, ColorSide.WHITE) &&
                        !isSquareAttacked(pos, 0, 2, ColorSide.WHITE) &&
                        pos[0][0] != null && pos[0][0].type == PieceType.ROOK && pos[0][0].color == ColorSide.BLACK) {
                    out.add(new Move(0, 4, 0, 2, false, true));
                }
            }
        }
    }

    private boolean isSquareAttacked(Piece[][] pos, int tr, int tc, ColorSide bySide) {
        // Pawn attacks
        int dir = (bySide == ColorSide.WHITE) ? -1 : 1;
        int pr = tr - dir;
        if (inBounds(pr, tc - 1) && isPiece(pos[pr][tc - 1], PieceType.PAWN, bySide)) return true;
        if (inBounds(pr, tc + 1) && isPiece(pos[pr][tc + 1], PieceType.PAWN, bySide)) return true;

        // Knight attacks
        int[][] kD = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] d : kD) {
            int nr = tr + d[0], nc = tc + d[1];
            if (inBounds(nr, nc) && isPiece(pos[nr][nc], PieceType.KNIGHT, bySide)) return true;
        }

        // King adjacency
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = tr + dr, nc = tc + dc;
                if (inBounds(nr, nc) && isPiece(pos[nr][nc], PieceType.KING, bySide)) return true;
            }

        // Sliding attacks: rook/queen (orthogonal)
        int[][] rookDirs = {{1,0},{-1,0},{0,1},{0,-1}};
        if (slidingAttacks(pos, tr, tc, bySide, rookDirs, PieceType.ROOK, PieceType.QUEEN)) return true;

        // Sliding attacks: bishop/queen (diagonal)
        int[][] bishopDirs = {{1,1},{1,-1},{-1,1},{-1,-1}};
        return slidingAttacks(pos, tr, tc, bySide, bishopDirs, PieceType.BISHOP, PieceType.QUEEN);
    }

    private boolean slidingAttacks(Piece[][] pos, int tr, int tc, ColorSide bySide, int[][] dirs, PieceType a, PieceType b) {
        for (int[] d : dirs) {
            int nr = tr + d[0], nc = tc + d[1];
            while (inBounds(nr, nc)) {
                Piece p = pos[nr][nc];
                if (p != null) {
                    if (p.color == bySide && (p.type == a || p.type == b)) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        return false;
    }

    private boolean isPiece(Piece p, PieceType t, ColorSide s) {
        return p != null && p.color == s && p.type == t;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private Piece[][] deepCopy(Piece[][] src) {
        Piece[][] dst = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = src[r][c];
                if (p != null) dst[r][c] = new Piece(p.type, p.color);
            }
        return dst;
    }

    private void applyMove(Piece[][] pos, Move m) {
        Piece moving = pos[m.fromR][m.fromC];
        pos[m.fromR][m.fromC] = null;

        if (m.enPassant && moving.type == PieceType.PAWN) {
            pos[m.toR][m.toC] = moving;
            int capturedR = moving.color == ColorSide.WHITE ? m.toR + 1 : m.toR - 1;
            pos[capturedR][m.toC] = null;
        } else if (moving.type == PieceType.PAWN && (m.toR == 0 || m.toR == 7)) {
            pos[m.toR][m.toC] = new Piece(PieceType.QUEEN, moving.color);
        } else if (m.promotion != null) {
            pos[m.toR][m.toC] = new Piece(m.promotion, moving.color);
        } else {
            pos[m.toR][m.toC] = moving;
        }

        // Simulate castling rook movement for correctness in hypothetical positions
        if (moving.type == PieceType.KING && m.castling) {
            if (m.fromC == 4 && m.toC == 6) { // king-side
                int rr = m.toR;
                if (rr == 7) { if (pos[7][7] != null) { pos[7][5] = pos[7][7]; pos[7][7] = null; } }
                if (rr == 0) { if (pos[0][7] != null) { pos[0][5] = pos[0][7]; pos[0][7] = null; } }
            } else if (m.fromC == 4 && m.toC == 2) { // queen-side
                int rr = m.toR;
                if (rr == 7) { if (pos[7][0] != null) { pos[7][3] = pos[7][0]; pos[7][0] = null; } }
                if (rr == 0) { if (pos[0][0] != null) { pos[0][3] = pos[0][0]; pos[0][0] = null; } }
            }
        }
    }
}