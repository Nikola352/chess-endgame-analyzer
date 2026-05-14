package com.github.nikola352.chess_endgame_analyzer.server.dto;

public class AnalyzeRequestDto {
    private String fen;
    private QueryType queryType;

    public AnalyzeRequestDto() {}

    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }

    public QueryType getQueryType() { return queryType; }
    public void setQueryType(QueryType queryType) { this.queryType = queryType; }
}
