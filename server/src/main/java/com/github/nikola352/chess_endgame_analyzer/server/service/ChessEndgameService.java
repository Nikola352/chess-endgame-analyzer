package com.github.nikola352.chess_endgame_analyzer.server.service;

import com.github.nikola352.chess_endgame_analyzer.model.models.AdjacentSquare;
import com.github.nikola352.chess_endgame_analyzer.model.models.DerivedFact;
import com.github.nikola352.chess_endgame_analyzer.model.models.Piece;
import com.github.nikola352.chess_endgame_analyzer.model.models.Position;
import com.github.nikola352.chess_endgame_analyzer.model.models.Recommendation;
import com.github.nikola352.chess_endgame_analyzer.server.dto.*;
import org.kie.api.KieBase;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChessEndgameService {
    private final KieBase kieBase;
    private final FenParser fenParser;

    public ChessEndgameService(KieBase kieBase, FenParser fenParser) {
        this.kieBase = kieBase;
        this.fenParser = fenParser;
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
                    if (event.getObject() instanceof DerivedFact) {
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
        for (int file = 0; file < 8; file++) {
            for (int rank = 1; rank <= 8; rank++) {
                String from = "" + (char) ('a' + file) + rank;
                for (int d = 0; d < 8; d++) {
                    int nf = file + dFiles[d];
                    int nr = rank + dRanks[d];
                    if (nf >= 0 && nf < 8 && nr >= 1 && nr <= 8) {
                        String to = "" + (char) ('a' + nf) + nr;
                        kSession.insert(new AdjacentSquare(from, to));
                    }
                }
            }
        }
    }

    private List<DerivedFactDto> collectDerivedFacts(KieSession kSession, Map<Object, Integer> insertionOrder) {
        List<DerivedFactDto> result = new ArrayList<>();
        for (Object obj : kSession.getObjects(o -> o instanceof DerivedFact)) {
            DerivedFact df = (DerivedFact) obj;
            String side = df.getSide() != null ? df.getSide().toString() : null;
            int order = insertionOrder.getOrDefault(obj, Integer.MAX_VALUE);
            result.add(new DerivedFactDto(df.getType(), side, df.getExplanation(), df.getLevel(), df.getReference(), order));
        }
        result.sort(Comparator.comparingInt(DerivedFactDto::getLevel)
                .thenComparingInt(DerivedFactDto::getInsertionOrder));
        return result;
    }

    private RecommendationDto collectRecommendation(KieSession kSession) {
        return kSession.getObjects(o -> o instanceof Recommendation)
                .stream()
                .map(obj -> (Recommendation) obj)
                .findFirst()
                .map(r -> new RecommendationDto(r.getTechnique(), r.getPlan(), r.getBookReference()))
                .orElse(null);
    }
}
