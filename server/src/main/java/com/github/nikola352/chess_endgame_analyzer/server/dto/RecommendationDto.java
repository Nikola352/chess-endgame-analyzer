package com.github.nikola352.chess_endgame_analyzer.server.dto;

public class RecommendationDto {
    private final String technique;
    private final String plan;
    private final String bookReference;

    public RecommendationDto(String technique, String plan, String bookReference) {
        this.technique = technique;
        this.plan = plan;
        this.bookReference = bookReference;
    }

    public String getTechnique() { return technique; }
    public String getPlan() { return plan; }
    public String getBookReference() { return bookReference; }
}
