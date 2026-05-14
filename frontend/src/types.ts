export type QueryType = "EVALUATE" | "DRAW_QUERY";

export interface DerivedFact {
  type: string;
  side: string | null;
  explanation: string;
  level: number;
  reference: string;
}

export interface Recommendation {
  technique: string;
  plan: string;
  bookReference: string;
}

export interface AnalyzeRequest {
  fen: string;
  queryType: QueryType;
}

export interface AnalyzeResponse {
  fen: string;
  sideToMove: string;
  endgameType: string;
  derivedFacts: DerivedFact[];
  recommendation: Recommendation | null;
  theoreticalDraw: boolean | null;
}
