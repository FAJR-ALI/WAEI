import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'success' | 'error' | 'info';
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private sequence = 0;

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  dismiss(id: number): void {
    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private push(type: Toast['type'], message: string): void {
    const toast: Toast = { id: ++this.sequence, type, message };
    this.toasts.update((toasts) => [...toasts, toast]);

    window.setTimeout(() => this.dismiss(toast.id), 5000);
  }
}
