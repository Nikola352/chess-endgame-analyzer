import type { QueryType } from "../types";

interface QuerySelectorProps {
  value: QueryType;
  onChange: (value: QueryType) => void;
}

const OPTIONS: { value: QueryType; label: string; description: string }[] = [
  {
    value: "EVALUATE",
    label: "Evaluate Position",
    description: "Forward chaining analysis",
  },
  {
    value: "DRAW_QUERY",
    label: "Draw Query",
    description: "Backward chaining proof",
  },
];

export function QuerySelector({ value, onChange }: QuerySelectorProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-zinc-400 text-xs font-medium uppercase tracking-wider">
        Query Mode
      </label>
      <div className="flex gap-2">
        {OPTIONS.map((opt) => {
          const active = value === opt.value;
          return (
            <label
              key={opt.value}
              className={`flex-1 flex flex-col gap-0.5 border rounded-lg px-3 py-2.5 cursor-pointer transition-colors ${
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
                className={`text-sm font-medium ${active ? "text-amber-400" : "text-zinc-300"}`}
              >
                {opt.label}
              </span>
              <span className="text-xs text-zinc-500">{opt.description}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
