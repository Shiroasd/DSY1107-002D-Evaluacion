import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService } from '../../services/inventory.service';
import { AuthService } from '../../services/auth.service';
import { Product, ProductRequest } from '../../models/product.model';
import { Category } from '../../models/category.model';
import { ProductModalComponent } from '../product-modal/product-modal.component';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductModalComponent],
  templateUrl: './inventory.component.html',
  styleUrls: ['./inventory.component.css']
})
export class InventoryComponent implements OnInit {
  private inventoryService = inject(InventoryService);
  public authService = inject(AuthService);

  public products: Product[] = [];
  public categories: Category[] = [];
  public isLoading: boolean = false;
  public viewMode: 'table' | 'cards' = 'table';

  // Filtros de búsqueda
  public searchQuery: string = '';
  public selectedCategoryId: number | null = null;
  public stockFilter: 'all' | 'in_stock' | 'low_stock' | 'out_of_stock' = 'all';

  // Modal de Producto
  public isModalOpen: boolean = false;
  public selectedProduct: Product | null = null;

  // Confirmación de eliminación
  public productToDelete: Product | null = null;

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  public loadCategories(): void {
    this.inventoryService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => console.error('Error al cargar categorías', err)
    });
  }

  public loadProducts(): void {
    this.isLoading = true;
    this.inventoryService.getProducts(this.selectedCategoryId, this.searchQuery).subscribe({
      next: (data) => {
        this.products = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error al cargar productos', err);
      }
    });
  }

  public onSearchChange(): void {
    this.loadProducts();
  }

  public onCategoryChange(): void {
    this.loadProducts();
  }

  // Métricas del Dashboard (KPIs)
  get totalProducts(): number {
    return this.products.length;
  }

  get lowStockCount(): number {
    return this.products.filter(p => p.stock > 0 && p.stock <= 10).length;
  }

  get outOfStockCount(): number {
    return this.products.filter(p => p.stock === 0).length;
  }

  get totalValuation(): number {
    return this.products.reduce((acc, p) => acc + (p.price * p.stock), 0);
  }

  // Filtrado reactivo en el cliente para el stock status
  get filteredProducts(): Product[] {
    return this.products.filter(product => {
      if (this.stockFilter === 'in_stock') {
        return product.stock > 10;
      } else if (this.stockFilter === 'low_stock') {
        return product.stock > 0 && product.stock <= 10;
      } else if (this.stockFilter === 'out_of_stock') {
        return product.stock === 0;
      }
      return true;
    });
  }

  // Modal Actions
  public openCreateModal(): void {
    this.selectedProduct = null;
    this.isModalOpen = true;
  }

  public openEditModal(product: Product): void {
    this.selectedProduct = product;
    this.isModalOpen = true;
  }

  public closeModal(): void {
    this.isModalOpen = false;
    this.selectedProduct = null;
  }

  public onSaveProduct(request: ProductRequest): void {
    if (this.selectedProduct) {
      // Actualizar (PUT)
      this.inventoryService.updateProduct(this.selectedProduct.id, request).subscribe({
        next: () => {
          this.closeModal();
          this.loadProducts();
        },
        error: (err) => console.error('Error en actualización', err)
      });
    } else {
      // Crear (POST)
      this.inventoryService.createProduct(request).subscribe({
        next: () => {
          this.closeModal();
          this.loadProducts();
        },
        error: (err) => console.error('Error en creación', err)
      });
    }
  }

  // Delete Action
  public confirmDelete(product: Product): void {
    this.productToDelete = product;
  }

  public cancelDelete(): void {
    this.productToDelete = null;
  }

  public executeDelete(): void {
    if (!this.productToDelete) return;

    this.inventoryService.deleteProduct(this.productToDelete.id).subscribe({
      next: () => {
        this.productToDelete = null;
        this.loadProducts();
      },
      error: (err) => {
        this.productToDelete = null;
        console.error('Error en eliminación', err);
      }
    });
  }
}
