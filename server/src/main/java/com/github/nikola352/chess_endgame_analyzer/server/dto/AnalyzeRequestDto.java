package com.github.nikola352.chess_endgame_analyzer.server.dto;

public class AnalyzeRequestDto {
    private String fen;
    private QueryType queryType;
    private String candidateMove;

    public AnalyzeRequestDto() {}

    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }

    public QueryType getQueryType() { return queryType; }
    public void setQueryType(QueryType queryType) { this.queryType = queryType; }

    public String getCandidateMove() { return candidateMove; }
    public void setCandidateMove(String candidateMove) { this.candidateMove = candidateMove; }
}
