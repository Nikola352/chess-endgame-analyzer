export function AnalysisPlaceholder() {
  return (
    <div className="border-2 border-dashed border-zinc-700 rounded-xl p-8 flex flex-col items-center gap-4 text-center">
      <div className="text-5xl text-zinc-600 select-none">♔</div>
      <h3 className="text-zinc-400 font-semibold text-lg">Analysis Results</h3>
      <p className="text-zinc-600 text-sm max-w-xs leading-relaxed">
        Load a position and click Analyze to see the reasoning chain, outcome,
        and strategic recommendation.
      </p>
      <div className="flex flex-wrap justify-center gap-2 mt-1">
        <span className="text-xs bg-zinc-800 text-zinc-500 px-3 py-1 rounded-full">
          Reasoning Chain
        </span>
        <span className="text-xs bg-zinc-800 text-zinc-500 px-3 py-1 rounded-full">
          Outcome
        </span>
        <span className="text-xs bg-zinc-800 text-zinc-500 px-3 py-1 rounded-full">
          Recommendation
        </span>
      </div>
    </div>
  );
}
