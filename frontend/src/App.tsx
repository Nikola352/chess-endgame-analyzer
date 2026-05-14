import { useState } from "react";
import { Chess } from "chess.js";
import type { AnalyzeResponse, QueryType } from "./types";
import { analyzePosition } from "./api";
import { ChessBoard } from "./components/ChessBoard";
import { ControlPanel } from "./components/ControlPanel";
import { AnalysisPlaceholder } from "./components/AnalysisPlaceholder";
import { AnalysisResults } from "./components/AnalysisResults";

const DEFAULT_FEN = "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1";

const EXAMPLE_POSITIONS = [
  { label: "key square", fen: "8/8/8/2K1k3/8/3P4/8/8 w - - 0 1" },
  { label: "defended key square", fen: "8/8/8/8/1k6/3P4/8/6K1 w - - 0 1" },
  { label: "pawn promotes freely", fen: "8/8/1P2k3/8/8/8/8/7K w - - 0 1" },
  {
    label: "defender king catches pawn",
    fen: "8/8/2k2P2/8/8/8/8/K7 b - - 0 1",
  },
  { label: "wrong color bishop", fen: "8/k7/8/P7/8/8/1K6/2B5 w - - 0 1" },
  { label: "rook pawn stalemate", fen: "8/8/8/8/8/k7/p7/K7 b - - 0 1" },
  { label: "philidor", fen: "4k3/R7/6r1/3KP3/8/8/8/8 w - - 0 1" },
  { label: "not philidor", fen: "4k3/8/R7/3KP3/8/6r1/8/8 w - - 0 1" },
  { label: "lucena", fen: "3K4/3P1k2/8/8/8/8/7r/4R3 w - - 0 1" },
  { label: "rook only draw", fen: "8/8/1r1k4/8/8/5R2/8/K7 b - - 0 1" },
  { label: "queen vs pawn stalemate", fen: "8/8/8/3K4/4Q3/8/7p/7k b - - 0 1" },
  { label: "queen triangulation", fen: "8/8/8/3K4/4Q3/8/4p3/4k3 b - - 0 1" },
];

function App() {
  const [inputFen, setInputFen] = useState(DEFAULT_FEN);
  const [activeFen, setActiveFen] = useState(DEFAULT_FEN);
  const [queryType, setQueryType] = useState<QueryType>("EVALUATE");
  const [fenError, setFenError] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analyzeResult, setAnalyzeResult] = useState<AnalyzeResponse | null>(null);
  const [analyzeError, setAnalyzeError] = useState<string | null>(null);

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

  function handleRandomPosition() {
    const pos =
      EXAMPLE_POSITIONS[Math.floor(Math.random() * EXAMPLE_POSITIONS.length)];
    setInputFen(pos.fen);
    setActiveFen(pos.fen);
    setFenError(null);
  }

  async function handleAnalyze() {
    setIsAnalyzing(true);
    setAnalyzeError(null);
    try {
      const result = await analyzePosition({
        fen: activeFen,
        queryType,
      });
      setAnalyzeResult(result);
    } catch (e) {
      setAnalyzeError(e instanceof Error ? e.message : "Analysis failed");
    } finally {
      setIsAnalyzing(false);
    }
  }

  return (
    <div className="min-h-screen bg-zinc-900 text-zinc-100">
      <header className="border-b border-zinc-800 px-6 py-4">
        <h1 className="text-xl font-bold text-white tracking-tight">
          Chess Endgame Analyzer
        </h1>
        <p className="text-zinc-500 text-sm mt-0.5">
          Expert system powered by Drools — de la Villa · Silman
        </p>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8 items-start">
          {/* Left column: board + compact inputs below */}
          <div className="col-span-3 flex flex-col gap-3">
            <ChessBoard fen={activeFen} />
            <p className="text-center font-mono text-xs text-zinc-600 truncate px-2">
              {activeFen}
            </p>
            <ControlPanel
              inputFen={inputFen}
              fenError={fenError}
              queryType={queryType}
              isAnalyzing={isAnalyzing}
              onFenChange={handleFenChange}
              onFenLoad={handleFenLoad}
              onRandomPosition={handleRandomPosition}
              onQueryTypeChange={setQueryType}
              onAnalyze={handleAnalyze}
            />
          </div>

          {/* Right column: reasoning chain, recommendation */}
          <div className="col-span-2 flex flex-col gap-4">
            {analyzeError && (
              <div className="text-red-400 text-sm border border-red-700 rounded p-3">
                {analyzeError}
              </div>
            )}
            {analyzeResult ? (
              <AnalysisResults result={analyzeResult} />
            ) : (
              <AnalysisPlaceholder />
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
