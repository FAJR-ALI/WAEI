import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { apiError, dateTime, money, statusClass, titleCase } from '../../core/format';
import { Payment, TrainingRequest } from '../../core/models';
import { ToastService } from '../../core/toast.service';

const FILTERS = ['ALL', 'PENDING', 'ACCEPTED', 'PAYMENT_PENDING', 'PAID', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'];

@Component({
  selector: 'app-beneficiary-requests',
  imports: [RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">My requests</h1>
          <p class="page-subtitle">Follow each request from provider approval through payment and delivery.</p>
        </div>
        <a class="btn btn-primary" routerLink="/catalog">New request</a>
      </div>

      <div class="filter-row">
        @for (filter of filters; track filter) {
          <button
            class="filter-chip"
            [class.active]="statusFilter() === filter"
            (click)="statusFilter.set(filter)"
          >
            {{ label(filter) }}
          </button>
        }
      </div>

      <div class="card">
        @if (loading()) {
          <div class="empty">Loading your requests…</div>
        } @else if (filtered().length === 0) {
          <div class="empty">
            <strong>No requests in this view</strong>
            Browse the program catalog to submit your first training request.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Program</th>
                  <th>Schedule</th>
                  <th>Participants</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Price</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (request of filtered(); track request.id) {
                  <tr>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">{{ request.trainingProgramTitle }}</span>
                        <span class="cell-muted">{{ request.providerOrganizationName }}</span>
                        @if (request.notes) {
                          <span class="cell-muted">{{ request.notes }}</span>
                        }
                        @if (request.cancellationReason) {
                          <span class="cell-muted">{{ request.cancellationReason }}</span>
                        }
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
                        <div class="cell-stack">
                          <span [class]="badge(payment.status)">{{ label(payment.status) }}</span>
                          @if (payment.status === 'REFUNDED') {
                            <span class="cell-muted">{{ amount(payment.refundedAmount) }} refunded</span>
                          }
                        </div>
                      } @else {
                        <span class="cell-muted">No payment yet</span>
                      }
                    </td>
                    <td class="cell-strong">{{ amount(request.programPrice) }}</td>
                    <td>
                      <div class="btn-row">
                        @if (request.status === 'ACCEPTED') {
                          <button class="btn btn-primary btn-sm" [disabled]="busy() === request.id" (click)="pay(request)">
                            Pay now
                          </button>
                          <button class="btn btn-danger btn-sm" (click)="cancel(request)">Cancel</button>
                        }

                        @if (request.status === 'PAYMENT_PENDING') {
                          @if (paymentFor(request)?.checkoutUrl) {
                            <button class="btn btn-primary btn-sm" (click)="resumePayment(request)">Complete payment</button>
                          }
                          <button class="btn btn-secondary btn-sm" (click)="refresh(request)">Refresh</button>
                          <button class="btn btn-danger btn-sm" (click)="cancel(request)">Cancel</button>
                        }

                        @if (request.status === 'PAID') {
                          <button class="btn btn-danger btn-sm" (click)="cancel(request)">Cancel &amp; refund</button>
                        }

                        @if (request.status === 'PENDING') {
                          <button class="btn btn-danger btn-sm" (click)="cancel(request)">Cancel</button>
                          <button class="btn btn-ghost btn-sm" (click)="remove(request)">Delete</button>
                        }

                        @if (request.status === 'REJECTED') {
                          <button class="btn btn-ghost btn-sm" (click)="remove(request)">Delete</button>
                        }

                        @if (request.status === 'COMPLETED') {
                          <span class="cell-muted">Training delivered</span>
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
export class BeneficiaryRequestsPage implements OnInit {
  readonly requests = signal<TrainingRequest[]>([]);
  readonly loading = signal(true);
  readonly busy = signal<number | null>(null);
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
    private readonly auth: AuthService,
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

  pay(request: TrainingRequest): void {
    this.busy.set(request.id);
    this.api.addPayment(request.id).subscribe({
      next: (payment) => {
        this.busy.set(null);
        if (payment.moyasarInvoiceId) {
          sessionStorage.setItem('waei.pendingInvoiceId', payment.moyasarInvoiceId);
        }
        if (payment.checkoutUrl) {
          window.location.href = payment.checkoutUrl;
        } else {
          this.toast.error('Moyasar did not return a checkout URL');
        }
      },
      error: (error) => {
        this.busy.set(null);
        this.toast.error(apiError(error));
      },
    });
  }

  resumePayment(request: TrainingRequest): void {
    const payment = this.paymentFor(request);
    if (!payment?.checkoutUrl) {
      this.toast.error('No checkout link is available for this payment');
      return;
    }
    if (payment.moyasarInvoiceId) {
      sessionStorage.setItem('waei.pendingInvoiceId', payment.moyasarInvoiceId);
    }
    window.location.href = payment.checkoutUrl;
  }

  refresh(request: TrainingRequest): void {
    const payment = this.paymentFor(request);
    if (!payment?.moyasarInvoiceId) {
      this.toast.error('This payment has no Moyasar invoice to sync');
      return;
    }

    this.busy.set(request.id);
    this.api.syncByInvoice(payment.moyasarInvoiceId).subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.toast.success(`Payment status: ${titleCase(updated.status)}`);
        this.load();
      },
      error: (error) => {
        this.busy.set(null);
        this.toast.error(apiError(error));
      },
    });
  }

  cancel(request: TrainingRequest): void {
    const message = request.status === 'PAID'
      ? 'Cancel this request and refund the payment? This cannot be undone.'
      : 'Cancel this training request?';

    if (!window.confirm(message)) {
      return;
    }

    this.api.cancelRequest(request.id, 'Cancelled by the beneficiary').subscribe({
      next: () => {
        this.toast.success('Training request cancelled');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  remove(request: TrainingRequest): void {
    if (!window.confirm('Delete this training request permanently?')) {
      return;
    }

    this.api.deleteRequest(request.id).subscribe({
      next: () => {
        this.toast.success('Training request deleted');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  private load(): void {
    const organizationId = this.auth.user()?.organizationId;
    if (!organizationId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.api.getRequestsByOrganization(organizationId).subscribe({
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
