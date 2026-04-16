import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../core/services/api.services';
import { PolicyResponse, AuditLog } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';

// ─── Admin Policies ──────────────────────────────────────────────────────────
@Component({
  standalone: false,
  selector: 'app-admin-policies',
  template: `
  <div class="page">
    <header class="topbar">
      <div><h1 class="page-title">Policy Management</h1><p class="page-sub">All customer policies across the platform</p></div>
      <div class="topbar-actions"><button class="btn btn-ghost btn-sm" (click)="load()">↺ Refresh</button></div>
    </header>
    <div class="content">
      <app-loading *ngIf="loading"></app-loading>
      <div class="card fade-up" *ngIf="!loading">
        <div class="card-header"><div class="card-title">All Policies</div><div class="card-sub">{{ policies.length }} total</div></div>
        <div class="empty-state" *ngIf="policies.length===0">
          <div class="empty-icon">🛡️</div><h3>No policies</h3>
        </div>
        <div class="table-wrap" *ngIf="policies.length>0">
          <table>
            <thead><tr><th>Policy No.</th><th>Type</th><th>Coverage</th><th>Premium</th><th>Frequency</th><th>Customer</th><th>Expires</th><th>Status</th><th></th></tr></thead>
            <tbody>
              <tr *ngFor="let p of policies">
                <td class="cell-mono">{{ p.policyNumber }}</td>
                <td class="cell-name">{{ p.policyTypeName }}</td>
                <td class="cell-amount text-accent">₹{{ p.coverageAmount?.toLocaleString('en-IN') }}</td>
                <td class="cell-amount">₹{{ p.premiumAmount?.toLocaleString('en-IN') }}</td>
                <td style="color:var(--muted)">{{ p.paymentFrequency }}</td>
                <td class="cell-mono">#{{ p.customerId }}</td>
                <td style="color:var(--muted)">{{ p.endDate | date:'dd MMM yyyy' }}</td>
                <td><span class="badge" [class]="getBadge(p.status)">{{ p.status }}</span></td>
                <td>
                  <button *ngIf="p.status==='ACTIVE'" class="btn btn-danger btn-sm" (click)="cancelPolicy(p.id)" [disabled]="actionLoading">Cancel</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>`,
  styles: [`.page{display:flex;flex-direction:column;min-height:100vh}.topbar{background:var(--surface);border-bottom:1px solid var(--border);padding:0 32px;height:var(--topbar-h);display:flex;align-items:center;gap:16px;position:sticky;top:0;z-index:10}.page-title{font-family:var(--font-head);font-size:18px;font-weight:700}.page-sub{font-size:12px;color:var(--muted);margin-top:1px}.topbar-actions{margin-left:auto;display:flex;gap:10px}.content{padding:28px 32px;flex:1}`]
})
export class AdminPoliciesComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;
  actionLoading = false;
  constructor(private adminService: AdminService, private toast: ToastService) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true;
    this.adminService.getAllPolicies().subscribe({
      next: r => { this.policies = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
  cancelPolicy(id: number): void {
    this.actionLoading = true;
    this.adminService.cancelPolicy(id, 'Admin cancellation').subscribe({
      next: () => { this.toast.success('Policy cancelled.'); this.actionLoading = false; this.load(); },
      error: err => { this.actionLoading = false; this.toast.error(err.error?.message || 'Failed.'); }
    });
  }
  getBadge(s: string): string {
    return { ACTIVE:'badge-active', CREATED:'badge-created', EXPIRED:'badge-expired', CANCELLED:'badge-cancelled' }[s] || 'badge-expired';
  }
}

// ─── Audit Logs ──────────────────────────────────────────────────────────────
@Component({
  standalone: false,
  selector: 'app-audit-logs',
  template: `
  <div class="page">
    <header class="topbar">
      <div><h1 class="page-title">Audit Logs</h1><p class="page-sub">Complete admin activity history</p></div>
      <div class="topbar-actions"><button class="btn btn-ghost btn-sm" (click)="load()">↺ Refresh</button></div>
    </header>
    <div class="content">
      <app-loading *ngIf="loading"></app-loading>
      <div class="card fade-up" *ngIf="!loading">
        <div class="card-header"><div class="card-title">All Audit Logs</div><div class="card-sub">{{ logs.length }} entries</div></div>
        <div class="empty-state" *ngIf="logs.length===0">
          <div class="empty-icon">≡</div><h3>No logs</h3><p>Audit logs will appear as admins take actions.</p>
        </div>
        <div *ngIf="logs.length>0">
          <div class="log-row" *ngFor="let l of logs">
            <div class="log-entity">{{ l.entityType }}</div>
            <div class="log-body">
              <div class="log-action">{{ l.action }}</div>
              <div class="log-meta">Entity #{{ l.entityId }} · Admin #{{ l.performedBy }}<span *ngIf="l.remarks"> · {{ l.remarks }}</span></div>
            </div>
            <div class="log-date">{{ l.createdAt | date:'dd MMM yyyy, HH:mm' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>`,
  styles: [`.page{display:flex;flex-direction:column;min-height:100vh}.topbar{background:var(--surface);border-bottom:1px solid var(--border);padding:0 32px;height:var(--topbar-h);display:flex;align-items:center;gap:16px;position:sticky;top:0;z-index:10}.page-title{font-family:var(--font-head);font-size:18px;font-weight:700}.page-sub{font-size:12px;color:var(--muted);margin-top:1px}.topbar-actions{margin-left:auto;display:flex;gap:10px}.content{padding:28px 32px;flex:1}.log-row{display:flex;align-items:flex-start;gap:14px;padding:14px 20px;border-bottom:1px solid var(--border);transition:background .12s}.log-row:last-child{border-bottom:none}.log-row:hover{background:var(--surface2)}.log-entity{background:var(--surface2);border:1px solid var(--border2);border-radius:6px;padding:3px 8px;font-size:10px;font-weight:600;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;white-space:nowrap;flex-shrink:0;margin-top:2px}.log-body{flex:1}.log-action{font-size:13px;color:var(--text);font-weight:500}.log-meta{font-size:11px;color:var(--muted);margin-top:3px}.log-date{font-size:11px;color:var(--dim);white-space:nowrap;flex-shrink:0}`]
})
export class AuditLogsComponent implements OnInit {
  logs: AuditLog[] = [];
  loading = true;
  constructor(private adminService: AdminService) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true;
    this.adminService.getAuditLogs().subscribe({
      next: r => { this.logs = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
