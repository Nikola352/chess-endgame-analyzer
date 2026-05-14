import type { AnalyzeRequest, AnalyzeResponse } from "./types";

const API_BASE = "http://localhost:8080";

export async function analyzePosition(req: AnalyzeRequest): Promise<AnalyzeResponse> {
  const res = await fetch(`${API_BASE}/api/analyze`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `HTTP ${res.status}`);
  }
  return res.json() as Promise<AnalyzeResponse>;
}
