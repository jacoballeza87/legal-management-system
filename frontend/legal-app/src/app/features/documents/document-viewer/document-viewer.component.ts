// src/app/features/documents/document-viewer/document-viewer.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DocumentsActions } from '../../../store/documents/actions/documents.actions';
import {
  selectSelectedDocument, selectLoadingDetail, selectError
} from '../../../store/documents/selectors/documents.selectors';
import { CaseDocument, CATEGORY_LABELS } from '../../../core/models/document.model';

@Component({
  selector: 'app-document-viewer',
  template: `
    <div class="viewer-page" *ngIf="!(loading$ | async); else loader">
      <button class="back-btn" routerLink="/documents">← Volver al repositorio</button>

      <ng-container *ngIf="doc$ | async as doc">

        <!-- Header -->
        <div class="viewer-header">
          <div>
            <h1>{{ doc.originalFileName }}</h1>
            <div class="doc-pills">
              <span class="tag">{{ getCategoryLabel(doc.category) }}</span>
              <span class="case-link" [routerLink]="['/cases', doc.caseId]">{{ doc.caseNumber }}</span>
              <span class="pill pill-active" *ngIf="doc.status === 'ACTIVE'">ACTIVO</span>
              <span class="pill pill-deleted" *ngIf="doc.status === 'DELETED'">ELIMINADO</span>
            </div>
          </div>
          <div class="viewer-actions">
            <button class="icon-btn" title="Copiar URL" (click)="copyUrl(doc)">🔗</button>
            <button class="icon-btn" title="Imprimir" (click)="print()">🖨</button>
            <button class="btn btn-outline" (click)="download(doc)">⬇ Descargar</button>
            <button class="btn btn-danger btn-sm"
                    *ngIf="doc.status === 'ACTIVE'"
                    (click)="confirmDelete(doc)">🗑 Eliminar</button>
          </div>
        </div>

        <div class="viewer-layout">

          <!-- Preview área -->
          <div class="viewer-main">
            <div class="viewer-toolbar">
              <button class="icon-btn" *ngIf="isPdf(doc)">◀</button>
              <button class="icon-btn" *ngIf="isPdf(doc)">▶</button>
              <button class="icon-btn">🔍−</button>
              <span class="zoom-label">100%</span>
              <button class="icon-btn">🔍+</button>
              <div class="spacer"></div>
              <span class="toolbar-filename">{{ doc.originalFileName }}</span>
              <div class="spacer"></div>
              <button class="icon-btn" (click)="openInTab(doc)">⬆ Abrir</button>
            </div>

            <div class="viewer-body">
              <!-- Imagen -->
              <img *ngIf="isImage(doc) && previewUrl"
                   [src]="previewUrl" class="preview-img" [alt]="doc.originalFileName">

              <!-- PDF / Otros -->
              <div class="preview-placeholder" *ngIf="!isImage(doc) || !previewUrl">
                <div class="big-icon">{{ getMimeIcon(doc.mimeType) }}</div>
                <div class="preview-name">{{ doc.originalFileName }}</div>
                <div class="preview-info">{{ getMimeLabel(doc.mimeType) }} · {{ formatSize(doc.fileSize) }}</div>
                <button class="btn btn-gold" (click)="openInTab(doc)">
                  🔗 Abrir en nueva pestaña
                </button>
              </div>
            </div>
          </div>

          <!-- Panel lateral -->
          <div class="viewer-side">

            <!-- Detalles -->
            <div class="card">
              <div class="card-header"><div class="card-title">📋 Detalles</div></div>
              <div class="detail-rows">
                <div class="detail-row">
                  <span class="dr-label">Nombre</span>
                  <span class="dr-val">{{ doc.originalFileName }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Caso</span>
                  <span class="dr-val case-link" [routerLink]="['/cases', doc.caseId]">{{ doc.caseNumber }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Categoría</span>
                  <span class="dr-val">{{ getCategoryLabel(doc.category) }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Tamaño</span>
                  <span class="dr-val">{{ formatSize(doc.fileSize) }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Tipo MIME</span>
                  <span class="dr-val mime-val">{{ doc.mimeType }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Versión</span>
                  <span class="dr-val"><span class="tag">v{{ doc.version }}</span></span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Subido por</span>
                  <span class="dr-val">{{ doc.uploadedByName }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Fecha</span>
                  <span class="dr-val">{{ doc.uploadedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
                <div class="detail-row">
                  <span class="dr-label">Checksum MD5</span>
                  <span class="dr-val checksum">{{ doc.checksum }}</span>
                </div>
              </div>
            </div>

            <!-- Acciones rápidas -->
            <div class="card">
              <div class="card-header"><div class="card-title">📥 Acciones</div></div>
              <div class="quick-actions">
                <button class="btn btn-outline btn-sm full-width"
                        (click)="download(doc)">⬇ Descargar</button>
                <button class="btn btn-outline btn-sm full-width"
                        (click)="copyUrl(doc)">🔗 Copiar URL presignada</button>
                <button class="btn btn-outline btn-sm full-width"
                        routerLink="/documents/upload"
                        [queryParams]="{caseId: doc.caseId, replace: doc.id}">
                  ↑ Subir nueva versión
                </button>
              </div>
            </div>

            <!-- Historial de versiones -->
            <div class="card" *ngIf="doc.version > 1 || doc.previousVersionId">
              <div class="card-header"><div class="card-title">🕐 Versiones</div></div>
              <div class="ver-list">
                <div class="ver-item current">
                  <span class="tag gold-tag">v{{ doc.version }} actual</span>
                  <span class="ver-date">{{ doc.uploadedAt | date:'dd/MM/yyyy' }}</span>
                  <button class="icon-btn" (click)="download(doc)">⬇</button>
                </div>
              </div>
            </div>

          </div>
        </div>

      </ng-container>

      <!-- Error -->
      <div class="error-state" *ngIf="error$ | async as error">
        <div>⚠️ {{ error }}</div>
        <button class="btn btn-outline" routerLink="/documents">Volver</button>
      </div>

    </div>

    <ng-template #loader>
      <div class="loading-state"><div class="skeleton"></div></div>
    </ng-template>

    <!-- Confirm delete modal -->
    <div class="modal-overlay" [class.open]="showDeleteModal" (click)="showDeleteModal=false">
      <div class="modal" (click)="$event.stopPropagation()" *ngIf="currentDoc">
        <div class="modal-header">
          <div class="modal-title">⚠️ Eliminar documento</div>
          <button class="modal-close" (click)="showDeleteModal=false">✕</button>
        </div>
        <p>¿Eliminar <strong>{{ currentDoc.originalFileName }}</strong>?</p>
        <p class="modal-hint">Se moverá a papelera (S3/deleted/) y se eliminará en 90 días.</p>
        <div class="modal-footer">
          <button class="btn btn-outline" (click)="showDeleteModal=false">Cancelar</button>
          <button class="btn btn-danger" (click)="executeDelete()">Eliminar</button>
        </div>
      </div>
    </div>
  `
})
export class DocumentViewerComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  doc$     = this.store.select(selectSelectedDocument);
  loading$ = this.store.select(selectLoadingDetail);
  error$   = this.store.select(selectError);

  showDeleteModal = false;
  currentDoc: CaseDocument | null = null;
  previewUrl: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) {
        this.store.dispatch(DocumentsActions.selectDocument({ id: +params['id'] }));
      }
    });
    this.doc$.pipe(takeUntil(this.destroy$)).subscribe(doc => {
      this.currentDoc = doc;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.previewUrl) URL.revokeObjectURL(this.previewUrl);
  }

  download(doc: CaseDocument): void {
    this.store.dispatch(DocumentsActions.getDownloadUrl({ id: doc.id }));
  }

  openInTab(doc: CaseDocument): void {
    if (doc.downloadUrl) window.open(doc.downloadUrl, '_blank');
    else this.download(doc);
  }

  copyUrl(doc: CaseDocument): void {
    if (doc.downloadUrl) {
      navigator.clipboard.writeText(doc.downloadUrl);
    }
  }

  print(): void { window.print(); }

  confirmDelete(doc: CaseDocument): void {
    this.currentDoc = doc;
    this.showDeleteModal = true;
  }

  executeDelete(): void {
    if (this.currentDoc) {
      this.store.dispatch(DocumentsActions.deleteDocument({ id: this.currentDoc.id }));
      this.showDeleteModal = false;
      this.router.navigate(['/documents']);
    }
  }

  // ── Type guards ──────────────────────────────────────────────────────
  isPdf(doc: CaseDocument): boolean   { return doc.mimeType?.includes('pdf') ?? false; }
  isImage(doc: CaseDocument): boolean { return doc.mimeType?.includes('image') ?? false; }

  getMimeIcon(mime: string): string {
    if (mime?.includes('pdf'))   return '📄';
    if (mime?.includes('sheet')) return '📊';
    if (mime?.includes('word'))  return '📝';
    if (mime?.includes('image')) return '🖼';
    if (mime?.includes('zip'))   return '📦';
    return '📎';
  }

  getMimeLabel(mime: string): string {
    if (mime?.includes('pdf'))   return 'PDF';
    if (mime?.includes('sheet')) return 'XLSX';
    if (mime?.includes('word'))  return 'DOCX';
    if (mime?.includes('image')) return 'Imagen';
    return mime?.split('/')[1]?.toUpperCase() ?? 'Archivo';
  }

  getCategoryLabel(cat: any): string { return CATEGORY_LABELS[cat as DocumentCategory] ?? cat; }

  formatSize(bytes: number): string {
    if (!bytes) return '—';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1024*1024)  return `${(bytes/1024).toFixed(0)} KB`;
    return `${(bytes/1024/1024).toFixed(1)} MB`;
  }
}

import { DocumentCategory } from '../../../core/models/document.model';
