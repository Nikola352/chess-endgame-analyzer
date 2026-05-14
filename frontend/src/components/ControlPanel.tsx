import type { QueryType } from "../types";
import { FENInput } from "./FENInput";
import { QuerySelector } from "./QuerySelector";

interface ControlPanelProps {
  inputFen: string;
  fenError: string | null;
  queryType: QueryType;
  isAnalyzing: boolean;
  onFenChange: (fen: string) => void;
  onFenLoad: () => void;
  onRandomPosition: () => void;
  onQueryTypeChange: (qt: QueryType) => void;
  onAnalyze: () => void;
}

export function ControlPanel({
  inputFen,
  fenError,
  queryType,
  isAnalyzing,
  onFenChange,
  onFenLoad,
  onRandomPosition,
  onQueryTypeChange,
  onAnalyze,
}: ControlPanelProps) {
  const analyzeDisabled = isAnalyzing || fenError !== null;

  return (
    <div className="flex flex-col gap-2">
      <FENInput
        inputFen={inputFen}
        fenError={fenError}
        onChange={onFenChange}
        onLoad={onFenLoad}
        onRandomPosition={onRandomPosition}
      />
      <div className="flex gap-2 items-center">
        <QuerySelector value={queryType} onChange={onQueryTypeChange} compact />
        <button
          onClick={onAnalyze}
          disabled={analyzeDisabled}
          className={`shrink-0 py-2 px-4 rounded-lg font-semibold text-sm transition-all flex items-center gap-1.5 ${
            analyzeDisabled
              ? "bg-zinc-700 text-zinc-500 cursor-not-allowed"
              : "bg-amber-600 hover:bg-amber-500 text-white cursor-pointer"
          }`}
        >
          {isAnalyzing ? (
            <>
              <svg className="animate-spin h-3.5 w-3.5" viewBox="0 0 24 24" fill="none">
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
              Analyzing
            </>
          ) : (
            "Analyze"
          )}
        </button>
      </div>
    </div>
  );
}
