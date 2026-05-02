package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class Position {
    private String fen;
    private String sideToMove;
    private String endgameType;

    public Position() {
    }

    public Position(String fen, String sideToMove, String endgameType) {
        this.fen = fen;
        this.sideToMove = sideToMove;
        this.endgameType = endgameType;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public String getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
    }

    public String getEndgameType() {
        return endgameType;
    }

    public void setEndgameType(String endgameType) {
        this.endgameType = endgameType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        if (!Objects.equals(fen, position.fen)) return false;
        if (!Objects.equals(sideToMove, position.sideToMove)) return false;
        return Objects.equals(endgameType, position.endgameType);
    }

    @Override
    public int hashCode() {
        int result = fen != null ? fen.hashCode() : 0;
        result = 31 * result + (sideToMove != null ? sideToMove.hashCode() : 0);
        result = 31 * result + (endgameType != null ? endgameType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Position{fen='" + fen + "', sideToMove='" + sideToMove + "', endgameType='" + endgameType + "'}";
    }
}
