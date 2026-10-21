import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { apiError, money } from '../../core/format';
import { TrainingProgram } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-provider-programs',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">My programs</h1>
          <p class="page-subtitle">Publish the training programs that beneficiary organizations can request.</p>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">Add program</button>
      </div>

      <div class="card">
        @if (loading()) {
          <div class="empty">Loading programs…</div>
        } @else if (programs().length === 0) {
          <div class="empty">
            <strong>No programs yet</strong>
            Create your first program so beneficiary organizations can start requesting it.
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Program</th>
                  <th>Category</th>
                  <th>Delivery</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (program of programs(); track program.id) {
                  <tr>
                    <td>
                      <div class="cell-stack">
                        <span class="cell-strong">{{ program.title }}</span>
                        <span class="cell-muted">{{ program.description }}</span>
                      </div>
                    </td>
                    <td>{{ program.category }}</td>
                    <td>{{ delivery(program.deliveryMode) }}</td>
                    <td class="cell-strong">{{ amount(program.price) }}</td>
                    <td>
                      <span [class]="program.isActive ? 'badge badge-active' : 'badge badge-cancelled'">
                        {{ program.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                    <td>
                      <div class="btn-row">
                        <button class="btn btn-secondary btn-sm" (click)="openEdit(program)">Edit</button>
                        <button class="btn btn-secondary btn-sm" (click)="toggle(program)">
                          {{ program.isActive ? 'Deactivate' : 'Activate' }}
                        </button>
                        <button class="btn btn-danger btn-sm" (click)="remove(program)">Delete</button>
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
              <div class="card-title">{{ editing() ? 'Edit program' : 'New program' }}</div>
              <div class="card-hint">Programs are published under your provider organization.</div>
            </div>
            <button class="icon-btn" (click)="closeForm()">×</button>
          </div>

          <form (ngSubmit)="save()">
            <div class="modal-body">
              <div class="form-grid">
                <div class="field span-2">
                  <label for="title">Title</label>
                  <input id="title" name="title" [(ngModel)]="form.title" required maxlength="150" />
                </div>

                <div class="field">
                  <label for="category">Category</label>
                  <input id="category" name="category" [(ngModel)]="form.category" required maxlength="100" placeholder="Leadership, Safety, Technology…" />
                </div>

                <div class="field">
                  <label for="price">Price (SAR)</label>
                  <input id="price" name="price" type="number" min="1" step="0.01" [(ngModel)]="form.price" required />
                </div>

                <div class="field span-2">
                  <label for="description">Description</label>
                  <textarea id="description" name="description" [(ngModel)]="form.description" required maxlength="5000"></textarea>
                </div>

                <div class="field">
                  <label for="deliveryMode">Delivery mode</label>
                  <select id="deliveryMode" name="deliveryMode" [(ngModel)]="form.deliveryMode">
                    <option value="IN_PERSON">In person</option>
                    <option value="ONLINE">Online</option>
                    <option value="HYBRID">Hybrid</option>
                  </select>
                </div>

                <div class="field">
                  <label for="active">Availability</label>
                  <select id="active" name="active" [(ngModel)]="form.isActive">
                    <option [ngValue]="true">Active</option>
                    <option [ngValue]="false">Inactive</option>
                  </select>
                  <span class="field-hint">Only active programs appear in the beneficiary catalog.</span>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeForm()">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving()">
                {{ saving() ? 'Saving…' : 'Save program' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class ProviderProgramsPage implements OnInit {
  readonly programs = signal<TrainingProgram[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<TrainingProgram | null>(null);

  form = this.blankForm();

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  amount(value: number): string {
    return money(value);
  }

  delivery(value: string): string {
    return value.replace('_', ' ');
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = this.blankForm();
    this.showForm.set(true);
  }

  openEdit(program: TrainingProgram): void {
    this.editing.set(program);
    this.form = {
      title: program.title,
      category: program.category,
      description: program.description,
      price: Number(program.price),
      deliveryMode: program.deliveryMode,
      isActive: program.isActive,
    };
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  save(): void {
    if (!this.form.title || !this.form.category || !this.form.description) {
      this.toast.error('Title, category and description are required');
      return;
    }

    this.saving.set(true);
    const program = this.editing();
    const request = program
      ? this.api.updateProgram(program.id, this.form)
      : this.api.addProgram(this.form);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(program ? 'Program updated' : 'Program created');
        this.showForm.set(false);
        this.load();
      },
      error: (error) => {
        this.saving.set(false);
        this.toast.error(apiError(error));
      },
    });
  }

  toggle(program: TrainingProgram): void {
    this.api.toggleProgram(program.id).subscribe({
      next: () => this.load(),
      error: (error) => this.toast.error(apiError(error)),
    });
  }

  remove(program: TrainingProgram): void {
    if (!window.confirm(`Delete "${program.title}"? Programs with training requests cannot be deleted.`)) {
      return;
    }

    this.api.deleteProgram(program.id).subscribe({
      next: () => {
        this.toast.success('Program deleted');
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
    this.api.getProgramsByOrganization(organizationId).subscribe({
      next: (programs) => {
        this.programs.set(programs);
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
      title: '',
      category: '',
      description: '',
      price: 0,
      deliveryMode: 'IN_PERSON',
      isActive: true,
    };
  }
}
