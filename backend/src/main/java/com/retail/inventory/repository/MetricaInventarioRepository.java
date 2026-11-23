package com.retail.inventory.repository;

import com.retail.inventory.domain.MetricaInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricaInventarioRepository extends JpaRepository<MetricaInventario, Long> {
    Page<MetricaInventario> findByIdGreaterThan(Long id, Pageable pageable);
}
