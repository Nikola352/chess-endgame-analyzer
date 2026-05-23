package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class AdjacentSquare {
    private String from;
    private String to;

    public AdjacentSquare() {
    }

    public AdjacentSquare(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdjacentSquare that = (AdjacentSquare) o;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return "AdjacentSquare{from='" + from + "', to='" + to + "'}";
    }
}
