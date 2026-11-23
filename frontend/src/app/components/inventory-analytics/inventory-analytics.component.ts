import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, exhaustMap, finalize, forkJoin, merge, of, timer } from 'rxjs';
import { InventoryActivity, InventoryAnalytics, InventoryService } from '../../services/inventory.service';

@Component({
  selector: 'app-inventory-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inventory-analytics.component.html',
  styleUrls: ['../inventory/inventory.component.css']
})
export class InventoryAnalyticsComponent {
  private readonly inventory = inject(InventoryService);
  private readonly refresh = new Subject<void>();

  summary?: InventoryAnalytics;
  topProducts: InventoryActivity[] = [];
  loading = true;
  error = false;

  constructor() {
    merge(timer(0, 15000), this.refresh).pipe(
      exhaustMap(() => {
        this.loading = true;
        return forkJoin({
          summary: this.inventory.getInventorySummary(),
          topProducts: this.inventory.getTopInventoryProducts()
        }).pipe(
          catchError(() => of(null)),
          finalize(() => { this.loading = false; })
        );
      }),
      takeUntilDestroyed()
    ).subscribe(result => {
      this.error = result === null;
      if (result) {
        this.summary = result.summary;
        this.topProducts = result.topProducts;
      }
    });
  }

  refreshNow(): void {
    this.refresh.next();
  }
}
