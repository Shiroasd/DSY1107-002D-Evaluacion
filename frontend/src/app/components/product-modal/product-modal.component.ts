import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Product, ProductRequest } from '../../models/product.model';
import { Category } from '../../models/category.model';

@Component({
  selector: 'app-product-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-modal.component.html',
  styleUrls: ['./product-modal.component.css']
})
export class ProductModalComponent implements OnInit {
  private fb = inject(FormBuilder);

  @Input() product: Product | null = null;
  @Input() categories: Category[] = [];
  @Output() save = new EventEmitter<ProductRequest>();
  @Output() close = new EventEmitter<void>();

  public form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      sku: [
        this.product ? this.product.sku : '',
        [Validators.required, Validators.minLength(3), Validators.maxLength(50)]
      ],
      name: [
        this.product ? this.product.name : '',
        [Validators.required, Validators.minLength(2), Validators.maxLength(150)]
      ],
      price: [
        this.product ? this.product.price : null,
        [Validators.required, Validators.min(0.01)]
      ],
      stock: [
        this.product ? this.product.stock : null,
        [Validators.required, Validators.min(0)]
      ],
      categoryId: [
        this.product ? this.product.categoryId : (this.categories.length > 0 ? this.categories[0].id : ''),
        [Validators.required]
      ]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const request: ProductRequest = {
      sku: val.sku.trim(),
      name: val.name.trim(),
      price: Number(val.price),
      stock: Number(val.stock),
      categoryId: Number(val.categoryId)
    };

    this.save.emit(request);
  }

  onCancel(): void {
    this.close.emit();
  }
}
