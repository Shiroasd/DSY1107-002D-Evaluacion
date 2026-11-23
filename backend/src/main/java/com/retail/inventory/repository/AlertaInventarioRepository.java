package com.retail.inventory.repository;

import com.retail.inventory.domain.AlertaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertaInventarioRepository extends JpaRepository<AlertaInventario, UUID> {
}
