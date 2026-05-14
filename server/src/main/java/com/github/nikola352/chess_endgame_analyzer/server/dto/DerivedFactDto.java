package com.github.nikola352.chess_endgame_analyzer.server.dto;

public class DerivedFactDto {
    private final String type;
    private final String side;
    private final String explanation;
    private final int level;
    private final String reference;

    public DerivedFactDto(String type, String side, String explanation, int level, String reference) {
        this.type = type;
        this.side = side;
        this.explanation = explanation;
        this.level = level;
        this.reference = reference;
    }

    public String getType() { return type; }
    public String getSide() { return side; }
    public String getExplanation() { return explanation; }
    public int getLevel() { return level; }
    public String getReference() { return reference; }
}
