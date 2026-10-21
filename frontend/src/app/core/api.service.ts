import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Account,
  ApiMessage,
  Organization,
  Payment,
  TrainingProgram,
  TrainingRequest,
} from './models';

const API = '/api/v1';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  // Programs
  getPrograms(): Observable<TrainingProgram[]> {
    return this.http.get<TrainingProgram[]>(`${API}/training-program/get`);
  }

  getActivePrograms(): Observable<TrainingProgram[]> {
    return this.http.get<TrainingProgram[]>(`${API}/training-program/get-active`);
  }

  getProgramsByOrganization(organizationId: number): Observable<TrainingProgram[]> {
    return this.http.get<TrainingProgram[]>(`${API}/training-program/get-by-organization/${organizationId}`);
  }

  addProgram(body: unknown): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-program/add`, body);
  }

  updateProgram(id: number, body: unknown): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/training-program/update/${id}`, body);
  }

  toggleProgram(id: number): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/training-program/change-status/${id}`, {});
  }

  deleteProgram(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${API}/training-program/delete/${id}`);
  }

  // Training requests
  getRequests(): Observable<TrainingRequest[]> {
    return this.http.get<TrainingRequest[]>(`${API}/training-request/get`);
  }

  getRequest(id: number): Observable<TrainingRequest> {
    return this.http.get<TrainingRequest>(`${API}/training-request/get/${id}`);
  }

  getRequestsByOrganization(organizationId: number): Observable<TrainingRequest[]> {
    return this.http.get<TrainingRequest[]>(`${API}/training-request/get-by-organization/${organizationId}`);
  }

  getProviderRequests(): Observable<TrainingRequest[]> {
    return this.http.get<TrainingRequest[]>(`${API}/training-request/my-provider-requests`);
  }

  addRequest(body: unknown): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/add`, body);
  }

  updateRequest(id: number, body: unknown): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/training-request/update/${id}`, body);
  }

  acceptRequest(id: number): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/${id}/accept`, {});
  }

  rejectRequest(id: number, reason?: string): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/${id}/reject`, { reason });
  }

  startRequest(id: number): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/${id}/start`, {});
  }

  completeRequest(id: number): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/${id}/complete`, {});
  }

  cancelRequest(id: number, reason?: string): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/training-request/${id}/cancel`, { reason });
  }

  deleteRequest(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${API}/training-request/delete/${id}`);
  }

  // Payments
  getPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${API}/payment/get`);
  }

  addPayment(trainingRequestId: number): Observable<Payment> {
    return this.http.post<Payment>(`${API}/payment/add`, { trainingRequestId });
  }

  refundPayment(id: number): Observable<Payment> {
    return this.http.post<Payment>(`${API}/payment/${id}/refund`, {});
  }

  syncPayment(id: number): Observable<Payment> {
    return this.http.post<Payment>(`${API}/payment/${id}/sync`, {});
  }

  syncByInvoice(invoiceId: string): Observable<Payment> {
    return this.http.post<Payment>(`${API}/payment/sync-by-invoice/${invoiceId}`, {});
  }

  // Organizations
  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${API}/organization/get`);
  }

  addOrganization(body: unknown): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/organization/add`, body);
  }

  updateOrganization(id: number, body: unknown): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/organization/update/${id}`, body);
  }

  deleteOrganization(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${API}/organization/delete/${id}`);
  }

  // Accounts
  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${API}/account/get`);
  }

  getAccountsByOrganization(organizationId: number): Observable<Account[]> {
    return this.http.get<Account[]>(`${API}/account/get-by-organization/${organizationId}`);
  }

  addAccount(body: unknown): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${API}/account/add`, body);
  }

  updateAccount(id: number, body: unknown): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/account/update/${id}`, body);
  }

  changeAccountStatus(id: number, status: string): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${API}/account/status/${id}`, { status });
  }

  deleteAccount(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${API}/account/delete/${id}`);
  }
}
