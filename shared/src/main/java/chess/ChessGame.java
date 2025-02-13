package chess;

import java.util.Collection;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    private ChessBoard board;
    private TeamColor currTurn = TeamColor.WHITE;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        currTurn = team;
    }

    private boolean isMoveSafe(ChessMove move, TeamColor team) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();

        // Save current board state
        ChessPiece movingPiece = board.getPiece(start);
        ChessPiece capturedPiece = board.getPiece(end);

        // If castling, also check the square the king moves through
        if (movingPiece.getPieceType() == ChessPiece.PieceType.KING && Math.abs(start.getColumn() - end.getColumn()) == 2) {
            int kingRow = (team == TeamColor.WHITE) ? 1 : 8;
            ChessPosition throughSquare = new ChessPosition(kingRow, (start.getColumn() + end.getColumn()) / 2);
            if (isInCheck(team)
                    || isSquareAttacked(throughSquare, team)
                    || isSquareAttacked(end, team)) {
                return false;
            }
        }

        board.addPiece(start, null);
        board.addPiece(end, movingPiece);

        boolean inCheck = isInCheck(team);

        board.addPiece(start, movingPiece);
        board.addPiece(end, capturedPiece);

        return !inCheck;
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        TeamColor playerColor = piece.getTeamColor();
        possibleMoves.removeIf(move -> !isMoveSafe(move, playerColor));

        return possibleMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition startPos = move.getStartPosition();
        ChessPosition endPos = move.getEndPosition();

        ChessPiece piece = board.getPiece(startPos);
        if (piece == null) {
            throw new InvalidMoveException();
        }

        if (piece.getTeamColor() != currTurn) {
            throw new InvalidMoveException();
        }

        Collection<ChessMove> legalMoves = validMoves(startPos);
        if (legalMoves == null || !legalMoves.contains(move)) {
            throw new InvalidMoveException();
        }

        // Move the piece
        board.addPiece(startPos, null);
        if (move.getPromotionPiece() == null) {
            board.addPiece(endPos, piece);
        }
        else {
            board.addPiece(endPos, new ChessPiece(currTurn, move.getPromotionPiece()));
        }

        // Handle castling
        if (piece.getPieceType() == ChessPiece.PieceType.KING && Math.abs(startPos.getColumn() - endPos.getColumn()) == 2) {
            int kingRow = (currTurn == TeamColor.WHITE) ? 1 : 8;
            if (endPos.getColumn() == 7) { // Kingside castling
                ChessPosition rookStart = new ChessPosition(kingRow, 8);
                ChessPosition rookEnd = new ChessPosition(kingRow, 6);
                ChessPiece rookPiece = board.getPiece(rookStart);
                board.addPiece(rookStart, null);
                board.addPiece(rookEnd, rookPiece);
                rookPiece.setHasMoved(true);
            }
            else if (endPos.getColumn() == 3) { // Queenside castling
                ChessPosition rookStart = new ChessPosition(kingRow, 1);
                ChessPosition rookEnd = new ChessPosition(kingRow, 4);
                ChessPiece rookPiece = board.getPiece(rookStart);
                board.addPiece(rookStart, null);
                board.addPiece(rookEnd, rookPiece);
                rookPiece.setHasMoved(true);
            }
        }

        // Handle en passant
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            if (Math.abs(startPos.getRow() - endPos.getRow()) == 2) {
                int enPassantRow = (startPos.getRow() + endPos.getRow()) / 2;
                board.setEnPassantSquare(new ChessPosition(enPassantRow, startPos.getColumn()));
            }
            else if (Math.abs(startPos.getColumn() - endPos.getColumn()) == 1
                    && endPos.equals(board.getEnPassantSquare())) {
                int capturedRow = (piece.getTeamColor() == TeamColor.WHITE) ? endPos.getRow() - 1 : endPos.getRow() + 1;
                ChessPosition capturedPos = new ChessPosition(capturedRow, endPos.getColumn());
                board.addPiece(capturedPos, null);
                board.setEnPassantSquare(null);
            }
            else {
                board.setEnPassantSquare(null);
            }
        }
        else {
            board.setEnPassantSquare(null);
        }

        ChessPiece movedPiece = board.getPiece(endPos);
        if (movedPiece != null) {
            movedPiece.setHasMoved(true);
        }

        currTurn = (currTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = null;
        for (int i = 1; i < 9; i++) {
            for (int j = 1; j < 9; j++) {
                ChessPosition currPosition = new ChessPosition(i, j);
                ChessPiece currPiece = board.getPiece(currPosition);
                if (currPiece != null) {
                    if (currPiece.getPieceType() == ChessPiece.PieceType.KING && currPiece.getTeamColor() == teamColor) {
                        kingPosition = currPosition;
                    }
                }
            }
        }

        if (kingPosition == null) {
            return false;
        }

        for (int i = 1; i < 9; i++) {
            for (int j = 1; j < 9; j++) {
                ChessPosition currPosition = new ChessPosition(i, j);
                ChessPiece currPiece = board.getPiece(currPosition);
                if (currPiece == null) {
                    continue;
                }
                if (currPiece.getTeamColor() == teamColor) {
                    continue;
                }

                Collection<ChessMove> currMoves = board.getPiece(currPosition).pieceMoves(board, currPosition);
                for (ChessMove move : currMoves) {
                    if (move.getEndPosition().equals(kingPosition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

        return noValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        return noValidMoves(teamColor);
    }

    /**
     * Helper function to determine if there are no valid moves for a team
     *
     * @param teamColor the team to find valid moves for
     * @return True if there are no valid moves for the current team
     */
    private boolean noValidMoves(TeamColor teamColor) {
        for (int i = 1; i < 9; i++) {
            for (int j = 1; j < 9; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(position);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isSquareAttacked(ChessPosition square, TeamColor defendingColor) {
        TeamColor attackingColor = (defendingColor == TeamColor.WHITE)
                ? TeamColor.BLACK
                : TeamColor.WHITE;

        // Loop over all board squares.
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition attackerPos = new ChessPosition(row, col);
                ChessPiece attackerPiece = board.getPiece(attackerPos);
                if (attackerPiece == null) {
                    continue;
                }
                if (attackerPiece.getTeamColor() != attackingColor) {
                    continue;
                }

                // Generate moves without castling
                Collection<ChessMove> attackerMoves = attackerPiece.pieceMovesNoCastling(board, attackerPos);

                for (ChessMove move : attackerMoves) {
                    ChessPosition endPos = move.getEndPosition();
                    ChessPiece occupant = board.getPiece(endPos);
                    if (occupant != null && occupant.getTeamColor() == attackerPiece.getTeamColor()) {
                        continue;
                    }

                    if (endPos.equals(square)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }
}
