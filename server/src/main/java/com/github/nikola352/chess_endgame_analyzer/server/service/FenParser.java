package com.github.nikola352.chess_endgame_analyzer.server.service;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;
import com.github.nikola352.chess_endgame_analyzer.model.models.Position;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FenParser {
    private static final String WHITE = "WHITE";
    private static final String BLACK = "BLACK";

    public static class Result {
        private final Position position;
        private final List<Piece> pieces;

        public Result(Position position, List<Piece> pieces) {
            this.position = position;
            this.pieces = pieces;
        }

        public Position getPosition() { return position; }
        public List<Piece> getPieces() { return pieces; }
    }

    public Result parse(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN is required");
        }

        String[] parts = fen.split(" ");
        String boardPart = parts[0];
        String sideToMove = parts.length > 1 ? parts[1] : "w";

        List<Piece> pieces = new ArrayList<>();

        String[] ranks = boardPart.split("/");
        for (int rank = 0; rank < 8; rank++) {
            int file = 0;
            String rankStr = ranks[rank];

            for (char c : rankStr.toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    Piece.Type type = getPieceType(c);
                    Piece.Color color = Character.isUpperCase(c) ? Piece.Color.WHITE : Piece.Color.BLACK;
                    pieces.add(new Piece(type, color, getSquare(7 - rank, file)));
                    file++;
                }
            }
        }

        String sideToMoveStr = sideToMove.equals("w") ? WHITE : BLACK;
        Position position = new Position(fen, sideToMoveStr, null);
        return new Result(position, pieces);
    }

    private Piece.Type getPieceType(char c) {
        char lower = Character.toLowerCase(c);
        switch (lower) {
            case 'p': return Piece.Type.PAWN;
            case 'n': return Piece.Type.KNIGHT;
            case 'b': return Piece.Type.BISHOP;
            case 'r': return Piece.Type.ROOK;
            case 'q': return Piece.Type.QUEEN;
            case 'k': return Piece.Type.KING;
            default: throw new IllegalArgumentException("Invalid piece character: " + c);
        }
    }

    private String getSquare(int rank, int file) {
        if (rank < 0 || rank >= 8 || file < 0 || file >= 8) {
            throw new IllegalArgumentException("Invalid square");
        }
        char fileChar = (char) ('a' + file);
        return fileChar + Integer.toString(rank + 1);
    }
}
