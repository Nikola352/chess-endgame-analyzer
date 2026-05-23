package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class KeySquare {
    private String pawnFile;
    private Piece.Color pawnColor;
    private String square;

    public KeySquare() {
    }

    public KeySquare(String pawnFile, Piece.Color pawnColor, String square) {
        this.pawnFile = pawnFile;
        this.pawnColor = pawnColor;
        this.square = square;
    }

    public String getPawnFile() { return pawnFile; }
    public void setPawnFile(String pawnFile) { this.pawnFile = pawnFile; }

    public Piece.Color getPawnColor() { return pawnColor; }
    public void setPawnColor(Piece.Color pawnColor) { this.pawnColor = pawnColor; }

    public String getSquare() { return square; }
    public void setSquare(String square) { this.square = square; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeySquare that = (KeySquare) o;
        return Objects.equals(pawnFile, that.pawnFile)
                && pawnColor == that.pawnColor
                && Objects.equals(square, that.square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pawnFile, pawnColor, square);
    }

    @Override
    public String toString() {
        return "KeySquare{pawnFile='" + pawnFile + "', pawnColor=" + pawnColor + ", square='" + square + "'}";
    }
}
