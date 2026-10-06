package com.retail.inventory.dto;

import java.io.Serializable;

public class StockModificadoEvent implements Serializable {
    
    private Long productId;
    private String sku;
    private String nombre;
    private Integer stockAnterior;
    private Integer stockActual;
    private String tipoModificacion;
    private String fechaMovimiento;
    private String usuarioResponsable;

    // Constructor vacío requerido por Spring/Jackson
    public StockModificadoEvent() {
    }

    // Getters y Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getStockAnterior() { return stockAnterior; }
    public void setStockAnterior(Integer stockAnterior) { this.stockAnterior = stockAnterior; }

    public Integer getStockActual() { return stockActual; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }

    public String getTipoModificacion() { return tipoModificacion; }
    public void setTipoModificacion(String tipoModificacion) { this.tipoModificacion = tipoModificacion; }

    public String getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(String fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public String getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(String usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }
}