import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { apiError, dateTime, money, statusClass, titleCase } from '../../core/format';
import { Payment } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-admin-payments',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Payments</h1>
          <p class="page-subtitle">Monitor Moyasar payments, commissions, and refunds across the platform.</p>
        </div>
      </div>

      <div class="grid grid-3">
        <div class="card stat">
          <span class="stat-label">Paid volume</span>
          <span class="stat-value">{{ amount(totals().paid) }}</span>
          <span class="stat-meta">{{ totals().paidCount }} successful payments</span>
        </div>
        <div class="card stat">
          <span class="stat-label">Platform commission</span>
          <span class="stat-value">{{ amount(totals().commission) }}</span>
          <span class="stat-meta">Recorded commission on paid payments</span>
        </div>
        <div class="card stat">
          <span class="stat-label">Refunded</span>
          <span class="stat-value">{{ amount(totals().refunded) }}</span>
          <span class="stat-meta">{{ totals().refundedCount }} refunds</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">All payments</div>
          <input class="search" type="search" placeholder="Search by request or Moyasar id…" [ngModel]="search()" (ngModelChange)="search.set($event)" />
        </div>

        @if (loading()) {
          <div class="empty">Loading payments…</div>
        } @else if (filtered().length === 0) {
          <div class="empty">
            <strong>No payments found</strong>
            Payments appear here when a beneficiary starts checkout for an accepted request.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Request</th>
                  <th>Amount</th>
                  <th>Commission</th>
                  <th>Provider</th>
                  <th>Status</th>
                  <th>Moyasar</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (payment of filtered(); track payment.id) {
                  <tr>
                    <td class="cell-strong">#{{ payment.id }}</td>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">#{{ payment.trainingRequestId }}</span>
                        <span class="cell-muted">Training request</span>
                      </div>
                    </td>
                    <td class="cell-strong">{{ amount(payment.amount) }}</td>
                    <td>{{ amount(payment.commissionAmount) }}</td>
                    <td>{{ amount(payment.providerAmount) }}</td>
                    <td><span [class]="badge(payment.status)">{{ label(payment.status) }}</span></td>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-muted">{{ payment.moyasarPaymentId || 'No payment id' }}</span>
                        @if (payment.moyasarInvoiceId) {
                          <span class="cell-muted">Invoice {{ payment.moyasarInvoiceId.slice(0, 8) }}…</span>
                        }
                      </div>
                    </td>
                    <td class="cell-muted">{{ date(payment.createdAt) }}</td>
                    <td>
                      <div class="btn-row">
                        @if (payment.status === 'PAID') {
                          <button class="btn btn-danger btn-sm" (click)="refund(payment)">Refund</button>
                        }
                        @if (payment.moyasarPaymentId && payment.status === 'PENDING') {
                          <button class="btn btn-secondary btn-sm" (click)="sync(payment)">Sync</button>
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
      .search {
        min-width: 260px;
        padding: 9px 12px;
        border-radius: 9px;
        border: 1px solid var(--line);
        font: inherit;
        font-size: 13px;
        outline: none;
      }
    `,
  ],
})
export class AdminPaymentsPage implements OnInit {
  readonly payments = signal<Payment[]>([]);
  readonly loading = signal(true);
  readonly search = signal('');

  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.payments();
    }
    return this.payments().filter((payment) =>
      [String(payment.id), String(payment.trainingRequestId), payment.moyasarPaymentId, payment.moyasarInvoiceId, payment.status]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(term)
    );
  });

  readonly totals = computed(() => {
    const payments = this.payments();
    const paid = payments.filter((payment) => payment.status === 'PAID');
    const refunded = payments.filter((payment) => payment.status === 'REFUNDED');

    return {
      paid: paid.reduce((sum, payment) => sum + Number(payment.amount), 0),
      paidCount: paid.length,
      commission: paid.reduce((sum, payment) => sum + Number(payment.commissionAmount ?? 0), 0),
      refunded: refunded.reduce((sum, payment) => sum + Number(payment.refundedAmount ?? payment.amount), 0),
      refundedCount: refunded.length,
    };
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

  refund(payment: Payment): void {
    if (!window.confirm(`Refund ${this.amount(payment.amount)} for payment #${payment.id}?`)) {
      return;
    }

    this.api.refundPayment(payment.id).subscribe({
      next: () => {
        this.toast.success('Payment refunded');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  sync(payment: Payment): void {
    this.api.syncPayment(payment.id).subscribe({
      next: (updated) => {
        this.toast.success(`Payment status: ${titleCase(updated.status)}`);
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getPayments().subscribe({
      next: (payments) => {
        this.payments.set(payments);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiError(error));
      },
    });
  }
}
