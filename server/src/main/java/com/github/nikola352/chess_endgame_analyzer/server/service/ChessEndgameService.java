package com.github.nikola352.chess_endgame_analyzer.server.service;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;
import com.github.nikola352.chess_endgame_analyzer.model.models.Position;
import com.github.nikola352.chess_endgame_analyzer.server.dto.*;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class ChessEndgameService {
    private final KieContainer kieContainer;
    private final FenParser fenParser;
    private final FactType adjacentSquareType;
    private final FactType candidateMoveType;
    private final FactType derivedFactType;
    private final FactType recommendationType;

    public ChessEndgameService(KieContainer kieContainer, FenParser fenParser) {
        this.kieContainer = kieContainer;
        this.fenParser = fenParser;
        KieBase kieBase = kieContainer.getKieBase("ChessEndgameKBase");
        this.adjacentSquareType = kieBase.getFactType("rules", "AdjacentSquare");
        this.candidateMoveType = kieBase.getFactType("rules", "CandidateMove");
        this.derivedFactType = kieBase.getFactType("rules", "DerivedFact");
        this.recommendationType = kieBase.getFactType("rules", "Recommendation");
    }

    public AnalyzeResponseDto analyze(AnalyzeRequestDto request) throws IllegalArgumentException {
        if (request.getQueryType() == null) {
            throw new IllegalArgumentException("queryType is required");
        }
        FenParser.Result parsed = fenParser.parse(request.getFen());
        KieSession kSession = prepareSession(request, parsed);
        try {
            kSession.fireAllRules();

            Boolean theoreticalDraw = null;
            if (request.getQueryType() == QueryType.DRAW_QUERY) {
                QueryResults qr = kSession.getQueryResults("isTheoreticalDraw");
                theoreticalDraw = qr.iterator().hasNext();
            }

            List<DerivedFactDto> derivedFacts = collectDerivedFacts(kSession);
            RecommendationDto recommendation = collectRecommendation(kSession);
            CandidateMoveDto candidateMove = collectCandidateMove(kSession);

            Position position = (Position) kSession.getObjects(o -> o instanceof Position)
                    .stream().findFirst().orElse(null);
            String endgameType = position != null ? position.getEndgameType() : null;

            return new AnalyzeResponseDto(
                    request.getFen(),
                    parsed.getPosition().getSideToMove(),
                    endgameType,
                    derivedFacts,
                    recommendation,
                    candidateMove,
                    theoreticalDraw
            );
        } finally {
            kSession.dispose();
        }
    }

    private KieSession prepareSession(AnalyzeRequestDto request, FenParser.Result parsed) {
        KieSession kSession = kieContainer.newKieSession("ChessEndgameKSession");

        kSession.insert(parsed.getPosition());
        for (Piece piece : parsed.getPieces()) {
            kSession.insert(piece);
        }

        insertAdjacentSquares(kSession);

        if (request.getCandidateMove() != null && !request.getCandidateMove().isBlank()) {
            insertCandidateMove(kSession, request.getCandidateMove(), parsed);
        }

        return kSession;
    }

    private void insertAdjacentSquares(KieSession kSession) {
        int[] dFiles = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dRanks = {-1, -1, -1, 0, 0, 1, 1, 1};
        try {
            for (int file = 0; file < 8; file++) {
                for (int rank = 1; rank <= 8; rank++) {
                    String from = "" + (char) ('a' + file) + rank;
                    for (int d = 0; d < 8; d++) {
                        int nf = file + dFiles[d];
                        int nr = rank + dRanks[d];
                        if (nf >= 0 && nf < 8 && nr >= 1 && nr <= 8) {
                            String to = "" + (char) ('a' + nf) + nr;
                            Object as = adjacentSquareType.newInstance();
                            adjacentSquareType.set(as, "from", from);
                            adjacentSquareType.set(as, "to", to);
                            kSession.insert(as);
                        }
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate AdjacentSquare", e);
        }
    }

    private void insertCandidateMove(KieSession kSession, String notation, FenParser.Result parsed) {
        String trimmed = notation.trim();
        String toSquare = trimmed.substring(trimmed.length() - 2);
        String pieceStr;
        switch (trimmed.charAt(0)) {
            case 'K': pieceStr = Piece.Type.KING.name(); break;
            case 'Q': pieceStr = Piece.Type.QUEEN.name(); break;
            case 'R': pieceStr = Piece.Type.ROOK.name(); break;
            case 'B': pieceStr = Piece.Type.BISHOP.name(); break;
            case 'N': pieceStr = Piece.Type.KNIGHT.name(); break;
            default:  pieceStr = Piece.Type.PAWN.name(); break;
        }

        String sideToMove = parsed.getPosition().getSideToMove();
        Piece.Color movingColor = "WHITE".equals(sideToMove) ? Piece.Color.WHITE : Piece.Color.BLACK;
        Piece.Type movingType = Piece.Type.valueOf(pieceStr);

        String fromSquare = parsed.getPieces().stream()
                .filter(p -> p.getType() == movingType && p.getColor() == movingColor)
                .map(Piece::getSquare)
                .findFirst()
                .orElse(null);

        try {
            Object cm = candidateMoveType.newInstance();
            candidateMoveType.set(cm, "notation", notation);
            candidateMoveType.set(cm, "piece", pieceStr);
            candidateMoveType.set(cm, "fromSquare", fromSquare);
            candidateMoveType.set(cm, "toSquare", toSquare);
            candidateMoveType.set(cm, "quality", null);
            candidateMoveType.set(cm, "explanation", null);
            kSession.insert(cm);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate CandidateMove", e);
        }
    }

    private List<DerivedFactDto> collectDerivedFacts(KieSession kSession) {
        Collection<?> objects = kSession.getObjects(
                o -> o.getClass().getSimpleName().equals("DerivedFact"));
        List<DerivedFactDto> result = new ArrayList<>();
        for (Object obj : objects) {
            String type = (String) derivedFactType.get(obj, "type");
            Object sideObj = derivedFactType.get(obj, "side");
            String side = sideObj != null ? sideObj.toString() : null;
            String explanation = (String) derivedFactType.get(obj, "explanation");
            int level = (Integer) derivedFactType.get(obj, "level");
            String reference = (String) derivedFactType.get(obj, "reference");
            result.add(new DerivedFactDto(type, side, explanation, level, reference));
        }
        result.sort(Comparator.comparingInt(DerivedFactDto::getLevel).thenComparing(DerivedFactDto::getType));
        return result;
    }

    private RecommendationDto collectRecommendation(KieSession kSession) {
        return kSession.getObjects(o -> o.getClass().getSimpleName().equals("Recommendation"))
                .stream()
                .findFirst()
                .map(obj -> new RecommendationDto(
                        (String) recommendationType.get(obj, "technique"),
                        (String) recommendationType.get(obj, "plan"),
                        (String) recommendationType.get(obj, "bookReference")))
                .orElse(null);
    }

    private CandidateMoveDto collectCandidateMove(KieSession kSession) {
        return kSession.getObjects(o -> o.getClass().getSimpleName().equals("CandidateMove"))
                .stream()
                .findFirst()
                .map(obj -> new CandidateMoveDto(
                        (String) candidateMoveType.get(obj, "notation"),
                        (String) candidateMoveType.get(obj, "piece"),
                        (String) candidateMoveType.get(obj, "fromSquare"),
                        (String) candidateMoveType.get(obj, "toSquare"),
                        (String) candidateMoveType.get(obj, "quality"),
                        (String) candidateMoveType.get(obj, "explanation")))
                .orElse(null);
    }
}
