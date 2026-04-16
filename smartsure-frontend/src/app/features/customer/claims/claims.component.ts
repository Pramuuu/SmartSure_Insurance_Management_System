import { Component, OnInit } from '@angular/core';
import { ClaimService } from '../../../core/services/api.services';
import { PolicyService } from '../../../core/services/policy.service';
import { ToastService } from '../../../core/services/toast.service';
import { ClaimResponse, PolicyResponse, ClaimRequest } from '../../../core/models';

type ClaimStep = 'list' | 'create' | 'upload' | 'done';

@Component({
  standalone: false,
  selector: 'app-customer-claims',
  templateUrl: './claims.component.html',
  styleUrls: ['./claims.component.scss']
})
export class CustomerClaimsComponent implements OnInit {
  claims: ClaimResponse[] = [];
  policies: PolicyResponse[] = [];
  loading = true;
  claimStep: ClaimStep = 'list';
  newClaim: Partial<ClaimRequest> = {};
  createdClaim: ClaimResponse | null = null;
  uploadLoading = false;
  createLoading = false;
  submitLoading = false;
  selectedFile: File | null = null;
  selectedEvidence: File | null = null;

  constructor(
    private claimService: ClaimService,
    private policyService: PolicyService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadClaims();
    this.policyService.getMyPolicies(0, 50).subscribe({
      next: r => this.policies = r.content.filter(p => p.status === 'ACTIVE')
    });
  }

  loadClaims(): void {
    this.loading = true;
    this.claimService.getMyClaims().subscribe({
      next: r => { this.claims = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  startClaim(): void { this.newClaim = {}; this.claimStep = 'create'; }

  createClaim(): void {
    if (!this.newClaim.policyId || !this.newClaim.claimAmount || !this.newClaim.description || !this.newClaim.incidentDate) {
      this.toast.warn('Please fill in all required fields.'); return;
    }
    this.createLoading = true;
    this.claimService.createClaim(this.newClaim as ClaimRequest).subscribe({
      next: c => { this.createdClaim = c; this.createLoading = false; this.claimStep = 'upload'; this.toast.success('Claim draft created!'); },
      error: err => { this.createLoading = false; this.toast.error(err.error?.message || 'Failed to create claim.'); }
    });
  }

  uploadEvidence(): void {
    if (!this.selectedEvidence || !this.createdClaim) return;
    this.uploadLoading = true;
    this.claimService.uploadEvidence(this.createdClaim.id, this.selectedEvidence).subscribe({
      next: () => { this.uploadLoading = false; this.toast.success('Evidence uploaded!'); },
      error: () => { this.uploadLoading = false; this.toast.error('Upload failed.'); }
    });
  }

  submitClaim(): void {
    if (!this.createdClaim) return;
    this.submitLoading = true;
    this.claimService.submitClaim(this.createdClaim.id).subscribe({
      next: c => { this.createdClaim = c; this.submitLoading = false; this.claimStep = 'done'; this.loadClaims(); this.toast.success('Claim submitted successfully!'); },
      error: err => { this.submitLoading = false; this.toast.error(err.error?.message || 'Submit failed.'); }
    });
  }

  onFileChange(e: Event, type: 'evidence' | 'form'): void {
    const f = (e.target as HTMLInputElement).files?.[0];
    if (type === 'evidence') this.selectedEvidence = f || null;
    else this.selectedFile = f || null;
  }

  getBadge(s: string): string {
    return { DRAFT:'badge-draft', SUBMITTED:'badge-submitted', UNDER_REVIEW:'badge-under-review', APPROVED:'badge-approved', REJECTED:'badge-rejected' }[s] || 'badge-draft';
  }
}
