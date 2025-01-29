package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor teamColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.teamColor = pieceColor;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return teamColor == that.teamColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamColor, type);
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
        return teamColor;
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
        Collection<ChessMove> moves = new ArrayList<>();
        switch (type) {
            case KING:
                kingMoves(moves, board, myPosition);
                break;
            case QUEEN:
                queenMoves(moves, board, myPosition);
                break;
            case BISHOP:
                bishopMoves(moves, board, myPosition);
                break;
            case KNIGHT:
                knightMoves(moves, board, myPosition);
                break;
            case ROOK:
                rookMoves(moves, board, myPosition);
                break;
            case PAWN:
                pawnMoves(moves, board, myPosition);
                break;
        }
        return moves;
    }

    private boolean validatePosition(ChessBoard board, ChessPosition pos) {
        if (!pos.isInBounds()) {
            return false;
        }

        if (board.getPiece(pos) == null) {
            return true;
        }
        return board.getPiece(pos) != null && board.getPiece(pos).getTeamColor() != this.getTeamColor();
    }

    // Walk along a line from a piece in a specified direction until we hit something
    // Place all valid moves into the moves collection
    private void generateLinearMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos, int dx, int dy) {
        int row = pos.getRow();
        int col = pos.getColumn();

        while (true) {
            row += dx;
            col += dy;

            ChessPosition newPosition = new ChessPosition(row, col);
            ChessMove newMove = new ChessMove(pos, newPosition, null);

            if (!newPosition.isInBounds()) {
                break;
            }

            if (board.getPiece(newPosition) != null) {
                if (board.getPiece(newPosition).getTeamColor() != this.getTeamColor()) {
                    moves.add(newMove);
                }
                break;
            }
            moves.add(newMove);
        }
    }

    private void kingMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        for (int i = pos.getRow() - 1; i <= pos.getRow() + 1; i++) {
            for (int j = pos.getColumn() - 1; j <= pos.getColumn() + 1; j++) {
                ChessPosition newPos = new ChessPosition(i, j);
                if (validatePosition(board, newPos)) {
                    moves.add(new ChessMove(pos, newPos, null));
                }
            }
        }
    }

    private void queenMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        bishopMoves(moves, board, pos);
        rookMoves(moves, board, pos);
    }

    private void bishopMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        generateLinearMoves(moves, board, pos, 1, 1);
        generateLinearMoves(moves, board, pos, 1, -1);
        generateLinearMoves(moves, board, pos, -1, 1);
        generateLinearMoves(moves, board, pos, -1, -1);
    }

    private void knightMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        ChessPosition newPos;

        newPos = new ChessPosition(pos.getRow() + 1, pos.getColumn() + 2);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() + 1, pos.getColumn() - 2);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() - 1, pos.getColumn() + 2);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() - 1, pos.getColumn() - 2);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() + 2, pos.getColumn() + 1);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() + 2, pos.getColumn() - 1);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() - 2, pos.getColumn() + 1);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
        newPos = new ChessPosition(pos.getRow() - 2, pos.getColumn() - 1);
        if (validatePosition(board, newPos)) {
            moves.add(new ChessMove(pos, newPos, null));
        }
    }

    private void rookMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        generateLinearMoves(moves, board, pos, 0, 1);
        generateLinearMoves(moves, board, pos, 0, -1);
        generateLinearMoves(moves, board, pos, 1, 0);
        generateLinearMoves(moves, board, pos, -1, 0);
    }

    private void pawnMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos) {
        boolean firstMove = false;
        int direction = this.getTeamColor() == ChessGame.TeamColor.WHITE ? 1 : -1;
        if (pos.getRow() == 2 && this.getTeamColor() == ChessGame.TeamColor.WHITE
                || pos.getRow() == 7 && this.getTeamColor() == ChessGame.TeamColor.BLACK) {
            firstMove = true;
        }

        // Check straight moves first
        ChessPosition newPos = new ChessPosition(pos.getRow() + direction, pos.getColumn());
        if (newPos.isInBounds()
                && board.getPiece(newPos) == null) {
            pawnPromotionHelper(moves, pos, newPos);

            if (firstMove) {
                newPos = new ChessPosition(pos.getRow() + 2 * direction, pos.getColumn());
                if (newPos.isInBounds()
                        && board.getPiece(newPos) == null) {
                    moves.add(new ChessMove(pos, newPos, null));
                }
            }
        }

        // Now check diagonal captures
        newPos = new ChessPosition(pos.getRow() + direction, pos.getColumn() + 1);
        pawnValidMoves(moves, board, pos, newPos);
        newPos = new ChessPosition(pos.getRow() + direction, pos.getColumn() - 1);
        pawnValidMoves(moves, board, pos, newPos);
    }

    private void pawnValidMoves(Collection<ChessMove> moves, ChessBoard board, ChessPosition pos, ChessPosition newPos) {
        if (newPos.isInBounds()
                && board.getPiece(newPos) != null
                && board.getPiece(newPos).getTeamColor() != this.getTeamColor()) {
            pawnPromotionHelper(moves, pos, newPos);
        }
    }

    private void pawnPromotionHelper(Collection<ChessMove> moves, ChessPosition pos, ChessPosition newPos) {
        if (newPos.getRow() == 8 && this.getTeamColor() == ChessGame.TeamColor.WHITE
                || newPos.getRow() == 1 && this.getTeamColor() == ChessGame.TeamColor.BLACK) {
            moves.add(new ChessMove(pos, newPos, PieceType.QUEEN));
            moves.add(new ChessMove(pos, newPos, PieceType.BISHOP));
            moves.add(new ChessMove(pos, newPos, PieceType.ROOK));
            moves.add(new ChessMove(pos, newPos, PieceType.KNIGHT));
        }
        else {
            moves.add(new ChessMove(pos, newPos, null));
        }
    }
}
