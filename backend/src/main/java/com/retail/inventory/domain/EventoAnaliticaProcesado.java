package com.retail.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "eventos_analitica_procesados")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoAnaliticaProcesado {
    @Id
    private UUID eventId;

    public EventoAnaliticaProcesado(UUID eventId) {
        this.eventId = eventId;
    }
}
