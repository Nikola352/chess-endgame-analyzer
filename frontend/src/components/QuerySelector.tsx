import type { QueryType } from "../types";

interface QuerySelectorProps {
  value: QueryType;
  onChange: (value: QueryType) => void;
  compact?: boolean;
}

const OPTIONS: { value: QueryType; label: string; shortLabel: string; description: string }[] = [
  {
    value: "EVALUATE",
    label: "Evaluate Position",
    shortLabel: "Evaluate",
    description: "Forward chaining analysis",
  },
  {
    value: "DRAW_QUERY",
    label: "Draw Query",
    shortLabel: "Draw",
    description: "Backward chaining proof",
  },
];

export function QuerySelector({ value, onChange, compact = false }: QuerySelectorProps) {
  return (
    <div className={compact ? "flex gap-1.5 shrink-0" : "flex flex-col gap-1.5"}>
      {!compact && (
        <label className="text-zinc-400 text-xs font-medium uppercase tracking-wider">
          Query Mode
        </label>
      )}
      <div className="flex gap-1.5">
        {OPTIONS.map((opt) => {
          const active = value === opt.value;
          return (
            <label
              key={opt.value}
              className={`flex flex-col border rounded-lg cursor-pointer transition-colors ${
                compact ? "px-2.5 py-1.5 gap-0" : "flex-1 px-3 py-2.5 gap-0.5"
              } ${
                active
                  ? "bg-amber-600/20 border-amber-500"
                  : "bg-zinc-800 border-zinc-600 hover:border-zinc-500"
              }`}
            >
              <input
                type="radio"
                name="queryType"
                value={opt.value}
                checked={active}
                onChange={() => onChange(opt.value)}
                className="sr-only"
              />
              <span
                className={`font-medium ${compact ? "text-xs" : "text-sm"} ${
                  active ? "text-amber-400" : "text-zinc-300"
                }`}
              >
                {compact ? opt.shortLabel : opt.label}
              </span>
              {!compact && (
                <span className="text-xs text-zinc-500">{opt.description}</span>
              )}
            </label>
          );
        })}
      </div>
    </div>
  );
}
