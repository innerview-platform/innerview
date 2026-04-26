package com.innerview.spring.service;

import com.innerview.spring.dto.CodeUpdatePayload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface SharedCodeEditorService {
     void init(String roomId);
     void addUserToSession(UUID userId,String roomId);
     void removeUserFromSession(UUID userId,String roomId);
     void updateCode(String roomId, CodeUpdatePayload newCode);
     CodeUpdatePayload getCodeSnapshot(String roomId);
     void compileCode(String roomId,CodeUpdatePayload newCode);
}

