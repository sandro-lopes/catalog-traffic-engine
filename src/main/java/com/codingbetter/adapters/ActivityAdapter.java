package com.codingbetter.adapters;

import java.time.Instant;
import java.util.List;

/**
 * Interface comum para todos os adapters de extração.
 * Cada adapter é responsável apenas por tradução, sem lógica de negócio.
 */
public interface ActivityAdapter {

    /**
     * Extrai eventos de atividade raw de uma fonte específica.
     * @param window Janela de tempo para extração
     * @return Lista de eventos raw (ainda não normalizados)
     */
    List<RawActivityEvent> extract(TimeWindow window);

    /**
     * Retorna metadados do adapter (fonte, versão, etc.)
     */
    AdapterMetadata getMetadata();

    class TimeWindow {
        private final Instant start;
        private final Instant end;

        public TimeWindow(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() {
            return start;
        }

        public Instant getEnd() {
            return end;
        }
    }

    class RawActivityEvent {
        private String serviceId;
        private Object rawData; // Dados específicos da fonte
        private String source;

        public RawActivityEvent(String serviceId, Object rawData, String source) {
            this.serviceId = serviceId;
            this.rawData = rawData;
            this.source = source;
        }

        public String getServiceId() {
            return serviceId;
        }

        public Object getRawData() {
            return rawData;
        }

        public String getSource() {
            return source;
        }
    }

    class AdapterMetadata {
        private String name;
        private String version;
        private String source;

        public AdapterMetadata(String name, String version, String source) {
            this.name = name;
            this.version = version;
            this.source = source;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getSource() {
            return source;
        }
    }
}

