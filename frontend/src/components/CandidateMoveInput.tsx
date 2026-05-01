interface CandidateMoveInputProps {
  value: string;
  onChange: (value: string) => void;
}

export function CandidateMoveInput({
  value,
  onChange,
}: CandidateMoveInputProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-zinc-400 text-xs font-medium uppercase tracking-wider">
        Candidate Move
      </label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="e.g. Kd5 (optional)"
        className="w-full font-mono text-sm bg-zinc-800 text-zinc-100 border border-zinc-600 rounded-lg px-3 py-2 focus:outline-none focus:border-zinc-400 placeholder:text-zinc-600 transition-colors"
      />
      <p className="text-zinc-600 text-xs">
        Leave blank to skip move quality assessment.
      </p>
    </div>
  );
}
