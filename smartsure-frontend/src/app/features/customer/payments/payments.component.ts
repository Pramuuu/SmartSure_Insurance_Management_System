import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../../../core/services/api.services';
import { PaymentResponse } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-customer-payments',
  template: `
  <div class="page">
    <header class="topbar">
      <div><h1 class="page-title">Payment History</h1><p class="page-sub">All your payment transactions</p></div>
      <div class="topbar-actions"><button class="btn btn-ghost btn-sm" (click)="load()">↺ Refresh</button></div>
    </header>
    <div class="content">
      <app-loading *ngIf="loading"></app-loading>
      <div class="card fade-up" *ngIf="!loading">
        <div class="card-header"><div class="card-title">Transactions</div><div class="card-sub">{{ payments.length }} payments found</div></div>
        <div class="empty-state" *ngIf="payments.length===0">
          <div class="empty-icon">💳</div><h3>No payments yet</h3><p>Your payment history will appear here.</p>
        </div>
        <div class="table-wrap" *ngIf="payments.length>0">
          <table>
            <thead><tr><th>Payment ID</th><th>Amount</th><th>Status</th><th>Date</th><th>Razorpay ID</th></tr></thead>
            <tbody>
              <tr *ngFor="let p of payments">
                <td class="cell-mono">#{{ p.id }}</td>
                <td class="cell-amount text-accent">₹{{ p.amount?.toLocaleString('en-IN') }}</td>
                <td><span class="badge" [class]="getBadge(p.status)">{{ p.status }}</span></td>
                <td style="color:var(--muted)">{{ p.createdAt | date:'dd MMM yyyy, hh:mm a' }}</td>
                <td class="cell-mono">{{ p.razorpayPaymentId || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>`,
  styles: [`.page{display:flex;flex-direction:column;min-height:100vh}.topbar{background:var(--surface);border-bottom:1px solid var(--border);padding:0 32px;height:var(--topbar-h);display:flex;align-items:center;gap:16px;position:sticky;top:0;z-index:10}.page-title{font-family:var(--font-head);font-size:18px;font-weight:700}.page-sub{font-size:12px;color:var(--muted);margin-top:1px}.topbar-actions{margin-left:auto;display:flex;gap:10px}.content{padding:28px 32px;flex:1}`]
})
export class CustomerPaymentsComponent implements OnInit {
  payments: PaymentResponse[] = [];
  loading = true;
  constructor(private paymentService: PaymentService, private toast: ToastService) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true;
    this.paymentService.getMyPayments().subscribe({
      next: r => { this.payments = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
  getBadge(s: string): string {
    return { INITIATED:'badge-pending', SUCCESS:'badge-paid', CONFIRMED:'badge-paid', FAILED:'badge-rejected', REFUNDED:'badge-expired' }[s] || 'badge-pending';
  }
}
