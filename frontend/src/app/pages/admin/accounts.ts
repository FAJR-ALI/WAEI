import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { apiError, dateOnly, statusClass, titleCase } from '../../core/format';
import { Account, Organization } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-admin-accounts',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Accounts</h1>
          <p class="page-subtitle">Create organization administrators and activate their access to the platform.</p>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">Add account</button>
      </div>

      <div class="card">
        @if (loading()) {
          <div class="empty">Loading accounts…</div>
        } @else if (accounts().length === 0) {
          <div class="empty">
            <strong>No accounts yet</strong>
            Add an organization administrator after registering its organization.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Account</th>
                  <th>Role</th>
                  <th>Organization</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (account of accounts(); track account.id) {
                  <tr>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">{{ account.fullName }}</span>
                        <span class="cell-muted">{{ account.email }}</span>
                      </div>
                    </td>
                    <td>{{ label(account.role) }}</td>
                    <td>
                      @if (account.organizationName) {
                        <div class="cell-stack">
                          <span>{{ account.organizationName }}</span>
                          <span class="cell-muted">{{ account.organizationType }}</span>
                        </div>
                      } @else {
                        <span class="cell-muted">Platform</span>
                      }
                    </td>
                    <td><span [class]="badge(account.status)">{{ label(account.status) }}</span></td>
                    <td class="cell-muted">{{ date(account.createdAt) }}</td>
                    <td>
                      <div class="btn-row">
                        @if (account.status !== 'ACTIVE') {
                          <button class="btn btn-primary btn-sm" (click)="changeStatus(account, 'ACTIVE')">Activate</button>
                        }
                        @if (account.status === 'ACTIVE') {
                          <button class="btn btn-secondary btn-sm" (click)="changeStatus(account, 'SUSPENDED')">Suspend</button>
                        }
                        <button class="btn btn-danger btn-sm" (click)="remove(account)">Delete</button>
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

    @if (showForm()) {
      <div class="modal-backdrop" (click)="closeForm()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div>
              <div class="card-title">New account</div>
              <div class="card-hint">Organization administrators can sign in once the account is activated.</div>
            </div>
            <button class="icon-btn" (click)="closeForm()">×</button>
          </div>

          <form (ngSubmit)="save()">
            <div class="modal-body">
              <div class="form-grid">
                <div class="field">
                  <label for="fullName">Full name</label>
                  <input id="fullName" name="fullName" [(ngModel)]="form.fullName" required />
                </div>

                <div class="field">
                  <label for="email">Email</label>
                  <input id="email" name="email" type="email" [(ngModel)]="form.email" required />
                </div>

                <div class="field">
                  <label for="password">Temporary password</label>
                  <input id="password" name="password" type="password" [(ngModel)]="form.password" minlength="8" required />
                  <span class="field-hint">At least 8 characters.</span>
                </div>

                <div class="field">
                  <label for="role">Role</label>
                  <select id="role" name="role" [(ngModel)]="form.role">
                    <option value="ORG_ADMIN">Organization admin</option>
                    <option value="PLATFORM_ADMIN">Platform admin</option>
                  </select>
                </div>

                @if (form.role === 'ORG_ADMIN') {
                  <div class="field span-2">
                    <label for="organization">Organization</label>
                    <select id="organization" name="organization" [(ngModel)]="form.organizationId" required>
                      <option [ngValue]="null" disabled>Select an organization</option>
                      @for (organization of organizations(); track organization.id) {
                        <option [ngValue]="organization.id">{{ organization.name }} · {{ organization.type }}</option>
                      }
                    </select>
                  </div>
                }
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeForm()">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving()">
                {{ saving() ? 'Creating…' : 'Create account' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class AdminAccountsPage implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly organizations = signal<Organization[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly saving = signal(false);

  form = this.blankForm();

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
    this.api.getOrganizations().subscribe({
      next: (organizations) => this.organizations.set(organizations),
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  label(value: string): string {
    return titleCase(value);
  }

  badge(value: string): string {
    return statusClass(value);
  }

  date(value?: string | null): string {
    return dateOnly(value);
  }

  openCreate(): void {
    this.form = this.blankForm();
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  save(): void {
    if (!this.form.fullName || !this.form.email || !this.form.password) {
      this.toast.error('Full name, email and password are required');
      return;
    }

    if (this.form.role === 'ORG_ADMIN' && !this.form.organizationId) {
      this.toast.error('Choose the organization for this administrator');
      return;
    }

    this.saving.set(true);
    this.api.addAccount(this.form).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Account created. Activate it to allow sign-in.');
        this.showForm.set(false);
        this.load();
      },
      error: (error) => {
        this.saving.set(false);
        this.toast.error(apiError(error));
      },
    });
  }

  changeStatus(account: Account, status: string): void {
    this.api.changeAccountStatus(account.id, status).subscribe({
      next: () => {
        this.toast.success(`Account ${status.toLowerCase()}`);
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  remove(account: Account): void {
    if (!window.confirm(`Delete the account for ${account.fullName}?`)) {
      return;
    }

    this.api.deleteAccount(account.id).subscribe({
      next: () => {
        this.toast.success('Account deleted');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiError(error));
      },
    });
  }

  private blankForm() {
    return {
      fullName: '',
      email: '',
      password: '',
      role: 'ORG_ADMIN',
      organizationId: null as number | null,
    };
  }
}
