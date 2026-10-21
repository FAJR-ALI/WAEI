import { Component, OnInit, computed, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { apiError, dateTime, money, statusClass, titleCase } from '../../core/format';
import { Payment, TrainingRequest } from '../../core/models';
import { ToastService } from '../../core/toast.service';

const FILTERS = ['ALL', 'PENDING', 'ACCEPTED', 'PAID', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'];

@Component({
  selector: 'app-provider-requests',
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Incoming requests</h1>
          <p class="page-subtitle">Accept requests, deliver the training, and mark each request as complete.</p>
        </div>
      </div>

      <div class="filter-row">
        @for (filter of filters; track filter) {
          <button class="filter-chip" [class.active]="statusFilter() === filter" (click)="statusFilter.set(filter)">
            {{ label(filter) }}
          </button>
        }
      </div>

      <div class="card">
        @if (loading()) {
          <div class="empty">Loading incoming requests…</div>
        } @else if (filtered().length === 0) {
          <div class="empty">
            <strong>No requests in this view</strong>
            Beneficiary organizations will appear here when they request one of your programs.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Program</th>
                  <th>Beneficiary</th>
                  <th>Schedule</th>
                  <th>Participants</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (request of filtered(); track request.id) {
                  <tr>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">{{ request.trainingProgramTitle }}</span>
                        <span class="cell-muted">Request #{{ request.id }} · {{ date(request.requestedAt) }}</span>
                        @if (request.notes) {
                          <span class="cell-muted">{{ request.notes }}</span>
                        }
                      </div>
                    </td>
                    <td>
                      <div class="cell-stack">
                        <span>{{ request.beneficiaryOrganizationName }}</span>
                        <span class="cell-muted">{{ request.createdByFullName }}</span>
                      </div>
                    </td>
                    <td>
                      <div class="cell-stack">
                        <span>{{ date(request.scheduledStart) }}</span>
                        <span class="cell-muted">to {{ date(request.scheduledEnd) }}</span>
                      </div>
                    </td>
                    <td>{{ request.participantCount }}</td>
                    <td><span [class]="badge(request.status)">{{ label(request.status) }}</span></td>
                    <td>
                      @if (paymentFor(request); as payment) {
                        <span [class]="badge(payment.status)">{{ label(payment.status) }}</span>
                        @if (payment.status === 'PAID') {
                          <div class="cell-muted" style="margin-top: 4px;">{{ amount(payment.providerAmount) }} net</div>
                        }
                      } @else {
                        <span class="cell-muted">No payment yet</span>
                      }
                    </td>
                    <td>
                      <div class="btn-row">
                        @if (request.status === 'PENDING') {
                          <button class="btn btn-primary btn-sm" (click)="accept(request)">Accept</button>
                          <button class="btn btn-danger btn-sm" (click)="reject(request)">Reject</button>
                        }

                        @if (request.status === 'PAID') {
                          <button class="btn btn-primary btn-sm" (click)="start(request)">Start training</button>
                        }

                        @if (request.status === 'IN_PROGRESS') {
                          <button class="btn btn-primary btn-sm" (click)="complete(request)">Mark complete</button>
                        }

                        @if (request.status === 'ACCEPTED' || request.status === 'PAYMENT_PENDING') {
                          <span class="cell-muted">Awaiting beneficiary payment</span>
                        }

                        @if (request.status === 'COMPLETED') {
                          <span class="cell-muted">Completed {{ date(request.completedAt) }}</span>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .filter-row {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }

      .filter-chip {
        padding: 7px 12px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: #fff;
        color: var(--muted);
        font-size: 11.5px;
        font-weight: 650;
        cursor: pointer;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .filter-chip.active {
        background: var(--navy);
        border-color: var(--navy);
        color: #fff;
      }
    `,
  ],
})
export class ProviderRequestsPage implements OnInit {
  readonly requests = signal<TrainingRequest[]>([]);
  readonly loading = signal(true);
  readonly statusFilter = signal('ALL');
  readonly filters = FILTERS;

  readonly filtered = computed(() => {
    const status = this.statusFilter();
    return status === 'ALL'
      ? this.requests()
      : this.requests().filter((request) => request.status === status);
  });

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  label(value: string): string {
    return titleCase(value);
  }

  badge(value: string): string {
    return statusClass(value);
  }

  amount(value?: number | null): string {
    return money(value);
  }

  date(value?: string | null): string {
    return dateTime(value);
  }

  paymentFor(request: TrainingRequest): Payment | undefined {
    return [...(request.payments ?? [])].sort((a, b) => b.id - a.id)[0];
  }

  accept(request: TrainingRequest): void {
    this.api.acceptRequest(request.id).subscribe({
      next: () => {
        this.toast.success('Request accepted');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  reject(request: TrainingRequest): void {
    const reason = window.prompt('Reason for rejecting this request (optional):', 'Not available for the requested date');
    if (reason === null) {
      return;
    }

    this.api.rejectRequest(request.id, reason).subscribe({
      next: () => {
        this.toast.success('Request rejected');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  start(request: TrainingRequest): void {
    if (!window.confirm('Start this training? This confirms the session has begun.')) {
      return;
    }

    this.api.startRequest(request.id).subscribe({
      next: () => {
        this.toast.success('Training started');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  complete(request: TrainingRequest): void {
    if (!window.confirm('Mark this training as completed?')) {
      return;
    }

    this.api.completeRequest(request.id).subscribe({
      next: () => {
        this.toast.success('Training completed');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getProviderRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiError(error));
      },
    });
  }
}
