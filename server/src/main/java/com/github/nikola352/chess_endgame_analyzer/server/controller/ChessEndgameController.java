package com.github.nikola352.chess_endgame_analyzer.server.controller;

import com.github.nikola352.chess_endgame_analyzer.server.dto.AnalyzeRequestDto;
import com.github.nikola352.chess_endgame_analyzer.server.service.ChessEndgameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyze")
public class ChessEndgameController {
    private final ChessEndgameService service;

    public ChessEndgameController(ChessEndgameService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequestDto request) {
        try {
            return ResponseEntity.ok(service.analyze(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
