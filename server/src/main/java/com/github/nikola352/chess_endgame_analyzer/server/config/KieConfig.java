package com.github.nikola352.chess_endgame_analyzer.server.config;

import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KieConfig {

    @Bean
    public KieBase kieBase() throws IOException {
        KieHelper kieHelper = new KieHelper();

        String[] drlFiles = {
            "/rules/types.drl",
            "/rules/backward-chaining-queries.drl",
            "/rules/level1-classification.drl",
            "/rules/level1-detection.drl",
            "/rules/level2-evaluation.drl",
            "/rules/level3-recommendations.drl",
            "/rules/level4-candidate-moves.drl"
        };
        for (String path : drlFiles) {
            InputStream is = KieConfig.class.getResourceAsStream(path);
            if (is == null) throw new RuntimeException("DRL resource not found: " + path);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            kieHelper.addContent(content, ResourceType.DRL);
        }

        List<Map<String, Object>> data = loadCsvData();
        try (InputStream tpl = KieConfig.class.getResourceAsStream("/templates/key-squares.drt")) {
            ObjectDataCompiler compiler = new ObjectDataCompiler();
            String compiledDrl = compiler.compile(data, tpl);
            kieHelper.addContent(compiledDrl, ResourceType.DRL);
        }

        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("KieBase compilation failed: " + results.getMessages(Message.Level.ERROR));
        }
        return kieHelper.build();
    }

    private List<Map<String, Object>> loadCsvData() throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        try (InputStream is = KieConfig.class.getResourceAsStream("/templates/key-squares.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = line.split(",");
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                data.add(row);
            }
        }
        return data;
    }
}
