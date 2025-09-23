package ru.izpz.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.auth.dto.TokenResponse;
import ru.izpz.auth.model.TokenEntity;
import ru.izpz.auth.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenPersistenceService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void upsertToken(String login, String password, TokenResponse tr) {
        try {
            TokenEntity token = tokenRepository.findForUpdate(login)
                    .orElseGet(() -> {
                        TokenEntity t = new TokenEntity();
                        t.setLogin(login);
                        return t;
                    });

            token.setPassword(password);
            token.setAccessToken(tr.getAccessToken());
            token.setRefreshToken(tr.getRefreshToken());
            token.setExpiresAt(LocalDateTime.now().plusSeconds(tr.getExpiresIn()));

            tokenRepository.save(token);
        } catch (DataIntegrityViolationException e) {
            log.warn("Запись для {} уже создана другим потоком/нодой", login);
        }
    }

    @Transactional(readOnly = true)
    public Optional<TokenEntity> findById(String login) {
        return tokenRepository.findById(login);
    }

    @Transactional(readOnly = true)
    public List<TokenEntity> findAll() {
        return tokenRepository.findAll();
    }
}
