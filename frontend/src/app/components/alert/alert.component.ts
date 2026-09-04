import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { InventoryService, ApiErrorMessage } from '../../services/inventory.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent implements OnInit, OnDestroy {
  private inventoryService = inject(InventoryService);
  private subscription?: Subscription;

  public activeAlert: ApiErrorMessage | null = null;

  ngOnInit(): void {
    this.subscription = this.inventoryService.errorNotification$.subscribe((error) => {
      this.activeAlert = error;
      // Desvanecer después de 10 segundos automáticamente
      setTimeout(() => {
        if (this.activeAlert === error) {
          this.activeAlert = null;
        }
      }, 10000);
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  dismiss(): void {
    this.activeAlert = null;
  }
}
