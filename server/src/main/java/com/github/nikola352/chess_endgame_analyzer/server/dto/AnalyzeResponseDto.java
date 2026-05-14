package com.github.nikola352.chess_endgame_analyzer.server.dto;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;

import java.util.List;

public class AnalyzeResponseDto {
    private final String fen;
    private final String sideToMove;
    private final String endgameType;
    private final List<DerivedFactDto> derivedFacts;
    private final RecommendationDto recommendation;
    private final CandidateMoveDto candidateMove;
    private final Boolean theoreticalDraw;

    public AnalyzeResponseDto(String fen, Piece.Color sideToMove, String endgameType,
                              List<DerivedFactDto> derivedFacts, RecommendationDto recommendation,
                              CandidateMoveDto candidateMove, Boolean theoreticalDraw) {
        this.fen = fen;
        this.sideToMove = sideToMove.name();
        this.endgameType = endgameType;
        this.derivedFacts = derivedFacts;
        this.recommendation = recommendation;
        this.candidateMove = candidateMove;
        this.theoreticalDraw = theoreticalDraw;
    }

    public String getFen() { return fen; }
    public String getSideToMove() { return sideToMove; }
    public String getEndgameType() { return endgameType; }
    public List<DerivedFactDto> getDerivedFacts() { return derivedFacts; }
    public RecommendationDto getRecommendation() { return recommendation; }
    public CandidateMoveDto getCandidateMove() { return candidateMove; }
    public Boolean getTheoreticalDraw() { return theoreticalDraw; }
}
