import { Chessboard } from "react-chessboard";

interface ChessBoardProps {
  fen: string;
}

export function ChessBoard({ fen }: ChessBoardProps) {
  return (
    <div className="w-full aspect-square">
      <Chessboard
        options={{
          position: fen,
          allowDragging: false,
          boardStyle: { width: "100%", height: "100%" },
        }}
      />
    </div>
  );
}
