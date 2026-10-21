import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { apiError, money, nowLocalInput, toLocalDateTime } from '../../core/format';
import { TrainingProgram } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-catalog',
  imports: [FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Program catalog</h1>
          <p class="page-subtitle">
            Browse active programs from training providers. Prices are fixed per program, and you choose the date and time for delivery.
          </p>
        </div>
        <input
          class="catalog-search"
          type="search"
          placeholder="Search by title, category or provider…"
          [ngModel]="search()"
          (ngModelChange)="search.set($event)"
        />
      </div>

      @if (loading()) {
        <div class="card empty">Loading programs…</div>
      } @else if (programs().length === 0) {
        <div class="card empty">
          <strong>No active programs yet</strong>
          Providers have not published any active training programs.
        </div>
      } @else if (filtered().length === 0) {
        <div class="card empty">
          <strong>No matching programs</strong>
          Try a different search term.
        </div>
      } @else {
        <div class="program-grid">
          @for (program of filtered(); track program.id) {
            <article class="program-card">
              <div class="program-head">
                <div>
                  <div class="program-title">{{ program.title }}</div>
                  <div class="program-provider">{{ program.organizationName }} · {{ program.category }}</div>
                </div>
                <span class="badge badge-active">{{ delivery(program.deliveryMode) }}</span>
              </div>

              <p class="program-desc">{{ program.description }}</p>

              <div class="program-meta">
                <div class="price">
                  {{ amount(program.price) }} <small>fixed price</small>
                </div>
                <button class="btn btn-primary btn-sm" (click)="openRequest(program)">Request training</button>
              </div>
            </article>
          }
        </div>
      }
    </div>

    @if (selected(); as program) {
      <div class="modal-backdrop" (click)="closeRequest()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div>
              <div class="card-title">Request {{ program.title }}</div>
              <div class="card-hint">{{ program.organizationName }} · {{ amount(program.price) }}</div>
            </div>
            <button class="icon-btn" (click)="closeRequest()">×</button>
          </div>

          <form (ngSubmit)="submit()">
            <div class="modal-body">
              <div class="form-grid">
                <div class="field">
                  <label for="participants">Participants</label>
                  <input id="participants" type="number" min="1" name="participants" [(ngModel)]="participantCount" required />
                </div>

                <div class="field">
                  <label>Price</label>
                  <input [value]="amount(program.price)" disabled />
                  <span class="field-hint">The program price is fixed and confirmed by the provider.</span>
                </div>

                <div class="field">
                  <label for="start">Training start</label>
                  <input id="start" type="datetime-local" name="start" [min]="minDate" [(ngModel)]="scheduledStart" required />
                </div>

                <div class="field">
                  <label for="end">Training end</label>
                  <input id="end" type="datetime-local" name="end" [min]="scheduledStart || minDate" [(ngModel)]="scheduledEnd" required />
                </div>

                <div class="field span-2">
                  <label for="notes">Notes for the provider</label>
                  <textarea id="notes" name="notes" [(ngModel)]="notes" placeholder="Audience, location requirements, special arrangements…"></textarea>
                </div>
              </div>

              <div class="notice" style="margin-top: 16px;">
                The date and time are checked for conflicts. A slot is held when you submit the request, and the provider must accept it before payment.
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeRequest()">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving()">
                {{ saving() ? 'Submitting…' : 'Submit request' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .catalog-search {
        min-width: 280px;
        padding: 11px 14px;
        border-radius: 10px;
        border: 1px solid var(--line);
        background: #fff;
        font: inherit;
        font-size: 13px;
        outline: none;
      }

      .catalog-search:focus {
        border-color: var(--primary);
        box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.14);
      }

      @media (max-width: 640px) {
        .catalog-search {
          min-width: 0;
          width: 100%;
        }
      }
    `,
  ],
})
export class CatalogPage implements OnInit {
  readonly programs = signal<TrainingProgram[]>([]);
  readonly search = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly selected = signal<TrainingProgram | null>(null);

  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.programs();
    }
    return this.programs().filter((program) =>
      [program.title, program.category, program.organizationName, program.description]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(term)
    );
  });

  readonly minDate = nowLocalInput();

  participantCount = 1;
  scheduledStart = '';
  scheduledEnd = '';
  notes = '';

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.api.getActivePrograms().subscribe({
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

  amount(value: number): string {
    return money(value);
  }

  delivery(value: string): string {
    return value.replace('_', ' ');
  }

  openRequest(program: TrainingProgram): void {
    this.selected.set(program);
    this.participantCount = 1;
    this.scheduledStart = '';
    this.scheduledEnd = '';
    this.notes = '';
  }

  closeRequest(): void {
    this.selected.set(null);
  }

  submit(): void {
    const program = this.selected();
    if (!program) {
      return;
    }

    if (!this.scheduledStart || !this.scheduledEnd) {
      this.toast.error('Choose the training start and end date/time');
      return;
    }

    if (new Date(this.scheduledEnd) <= new Date(this.scheduledStart)) {
      this.toast.error('The training end time must be after the start time');
      return;
    }

    this.saving.set(true);

    this.api
      .addRequest({
        trainingProgramId: program.id,
        participantCount: this.participantCount,
        scheduledStart: toLocalDateTime(this.scheduledStart),
        scheduledEnd: toLocalDateTime(this.scheduledEnd),
        notes: this.notes,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.toast.success('Training request submitted');
          this.router.navigateByUrl('/requests');
        },
        error: (error) => {
          this.saving.set(false);
          this.toast.error(apiError(error));
        },
      });
  }
}
