import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventoryService } from '../../services/inventory.service';
import { AuthService } from '../../services/auth.service';
import { Category, CategoryRequest } from '../../models/category.model';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css']
})
export class CategoriesComponent implements OnInit {
  private inventoryService = inject(InventoryService);
  private fb = inject(FormBuilder);
  public authService = inject(AuthService);

  public categories: Category[] = [];
  public isLoading: boolean = false;
  public isCreating: boolean = false;

  public categoryForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  public loadCategories(): void {
    this.isLoading = true;
    this.inventoryService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error al cargar categorías', err);
      }
    });
  }

  public onSubmit(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const request: CategoryRequest = {
      name: this.categoryForm.value.name.trim(),
      description: this.categoryForm.value.description ? this.categoryForm.value.description.trim() : ''
    };

    this.isCreating = true;
    this.inventoryService.createCategory(request).subscribe({
      next: () => {
        this.isCreating = false;
        this.categoryForm.reset();
        this.loadCategories();
      },
      error: (err) => {
        this.isCreating = false;
        console.error('Error al crear categoría', err);
      }
    });
  }
}
