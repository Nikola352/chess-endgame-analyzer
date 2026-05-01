import { useState } from "react";
import { Chess } from "chess.js";
import type { QueryType } from "./types";
import { ChessBoard } from "./components/ChessBoard";
import { ControlPanel } from "./components/ControlPanel";
import { AnalysisPlaceholder } from "./components/AnalysisPlaceholder";

const DEFAULT_FEN = "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1";

function App() {
  const [inputFen, setInputFen] = useState(DEFAULT_FEN);
  const [activeFen, setActiveFen] = useState(DEFAULT_FEN);
  const [queryType, setQueryType] = useState<QueryType>("EVALUATE");
  const [candidateMove, setCandidateMove] = useState("");
  const [fenError, setFenError] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  function handleFenChange(fen: string) {
    setInputFen(fen);
    setFenError(null);
  }

  function handleFenLoad() {
    try {
      new Chess(inputFen.trim());
      setFenError(null);
      setActiveFen(inputFen.trim());
    } catch (e) {
      setFenError((e as Error).message ?? "Invalid FEN string");
    }
  }

  function handleAnalyze() {
    setIsAnalyzing(true);
    setTimeout(() => setIsAnalyzing(false), 1500);
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <header className="border-b border-zinc-800 px-6 py-4">
        <h1 className="text-xl font-bold text-white tracking-tight">
          Chess Endgame Analyzer
        </h1>
        <p className="text-zinc-500 text-sm mt-0.5">
          Expert system powered by Drools — de la Villa · Silman
        </p>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-[3fr_2fr] gap-8 items-start">
          <div className="flex flex-col gap-3">
            <ChessBoard fen={activeFen} />
            <p className="text-center font-mono text-xs text-zinc-600 truncate px-2">
              {activeFen}
            </p>
          </div>

          <div className="flex flex-col gap-6">
            <ControlPanel
              inputFen={inputFen}
              fenError={fenError}
              queryType={queryType}
              candidateMove={candidateMove}
              isAnalyzing={isAnalyzing}
              onFenChange={handleFenChange}
              onFenLoad={handleFenLoad}
              onQueryTypeChange={setQueryType}
              onCandidateMoveChange={setCandidateMove}
              onAnalyze={handleAnalyze}
            />
            <AnalysisPlaceholder />
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
