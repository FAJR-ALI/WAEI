import { Component, computed, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar" [class.open]="menuOpen()">
        <div class="brand">
          <div class="brand-mark">W</div>
          <div>
            <div class="brand-name">WAEI</div>
            <div class="brand-sub">Training Platform</div>
          </div>
        </div>

        <nav class="nav-section">
          <div class="nav-label">Workspace</div>
          <a class="nav-link" routerLink="/dashboard" routerLinkActive="active" (click)="closeMenu()">
            <span class="nav-icon">◈</span> Dashboard
          </a>

          @if (isBeneficiary()) {
            <a class="nav-link" routerLink="/catalog" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">▤</span> Program Catalog
            </a>
            <a class="nav-link" routerLink="/requests" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">◎</span> My Requests
            </a>
          }

          @if (isProvider()) {
            <a class="nav-link" routerLink="/provider/programs" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">▤</span> My Programs
            </a>
            <a class="nav-link" routerLink="/provider/requests" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">◎</span> Incoming Requests
            </a>
          }

          @if (isPlatformAdmin()) {
            <div class="nav-label" style="margin-top: 18px;">Administration</div>
            <a class="nav-link" routerLink="/admin/organizations" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">⌂</span> Organizations
            </a>
            <a class="nav-link" routerLink="/admin/accounts" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">◉</span> Accounts
            </a>
            <a class="nav-link" routerLink="/admin/payments" routerLinkActive="active" (click)="closeMenu()">
              <span class="nav-icon">▦</span> Payments
            </a>
          }
        </nav>

        <div class="sidebar-footer">
          <div class="sidebar-user">
            <div class="avatar">{{ initials() }}</div>
            <div>
              <div class="sidebar-user-name">{{ user()?.fullName }}</div>
              <div class="sidebar-user-meta">{{ roleLabel() }}</div>
            </div>
          </div>
          <button class="btn btn-ghost btn-sm" style="justify-content: flex-start;" (click)="logout()">
            ↪ Sign out
          </button>
        </div>
      </aside>

      <div class="main">
        <header class="topbar">
          <div class="btn-row">
            <button class="icon-btn menu-toggle" (click)="toggleMenu()">☰</button>
            <span class="topbar-title">Training marketplace</span>
          </div>
          <div class="topbar-actions">
            @if (user()?.organizationName) {
              <span class="org-chip"><span class="org-dot"></span>{{ user()?.organizationName }}</span>
            }
            <span class="org-chip">{{ roleLabel() }}</span>
          </div>
        </header>

        <router-outlet />

        <div class="toast-stack">
          @for (toast of toasts(); track toast.id) {
            <div class="toast" [class]="'toast ' + toast.type">
              <span>{{ toast.message }}</span>
              <button (click)="dismiss(toast.id)">×</button>
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class Shell {
  readonly menuOpen = signal(false);
  readonly user = computed(() => this.auth.user());
  readonly toasts = computed(() => this.toastService.toasts());

  constructor(
    private readonly auth: AuthService,
    private readonly toastService: ToastService
  ) {}

  isPlatformAdmin(): boolean {
    return this.auth.isPlatformAdmin();
  }

  isProvider(): boolean {
    return this.auth.isProvider();
  }

  isBeneficiary(): boolean {
    return this.auth.isBeneficiary();
  }

  initials(): string {
    const name = this.user()?.fullName ?? 'U';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  roleLabel(): string {
    if (this.auth.isPlatformAdmin()) {
      return 'Platform Admin';
    }
    if (this.auth.isProvider()) {
      return 'Training Provider';
    }
    return 'Beneficiary';
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  logout(): void {
    this.auth.logout();
  }
}
