import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PolicyType, PolicyPurchaseRequest, PolicyResponse, PolicyPageResponse,
  PremiumCalculationRequest, PremiumCalculationResponse, PremiumResponse
} from '../models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private base = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  getPolicyTypes(): Observable<PolicyType[]> {
    return this.http.get<PolicyType[]>(`${this.base}/policy-types`);
  }

  calculatePremium(req: PremiumCalculationRequest): Observable<PremiumCalculationResponse> {
    return this.http.post<PremiumCalculationResponse>(`${this.base}/policies/calculate-premium`, req);
  }

  purchasePolicy(req: PolicyPurchaseRequest): Observable<PolicyResponse> {
    return this.http.post<PolicyResponse>(`${this.base}/policies/purchase`, req);
  }

  getMyPolicies(page = 0, size = 10): Observable<PolicyPageResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PolicyPageResponse>(`${this.base}/policies/my`, { params });
  }

  getPolicyById(id: number): Observable<PolicyResponse> {
    return this.http.get<PolicyResponse>(`${this.base}/policies/${id}`);
  }

  cancelPolicy(id: number, reason?: string): Observable<PolicyResponse> {
    const params = reason ? new HttpParams().set('reason', reason) : undefined;
    return this.http.put<PolicyResponse>(`${this.base}/policies/${id}/cancel`, {}, { params });
  }

  getPolicyPremiums(id: number): Observable<PremiumResponse[]> {
    return this.http.get<PremiumResponse[]>(`${this.base}/policies/${id}/premiums`);
  }

  payPremium(premiumId: number): Observable<PremiumResponse> {
    return this.http.post<PremiumResponse>(`${this.base}/policies/premiums/pay`, { premiumId });
  }
}
