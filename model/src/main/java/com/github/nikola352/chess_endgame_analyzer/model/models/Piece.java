package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class Piece {
    public enum Color {
        WHITE, BLACK,
    }

    public enum Type {
        KING, QUEEN, BISHOP, KNIGHT, ROOK, PAWN,
    }

    private Type type;
    private Color color;
    private String square;

    public Piece() {
    }

    public Piece(Type type, Color color, String square) {
        this.type = type;
        this.color = color;
        this.square = square;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getSquare() {
        return square;
    }

    public void setSquare(String square) {
        this.square = square;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        if (!Objects.equals(type, piece.type)) return false;
        if (!Objects.equals(color, piece.color)) return false;
        return Objects.equals(square, piece.square);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (square != null ? square.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Piece{type='" + type + "', color='" + color + "', square='" + square + "'}";
    }
}
