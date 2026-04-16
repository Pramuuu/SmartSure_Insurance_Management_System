import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../core/services/api.services';
import { ClaimResponse } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-admin-claims',
  templateUrl: './claims.component.html',
  styleUrls: ['./claims.component.scss']
})
export class AdminClaimsComponent implements OnInit {
  claims: ClaimResponse[] = [];
  loading = true;
  selectedClaim: ClaimResponse | null = null;
  remarks = '';
  actionLoading = false;
  filterStatus = 'ALL';

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.adminService.getAllClaims().subscribe({
      next: r => { this.claims = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get filtered(): ClaimResponse[] {
    if (this.filterStatus === 'ALL') return this.claims;
    return this.claims.filter(c => c.status === this.filterStatus);
  }

  select(c: ClaimResponse): void { this.selectedClaim = c; this.remarks = ''; }

  markReview(id: number): void {
    this.actionLoading = true;
    this.adminService.markUnderReview(id).subscribe({
      next: () => { this.toast.success('Claim marked as Under Review.'); this.actionLoading = false; this.load(); this.selectedClaim = null; },
      error: err => { this.actionLoading = false; this.toast.error(err.error?.message || 'Action failed.'); }
    });
  }

  approve(id: number): void {
    this.actionLoading = true;
    this.adminService.approveClaim(id, this.remarks || 'Approved').subscribe({
      next: () => { this.toast.success('Claim approved!'); this.actionLoading = false; this.load(); this.selectedClaim = null; },
      error: err => { this.actionLoading = false; this.toast.error(err.error?.message || 'Approval failed.'); }
    });
  }

  reject(id: number): void {
    if (!this.remarks) { this.toast.warn('Please provide a rejection reason.'); return; }
    this.actionLoading = true;
    this.adminService.rejectClaim(id, this.remarks).subscribe({
      next: () => { this.toast.success('Claim rejected.'); this.actionLoading = false; this.load(); this.selectedClaim = null; },
      error: err => { this.actionLoading = false; this.toast.error(err.error?.message || 'Rejection failed.'); }
    });
  }

  getBadge(s: string): string {
    return { DRAFT:'badge-draft', SUBMITTED:'badge-submitted', UNDER_REVIEW:'badge-under-review', APPROVED:'badge-approved', REJECTED:'badge-rejected' }[s] || 'badge-draft';
  }
}
