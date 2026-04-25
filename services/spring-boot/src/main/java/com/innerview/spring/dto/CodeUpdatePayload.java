package com.innerview.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CodeUpdatePayload {
   private String base64Vector; // The Yjs CRDT math data
   private String plainText;    // The human-readable code
}
