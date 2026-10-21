export function statusClass(status?: string | null): string {
  return `badge badge-${(status ?? 'unknown').toLowerCase()}`;
}

export function money(value?: number | null): string {
  return new Intl.NumberFormat('en-SA', {
    style: 'currency',
    currency: 'SAR',
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

export function dateTime(value?: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('en-SA', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

export function dateOnly(value?: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('en-SA', { dateStyle: 'medium' }).format(date);
}

export function titleCase(value?: string | null): string {
  if (!value) {
    return '—';
  }
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function toLocalDateTime(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  return value.length === 16 ? `${value}:00` : value;
}

export function apiError(error: unknown): string {
  const body = (error as { error?: unknown })?.error;

  if (typeof body === 'string' && body.trim()) {
    return body;
  }

  if (body && typeof body === 'object') {
    const message = (body as { message?: string }).message;
    if (message) {
      return message;
    }
    const values = Object.values(body as Record<string, unknown>);
    if (values.length > 0) {
      return String(values[0]);
    }
  }

  const message = (error as { message?: string })?.message;
  return message || 'Something went wrong';
}

export function nowLocalInput(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  return new Date(now.getTime() - offset * 60_000).toISOString().slice(0, 16);
}


export function latestPayment(request: { payments?: { status: string; checkoutUrl?: string | null }[] }) {
  if (!request.payments || request.payments.length === 0) {
    return undefined;
  }
  return [...request.payments].sort((a, b) => {
    const left = (a as { id?: number }).id ?? 0;
    const right = (b as { id?: number }).id ?? 0;
    return right - left;
  })[0];
}
