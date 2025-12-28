package io.github.anirudhk_tech.janus.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class QueryController {
    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        String traceId = UUID.randomUUID().toString(); // mock for now

        return new QueryResponse(
            traceId,
            "Not implemented yet",
            Map.of(),
            new QueryResponse.Explanation(
                List.of(),
                List.of(),
                "pending"
            )
        );
    }
}
