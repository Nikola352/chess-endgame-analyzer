package com.github.nikola352.chess_endgame_analyzer.server.dto;

public class CandidateMoveDto {
    private final String notation;
    private final String piece;
    private final String fromSquare;
    private final String toSquare;
    private final String quality;
    private final String explanation;

    public CandidateMoveDto(String notation, String piece, String fromSquare, String toSquare,
                             String quality, String explanation) {
        this.notation = notation;
        this.piece = piece;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.quality = quality;
        this.explanation = explanation;
    }

    public String getNotation() { return notation; }
    public String getPiece() { return piece; }
    public String getFromSquare() { return fromSquare; }
    public String getToSquare() { return toSquare; }
    public String getQuality() { return quality; }
    public String getExplanation() { return explanation; }
}
