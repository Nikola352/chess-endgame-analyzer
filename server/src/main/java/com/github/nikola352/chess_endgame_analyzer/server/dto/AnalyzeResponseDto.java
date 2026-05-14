package com.github.nikola352.chess_endgame_analyzer.server.dto;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;

import java.util.List;

public class AnalyzeResponseDto {
    private final String fen;
    private final String sideToMove;
    private final String endgameType;
    private final List<DerivedFactDto> derivedFacts;
    private final RecommendationDto recommendation;
    private final Boolean theoreticalDraw;

    public AnalyzeResponseDto(String fen, Piece.Color sideToMove, String endgameType,
                              List<DerivedFactDto> derivedFacts, RecommendationDto recommendation,
                              Boolean theoreticalDraw) {
        this.fen = fen;
        this.sideToMove = sideToMove.name();
        this.endgameType = endgameType;
        this.derivedFacts = derivedFacts;
        this.recommendation = recommendation;
        this.theoreticalDraw = theoreticalDraw;
    }

    public String getFen() { return fen; }
    public String getSideToMove() { return sideToMove; }
    public String getEndgameType() { return endgameType; }
    public List<DerivedFactDto> getDerivedFacts() { return derivedFacts; }
    public RecommendationDto getRecommendation() { return recommendation; }
    public Boolean getTheoreticalDraw() { return theoreticalDraw; }
}
