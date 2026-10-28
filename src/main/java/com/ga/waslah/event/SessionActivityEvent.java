package com.ga.waslah.event;

import java.util.List;

/**
 * Published inside the transaction that changed a study session. It is only
 * delivered after that transaction commits, so users are never told about a
 * change that was later rolled back.
*/
public record SessionActivityEvent(
        Long sessionId,
        String type,
        List<String> recipients,
        String message
) {
}
