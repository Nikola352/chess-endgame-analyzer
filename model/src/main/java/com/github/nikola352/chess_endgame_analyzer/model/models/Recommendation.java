package com.github.nikola352.chess_endgame_analyzer.model.models;

import java.util.Objects;

public class Recommendation {
    private String technique;
    private String plan;
    private String bookReference;

    public Recommendation() {
    }

    public Recommendation(String technique, String plan, String bookReference) {
        this.technique = technique;
        this.plan = plan;
        this.bookReference = bookReference;
    }

    public String getTechnique() { return technique; }
    public void setTechnique(String technique) { this.technique = technique; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getBookReference() { return bookReference; }
    public void setBookReference(String bookReference) { this.bookReference = bookReference; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recommendation that = (Recommendation) o;
        return Objects.equals(technique, that.technique)
                && Objects.equals(plan, that.plan)
                && Objects.equals(bookReference, that.bookReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technique, plan, bookReference);
    }

    @Override
    public String toString() {
        return "Recommendation{technique='" + technique + "'}";
    }
}
