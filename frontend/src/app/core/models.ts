export type PaymentStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'RELEASED';
export type PaymentType = 'ACH' | 'WIRE';
export type Decision = 'APPROVED' | 'REJECTED';
export type Role = 'INITIATOR' | 'APPROVER' | 'ADMIN';

export const STATUSES: PaymentStatus[] = ['PENDING_APPROVAL', 'APPROVED', 'RELEASED', 'REJECTED', 'DRAFT'];

export const STATUS_LABELS: Record<PaymentStatus, string> = {
  DRAFT: 'Draft',
  PENDING_APPROVAL: 'Pending approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  RELEASED: 'Released',
};

export interface ApprovalView {
  approver: string;
  decision: Decision;
  note: string | null;
  decidedAt: string;
}

export interface Payment {
  id: number;
  reference: string;
  type: PaymentType;
  amount: number;
  currency: string;
  debitAccount: string;
  beneficiaryName: string;
  beneficiaryAccount: string;
  memo: string | null;
  status: PaymentStatus;
  requiredApprovals: number;
  approvalsReceived: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  canDecide: boolean;
  approvals: ApprovalView[];
}

export interface StatusSummary {
  counts: Record<PaymentStatus, number>;
  total: number;
}

export interface CreatePaymentRequest {
  type: PaymentType;
  amount: number;
  currency: string;
  debitAccount: string;
  beneficiaryName: string;
  beneficiaryAccount: string;
  memo: string | null;
  submit: boolean;
}

export interface Session {
  token: string;
  username: string;
  roles: Role[];
  expiresAt: number;
}

/** Shape of the API's RFC 7807 error responses. */
export interface Problem {
  detail?: string;
  code?: string;
  title?: string;
}
