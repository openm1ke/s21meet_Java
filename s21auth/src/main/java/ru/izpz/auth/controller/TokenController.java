package ru.izpz.auth.controller;

import lombok.RequiredArgsConstructor;
import ru.izpz.auth.dto.TokenRequest;
import ru.izpz.auth.service.TokenService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping
    public ResponseEntity<String> generateToken(@Valid @RequestBody TokenRequest request) {
        String accessToken = tokenService.getAccessToken(request.getLogin(), request.getPassword());
        if (accessToken == null) {
            log.error("Не удалось сгенерировать токен для {}", request.getLogin());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(accessToken);
    }

    @GetMapping("/default")
    public ResponseEntity<String> getDefaultTokenController() {
        String accessToken = tokenService.getDefaultAccessToken();
        if (accessToken == null) {
            log.error("Не удалось сгенерировать токен по умолчанию");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(accessToken);
    }

    @GetMapping
    public ResponseEntity<String> getToken(@RequestParam String login) {
        return tokenService.findByLogin(login)
                .map(tokenEntity -> ResponseEntity.ok(tokenEntity.getAccessToken()))
                .orElse(ResponseEntity.notFound().build());
    }
}
