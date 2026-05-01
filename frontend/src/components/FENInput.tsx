interface FENInputProps {
  inputFen: string;
  fenError: string | null;
  onChange: (fen: string) => void;
  onLoad: () => void;
}

export function FENInput({
  inputFen,
  fenError,
  onChange,
  onLoad,
}: FENInputProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-zinc-400 text-xs font-medium uppercase tracking-wider">
        FEN String
      </label>
      <textarea
        value={inputFen}
        onChange={(e) => onChange(e.target.value)}
        rows={2}
        spellCheck={false}
        className={`w-full font-mono text-sm bg-zinc-800 text-zinc-100 border rounded-lg p-3 resize-none focus:outline-none transition-colors ${
          fenError
            ? "border-red-500 focus:border-red-400"
            : "border-zinc-600 focus:border-zinc-400"
        }`}
      />
      {fenError && <p className="text-red-400 text-xs">{fenError}</p>}
      <button
        onClick={onLoad}
        className="self-start bg-zinc-700 hover:bg-zinc-600 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors cursor-pointer"
      >
        Load Position
      </button>
    </div>
  );
}
