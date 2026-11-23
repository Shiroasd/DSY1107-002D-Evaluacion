package com.retail.inventory.consumer;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventarioMovimientoValidator {
    private final Validator validator;

    public InventarioMovimientoEvent validar(ConsumerRecord<String, InventarioMovimientoEvent> record) {
        InventarioMovimientoEvent event = record.value();
        if (event == null || !validator.validate(event).isEmpty()
            || !event.productoId().toString().equals(record.key())
            || event.diferencia() != event.stockNuevo() - event.stockAnterior()
            || !switch (event.tipo()) {
                case CREACION -> event.stockAnterior() == 0;
                case ENTRADA -> event.diferencia() > 0;
                case SALIDA -> event.diferencia() < 0;
                case AJUSTE -> event.diferencia() != 0;
            }) {
            throw new MovimientoInvalidoException();
        }
        return event;
    }

    public static class MovimientoInvalidoException extends IllegalArgumentException {
        public MovimientoInvalidoException() {
            super("Movimiento de inventario invalido");
        }
    }
}
