import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { dateTime, money, statusClass, titleCase } from '../../core/format';
import { Organization, Payment, TrainingProgram, TrainingRequest } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="page-subtitle">{{ subtitle() }}</p>
        </div>
        <a class="btn btn-primary" [routerLink]="primaryAction().link">{{ primaryAction().label }}</a>
      </div>

      <div class="grid grid-3">
        @for (stat of stats(); track stat.label) {
          <div class="card stat">
            <span class="stat-label">{{ stat.label }}</span>
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-meta">{{ stat.meta }}</span>
          </div>
        }
      </div>

      <div class="two-col">
        <div class="card">
          <div class="card-header">
            <div>
              <div class="card-title">Recent training requests</div>
              <div class="card-hint">The latest activity in your workspace</div>
            </div>
            <a class="btn btn-secondary btn-sm" [routerLink]="requestsLink()">View all</a>
          </div>

          @if (requests().length === 0) {
            <div class="empty">
              <strong>No training requests yet</strong>
              Requests will appear here once your organization starts booking programs.
            </div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Program</th>
                    <th>Schedule</th>
                    <th>Status</th>
                    <th>Price</th>
                  </tr>
                </thead>
                <tbody>
                  @for (request of requests().slice(0, 6); track request.id) {
                    <tr>
                      <td>
                        <div class="cell-stack">
                          <span class="cell-strong">{{ request.trainingProgramTitle }}</span>
                          <span class="cell-muted">
                            {{ isProvider() ? request.beneficiaryOrganizationName : request.providerOrganizationName }}
                          </span>
                        </div>
                      </td>
                      <td class="cell-muted">{{ dateTime(request.scheduledStart) }}</td>
                      <td><span [class]="badge(request.status)">{{ title(request.status) }}</span></td>
                      <td class="cell-strong">{{ amount(request.programPrice) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">Status overview</div>
          </div>
          <div class="card-body">
            <div class="timeline">
              @for (item of statusOverview(); track item.label) {
                <div class="timeline-item">
                  <span class="timeline-dot" [class.done]="item.count > 0"></span>
                  <div style="flex: 1;">
                    <div class="timeline-title">{{ item.label }}</div>
                    <div class="timeline-meta">{{ item.count }} request{{ item.count === 1 ? '' : 's' }}</div>
                  </div>
                  <strong>{{ item.count }}</strong>
                </div>
              }
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class DashboardPage implements OnInit {
  readonly requests = signal<TrainingRequest[]>([]);
  readonly stats = signal<{ label: string; value: string; meta: string }[]>([]);
  readonly subtitle = signal('');

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    if (this.auth.isPlatformAdmin()) {
      this.loadAdmin();
    } else if (this.auth.isProvider()) {
      this.loadProvider();
    } else {
      this.loadBeneficiary();
    }
  }

  isProvider(): boolean {
    return this.auth.isProvider();
  }

  requestsLink(): string {
    if (this.auth.isProvider()) {
      return '/provider/requests';
    }
    return '/requests';
  }

  primaryAction(): { label: string; link: string } {
    if (this.auth.isPlatformAdmin()) {
      return { label: 'Add organization', link: '/admin/organizations' };
    }
    if (this.auth.isProvider()) {
      return { label: 'Add program', link: '/provider/programs' };
    }
    return { label: 'Browse programs', link: '/catalog' };
  }

  statusOverview(): { label: string; count: number }[] {
    const all = this.requests();
    return [
      { label: 'Pending approval', count: all.filter((r) => r.status === 'PENDING').length },
      { label: 'Accepted / awaiting payment', count: all.filter((r) => r.status === 'ACCEPTED' || r.status === 'PAYMENT_PENDING').length },
      { label: 'Paid', count: all.filter((r) => r.status === 'PAID').length },
      { label: 'In progress', count: all.filter((r) => r.status === 'IN_PROGRESS').length },
      { label: 'Completed', count: all.filter((r) => r.status === 'COMPLETED').length },
    ];
  }

  badge(status: string): string {
    return statusClass(status);
  }

  title(status: string): string {
    return titleCase(status);
  }

  amount(value: number): string {
    return money(value);
  }

  dateTime(value?: string | null): string {
    return dateTime(value);
  }

  private loadBeneficiary(): void {
    this.subtitle.set('Track your organization’s training requests and upcoming sessions.');
    const organizationId = this.auth.user()?.organizationId;
    if (!organizationId) {
      return;
    }

    this.api.getRequestsByOrganization(organizationId).subscribe({
      next: (requests) => {
        this.requests.set(requests);
        const spent = requests
          .filter((r) => r.status === 'PAID' || r.status === 'IN_PROGRESS' || r.status === 'COMPLETED')
          .reduce((sum, r) => sum + Number(r.programPrice), 0);

        this.stats.set([
          { label: 'Total requests', value: String(requests.length), meta: 'All time' },
          { label: 'Awaiting action', value: String(requests.filter((r) => r.status === 'PENDING' || r.status === 'ACCEPTED').length), meta: 'Pending or ready to pay' },
          { label: 'Paid value', value: money(spent), meta: 'Approved and paid training' },
        ]);
      },
      error: (error) => this.toast.error(error.error?.message ?? 'Could not load requests'),
    });
  }

  private loadProvider(): void {
    this.subtitle.set('Review incoming requests and keep your training programs up to date.');
    const organizationId = this.auth.user()?.organizationId;

    this.api.getProviderRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        const paid = requests.filter((r) => ['PAID', 'IN_PROGRESS', 'COMPLETED'].includes(r.status));
        const revenue = paid.reduce((sum, r) => {
          const payment = r.payments?.find((p) => p.status === 'PAID');
          return sum + Number(payment?.providerAmount ?? 0);
        }, 0);

        this.stats.set([
          { label: 'Pending approval', value: String(requests.filter((r) => r.status === 'PENDING').length), meta: 'Waiting for your decision' },
          { label: 'Active deliveries', value: String(requests.filter((r) => r.status === 'PAID' || r.status === 'IN_PROGRESS').length), meta: 'Paid or in progress' },
          { label: 'Provider revenue', value: money(revenue), meta: 'Net of platform commission' },
        ]);
      },
      error: (error) => this.toast.error(error.error?.message ?? 'Could not load requests'),
    });

    if (organizationId) {
      this.api.getProgramsByOrganization(organizationId).subscribe({
        next: (programs: TrainingProgram[]) => {
          this.stats.update((stats) => [
            ...stats,
            { label: 'Programs', value: String(programs.length), meta: `${programs.filter((p) => p.isActive).length} active` },
          ]);
        },
      });
    }
  }

  private loadAdmin(): void {
    this.subtitle.set('Platform-wide activity across organizations, training requests, and payments.');

    this.api.getRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
      },
      error: (error) => this.toast.error(error.error?.message ?? 'Could not load requests'),
    });

    this.api.getPayments().subscribe({
      next: (payments: Payment[]) => {
        const paid = payments.filter((p) => p.status === 'PAID');
        const gmv = paid.reduce((sum, p) => sum + Number(p.amount), 0);
        const commission = paid.reduce((sum, p) => sum + Number(p.commissionAmount ?? 0), 0);
        const refunded = payments.filter((p) => p.status === 'REFUNDED').length;

        this.stats.set([
          { label: 'Paid volume', value: money(gmv), meta: `${paid.length} successful payments` },
          { label: 'Platform commission', value: money(commission), meta: 'Commission recorded on payments' },
          { label: 'Refunds', value: String(refunded), meta: 'Refunded payments' },
        ]);
      },
      error: (error) => this.toast.error(error.error?.message ?? 'Could not load payments'),
    });

    this.api.getOrganizations().subscribe({
      next: (organizations: Organization[]) => {
        this.stats.update((stats) => [
          ...stats,
          { label: 'Organizations', value: String(organizations.length), meta: `${organizations.filter((o) => o.type === 'PROVIDER' || o.type === 'BOTH').length} providers` },
        ]);
      },
    });
  }
}
