package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.edu.model.ProfileValidation;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVerificationMapperTest {

    private final ProfileVerificationMapper mapper = org.mapstruct.factory.Mappers.getMapper(ProfileVerificationMapper.class);

    @Test
    void toProfileCodeResponse_shouldMapFields() {
        ProfileValidation entity = new ProfileValidation();
        entity.setS21login("login");
        entity.setSecretCode("code");
        entity.setExpiresAt(OffsetDateTime.now());

        ProfileCodeResponse dto = mapper.toProfileCodeResponse(entity);

        assertEquals("login", dto.getS21login());
        assertEquals("code", dto.getSecretCode());
        assertNotNull(dto.getExpiresAt());
    }
}
