package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        switch (this.type) {
            case PAWN:
                addPawnMoves(board, myPosition, moves);
                break;
            case ROOK:
                addRookMoves(board, myPosition, moves);
                break;
            case KNIGHT:
                addKnightMoves(board, myPosition, moves);
                break;
            case BISHOP:
                addBishopMoves(board, myPosition, moves);
                break;
            case QUEEN:
                addQueenMoves(board, myPosition, moves);
                break;
            case KING:
                addKingMoves(board, myPosition, moves);
                break;
        }

        return moves;
    }

    private void addPawnMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();

        int direction = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? -1 : 1;
    }

    private void addRookMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();
    }

    private void addKnightMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();
    }

    private void addBishopMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();
    }

    private void addQueenMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();
    }

    private void addKingMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                int newRow = row + i;
                int newCol = col + j;
                ChessPosition newPos = new ChessPosition(newRow, newCol);
                if (newPos.isInBounds()) {
                    ChessPiece piece = board.getPiece(newPos);
                    if (piece == null || piece.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(pos, newPos, null));
                    }
                }
            }
        }
    }
}
