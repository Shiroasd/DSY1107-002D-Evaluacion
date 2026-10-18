package com.retail.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockModificadoEvent implements Serializable {

    private Long productId;
    private String sku;
    private String nombre;
    private Integer stockAnterior;
    private Integer stockActual;
    private String tipoModificacion;
    private String fechaMovimiento;
    private String usuarioResponsable;
}
