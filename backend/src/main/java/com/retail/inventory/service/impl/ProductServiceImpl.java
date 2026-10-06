package com.retail.inventory.service.impl;

import com.retail.inventory.config.RabbitMQConfig;
import com.retail.inventory.domain.Category;
import com.retail.inventory.domain.Product;
import com.retail.inventory.dto.ProductRequestDto;
import com.retail.inventory.dto.ProductResponseDto;
import com.retail.inventory.dto.StockModificadoEvent;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.exception.ResourceNotFoundException;
import com.retail.inventory.repository.CategoryRepository;
import com.retail.inventory.repository.ProductRepository;
import com.retail.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RabbitTemplate rabbitTemplate; // <-- Dependencia de RabbitMQ inyectada por Lombok

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Categoría no encontrada con ID: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllProducts();
        }
        return productRepository.searchByNameOrSku(query.trim()).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        String cleanSku = requestDto.getSku().trim().toUpperCase();

        if (productRepository.existsBySkuIgnoreCase(cleanSku)) {
            throw new BadRequestException("Ya existe un producto registrado con el código SKU: " + cleanSku);
        }

        Category category = categoryRepository.findById(requestDto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + requestDto.getCategoryId()));

        Product product = Product.builder()
            .sku(cleanSku)
            .name(requestDto.getName().trim())
            .description(requestDto.getDescription() != null && !requestDto.getDescription().isBlank() ? requestDto.getDescription().trim() : null)
            .price(requestDto.getPrice())
            .stock(requestDto.getStock())
            .category(category)
            .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        String cleanSku = requestDto.getSku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(cleanSku, id)) {
            throw new BadRequestException("Ya existe otro producto registrado con el código SKU: " + cleanSku);
        }

        Category category = categoryRepository.findById(requestDto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + requestDto.getCategoryId()));

        // Capturamos el stock anterior antes de modificarlo para el evento
        Integer stockAnterior = product.getStock();

        product.setSku(cleanSku);
        product.setName(requestDto.getName().trim());
        product.setDescription(requestDto.getDescription() != null && !requestDto.getDescription().isBlank() ? requestDto.getDescription().trim() : null);
        product.setPrice(requestDto.getPrice());
        product.setStock(requestDto.getStock());
        product.setCategory(category);

        // 1. Comunicación Síncrona: Se guarda en la base de datos de inmediato
        Product updated = productRepository.save(product);
        System.out.println("[SÍNCRONO] Producto actualizado exitosamente en BD. ID: " + updated.getId());

        // 2. Comunicación Asíncrona: Disparamos el evento a RabbitMQ
        if (stockAnterior != updated.getStock()) {
            StockModificadoEvent evento = new StockModificadoEvent();
            evento.setProductId(updated.getId());
            evento.setSku(updated.getSku());
            evento.setNombre(updated.getName());
            evento.setStockAnterior(stockAnterior);
            evento.setStockActual(updated.getStock());
            evento.setTipoModificacion("ACTUALIZACION_INVENTARIO");
            evento.setFechaMovimiento(LocalDateTime.now().toString());
            evento.setUsuarioResponsable("admin@tiendaperrito.cl"); // Simulado para la evaluación

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "inventario.stock.modificado", evento);
            System.out.println("[ASÍNCRONO] Evento de stock modificado enviado a RabbitMQ (Routing Key: inventario.stock.modificado)");
        }

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDto mapToResponse(Product product) {
        return ProductResponseDto.builder()
            .id(product.getId())
            .sku(product.getSku())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .stock(product.getStock())
            .categoryId(product.getCategory().getId())
            .categoryName(product.getCategory().getName())
            .build();
    }
}