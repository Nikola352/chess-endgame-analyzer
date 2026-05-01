import type { QueryType } from "../types";
import { FENInput } from "./FENInput";
import { QuerySelector } from "./QuerySelector";
import { CandidateMoveInput } from "./CandidateMoveInput";

interface ControlPanelProps {
  inputFen: string;
  fenError: string | null;
  queryType: QueryType;
  candidateMove: string;
  isAnalyzing: boolean;
  onFenChange: (fen: string) => void;
  onFenLoad: () => void;
  onQueryTypeChange: (qt: QueryType) => void;
  onCandidateMoveChange: (move: string) => void;
  onAnalyze: () => void;
}

export function ControlPanel({
  inputFen,
  fenError,
  queryType,
  candidateMove,
  isAnalyzing,
  onFenChange,
  onFenLoad,
  onQueryTypeChange,
  onCandidateMoveChange,
  onAnalyze,
}: ControlPanelProps) {
  const analyzeDisabled = isAnalyzing || fenError !== null;

  return (
    <div className="flex flex-col gap-6">
      <FENInput
        inputFen={inputFen}
        fenError={fenError}
        onChange={onFenChange}
        onLoad={onFenLoad}
      />
      <QuerySelector value={queryType} onChange={onQueryTypeChange} />
      <CandidateMoveInput
        value={candidateMove}
        onChange={onCandidateMoveChange}
      />
      <button
        onClick={onAnalyze}
        disabled={analyzeDisabled}
        className={`w-full py-3 px-6 rounded-lg font-semibold text-base transition-all flex items-center justify-center gap-2 ${
          analyzeDisabled
            ? "bg-zinc-700 text-zinc-500 cursor-not-allowed"
            : "bg-amber-600 hover:bg-amber-500 text-white cursor-pointer"
        }`}
      >
        {isAnalyzing ? (
          <>
            <svg
              className="animate-spin h-4 w-4"
              viewBox="0 0 24 24"
              fill="none"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              />
            </svg>
            Analyzing…
          </>
        ) : (
          "Analyze"
        )}
      </button>
    </div>
  );
}
