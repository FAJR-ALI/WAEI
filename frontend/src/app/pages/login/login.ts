import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <section class="login-hero">
        <div>
          <div class="brand" style="padding: 0;">
            <div class="brand-mark">W</div>
            <div>
              <div class="brand-name">WAEI</div>
              <div class="brand-sub">Training Platform</div>
            </div>
          </div>

          <h1 style="margin-top: 70px;">Connect providers with the organizations that need them.</h1>
          <p>
            Browse accredited training programs, request a session, pay securely through Moyasar,
            and follow every request from approval to completion.
          </p>

          <div class="hero-points">
            <div class="hero-point"><span>01</span> Beneficiary organizations request training in one place.</div>
            <div class="hero-point"><span>02</span> Providers approve requests and manage delivery.</div>
            <div class="hero-point"><span>03</span> Payments and refunds are tracked end to end.</div>
          </div>
        </div>
        <div style="font-size: 12px; color: #64748b;">Training marketplace · Saudi Arabia</div>
      </section>

      <section class="login-panel">
        <div class="login-card">
          <div>
            <h2>Welcome back</h2>
            <p class="muted" style="margin-top: 6px;">Sign in with your platform account to continue.</p>
          </div>

          <form class="login-form" (ngSubmit)="submit()">
            <div class="field">
              <label for="email">Email address</label>
              <input id="email" type="email" name="email" [(ngModel)]="email" autocomplete="email" required placeholder="you@organization.sa" />
            </div>

            <div class="field">
              <label for="password">Password</label>
              <input id="password" type="password" name="password" [(ngModel)]="password" autocomplete="current-password" required placeholder="••••••••" />
            </div>

            <button class="btn btn-primary" type="submit" [disabled]="loading() || !email || !password">
              {{ loading() ? 'Signing in…' : 'Sign in' }}
            </button>
          </form>

          <div class="login-hint">
            <strong>Demo administrator</strong><br />
            admin&#64;waei.sa · Admin&#64;12345
          </div>
        </div>
      </section>
    </div>
  `,
})
export class LoginPage {
  email = '';
  password = '';
  readonly loading = signal(false);

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService
  ) {
    if (auth.isLoggedIn()) {
      this.router.navigateByUrl(auth.homeRoute());
    }
  }

  submit(): void {
    if (!this.email || !this.password) {
      return;
    }

    this.loading.set(true);
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (error) => {
        this.loading.set(false);
        this.toast.error(error.error?.message ?? 'Could not sign in');
      },
    });
  }
}
