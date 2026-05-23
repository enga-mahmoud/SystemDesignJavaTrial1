import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Order } from '../../../shared/models/order.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html'
})
export class OrderHistoryComponent implements OnInit {
  orders: Order[] = [];
  loading = false;
  error = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loading = true;
    this.http.get<{ content: Order[]; totalElements: number }>(
      `${environment.apiUrl}/api/orders/my`
    ).subscribe({
      next: (response) => {
        this.orders = response.content ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Failed to load orders';
        this.loading = false;
      }
    });
  }

  statusClass(status: string): string {
    return status.toLowerCase();
  }
}
