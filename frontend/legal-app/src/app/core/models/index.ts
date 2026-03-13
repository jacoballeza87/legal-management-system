// ─── User & Auth Models ───────────────────────────────────────────────────────

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'LAWYER' | 'ACCOUNTANT' | 'USER' | 'VIEWER';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'GITHUB';

export interface User {
  id: number;
  name: string;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  bio?: string;
  jobTitle?: string;
  department?: string;
  barNumber?: string;
  status: UserStatus;
  isEmailVerified: boolean;
  lastLogin?: string;
  roles: Role[];
  categories: Category[];
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Role {
  id: number;
  name: UserRole;
  description: string;
  isSystemRole: boolean;
  permissions: Permission[];
}

export interface Permission {
  id: number;
  name: string;       // "cases:read", "users:admin"
  description: string;
  module: string;
  action: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  colorHex?: string;
  icon?: string;
  isActive: boolean;
}

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
  deviceId?: string;
  deviceName?: string;
  deviceType?: string;
}

export interface RegisterRequest {
  name: string;
  username: string;
  email: string;
  password: string;
  role?: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  deviceId: string;
  user: UserInfo;
}

export interface UserInfo {
  id: number;
  name: string;
  username: string;
  email: string;
  role: UserRole;
  avatarUrl?: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
  deviceId?: string;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface DeviceRegistration {
  id: number;
  deviceId: string;
  deviceName?: string;
  deviceType?: string;
  ipAddress?: string;
  lastUsed: string;
  registeredAt: string;
}

// ─── Case Models ──────────────────────────────────────────────────────────────

export type CaseStatus = 'OPEN' | 'IN_PROGRESS' | 'PENDING_REVIEW' | 'ON_HOLD' | 'CLOSED' | 'ARCHIVED' | 'CANCELLED';
export type CasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'CRITICAL';
export type CaseType = 'CIVIL' | 'CRIMINAL' | 'COMMERCIAL' | 'LABOR' | 'FAMILY' | 'ADMINISTRATIVE' | 'CONSTITUTIONAL' | 'INTELLECTUAL_PROPERTY' | 'TAX' | 'REAL_ESTATE' | 'OTHER';

export interface Case {
  id: number;
  caseNumber: string;
  title: string;
  description?: string;
  status: CaseStatus;
  priority: CasePriority;
  caseType: CaseType;
  ownerId: number;
  clientName: string;
  clientEmail?: string;
  clientPhone?: string;
  courtName?: string;
  courtCaseNumber?: string;
  categoryId?: number;
  dueDate?: string;
  closedAt?: string;
  estimatedHours?: number;
  billedHours: number;
  qrCodeUrl?: string;
  currentVersion: number;
  tags?: string;
  collaborators: Collaborator[];
  createdAt: string;
  updatedAt: string;
}

export interface CaseVersion {
  id: number;
  caseId: number;
  versionNumber: number;
  title: string;
  description?: string;
  content: string;
  changeSummary?: string;
  createdBy: number;
  createdByName?: string;
  status: 'DRAFT' | 'REVIEW' | 'APPROVED' | 'REJECTED';
  comments: VersionComment[];
  createdAt: string;
}

export interface Collaborator {
  id: number;
  userId: number;
  userName?: string;
  userEmail?: string;
  role: 'VIEWER' | 'EDITOR' | 'REVIEWER' | 'ADMIN';
  addedBy?: number;
  addedAt: string;
}

export interface VersionComment {
  id: number;
  versionId: number;
  content: string;
  authorId: number;
  authorName?: string;
  isEdited: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CaseStats {
  total: number;
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
  byType: Record<string, number>;
  overdue: number;
  dueSoon: number;
}

// ─── Notification Models ──────────────────────────────────────────────────────

export type NotificationType = 'EMAIL' | 'SMS' | 'IN_APP';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'READ';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  status: NotificationStatus;
  recipientId: number;
  relatedCaseId?: number;
  relatedCaseNumber?: string;
  createdAt: string;
  readAt?: string;
}

// ─── Document Models ──────────────────────────────────────────────────────────

export interface CaseDocument {
  id: number;
  caseId: number;
  fileName: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  s3Key: string;
  downloadUrl?: string;
  checksum?: string;
  uploadedBy: number;
  version: number;
  createdAt: string;
}

// ─── Shared ───────────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  errors?: Record<string, string>;
}
