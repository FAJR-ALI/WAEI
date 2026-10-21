import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { apiError, money, statusClass, titleCase } from '../../core/format';
import { Payment } from '../../core/models';

@Component({
  selector: 'app-payment-callback',
  imports: [RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Payment result</h1>
          <p class="page-subtitle">We are confirming the payment with Moyasar and updating your training request.</p>
        </div>
      </div>

      <div class="card" style="max-width: 680px;">
        <div class="card-body" style="display: flex; flex-direction: column; gap: 16px;">
          @if (loading()) {
            <div class="empty" style="padding: 30px 0;">Confirming payment…</div>
          } @else if (payment(); as result) {
            <div class="notice">
              Your payment was processed. The final status is shown below. If it is not paid yet, use “Refresh” on your requests page.
            </div>
            <div class="grid grid-2">
              <div class="field">
                <label>Status</label>
                <div><span [class]="badge(result.status)">{{ label(result.status) }}</span></div>
              </div>
              <div class="field">
                <label>Amount</label>
                <div class="price">{{ amount(result.amount) }}</div>
              </div>
              <div class="field">
                <label>Payment reference</label>
                <div class="cell-muted">{{ result.moyasarPaymentId || 'Pending confirmation' }}</div>
              </div>
              <div class="field">
                <label>Training request</label>
                <div class="cell-strong">#{{ result.trainingRequestId }}</div>
              </div>
            </div>
          } @else if (errorMessage()) {
            <div class="notice notice-danger">{{ errorMessage() }}</div>
            <div class="notice notice-warn">
              The payment may still be processing. Open your requests and use “Refresh” on the pending payment.
            </div>
          } @else {
            <div class="empty">No payment reference was found in this link.</div>
          }

          <div class="btn-row">
            <a class="btn btn-primary" routerLink="/requests">Back to my requests</a>
            <a class="btn btn-secondary" routerLink="/catalog">Browse programs</a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class PaymentCallbackPage implements OnInit {
  readonly loading = signal(true);
  readonly payment = signal<Payment | null>(null);
  readonly errorMessage = signal('');

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    const invoiceId =
      query.get('id') ??
      query.get('invoice_id') ??
      sessionStorage.getItem('waei.pendingInvoiceId');

    if (!invoiceId) {
      this.loading.set(false);
      return;
    }

    this.api.syncByInvoice(invoiceId).subscribe({
      next: (payment) => {
        sessionStorage.removeItem('waei.pendingInvoiceId');
        this.payment.set(payment);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiError(error));
        this.loading.set(false);
      },
    });
  }

  badge(value: string): string {
    return statusClass(value);
  }

  label(value: string): string {
    return titleCase(value);
  }

  amount(value: number): string {
    return money(value);
  }
}
