package com.codingbetter.schemas;

import com.codingbetter.schemas.v1.ServiceActivityEvent;
import com.codingbetter.schemas.v1.ServiceActivitySnapshot;

/**
 * Registry centralizado para versionamento de schemas.
 * Facilita evolução e compatibilidade retroativa.
 */
public class SchemaRegistry {

    public static final String SERVICE_ACTIVITY_EVENT_V1 = "ServiceActivityEvent.v1";
    public static final String SERVICE_ACTIVITY_SNAPSHOT_V1 = "ServiceActivitySnapshot.v1";

    public static Class<?> getSchemaClass(String schemaVersion) {
        return switch (schemaVersion) {
            case SERVICE_ACTIVITY_EVENT_V1 -> ServiceActivityEvent.class;
            case SERVICE_ACTIVITY_SNAPSHOT_V1 -> ServiceActivitySnapshot.class;
            default -> throw new IllegalArgumentException("Schema version não suportada: " + schemaVersion);
        };
    }

    public static boolean isSupported(String schemaVersion) {
        return schemaVersion.equals(SERVICE_ACTIVITY_EVENT_V1) ||
               schemaVersion.equals(SERVICE_ACTIVITY_SNAPSHOT_V1);
    }
}

