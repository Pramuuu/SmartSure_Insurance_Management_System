import { Component, OnInit } from '@angular/core';
import { PolicyService } from '../../../core/services/policy.service';
import { ClaimService } from '../../../core/services/api.services';
import { AuthService } from '../../../core/services/auth.service';
import { PolicyResponse, ClaimResponse } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';
import { Router } from '@angular/router';

@Component({
  standalone: false,
  selector: 'app-customer-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class CustomerDashboardComponent implements OnInit {
  policies: PolicyResponse[] = [];
  claims: ClaimResponse[] = [];
  loading = true;

  get activePolicies() { return this.policies.filter(p => p.status === 'ACTIVE'); }
  get totalCoverage() { return this.activePolicies.reduce((s, p) => s + (p.coverageAmount || 0), 0); }
  get pendingClaims() { return this.claims.filter(c => c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW').length; }
  get userName() { return this.auth.getCurrentUser()?.firstName || 'there'; }

  constructor(
    private policyService: PolicyService,
    private claimService: ClaimService,
    public auth: AuthService,
    private toast: ToastService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.policyService.getMyPolicies(0, 5).subscribe({
      next: res => { this.policies = res.content; this.loading = false; },
      error: () => { this.loading = false; }
    });
    this.claimService.getMyClaims().subscribe({ next: res => this.claims = res, error: () => {} });
  }

  formatAmount(n: number): string {
    if (n >= 10000000) return '₹' + (n / 10000000).toFixed(1) + 'Cr';
    if (n >= 100000)   return '₹' + (n / 100000).toFixed(1) + 'L';
    return '₹' + n.toLocaleString('en-IN');
  }

  getBadgeClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge-active', CREATED: 'badge-created',
      EXPIRED: 'badge-expired', CANCELLED: 'badge-cancelled'
    };
    return map[status] || 'badge-expired';
  }

  getClaimBadge(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-draft', SUBMITTED: 'badge-submitted',
      UNDER_REVIEW: 'badge-under-review', APPROVED: 'badge-approved', REJECTED: 'badge-rejected'
    };
    return map[status] || 'badge-draft';
  }
}
