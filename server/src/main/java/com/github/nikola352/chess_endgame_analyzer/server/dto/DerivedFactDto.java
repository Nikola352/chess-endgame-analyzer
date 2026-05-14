package com.github.nikola352.chess_endgame_analyzer.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DerivedFactDto {
    private final String type;
    private final String side;
    private final String explanation;
    private final int level;
    private final String reference;
    @JsonIgnore
    private final int insertionOrder;

    public DerivedFactDto(String type, String side, String explanation, int level, String reference, int insertionOrder) {
        this.type = type;
        this.side = side;
        this.explanation = explanation;
        this.level = level;
        this.reference = reference;
        this.insertionOrder = insertionOrder;
    }

    public String getType() { return type; }
    public String getSide() { return side; }
    public String getExplanation() { return explanation; }
    public int getLevel() { return level; }
    public String getReference() { return reference; }
    public int getInsertionOrder() { return insertionOrder; }
}
