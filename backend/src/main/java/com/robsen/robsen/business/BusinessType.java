package com.robsen.robsen.business;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum BusinessType {
    MANUFACTURER,
    WHOLESALER,
    DISTRIBUTOR,
    RETAILER,
    IMPORTER,
    EXPORTER,
    SERVICE_PROVIDER,
    COOPERATIVE,
    OTHER;

    public static List<String> normalize(List<String> values) {
        if (values == null) return List.of();
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        boolean supported = normalized.stream().allMatch(value ->
                Arrays.stream(BusinessType.values()).anyMatch(type -> type.name().equals(value)));
        if (!supported) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a supported business type.");
        }
        return normalized;
    }
}
