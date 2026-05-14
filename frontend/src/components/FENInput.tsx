interface FENInputProps {
  inputFen: string;
  fenError: string | null;
  onChange: (fen: string) => void;
  onLoad: () => void;
  onRandomPosition: () => void;
}

export function FENInput({ inputFen, fenError, onChange, onLoad, onRandomPosition }: FENInputProps) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between">
        <label className="text-zinc-400 text-xs font-medium uppercase tracking-wider">
          FEN
        </label>
        <button
          onClick={onRandomPosition}
          className="text-zinc-500 hover:text-zinc-300 text-xs underline underline-offset-2 transition-colors cursor-pointer"
        >
          Try random position
        </button>
      </div>
      <div className="flex gap-2">
        <input
          type="text"
          value={inputFen}
          onChange={(e) => onChange(e.target.value)}
          spellCheck={false}
          className={`flex-1 min-w-0 font-mono text-sm bg-zinc-800 text-zinc-100 border rounded-lg px-3 py-2 focus:outline-none transition-colors ${
            fenError
              ? "border-red-500 focus:border-red-400"
              : "border-zinc-600 focus:border-zinc-400"
          }`}
        />
        <button
          onClick={onLoad}
          className="shrink-0 bg-zinc-700 hover:bg-zinc-600 text-white text-sm font-medium px-3 py-2 rounded-lg transition-colors cursor-pointer"
        >
          Load
        </button>
      </div>
      {fenError && <p className="text-red-400 text-xs">{fenError}</p>}
    </div>
  );
}
