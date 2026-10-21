import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthUser } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'waei.token';
  private readonly userKey = 'waei.user';

  readonly user = signal<AuthUser | null>(this.readUser());

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>('/api/v1/auth/login', { email, password }).pipe(
      tap((response) => {
        localStorage.setItem(this.tokenKey, response.token ?? '');
        localStorage.setItem(this.userKey, JSON.stringify(response));
        this.user.set(response);
      })
    );
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>('/api/v1/auth/me').pipe(
      tap((response) => {
        const merged = { ...this.user(), ...response };
        localStorage.setItem(this.userKey, JSON.stringify(merged));
        this.user.set(merged);
      })
    );
  }

  logout(redirect = true): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.user.set(null);
    if (redirect) {
      window.location.href = '/login';
    }
  }

  get token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.token && !!this.user();
  }

  isPlatformAdmin(): boolean {
    return this.user()?.role === 'PLATFORM_ADMIN';
  }

  isProvider(): boolean {
    const type = this.user()?.organizationType;
    return this.user()?.role === 'ORG_ADMIN' && (type === 'PROVIDER' || type === 'BOTH');
  }

  isBeneficiary(): boolean {
    const type = this.user()?.organizationType;
    return this.user()?.role === 'ORG_ADMIN' && (type === 'BENEFICIARY' || type === 'BOTH');
  }

  homeRoute(): string {
    if (this.isPlatformAdmin()) {
      return '/admin/organizations';
    }
    if (this.isProvider()) {
      return '/provider/requests';
    }
    if (this.isBeneficiary()) {
      return '/catalog';
    }
    return '/dashboard';
  }

  private readUser(): AuthUser | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
