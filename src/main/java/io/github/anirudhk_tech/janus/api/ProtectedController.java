package io.github.anirudhk_tech.janus.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedController {
    @GetMapping("/protected/ping") // test
    public String ping() {
        return "pong";
    }
}
