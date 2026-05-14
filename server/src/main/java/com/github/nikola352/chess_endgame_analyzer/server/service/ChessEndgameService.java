package com.github.nikola352.chess_endgame_analyzer.server.service;

import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;
import com.github.nikola352.chess_endgame_analyzer.model.models.Position;
import com.github.nikola352.chess_endgame_analyzer.server.dto.*;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChessEndgameService {
    private final KieBase kieBase;
    private final FenParser fenParser;
    private final FactType adjacentSquareType;
    private final FactType derivedFactType;
    private final FactType recommendationType;

    public ChessEndgameService(KieBase kieBase, FenParser fenParser) {
        this.kieBase = kieBase;
        this.fenParser = fenParser;
        this.adjacentSquareType = kieBase.getFactType("rules", "AdjacentSquare");
        this.derivedFactType = kieBase.getFactType("rules", "DerivedFact");
        this.recommendationType = kieBase.getFactType("rules", "Recommendation");
    }

    public AnalyzeResponseDto analyze(AnalyzeRequestDto request) throws IllegalArgumentException {
        if (request.getQueryType() == null) {
            throw new IllegalArgumentException("queryType is required");
        }
        FenParser.Result parsed = fenParser.parse(request.getFen());
        KieSession kSession = prepareSession(parsed);
        try {
            kSession.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    System.out.println("FIRED: " + event.getMatch().getRule().getName());
                }
            });

            Map<Object, Integer> insertionOrder = new IdentityHashMap<>();
            AtomicInteger insertCounter = new AtomicInteger();
            kSession.addEventListener(new DefaultRuleRuntimeEventListener() {
                @Override
                public void objectInserted(ObjectInsertedEvent event) {
                    if (event.getObject().getClass().getSimpleName().equals("DerivedFact")) {
                        insertionOrder.put(event.getObject(), insertCounter.getAndIncrement());
                    }
                }
            });

            kSession.fireAllRules();

            Boolean theoreticalDraw = null;
            if (request.getQueryType() == QueryType.DRAW_QUERY) {
                QueryResults qr = kSession.getQueryResults("isTheoreticalDraw");
                theoreticalDraw = qr.iterator().hasNext();
            }

            List<DerivedFactDto> derivedFacts = collectDerivedFacts(kSession, insertionOrder);
            RecommendationDto recommendation = collectRecommendation(kSession);

            Position position = (Position) kSession.getObjects(o -> o instanceof Position)
                    .stream().findFirst().orElse(null);
            String endgameType = position != null ? position.getEndgameType() : null;

            return new AnalyzeResponseDto(
                    request.getFen(),
                    parsed.getPosition().getSideToMove(),
                    endgameType,
                    derivedFacts,
                    recommendation,
                    theoreticalDraw
            );
        } finally {
            kSession.dispose();
        }
    }

    private KieSession prepareSession(FenParser.Result parsed) {
        KieSession kSession = kieBase.newKieSession();

        kSession.insert(parsed.getPosition());
        for (Piece piece : parsed.getPieces()) {
            kSession.insert(piece);
        }

        insertAdjacentSquares(kSession);

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

    private List<DerivedFactDto> collectDerivedFacts(KieSession kSession, Map<Object, Integer> insertionOrder) {
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
            int order = insertionOrder.getOrDefault(obj, Integer.MAX_VALUE);
            result.add(new DerivedFactDto(type, side, explanation, level, reference, order));
        }
        result.sort(Comparator.comparingInt(DerivedFactDto::getLevel)
                .thenComparingInt(DerivedFactDto::getInsertionOrder));
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
}
