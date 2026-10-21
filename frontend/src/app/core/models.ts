export interface AuthUser {
  token?: string;
  tokenType?: string;
  accountId: number;
  fullName: string;
  email: string;
  role: 'PLATFORM_ADMIN' | 'ORG_ADMIN';
  status: string;
  organizationId?: number | null;
  organizationName?: string | null;
  organizationType?: 'BENEFICIARY' | 'PROVIDER' | 'BOTH' | null;
}

export interface TrainingProgram {
  id: number;
  title: string;
  category: string;
  description: string;
  price: number;
  deliveryMode: 'IN_PERSON' | 'ONLINE' | 'HYBRID';
  isActive: boolean;
  organizationId?: number | null;
  organizationName?: string | null;
}

export interface Payment {
  id: number;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED' | 'CANCELLED';
  moyasarPaymentId?: string | null;
  moyasarInvoiceId?: string | null;
  checkoutUrl?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  paidAt?: string | null;
  refundedAmount?: number | null;
  refundedAt?: string | null;
  trainingRequestId?: number | null;
  commissionRatePercent?: number | null;
  commissionAmount?: number | null;
  providerAmount?: number | null;
}

export interface TrainingRequest {
  id: number;
  participantCount: number;
  programPrice: number;
  notes?: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'PAYMENT_PENDING' | 'PAID' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  requestedAt?: string | null;
  scheduledStart?: string | null;
  scheduledEnd?: string | null;
  acceptedAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  trainingProgramId?: number | null;
  trainingProgramTitle?: string | null;
  providerOrganizationId?: number | null;
  providerOrganizationName?: string | null;
  beneficiaryOrganizationId?: number | null;
  beneficiaryOrganizationName?: string | null;
  createdByAccountId?: number | null;
  createdByFullName?: string | null;
  payments: Payment[];
}

export interface Organization {
  id: number;
  name: string;
  type: 'BENEFICIARY' | 'PROVIDER' | 'BOTH';
  sector: string;
  email: string;
  phoneNumber: string;
  verificationStatus: string;
  createdAt?: string | null;
  documentReference?: string | null;
}

export interface Account {
  id: number;
  fullName: string;
  email: string;
  role: 'PLATFORM_ADMIN' | 'ORG_ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING';
  createdAt?: string | null;
  organizationId?: number | null;
  organizationName?: string | null;
  organizationType?: string | null;
}

export interface ApiMessage {
  message: string;
}
