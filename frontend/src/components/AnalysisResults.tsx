import type { AnalyzeResponse, DerivedFact } from "../types";

interface AnalysisResultsProps {
  result: AnalyzeResponse;
}

const LEVEL_LABELS: Record<number, string> = {
  1: "Level 1 - Detection",
  2: "Level 2 - Evaluation",
  3: "Level 3 - Recommendation",
  4: "Level 4 - Move Quality",
};

const QUALITY_STYLES: Record<string, string> = {
  BEST: "bg-green-900 text-green-300 border border-green-700",
  GOOD: "bg-amber-900 text-amber-300 border border-amber-700",
  BAD: "bg-red-900 text-red-300 border border-red-700",
};

function groupByLevel(facts: DerivedFact[]): Map<number, DerivedFact[]> {
  const map = new Map<number, DerivedFact[]>();
  for (const f of facts) {
    const group = map.get(f.level) ?? [];
    group.push(f);
    map.set(f.level, group);
  }
  return map;
}

export function AnalysisResults({ result }: AnalysisResultsProps) {
  const factsByLevel = groupByLevel(result.derivedFacts);
  const levels = Array.from(factsByLevel.keys()).sort((a, b) => a - b);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-mono font-semibold text-amber-500 uppercase tracking-wide">
          {result.endgameType}
        </span>
        <span className="text-xs px-2 py-0.5 rounded bg-zinc-800 border border-zinc-700 text-zinc-300">
          {result.sideToMove} to move
        </span>
      </div>

      {result.theoreticalDraw !== null && (
        <div
          className={`rounded px-3 py-2 text-sm font-semibold border ${
            result.theoreticalDraw
              ? "bg-green-950 border-green-700 text-green-300"
              : "bg-red-950 border-red-700 text-red-300"
          }`}
        >
          {result.theoreticalDraw ? "Theoretical draw confirmed" : "Not a theoretical draw"}
        </div>
      )}

      {levels.length > 0 && (
        <div className="flex flex-col gap-3">
          <h3 className="text-xs font-semibold text-zinc-500 uppercase tracking-widest">
            Reasoning Chain
          </h3>
          {levels.map((level) => (
            <div key={level} className="flex flex-col gap-1.5">
              <span className="text-xs text-zinc-600 font-medium">
                {LEVEL_LABELS[level] ?? `Level ${level}`}
              </span>
              {factsByLevel.get(level)!.map((fact, i) => (
                <div
                  key={i}
                  className="rounded border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm"
                >
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-mono text-xs text-amber-600 font-semibold">
                      {fact.type}
                    </span>
                    {fact.side && (
                      <span className="text-xs px-1.5 py-0 rounded bg-zinc-800 text-zinc-400 border border-zinc-700">
                        {fact.side}
                      </span>
                    )}
                  </div>
                  <p className="text-zinc-300 leading-snug">{fact.explanation}</p>
                  {fact.reference && (
                    <p className="text-zinc-600 text-xs italic mt-1">{fact.reference}</p>
                  )}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {result.recommendation && (
        <div className="flex flex-col gap-1.5">
          <h3 className="text-xs font-semibold text-zinc-500 uppercase tracking-widest">
            Recommendation
          </h3>
          <div className="rounded border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm flex flex-col gap-1">
            <span className="font-mono text-xs text-amber-600 font-semibold">
              {result.recommendation.technique}
            </span>
            <p className="text-zinc-300 leading-snug">{result.recommendation.plan}</p>
            {result.recommendation.bookReference && (
              <p className="text-zinc-600 text-xs italic">{result.recommendation.bookReference}</p>
            )}
          </div>
        </div>
      )}

      {result.candidateMove && (
        <div className="flex flex-col gap-1.5">
          <h3 className="text-xs font-semibold text-zinc-500 uppercase tracking-widest">
            Candidate Move
          </h3>
          <div className="rounded border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-zinc-100">
                {result.candidateMove.notation}
              </span>
              <span className="text-xs text-zinc-500">
                {result.candidateMove.fromSquare} → {result.candidateMove.toSquare}
              </span>
              {result.candidateMove.quality && (
                <span
                  className={`ml-auto text-xs font-semibold px-2 py-0.5 rounded ${
                    QUALITY_STYLES[result.candidateMove.quality] ??
                    "bg-zinc-800 text-zinc-300 border border-zinc-700"
                  }`}
                >
                  {result.candidateMove.quality}
                </span>
              )}
            </div>
            {result.candidateMove.explanation && (
              <p className="text-zinc-300 leading-snug">{result.candidateMove.explanation}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
