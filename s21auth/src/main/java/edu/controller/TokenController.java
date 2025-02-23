package edu.controller;

import edu.model.TokenEntity;
import edu.dto.TokenRequest;
import edu.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<String> generateToken(@RequestBody TokenRequest request) {
        try {
            String accessToken = tokenService.getAccessToken(request.getLogin(), request.getPassword());
            if (accessToken == null) {
                log.error("Не удалось сгенерировать токен для {}", request.getLogin());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            return ResponseEntity.ok(accessToken);
        } catch (Exception e) {
            log.error("Ошибка генерации токена для {}: {}", request.getLogin(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<String> getToken(@RequestParam String login) {
        try {
            Optional<TokenEntity> tokenEntityOpt  = tokenService.findByLogin(login);
            return tokenEntityOpt.map(tokenEntity -> ResponseEntity.ok(tokenEntity.getAccessToken())).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            log.error("Ошибка получения токена для {}: {}", login, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
