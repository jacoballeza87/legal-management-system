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

  // 🔥 Endpoint base para el microservicio case-service
  private readonly api = `${environment.apiUrl}/cases`;

  constructor(private http: HttpClient) {}

  // ── CASES ──────────────────────────────────────────────────────────

  // RN-F-001 & RN-F-005: Obtener casos con filtros (Sirve para listas y Kanban)
  getCases(filters: CaseFilters): Observable<PagedCases> {
    // Note: Converted to string to satisfy HttpParams strict types
    let params = new HttpParams()
      .set('page', filters.page.toString())
      .set('size', filters.size.toString());

    if (filters.search)           params = params.set('search', filters.search);
    if (filters.status)           params = params.set('status', filters.status);
    if (filters.priority)         params = params.set('priority', filters.priority);
    if (filters.caseType)         params = params.set('caseType', filters.caseType);
    if (filters.assignedLawyerId) params = params.set('lawyerId', filters.assignedLawyerId.toString());
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
    // RN-F-018: Al crearlo, el backend enviará el evento Kafka para el PDF y notificaciones
    return this.http.post<Case>(this.api, request);
  }

  updateCase(id: number, request: UpdateCaseRequest): Observable<Case> {
    return this.http.put<Case>(`${this.api}/${id}`, request);
  }

  deleteCase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // 🔥 RN-F-003, RN-F-004, RN-O-005: Cambio de estados del caso
  // Este es el método que utilizará el Kanban Board en el evento `cdkDropListDropped`
  changeStatus(id: number, status: string): Observable<Case> {
    return this.http.patch<Case>(`${this.api}/${id}/status?status=${status}`, {});
  }

  // RN-F-022: Generación y compartición de Códigos QR
  regenerateQr(id: number): Observable<{ qrCodeUrl: string }> {
    return this.http.post<{ qrCodeUrl: string }>(`${this.api}/${id}/qr/regenerate`, {});
  }

  // ── VERSIONS ───────────────────────────────────────────────────────
  // RN-O-001 & RN-O-003: Inmutabilidad y gestión de actualizaciones

  getVersions(caseId: number): Observable<CaseVersion[]> {
    return this.http.get<CaseVersion[]>(`${this.api}/${caseId}/versions`);
  }

  createVersion(caseId: number, request: CreateVersionRequest): Observable<CaseVersion> {
    // RN-F-019 & RN-F-021: El backend disparará notificaciones por email y el PDF actualizado
    return this.http.post<CaseVersion>(`${this.api}/${caseId}/versions`, request);
  }

  updateVersionStatus(caseId: number, versionId: number, status: string): Observable<CaseVersion> {
    return this.http.patch<CaseVersion>(
      `${this.api}/${caseId}/versions/${versionId}/status`,
      { status }
    );
  }

  // ── COMMENTS ───────────────────────────────────────────────────────
  // RN-F-008 & RN-O-002: Comentarios de usuarios no asignados pero de la misma categoría

  addComment(caseId: number, versionId: number, request: AddCommentRequest): Observable<VersionComment> {
    return this.http.post<VersionComment>(
      `${this.api}/${caseId}/versions/${versionId}/comments`,
      request
    );
  }

  // ── COLLABORATORS ──────────────────────────────────────────────────
  // RN-F-007: Añadir colaboradores que obtienen permisos automáticos de administración

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
  // RN-F-009 & RN-O-007: Relaciones Padre-Hijo y vinculación de carpetas Drive

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
