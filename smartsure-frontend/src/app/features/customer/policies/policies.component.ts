import { Component, OnInit } from '@angular/core';
import { PolicyService } from '../../../core/services/policy.service';
import { PolicyResponse } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-customer-policies',
  templateUrl: './policies.component.html',
  styleUrls: ['./policies.component.scss']
})
export class CustomerPoliciesComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;
  selectedPolicy: PolicyResponse | null = null;
  cancelReason = '';
  showCancelModal = false;
  cancelLoading = false;
  filterStatus = 'ALL';

  constructor(private policyService: PolicyService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.policyService.getMyPolicies(0, 50).subscribe({
      next: res => { this.policies = res.content; this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Failed to load policies.'); }
    });
  }

  get filtered(): PolicyResponse[] {
    if (this.filterStatus === 'ALL') return this.policies;
    return this.policies.filter(p => p.status === this.filterStatus);
  }

  selectPolicy(p: PolicyResponse): void { this.selectedPolicy = p; }

  openCancel(p: PolicyResponse): void { this.selectedPolicy = p; this.showCancelModal = true; }

  confirmCancel(): void {
    if (!this.selectedPolicy) return;
    this.cancelLoading = true;
    this.policyService.cancelPolicy(this.selectedPolicy.id, this.cancelReason).subscribe({
      next: () => {
        this.toast.success('Policy cancelled successfully.');
        this.showCancelModal = false; this.cancelLoading = false; this.cancelReason = '';
        this.load();
      },
      error: err => {
        this.cancelLoading = false;
        this.toast.error(err.error?.message || 'Could not cancel policy.');
      }
    });
  }

  getBadgeClass(s: string): string {
    return { ACTIVE:'badge-active', CREATED:'badge-created', EXPIRED:'badge-expired', CANCELLED:'badge-cancelled' }[s] || 'badge-expired';
  }

  formatAmt(n: number): string {
    if (n >= 100000) return '₹' + (n/100000).toFixed(1) + 'L';
    return '₹' + n?.toLocaleString('en-IN');
  }

  getLifecycleSteps(status: string) {
    const steps = ['CREATED','ACTIVE','EXPIRED','CANCELLED'];
    const idx = steps.indexOf(status);
    return steps.map((s, i) => ({ label: s, state: i < idx ? 'done' : i === idx ? 'current' : 'future' }));
  }
}
