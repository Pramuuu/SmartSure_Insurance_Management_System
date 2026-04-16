import { Component, OnInit } from '@angular/core';
import { PolicyService } from '../../../core/services/policy.service';
import { PaymentService } from '../../../core/services/api.services';
import { ToastService } from '../../../core/services/toast.service';
import { Router } from '@angular/router';
import {
  PolicyType, PremiumCalculationResponse, PolicyResponse, PaymentResponse
} from '../../../core/models';

type Step = 'catalog' | 'quote' | 'details' | 'payment' | 'success';

@Component({
  standalone: false,
  selector: 'app-purchase',
  templateUrl: './purchase.component.html',
  styleUrls: ['./purchase.component.scss']
})
export class PurchaseComponent implements OnInit {
  step: Step = 'catalog';
  policyTypes: PolicyType[] = [];
  selectedType: PolicyType | null = null;
  quoteResult: PremiumCalculationResponse | null = null;
  purchasedPolicy: PolicyResponse | null = null;
  paymentResult: PaymentResponse | null = null;

  // Form fields
  coverageAmount = 500000;
  paymentFrequency: 'MONTHLY'|'QUARTERLY'|'SEMI_ANNUAL'|'ANNUAL' = 'MONTHLY';
  customerAge = 30;
  startDate = new Date().toISOString().split('T')[0];
  nomineeName = '';
  nomineeRelation = '';

  loading = false;
  quoteLoading = false;
  filterCategory = 'ALL';

  categories = ['ALL', 'HEALTH', 'AUTO', 'HOME', 'LIFE', 'TRAVEL'];

  categoryIcons: Record<string, string> = {
    HEALTH: '❤️', AUTO: '🚗', HOME: '🏠', LIFE: '🌱', TRAVEL: '✈️'
  };

  steps: { key: Step; label: string }[] = [
    { key: 'catalog', label: 'Choose Plan' },
    { key: 'quote',   label: 'Get Quote' },
    { key: 'details', label: 'Details' },
    { key: 'payment', label: 'Payment' },
    { key: 'success', label: 'Done' },
  ];

  constructor(
    private policyService: PolicyService,
    private paymentService: PaymentService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.policyService.getPolicyTypes().subscribe({
      next: t => this.policyTypes = t,
      error: () => this.toast.error('Failed to load policy types.')
    });
  }

  get filteredTypes(): PolicyType[] {
    if (this.filterCategory === 'ALL') return this.policyTypes;
    return this.policyTypes.filter(t => t.category === this.filterCategory);
  }

  selectType(t: PolicyType): void {
    this.selectedType = t;
    this.step = 'quote';
  }

  calculateQuote(): void {
    if (!this.selectedType) return;
    this.quoteLoading = true;
    this.policyService.calculatePremium({
      policyTypeId: this.selectedType.id,
      coverageAmount: this.coverageAmount,
      paymentFrequency: this.paymentFrequency,
      customerAge: this.customerAge
    }).subscribe({
      next: res => { this.quoteResult = res; this.quoteLoading = false; },
      error: err => { this.quoteLoading = false; this.toast.error(err.error?.message || 'Quote failed.'); }
    });
  }

  proceedToDetails(): void {
    if (!this.quoteResult) { this.toast.warn('Please calculate a quote first.'); return; }
    this.step = 'details';
  }

  purchasePolicy(): void {
    if (!this.selectedType) return;
    this.loading = true;
    this.policyService.purchasePolicy({
      policyTypeId: this.selectedType.id,
      coverageAmount: this.coverageAmount,
      paymentFrequency: this.paymentFrequency,
      startDate: this.startDate,
      nomineeName: this.nomineeName || undefined,
      nomineeRelation: this.nomineeRelation || undefined,
      customerAge: this.customerAge
    }).subscribe({
      next: policy => {
        this.purchasedPolicy = policy;
        this.initiatePayment(policy);
      },
      error: err => { this.loading = false; this.toast.error(err.error?.message || 'Purchase failed.'); }
    });
  }

  initiatePayment(policy: PolicyResponse): void {
    this.paymentService.initiatePayment({ policyId: policy.id, amount: policy.premiumAmount }).subscribe({
      next: res => {
        this.paymentResult = res;
        this.loading = false;
        this.step = 'payment';
      },
      error: err => { this.loading = false; this.toast.error(err.error?.message || 'Payment init failed.'); }
    });
  }

  confirmPayment(): void {
    if (!this.paymentResult) return;
    this.loading = true;
    this.paymentService.confirmPayment({
      paymentId: this.paymentResult.id,
      razorpayPaymentId: 'SIMULATED_' + Date.now(),
      razorpayOrderId: this.paymentResult.razorpayOrderId || '',
      razorpaySignature: 'SIMULATED_SIG'
    }).subscribe({
      next: () => { this.loading = false; this.step = 'success'; this.toast.success('Payment confirmed! Policy is active.'); },
      error: err => { this.loading = false; this.toast.error(err.error?.message || 'Payment confirm failed.'); }
    });
  }

  currentStepIndex(): number {
    return this.steps.findIndex(s => s.key === this.step);
  }

  formatAmt(n: number): string {
    if (!n) return '—';
    if (n >= 10000000) return '₹' + (n/10000000).toFixed(1) + 'Cr';
    if (n >= 100000) return '₹' + (n/100000).toFixed(0) + 'L';
    return '₹' + n.toLocaleString('en-IN');
  }

  freqLabel(): string {
    return { MONTHLY:'month', QUARTERLY:'quarter', SEMI_ANNUAL:'6 months', ANNUAL:'year' }[this.paymentFrequency] || '';
  }
}
