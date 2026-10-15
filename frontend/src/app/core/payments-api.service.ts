import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreatePaymentRequest, Payment, PaymentStatus, Problem, StatusSummary } from './models';

@Injectable({ providedIn: 'root' })
export class PaymentsApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/payments';

  list(status: PaymentStatus | null): Observable<Payment[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Payment[]>(this.base, { params });
  }

  summary(): Observable<StatusSummary> {
    return this.http.get<StatusSummary>(`${this.base}/summary`);
  }

  get(id: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.base}/${id}`);
  }

  /** The idempotency key makes a retried or double-clicked submit safe. */
  create(request: CreatePaymentRequest, idempotencyKey: string): Observable<Payment> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<Payment>(this.base, request, { headers });
  }

  approve(id: number, note: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${id}/approve`, { note: note || null });
  }

  reject(id: number, note: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${id}/reject`, { note });
  }

  release(id: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${id}/release`, {});
  }
}

export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const problem = err.error as Problem | null;
    if (problem?.detail) {
      return problem.detail;
    }
    if (err.status === 0) {
      return 'Cannot reach the PayFlow API. Is the backend running?';
    }
    if (err.status === 403) {
      return 'You do not have permission to do that.';
    }
    return `Request failed (${err.status}).`;
  }
  return 'Something went wrong.';
}
