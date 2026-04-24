package com.innerview.spring.core.util;

import java.util.concurrent.ThreadLocalRandom;

/** RoomUtil */
public class RoomUtil {

  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int ID_LENGTH = 6;

  public static String generateUniqueRoomId() {
    StringBuilder roomId = new StringBuilder(ID_LENGTH);
    for (int i = 0; i < ID_LENGTH; i++) {
      int randomIndex = ThreadLocalRandom.current().nextInt(CHARACTERS.length());
      roomId.append(CHARACTERS.charAt(randomIndex));
    }
    return roomId.toString();
  }
}
