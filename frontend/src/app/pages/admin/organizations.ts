import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { apiError, dateOnly, statusClass, titleCase } from '../../core/format';
import { Organization } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-admin-organizations',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Organizations</h1>
          <p class="page-subtitle">Register beneficiary organizations and training providers on the platform.</p>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">Add organization</button>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">All organizations</div>
          <input class="search" type="search" placeholder="Search organizations…" [ngModel]="search()" (ngModelChange)="search.set($event)" />
        </div>

        @if (loading()) {
          <div class="empty">Loading organizations…</div>
        } @else if (filtered().length === 0) {
          <div class="empty">
            <strong>No organizations found</strong>
            Add the first provider or beneficiary organization to get started.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Organization</th>
                  <th>Type</th>
                  <th>Sector</th>
                  <th>Contact</th>
                  <th>Verification</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (organization of filtered(); track organization.id) {
                  <tr>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">{{ organization.name }}</span>
                        <span class="cell-muted">{{ organization.email }}</span>
                      </div>
                    </td>
                    <td><span class="badge" [class]="typeClass(organization.type)">{{ organization.type }}</span></td>
                    <td>{{ organization.sector }}</td>
                    <td>
                      <div class="cell-stack">
                        <span>{{ organization.phoneNumber }}</span>
                        @if (organization.documentReference) {
                          <span class="cell-muted">{{ organization.documentReference }}</span>
                        }
                      </div>
                    </td>
                    <td><span [class]="badge(organization.verificationStatus)">{{ label(organization.verificationStatus) }}</span></td>
                    <td class="cell-muted">{{ date(organization.createdAt) }}</td>
                    <td>
                      <div class="btn-row">
                        <button class="btn btn-secondary btn-sm" (click)="openEdit(organization)">Edit</button>
                        <button class="btn btn-danger btn-sm" (click)="remove(organization)">Delete</button>
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
              <div class="card-title">{{ editing() ? 'Edit organization' : 'New organization' }}</div>
              <div class="card-hint">Organizations own programs, accounts, and training requests.</div>
            </div>
            <button class="icon-btn" (click)="closeForm()">×</button>
          </div>

          <form (ngSubmit)="save()">
            <div class="modal-body">
              <div class="form-grid">
                <div class="field span-2">
                  <label for="name">Organization name</label>
                  <input id="name" name="name" [(ngModel)]="form.name" required />
                </div>

                <div class="field">
                  <label for="type">Type</label>
                  <select id="type" name="type" [(ngModel)]="form.type">
                    <option value="PROVIDER">Training provider</option>
                    <option value="BENEFICIARY">Beneficiary</option>
                    <option value="BOTH">Both</option>
                  </select>
                </div>

                <div class="field">
                  <label for="sector">Sector</label>
                  <input id="sector" name="sector" [(ngModel)]="form.sector" required />
                </div>

                <div class="field">
                  <label for="email">Email</label>
                  <input id="email" name="email" type="email" [(ngModel)]="form.email" required />
                </div>

                <div class="field">
                  <label for="phone">Phone number</label>
                  <input id="phone" name="phone" [(ngModel)]="form.phoneNumber" pattern="^05[0-9]{8}$" placeholder="05xxxxxxxx" required />
                  <span class="field-hint">Saudi mobile number, 10 digits.</span>
                </div>

                <div class="field span-2">
                  <label for="document">Document reference</label>
                  <input id="document" name="document" [(ngModel)]="form.documentReference" placeholder="Commercial registration or accreditation reference" />
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeForm()">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving()">
                {{ saving() ? 'Saving…' : 'Save organization' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .search {
        min-width: 240px;
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
export class AdminOrganizationsPage implements OnInit {
  readonly organizations = signal<Organization[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<Organization | null>(null);
  readonly search = signal('');

  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.organizations();
    }
    return this.organizations().filter((organization) =>
      [organization.name, organization.email, organization.sector, organization.type]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(term)
    );
  });

  form = this.blankForm();

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

  date(value?: string | null): string {
    return dateOnly(value);
  }

  typeClass(type: string): string {
    return type === 'PROVIDER' ? 'badge badge-accepted' : type === 'BENEFICIARY' ? 'badge badge-in_progress' : 'badge badge-paid';
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = this.blankForm();
    this.showForm.set(true);
  }

  openEdit(organization: Organization): void {
    this.editing.set(organization);
    this.form = {
      name: organization.name,
      type: organization.type,
      sector: organization.sector,
      email: organization.email,
      phoneNumber: organization.phoneNumber,
      documentReference: organization.documentReference ?? '',
    };
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  save(): void {
    if (!this.form.name || !this.form.sector || !this.form.email || !this.form.phoneNumber) {
      this.toast.error('Name, sector, email and phone are required');
      return;
    }

    if (!/^05\d{8}$/.test(this.form.phoneNumber)) {
      this.toast.error('Phone number must be a valid Saudi number starting with 05');
      return;
    }

    this.saving.set(true);
    const organization = this.editing();
    const request = organization
      ? this.api.updateOrganization(organization.id, this.form)
      : this.api.addOrganization(this.form);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(organization ? 'Organization updated' : 'Organization created');
        this.showForm.set(false);
        this.load();
      },
      error: (error) => {
        this.saving.set(false);
        this.toast.error(apiError(error));
      },
    });
  }

  remove(organization: Organization): void {
    if (!window.confirm(`Delete "${organization.name}"? This also removes its accounts and programs if possible.`)) {
      return;
    }

    this.api.deleteOrganization(organization.id).subscribe({
      next: () => {
        this.toast.success('Organization deleted');
        this.load();
      },
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getOrganizations().subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
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
      name: '',
      type: 'PROVIDER',
      sector: '',
      email: '',
      phoneNumber: '',
      documentReference: '',
    };
  }
}
