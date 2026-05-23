package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class DerivedFact {
    private String type;
    private Piece.Color side;
    private String explanation;
    private int level;
    private String reference;

    public DerivedFact() {
    }

    public DerivedFact(String type, Piece.Color side, String explanation, int level, String reference) {
        this.type = type;
        this.side = side;
        this.explanation = explanation;
        this.level = level;
        this.reference = reference;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Piece.Color getSide() { return side; }
    public void setSide(Piece.Color side) { this.side = side; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DerivedFact that = (DerivedFact) o;
        return level == that.level
                && Objects.equals(type, that.type)
                && side == that.side
                && Objects.equals(explanation, that.explanation)
                && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, side, explanation, level, reference);
    }

    @Override
    public String toString() {
        return "DerivedFact{type='" + type + "', side=" + side + ", level=" + level + "}";
    }
}
