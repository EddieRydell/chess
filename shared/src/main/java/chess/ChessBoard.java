package chess;

import java.util.*;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

    private ChessPiece[][] board;
    private ChessPosition enPassantSquare = null;

    public ChessBoard() {
        board = new ChessPiece[8][8];
    }

    public ChessPosition getEnPassantSquare() {
        return enPassantSquare;
    }

    public void setEnPassantSquare(ChessPosition pos) {
        enPassantSquare = pos;
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board[position.getRow() - 1][position.getColumn() - 1];
    }

    private static final Map<Character, ChessPiece.PieceType> CHAR_TO_TYPE_MAP = Map.of(
            'p', ChessPiece.PieceType.PAWN,
            'n', ChessPiece.PieceType.KNIGHT,
            'r', ChessPiece.PieceType.ROOK,
            'q', ChessPiece.PieceType.QUEEN,
            'k', ChessPiece.PieceType.KING,
            'b', ChessPiece.PieceType.BISHOP);

    private static ChessBoard loadBoard(String boardText) {
        var board = new ChessBoard();
        int row = 8;
        int column = 1;
        for (var c : boardText.toCharArray()) {
            switch (c) {
                case '\n' -> {
                    column = 1;
                    row--;
                }
                case ' ' -> column++;
                case '|' -> {
                }
                default -> {
                    ChessGame.TeamColor color = Character.isLowerCase(c) ? ChessGame.TeamColor.BLACK
                            : ChessGame.TeamColor.WHITE;
                    var type = CHAR_TO_TYPE_MAP.get(Character.toLowerCase(c));
                    var position = new ChessPosition(row, column);
                    var piece = new ChessPiece(color, type);
                    board.addPiece(position, piece);
                    column++;
                }
            }
        }
        return board;
    }

    private static ChessBoard defaultBoard() {
        return loadBoard("""
                |r|n|b|q|k|b|n|r|
                |p|p|p|p|p|p|p|p|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |P|P|P|P|P|P|P|P|
                |R|N|B|Q|K|B|N|R|
                """);
    }

    private static List<ChessMove> loadMoves(ChessPosition startPosition, int[][] endPositions) {
        var validMoves = new ArrayList<ChessMove>();
        for (var endPosition : endPositions) {
            validMoves.add(new ChessMove(startPosition,
                    new ChessPosition(endPosition[0], endPosition[1]), null));
        }
        return validMoves;
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        board = defaultBoard().board;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n"); // Column labels

        for (int row = 8; row >= 1; row--) { // Chess boards print from rank 8 down to 1
            sb.append(row).append(" "); // Row label

            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board[row - 1][col - 1]; // Assuming board[row][col] indexing
                if (piece == null) {
                    sb.append(". "); // Empty square
                } else {
                    sb.append(getPieceChar(piece)).append(" ");
                }
            }
            sb.append(row).append("\n"); // Row label on the right side
        }

        sb.append("  a b c d e f g h\n"); // Column labels again at the bottom
        return sb.toString();
    }

    /**
     * Returns the correct character for a chess piece.
     * White pieces are uppercase, black pieces are lowercase.
     */
    private char getPieceChar(ChessPiece piece) {
        char c;
        switch (piece.getPieceType()) {
            case KING -> c = 'K';
            case QUEEN -> c = 'Q';
            case ROOK -> c = 'R';
            case BISHOP -> c = 'B';
            case KNIGHT -> c = 'N';
            case PAWN -> c = 'P';
            default -> c = '?'; // Should never happen
        }
        return piece.getTeamColor() == ChessGame.TeamColor.WHITE ? c : Character.toLowerCase(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessBoard that = (ChessBoard) o;
        return Objects.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }
}
