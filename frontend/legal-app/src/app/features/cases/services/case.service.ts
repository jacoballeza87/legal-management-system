import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import {
  Case, CaseVersion, CaseStats, CaseFilters, PagedCases,
  CreateCaseRequest, UpdateCaseRequest,
  CreateVersionRequest, AddCommentRequest, AddCollaboratorRequest,
  VersionComment, CaseCollaborator, CaseRelation
} from '../models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseService {

  // 🔥 CORRECCIÓN IMPORTANTE
  private readonly api = `${environment.apiUrl}/api/v1/cases`;

  constructor(private http: HttpClient) {}

  // ── CASES ──────────────────────────────────────────────────────────

  getCases(filters: CaseFilters): Observable<PagedCases> {
    let params = new HttpParams()
      .set('page', filters.page)
      .set('size', filters.size);

    if (filters.search)           params = params.set('search', filters.search);
    if (filters.status)           params = params.set('status', filters.status);
    if (filters.priority)         params = params.set('priority', filters.priority);
    if (filters.caseType)         params = params.set('caseType', filters.caseType);
    if (filters.assignedLawyerId) params = params.set('lawyerId', filters.assignedLawyerId);
    if (filters.dueDateFrom)      params = params.set('dueDateFrom', filters.dueDateFrom);
    if (filters.dueDateTo)        params = params.set('dueDateTo', filters.dueDateTo);
    if (filters.sortBy)           params = params.set('sortBy', filters.sortBy);
    if (filters.sortDir)          params = params.set('sortDir', filters.sortDir);

    return this.http.get<PagedCases>(this.api, { params });
  }

  getCaseById(id: number): Observable<Case> {
    return this.http.get<Case>(`${this.api}/${id}`);
  }

  getCaseByNumber(caseNumber: string): Observable<Case> {
    return this.http.get<Case>(`${this.api}/number/${caseNumber}`);
  }

  getStats(): Observable<CaseStats> {
    return this.http.get<CaseStats>(`${this.api}/stats`);
  }

  createCase(request: CreateCaseRequest): Observable<Case> {
    return this.http.post<Case>(this.api, request);
  }

  updateCase(id: number, request: UpdateCaseRequest): Observable<Case> {
    return this.http.put<Case>(`${this.api}/${id}`, request);
  }

  deleteCase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // 🔥 AJUSTADO A TU BACKEND REAL
  changeStatus(id: number, status: string): Observable<Case> {
    return this.http.patch<Case>(`${this.api}/${id}/status?status=${status}`, {});
  }

  regenerateQr(id: number): Observable<{ qrCodeUrl: string }> {
    return this.http.post<{ qrCodeUrl: string }>(`${this.api}/${id}/qr/regenerate`, {});
  }

  // ── VERSIONS ───────────────────────────────────────────────────────

  getVersions(caseId: number): Observable<CaseVersion[]> {
    return this.http.get<CaseVersion[]>(`${this.api}/${caseId}/versions`);
  }

  createVersion(caseId: number, request: CreateVersionRequest): Observable<CaseVersion> {
    return this.http.post<CaseVersion>(`${this.api}/${caseId}/versions`, request);
  }

  updateVersionStatus(caseId: number, versionId: number, status: string): Observable<CaseVersion> {
    return this.http.patch<CaseVersion>(
      `${this.api}/${caseId}/versions/${versionId}/status`,
      { status }
    );
  }

  // ── COMMENTS ───────────────────────────────────────────────────────

  addComment(caseId: number, versionId: number, request: AddCommentRequest): Observable<VersionComment> {
    return this.http.post<VersionComment>(
      `${this.api}/${caseId}/versions/${versionId}/comments`,
      request
    );
  }

  // ── COLLABORATORS ──────────────────────────────────────────────────

  getCollaborators(caseId: number): Observable<CaseCollaborator[]> {
    return this.http.get<CaseCollaborator[]>(`${this.api}/${caseId}/collaborators`);
  }

  addCollaborator(caseId: number, request: AddCollaboratorRequest): Observable<CaseCollaborator> {
    return this.http.post<CaseCollaborator>(`${this.api}/${caseId}/collaborators`, request);
  }

  removeCollaborator(caseId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${caseId}/collaborators/${userId}`);
  }

  // ── RELATIONS ──────────────────────────────────────────────────────

  addRelation(caseId: number, targetCaseId: number, relationType: string): Observable<CaseRelation> {
    return this.http.post<CaseRelation>(
      `${this.api}/${caseId}/relations`,
      { targetCaseId, relationType }
    );
  }

  removeRelation(caseId: number, relationId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${caseId}/relations/${relationId}`);
  }
}