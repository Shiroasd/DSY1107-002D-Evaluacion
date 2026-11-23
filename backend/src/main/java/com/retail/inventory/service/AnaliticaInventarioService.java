package com.retail.inventory.service;

import com.retail.inventory.domain.EventoAnaliticaProcesado;
import com.retail.inventory.domain.MetricaInventario;
import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.repository.MetricaInventarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.retail.inventory.domain.MetricaInventario.RESUMEN_ID;

@Service
@RequiredArgsConstructor
public class AnaliticaInventarioService {
    private final EntityManager entityManager;
    private final MetricaInventarioRepository repository;

    @Transactional
    public boolean procesar(InventarioMovimientoEvent event) {
        var resumen = entityManager.find(MetricaInventario.class, RESUMEN_ID, LockModeType.PESSIMISTIC_WRITE);
        if (entityManager.find(EventoAnaliticaProcesado.class, event.eventId()) != null) {
            return false;
        }
        if (resumen == null) {
            resumen = crearMetrica(RESUMEN_ID);
            entityManager.flush();
        }

        var producto = entityManager.find(MetricaInventario.class, event.productoId());
        if (producto == null) {
            producto = crearMetrica(event.productoId());
        }

        Instant procesado = Instant.now();
        resumen.acumular(event, procesado);
        producto.acumular(event, procesado);
        entityManager.persist(new EventoAnaliticaProcesado(event.eventId()));
        return true;
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        var metrica = repository.findById(RESUMEN_ID).orElseGet(() -> new MetricaInventario(RESUMEN_ID));
        return new Resumen(metrica.getMovimientos(), metrica.getEntradas(), metrica.getSalidas(),
            metrica.getUnidadesIngresadas(), metrica.getUnidadesRetiradas(),
            metrica.getUltimoMovimiento(), metrica.getUltimoProcesamiento());
    }

    @Transactional(readOnly = true)
    public Page<Producto> productos(Pageable pageable) {
        return repository.findByIdGreaterThan(RESUMEN_ID, pageable).map(metrica ->
            new Producto(metrica.getId(), metrica.getSku(), metrica.getNombre(), metrica.getMovimientos()));
    }

    private MetricaInventario crearMetrica(long id) {
        var metrica = new MetricaInventario(id);
        entityManager.persist(metrica);
        return metrica;
    }

    public record Resumen(long movimientos, long entradas, long salidas, long unidadesIngresadas,
                          long unidadesRetiradas, Instant ultimoMovimiento, Instant ultimoProcesamiento) {
    }

    public record Producto(Long productoId, String sku, String nombre, long movimientos) {
    }
}
