package com.retail.inventory.consumer;

import com.retail.inventory.config.RabbitMQConfig;
import com.retail.inventory.dto.StockModificadoEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InventarioConsumers {

    // Simula el microservicio de auditoría
    @RabbitListener(queues = RabbitMQConfig.QUEUE_AUDITORIA)
    public void registrarAuditoria(StockModificadoEvent evento) {
        System.out.println("==================================================");
        System.out.println("[ms-auditoria] Registrando movimiento en bitácora:");
        System.out.println(" -> Usuario responsable: " + evento.getUsuarioResponsable());
        System.out.println(" -> SKU: " + evento.getSku());
        System.out.println(" -> Tipo de Modificación: " + evento.getTipoModificacion());
        System.out.println("==================================================");
    }

    // Simula el microservicio de notificaciones
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES)
    public void procesarNotificacionStock(StockModificadoEvent evento) {
        // Lógica: Solo notificar si el stock queda en nivel crítico (ej. <= 3)
        if (evento.getStockActual() != null && evento.getStockActual() <= 3) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("[ms-notificaciones] ¡ALERTA DE STOCK CRÍTICO!");
            System.out.println(" -> Producto: " + evento.getNombre());
            System.out.println(" -> Quedan solo " + evento.getStockActual() + " unidades.");
            System.out.println(" -> Enviando correo al administrador...");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }
}