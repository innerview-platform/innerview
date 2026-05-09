package com.innerview.spring.service.redis;

/**
 * Centralized Redis key factory.
 *
 * Keeping all key patterns here means a single place to audit, rename,
 * or namespace-prefix keys (e.g., when migrating to a Redis Cluster slot
 * strategy using hash-tags like {roomId}).
 *
 * Cluster note: both keys for the same room use the same logical prefix
 * so they naturally hash to the same slot when you wrap roomId in braces:
 *   room:{roomId}:state  →  hash-tag = roomId
 * This guarantees both keys are on the same node, which is required for
 * pipelining and multi-key commands in Cluster mode.
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }


    private static final String ROOM_STATE = "room:%s:state";


    private static final String ROOM_TEXT = "room:%s:text";


    private static final String ROOM_VERSION = "room:%s:version";


    public static String roomState(String roomId) {
        return String.format(ROOM_STATE, roomId);
    }

    public static String roomText(String roomId) {
        return String.format(ROOM_TEXT, roomId);
    }

    public static String roomVersion(String roomId) {
        return String.format(ROOM_VERSION, roomId);
    }
}