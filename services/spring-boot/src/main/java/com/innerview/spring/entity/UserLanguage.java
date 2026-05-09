package com.innerview.spring.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_languages")
@Data
public class UserLanguage {
    @EmbeddedId
    private UserLanguageId id = new UserLanguageId();

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("languageId")
    @JoinColumn(name = "language_id")
    private ProgrammingLanguage language;
}