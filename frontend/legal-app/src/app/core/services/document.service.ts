// src/app/core/services/document.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, map, filter } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CaseDocument, DocumentStats, DocumentFilters, PagedDocuments,
  UploadDocumentRequest
} from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {

  private readonly api = `${environment.apiUrl}/api/documents`;

  constructor(private http: HttpClient) {}

  // ── CRUD ────────────────────────────────────────────────────────────

  getDocuments(filters: DocumentFilters): Observable<PagedDocuments> {
    let params = new HttpParams()
      .set('page', filters.page)
      .set('size', filters.size);
    if (filters.caseId)            params = params.set('caseId', filters.caseId);
    if (filters.category)          params = params.set('category', filters.category);
    if (filters.status)            params = params.set('status', filters.status);
    if (filters.search)            params = params.set('search', filters.search);
    if (filters.uploadedByUserId)  params = params.set('userId', filters.uploadedByUserId);
    return this.http.get<PagedDocuments>(this.api, { params });
  }

  getDocumentById(id: number): Observable<CaseDocument> {
    return this.http.get<CaseDocument>(`${this.api}/${id}`);
  }

  getDocumentsByCaseId(caseId: number): Observable<CaseDocument[]> {
    return this.http.get<CaseDocument[]>(`${this.api}/case/${caseId}`);
  }

  getStats(): Observable<DocumentStats> {
    return this.http.get<DocumentStats>(`${this.api}/stats`);
  }

  // ── UPLOAD CON PROGRESO ─────────────────────────────────────────────
  // Retorna Observable de progreso (0-100) y cuando termina emite el documento creado

  uploadDocument(request: UploadDocumentRequest): Observable<{ progress: number; document?: CaseDocument }> {
    const formData = new FormData();
    formData.append('file', request.file, request.file.name);
    formData.append('caseId', String(request.caseId));
    formData.append('category', request.category);
    if (request.description) formData.append('description', request.description);

    const req = new HttpRequest('POST', this.api, formData, {
      reportProgress: true,
    });

    return this.http.request<CaseDocument>(req).pipe(
      filter(event =>
        event.type === HttpEventType.UploadProgress ||
        event.type === HttpEventType.Response
      ),
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          const progress = event.total
            ? Math.round(100 * event.loaded / event.total)
            : 0;
          return { progress };
        }
        // HttpEventType.Response
        return { progress: 100, document: (event as any).body as CaseDocument };
      })
    );
  }

  // ── PRESIGNED URL (descarga) ─────────────────────────────────────────

  getDownloadUrl(id: number): Observable<{ url: string; expiresIn: number }> {
    return this.http.get<{ url: string; expiresIn: number }>(
      `${this.api}/${id}/download-url`
    );
  }

  // Descarga directa como Blob (para preview en browser)
  downloadAsBlob(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/${id}/download`, { responseType: 'blob' });
  }

  // ── SOFT DELETE ─────────────────────────────────────────────────────
  // Mueve el archivo a S3://bucket/deleted/ y marca como DELETED en BD

  softDelete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  restore(id: number): Observable<CaseDocument> {
    return this.http.patch<CaseDocument>(`${this.api}/${id}/restore`, {});
  }

  // ── VERSIONADO ──────────────────────────────────────────────────────

  uploadNewVersion(id: number, file: File): Observable<{ progress: number; document?: CaseDocument }> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    const req = new HttpRequest('POST', `${this.api}/${id}/versions`, formData, {
      reportProgress: true,
    });

    return this.http.request<CaseDocument>(req).pipe(
      filter(event =>
        event.type === HttpEventType.UploadProgress ||
        event.type === HttpEventType.Response
      ),
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          const progress = event.total
            ? Math.round(100 * event.loaded / event.total)
            : 0;
          return { progress };
        }
        return { progress: 100, document: (event as any).body as CaseDocument };
      })
    );
  }

  getVersionHistory(documentKey: string): Observable<CaseDocument[]> {
    return this.http.get<CaseDocument[]>(`${this.api}/versions/${documentKey}`);
  }
}
