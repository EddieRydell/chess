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

    private final PieceType type;

    private final ChessGame.TeamColor pieceColor;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
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
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
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

    private void addLinearMoves(ChessBoard board, ChessPosition pos, int rowDelta, int colDelta, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();

        while (true) {
            row += rowDelta;
            col += colDelta;

            ChessPosition currPos = new ChessPosition(row, col);
            ChessMove currMove = new ChessMove(pos, currPos, null);

            if (!currPos.isInBounds()) {
                return;
            }

            if (board.getPiece(currPos) != null) {
                if (board.getPiece(currPos).getTeamColor() != this.getTeamColor()) {
                    moves.add(currMove);
                }
                return;
            }

            moves.add(currMove);
        }
    }

    private void addPawnMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();

        int direction = (this.getTeamColor() == ChessGame.TeamColor.BLACK) ? -1 : 1;
        ChessPosition currPos = new ChessPosition(row + direction, col);

        if (currPos.isInBounds()
                && board.getPiece(currPos) == null) {
            if ((currPos.getRow() == 8 && direction == 1) || (currPos.getRow() == 1 && direction == -1)) {
                moves.add(new ChessMove(pos, currPos, PieceType.QUEEN));
                moves.add(new ChessMove(pos, currPos, PieceType.KNIGHT));
                moves.add(new ChessMove(pos, currPos, PieceType.ROOK));
                moves.add(new ChessMove(pos, currPos, PieceType.BISHOP));
            }
            else {
                moves.add(new ChessMove(pos, currPos, null));
            }
            if ((pos.getRow() == 2 && direction == 1) || (pos.getRow() == 7 && direction == -1)) {
                ChessPosition startingMove = new ChessPosition(currPos.getRow() + direction, currPos.getColumn());
                if (board.getPiece(startingMove) == null) {
                    moves.add(new ChessMove(pos, startingMove, null));
                }
            }
        }

        currPos = new ChessPosition(row + direction, col + 1);

        if (currPos.isInBounds()
                && board.getPiece(currPos) != null
                && board.getPiece(currPos).getTeamColor() != this.getTeamColor()) {
            if ((currPos.getRow() == 8 && direction == 1) || (currPos.getRow() == 1 && direction == -1)) {
                moves.add(new ChessMove(pos, currPos, PieceType.QUEEN));
                moves.add(new ChessMove(pos, currPos, PieceType.KNIGHT));
                moves.add(new ChessMove(pos, currPos, PieceType.ROOK));
                moves.add(new ChessMove(pos, currPos, PieceType.BISHOP));
            }
            else {
                moves.add(new ChessMove(pos, currPos, null));
            }
        }

        currPos = new ChessPosition(row + direction, col - 1);

        if (currPos.isInBounds()
                && board.getPiece(currPos) != null
                && board.getPiece(currPos).getTeamColor() != this.getTeamColor()) {
            if ((currPos.getRow() == 8 && direction == 1) || (currPos.getRow() == 1 && direction == -1)) {
                moves.add(new ChessMove(pos, currPos, PieceType.QUEEN));
                moves.add(new ChessMove(pos, currPos, PieceType.KNIGHT));
                moves.add(new ChessMove(pos, currPos, PieceType.ROOK));
                moves.add(new ChessMove(pos, currPos, PieceType.BISHOP));
            }
            else {
                moves.add(new ChessMove(pos, currPos, null));
            }
        }
    }

    private void addRookMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        addLinearMoves(board, pos, 1, 0, moves);  // Down
        addLinearMoves(board, pos, -1, 0, moves); // Up
        addLinearMoves(board, pos, 0, 1, moves);  // Right
        addLinearMoves(board, pos, 0, -1, moves); // Left
    }

    private void addKnightMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        int row = pos.getRow();
        int col = pos.getColumn();

        ArrayList<ChessPosition> possibleMoves = new ArrayList<>();

        possibleMoves.add(new ChessPosition(row + 1, col + 2));
        possibleMoves.add(new ChessPosition(row - 1, col + 2));
        possibleMoves.add(new ChessPosition(row + 1, col - 2));
        possibleMoves.add(new ChessPosition(row - 1, col - 2));
        possibleMoves.add(new ChessPosition(row + 2, col + 1));
        possibleMoves.add(new ChessPosition(row - 2, col + 1));
        possibleMoves.add(new ChessPosition(row + 2, col - 1));
        possibleMoves.add(new ChessPosition(row - 2, col - 1));

        possibleMoves.removeIf(i ->
                !i.isInBounds() || (board.getPiece(i) != null && board.getPiece(i).getTeamColor() == this.getTeamColor())
        );

        for (ChessPosition i : possibleMoves) {
            moves.add(new ChessMove(pos, i, null));
        }
    }

    private void addBishopMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        addLinearMoves(board, pos, 1, 1, moves);  // Diagonal down-right
        addLinearMoves(board, pos, -1, -1, moves); // Diagonal up-left
        addLinearMoves(board, pos, 1, -1, moves); // Diagonal down-left
        addLinearMoves(board, pos, -1, 1, moves); // Diagonal up-right
    }

    private void addQueenMoves(ChessBoard board, ChessPosition pos, ArrayList<ChessMove> moves) {
        addRookMoves(board, pos, moves);
        addBishopMoves(board, pos, moves);
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
