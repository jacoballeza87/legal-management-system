// src/app/core/models/case.model.ts

export type CaseStatus = 'ABIERTO' | 'EN_REVISION' | 'ACTIVO' | 'CERRADO';
export type CasePriority = 'URGENTE' | 'ALTA' | 'MEDIA' | 'BAJA';
export type CaseType = 'CIVIL' | 'PENAL' | 'LABORAL' | 'MERCANTIL' | 'FAMILIAR' | 'ADMINISTRATIVO';
export type VersionStatus = 'DRAFT' | 'REVIEW' | 'APPROVED';
export type RelationType = 'RELATED' | 'DERIVED_FROM' | 'CONSOLIDATED';

export interface Case {
  id: number;
  caseNumber: string;           // CASE-2024-00001
  title: string;
  description: string;
  clientName: string;
  caseType: CaseType;
  status: CaseStatus;
  priority: CasePriority;
  assignedLawyerId: number;
  assignedLawyerName?: string;
  courtName?: string;
  externalExpedientNumber?: string;
  dueDate?: string;             // ISO date string
  closedAt?: string;
  qrCodeBase64?: string;
  currentVersion?: string;
  collaborators?: CaseCollaborator[];
  relations?: CaseRelation[];
  createdAt: string;
  updatedAt: string;
}

export interface CaseVersion {
  id: number;
  caseId: number;
  versionNumber: string;        // "3.0"
  status: VersionStatus;
  changes: string;
  lawyerComment?: string;
  createdByUserId: number;
  createdByName?: string;
  createdAt: string;
  comments?: VersionComment[];
}

export interface VersionComment {
  id: number;
  versionId: number;
  content: string;
  authorId: number;
  authorName?: string;
  createdAt: string;
}

export interface CaseCollaborator {
  id: number;
  caseId: number;
  userId: number;
  userName?: string;
  userRole?: string;
  collaboratorRole: 'VIEWER' | 'COLLABORATOR' | 'ACCOUNTANT';
  addedAt: string;
}

export interface CaseRelation {
  id: number;
  sourceCaseId: number;
  targetCaseId: number;
  targetCaseNumber?: string;
  targetCaseTitle?: string;
  targetCaseStatus?: CaseStatus;
  relationType: RelationType;
  createdAt: string;
}

export interface CaseStats {
  totalCases: number;
  openCases: number;
  activeCases: number;
  closedCases: number;
  inReviewCases: number;
  urgentCases: number;
  byType: Record<CaseType, number>;
  byPriority: Record<CasePriority, number>;
}

export interface CaseFilters {
  search?: string;
  status?: CaseStatus;
  priority?: CasePriority;
  caseType?: CaseType;
  assignedLawyerId?: number;
  dueDateFrom?: string;
  dueDateTo?: string;
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface PagedCases {
  content: Case[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;       // current page (0-indexed)
}

// ── Request DTOs ─────────────────────────────────────────────────────

export interface CreateCaseRequest {
  title: string;
  description: string;
  clientName: string;
  caseType: CaseType;
  priority: CasePriority;
  assignedLawyerId: number;
  courtName?: string;
  externalExpedientNumber?: string;
  dueDate?: string;
  collaboratorIds?: number[];
  relatedCaseIds?: { caseId: number; relationType: RelationType }[];
}

export interface UpdateCaseRequest extends Partial<CreateCaseRequest> {
  status?: CaseStatus;
}

export interface CreateVersionRequest {
  status: VersionStatus;
  changes: string;
  lawyerComment?: string;
}

export interface AddCommentRequest {
  content: string;
}

export interface AddCollaboratorRequest {
  userId: number;
  collaboratorRole: 'VIEWER' | 'COLLABORATOR' | 'ACCOUNTANT';
}
