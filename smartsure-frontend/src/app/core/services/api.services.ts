import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClaimRequest, ClaimResponse, PaymentRequest, PaymentResponse,
  ConfirmPaymentRequest, DashboardMetrics, AuditLog, PolicyResponse, PolicyType
} from '../models';
import { environment } from '../../../environments/environment';

// ─── Claim Service ───────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class ClaimService {
  private base = `${environment.apiUrl}/api/claims`;
  constructor(private http: HttpClient) {}

  createClaim(req: ClaimRequest): Observable<ClaimResponse> {
    return this.http.post<ClaimResponse>(this.base, req);
  }
  getMyClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.base}/my`);
  }
  getClaimById(id: number): Observable<ClaimResponse> {
    return this.http.get<ClaimResponse>(`${this.base}/${id}`);
  }
  submitClaim(id: number): Observable<ClaimResponse> {
    return this.http.put<ClaimResponse>(`${this.base}/${id}/submit`, {});
  }
  uploadEvidence(id: number, file: File): Observable<ClaimResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ClaimResponse>(`${this.base}/${id}/upload/evidence`, form);
  }
  uploadClaimForm(id: number, file: File): Observable<ClaimResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ClaimResponse>(`${this.base}/${id}/upload/claim-form`, form);
  }
  deleteClaim(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

// ─── Payment Service ─────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private base = `${environment.apiUrl}/api/payments`;
  constructor(private http: HttpClient) {}

  initiatePayment(req: PaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.base}/initiate`, req);
  }
  confirmPayment(req: ConfirmPaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.base}/confirm`, req);
  }
  getMyPayments(): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.base}/my`);
  }
  getPaymentById(id: number): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.base}/${id}`);
  }
}

// ─── Admin Service ───────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = `${environment.apiUrl}/api/admin`;
  constructor(private http: HttpClient) {}

  getDashboard(): Observable<DashboardMetrics> {
    return this.http.get<DashboardMetrics>(`${this.base}/dashboard`);
  }
  getAllClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.base}/claims`);
  }
  getUnderReviewClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.base}/claims/under-review`);
  }
  approveClaim(id: number, remarks: string): Observable<ClaimResponse> {
    return this.http.put<ClaimResponse>(`${this.base}/claims/${id}/approve`, { remarks });
  }
  rejectClaim(id: number, remarks: string): Observable<ClaimResponse> {
    return this.http.put<ClaimResponse>(`${this.base}/claims/${id}/reject`, { remarks });
  }
  markUnderReview(id: number): Observable<ClaimResponse> {
    return this.http.put<ClaimResponse>(`${this.base}/claims/${id}/review`, {});
  }
  getAllPolicies(): Observable<PolicyResponse[]> {
    return this.http.get<PolicyResponse[]>(`${this.base}/policies`);
  }
  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/users`);
  }
  getAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.base}/audit-logs`);
  }
  getRecentActivity(limit = 10): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.base}/audit-logs/recent?limit=${limit}`);
  }
  cancelPolicy(policyId: number, reason?: string): Observable<PolicyResponse> {
    return this.http.put<PolicyResponse>(`${this.base}/policies/${policyId}/cancel`, {}, {
      params: reason ? { reason } : {}
    });
  }
}
