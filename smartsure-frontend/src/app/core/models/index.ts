// ─── Auth ───────────────────────────────────────────────────────────────────
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { firstName: string; lastName: string; email: string; password: string; role: string; }
export interface AuthResponse { token: string; email: string; role: string; }

// ─── User ───────────────────────────────────────────────────────────────────
export interface User { id: number; email: string; firstName: string; lastName: string; role: string; }

// ─── PolicyType ─────────────────────────────────────────────────────────────
export interface PolicyType {
  id: number; name: string; category: string;
  basePremium?: number; maxCoverageAmount?: number; termInMonths?: number;
  minAge?: number; maxAge?: number; description?: string; active?: boolean;
}

// ─── Policy ─────────────────────────────────────────────────────────────────
export type PaymentFrequency = 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL';
export type PolicyStatus = 'CREATED' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export interface PolicyPurchaseRequest {
  policyTypeId: number; coverageAmount: number;
  paymentFrequency: PaymentFrequency; startDate: string;
  nomineeName?: string; nomineeRelation?: string; customerAge?: number;
}

export interface PolicyResponse {
  id: number; policyNumber: string; status: PolicyStatus;
  coverageAmount: number; premiumAmount: number;
  paymentFrequency: PaymentFrequency; startDate: string; endDate: string;
  nomineeName?: string; nomineeRelation?: string;
  policyTypeName?: string; policyTypeCategory?: string;
  customerId?: number; createdAt?: string;
}

export interface PolicyPageResponse {
  content: PolicyResponse[]; pageNumber: number; pageSize: number;
  totalElements: number; totalPages: number;
}

// ─── Premium Calculation ────────────────────────────────────────────────────
export interface PremiumCalculationRequest {
  policyTypeId: number; coverageAmount: number;
  paymentFrequency: PaymentFrequency; customerAge?: number;
}
export interface PremiumCalculationResponse {
  annualPremium: number; premiumPerPeriod: number;
  paymentFrequency: PaymentFrequency; coverageFactor?: number; ageFactor?: number;
}

// ─── Premium ────────────────────────────────────────────────────────────────
export type PremiumStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'WAIVED';
export interface PremiumResponse {
  id: number; dueDate: string; amount: number; status: PremiumStatus;
  paidDate?: string; paymentMethod?: string; txnReference?: string;
}

// ─── Payment ────────────────────────────────────────────────────────────────
export interface PaymentRequest { policyId: number; amount: number; }
export interface PaymentResponse {
  id: number; amount: number; status: string;
  razorpayOrderId?: string; razorpayKeyId?: string;
  razorpayPaymentId?: string; createdAt?: string;
}
export interface ConfirmPaymentRequest {
  paymentId: number; razorpayPaymentId: string;
  razorpayOrderId: string; razorpaySignature: string;
}

// ─── Claim ──────────────────────────────────────────────────────────────────
export type ClaimStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export interface ClaimRequest {
  policyId: number; claimAmount: number; description: string; incidentDate: string;
}
export interface ClaimResponse {
  id: number; claimNumber: string; status: ClaimStatus;
  claimAmount: number; description: string; incidentDate: string;
  remarks?: string; policyId?: number; policyNumber?: string; createdAt?: string;
}

// ─── Admin ──────────────────────────────────────────────────────────────────
export interface DashboardMetrics {
  totalUsers: number; totalPolicies: number;
  totalClaims: number; pendingClaims: number;
  recentActivity?: AuditLog[];
}
export interface AuditLog {
  id: number; entityType: string; entityId: number; action: string;
  performedBy: number; remarks?: string; createdAt: string;
}
export interface ClaimStatusUpdateRequest { remarks: string; }
