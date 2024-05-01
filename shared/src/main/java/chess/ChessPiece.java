package chess;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static chess.ChessBoard.BOARD_SIZE;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        pieceType = type;
        teamColor = pieceColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessPiece that = (ChessPiece) o;
        return teamColor == that.teamColor && pieceType == that.pieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamColor, pieceType);
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

    private ChessGame.TeamColor teamColor;
    private PieceType pieceType;

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return teamColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return pieceType;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        ChessPosition newPosition;
        switch (pieceType) {
            case KING:
                for (int i = max(myPosition.getRow() - 1, 0); i < min(BOARD_SIZE, myPosition.getRow() + 1); i++) {
                    for (int j = max(myPosition.getColumn() - 1, 0); j < min(BOARD_SIZE, myPosition.getColumn() + 1); j++) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(i, j), null));
                    }
                }
                break;
            case QUEEN:

                break;
            case BISHOP:
                newPosition = new ChessPosition(myPosition.getRow() + 1, myPosition.getColumn() + 1);
                while (newPosition.isValidPosition()) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                    newPosition = new ChessPosition(newPosition.getRow() + 1, newPosition.getColumn() + 1);
                }
                newPosition = new ChessPosition(myPosition.getRow() - 1, myPosition.getColumn() - 1);
                while (newPosition.isValidPosition()) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                    newPosition = new ChessPosition(newPosition.getRow() - 1, newPosition.getColumn() - 1);
                }
                newPosition = new ChessPosition(myPosition.getRow() + 1, myPosition.getColumn() -1);
                while (newPosition.isValidPosition()) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                    newPosition = new ChessPosition(newPosition.getRow() + 1, newPosition.getColumn() - 1);
                }
                newPosition = new ChessPosition(myPosition.getRow() - 1, myPosition.getColumn() + 1);
                while (newPosition.isValidPosition()) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                    newPosition = new ChessPosition(newPosition.getRow() - 1, newPosition.getColumn() + 1);
                }
                break;
            case KNIGHT:

                break;
            case ROOK:

                break;
            case PAWN:

                break;
        }
        return moves;
    }
}
