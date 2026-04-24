package com.innerview.spring.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface SharedCodeEditorService {
    public void init(String roomId);
    public void addUserToSession(UUID userId,String roomId);
    public void removeUserFromSession(UUID userId,String roomId);
    public void updateCode(String roomId, String newCode);
    public String getCodeSnapshot(String roomId);
}
