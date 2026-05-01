package com.github.nikola352.chess_engame_analyzer.model.models;

import java.util.Objects;

public class Piece {
    private String type;
    private String color;
    private String square;

    public Piece() {
    }

    public Piece(String type, String color, String square) {
        this.type = type;
        this.color = color;
        this.square = square;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
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
