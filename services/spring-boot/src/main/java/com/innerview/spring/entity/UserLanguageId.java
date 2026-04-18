package com.innerview.spring.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Data
public class UserLanguageId implements Serializable {
    private UUID userId;
    private UUID languageId;

    public UserLanguageId() {}

    public UserLanguageId(UUID userId, UUID languageId) {
        this.userId = userId;
        this.languageId = languageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLanguageId that = (UserLanguageId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, languageId);
    }
}