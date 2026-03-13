// src/app/features/documents/document-repository/document-repository.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FormControl } from '@angular/forms';
import { DocumentsActions } from '../../../store/documents/actions/documents.actions';
import {
  selectAllDocuments, selectStats, selectLoading, selectViewMode,
  selectActiveCategoryFilter, selectTotalElements, selectError
} from '../../../store/documents/selectors/documents.selectors';
import {
  CaseDocument, DocumentCategory, CATEGORY_LABELS, CATEGORY_ICONS
} from '../../../core/models/document.model';

@Component({
  selector: 'app-document-repository',
  template: `
    <div class="docs-page">

      <!-- Stat chips -->
      <div class="stat-chips" *ngIf="stats$ | async as s">
        <div class="stat-chip">
          <span class="chip-icon">📎</span>
          <div><div class="chip-val">{{ s.totalDocuments }}</div><div class="chip-lbl">Total archivos</div></div>
        </div>
        <div class="stat-chip">
          <span class="chip-icon">📄</span>
          <div><div class="chip-val">{{ s.byMimeType['application/pdf'] || 0 }}</div><div class="chip-lbl">PDFs</div></div>
        </div>
        <div class="stat-chip">
          <span class="chip-icon">💾</span>
          <div><div class="chip-val">{{ s.totalSizeFormatted }}</div><div class="chip-lbl">Almacenados</div></div>
        </div>
        <div class="stat-chip">
          <span class="chip-icon">📂</span>
          <div><div class="chip-val">{{ s.casesWithDocuments }}</div><div class="chip-lbl">Casos con docs</div></div>
        </div>
      </div>

      <!-- Header -->
      <div class="page-header">
        <div>
          <h1>Repositorio de Documentos</h1>
          <p class="subtitle">Gestión centralizada — S3 + Google Drive</p>
        </div>
        <div class="header-actions">
          <!-- Grid / List toggle -->
          <div class="view-toggle">
            <button [class.active]="(viewMode$ | async) === 'grid'"
                    (click)="setViewMode('grid')" title="Cuadrícula">⊞</button>
            <button [class.active]="(viewMode$ | async) === 'list'"
                    (click)="setViewMode('list')" title="Lista">☰</button>
          </div>
          <button class="btn btn-outline btn-sm" (click)="exportList()">⬇ Exportar</button>
          <button class="btn btn-gold" routerLink="/documents/upload">⬆ Subir Archivos</button>
        </div>
      </div>

      <div class="repo-layout">

        <!-- Sidebar de categorías -->
        <div class="cat-sidebar">
          <div class="cat-section-title">Categorías</div>

          <div class="cat-item" [class.active]="(activeCategory$ | async) === null"
               (click)="setCategory(null)">
            <span>📁</span> Todos
            <span class="cat-count">{{ (totalElements$ | async) || 0 }}</span>
          </div>

          <div *ngFor="let cat of categories"
               class="cat-item"
               [class.active]="(activeCategory$ | async) === cat"
               (click)="setCategory(cat)">
            <span>{{ getCategoryIcon(cat) }}</span>
            {{ getCategoryLabel(cat) }}
          </div>

          <div class="cat-divider"></div>
          <div class="cat-section-title">Acceso rápido</div>
          <div class="cat-item" (click)="filterRecent()"><span>🕐</span> Recientes</div>
          <div class="cat-item" (click)="filterMine()"><span>👤</span> Mis uploads</div>
        </div>

        <!-- Contenido principal -->
        <div class="repo-content">

          <!-- Filtros -->
          <div class="filters-bar">
            <div class="search-box">
              <span class="search-icon">🔍</span>
              <input [formControl]="searchCtrl"
                     placeholder="Buscar por nombre, caso, descripción...">
            </div>
            <select (change)="setCaseFilter($any($event.target).value)">
              <option value="">Todos los casos</option>
            </select>
            <select (change)="setMimeFilter($any($event.target).value)">
              <option value="">Todos los tipos</option>
              <option value="application/pdf">PDF</option>
              <option value="application/vnd.openxmlformats-officedocument.wordprocessingml.document">DOCX</option>
              <option value="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">XLSX</option>
              <option value="image/png,image/jpeg">Imágenes</option>
            </select>
          </div>

          <!-- Error -->
          <div class="error-banner" *ngIf="error$ | async as error">⚠ {{ error }}</div>

          <!-- Loading -->
          <div class="loading-bar" *ngIf="loading$ | async"></div>

          <!-- GRID VIEW -->
          <div class="doc-grid" *ngIf="(viewMode$ | async) === 'grid'">
            <div *ngFor="let doc of docs$ | async; trackBy: trackById"
                 class="doc-card"
                 (click)="openDocument(doc)">

              <!-- Hover actions -->
              <div class="doc-hover-actions">
                <button class="doc-action-btn"
                        (click)="$event.stopPropagation(); downloadDoc(doc)"
                        title="Descargar">⬇</button>
                <button class="doc-action-btn"
                        (click)="$event.stopPropagation(); confirmDelete(doc)"
                        title="Eliminar">🗑</button>
              </div>

              <div class="doc-preview" [ngClass]="getMimeClass(doc.mimeType)">
                {{ getMimeIcon(doc.mimeType) }}
                <span class="doc-ver-badge">v{{ doc.version }}</span>
              </div>
              <div class="doc-info">
                <div class="doc-name" [title]="doc.originalFileName">{{ doc.originalFileName }}</div>
                <div class="doc-meta">
                  <span>{{ formatSize(doc.fileSize) }}</span>
                  <span>{{ getMimeLabel(doc.mimeType) }}</span>
                </div>
                <div class="doc-case-link">{{ doc.caseNumber }}</div>
              </div>
            </div>

            <!-- Empty state -->
            <div class="empty-state" *ngIf="(docs$ | async)?.length === 0 && !(loading$ | async)">
              <div>📭</div>
              <p>No hay documentos en esta categoría</p>
              <button class="btn btn-outline btn-sm" routerLink="/documents/upload">Subir primero</button>
            </div>
          </div>

          <!-- LIST VIEW -->
          <div class="table-wrap" *ngIf="(viewMode$ | async) === 'list'">
            <table>
              <thead>
                <tr>
                  <th>Archivo</th>
                  <th>Categoría</th>
                  <th>Caso</th>
                  <th>Tamaño</th>
                  <th>Versión</th>
                  <th>Subido por</th>
                  <th>Fecha</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let doc of docs$ | async; trackBy: trackById"
                    (click)="openDocument(doc)">
                  <td class="file-cell">
                    <span class="file-icon">{{ getMimeIcon(doc.mimeType) }}</span>
                    <div>
                      <div class="file-name">{{ doc.originalFileName }}</div>
                      <div class="file-mime">{{ getMimeLabel(doc.mimeType) }}</div>
                    </div>
                  </td>
                  <td><span class="tag">{{ getCategoryLabel(doc.category) }}</span></td>
                  <td class="case-cell">{{ doc.caseNumber }}</td>
                  <td>{{ formatSize(doc.fileSize) }}</td>
                  <td><span class="tag">v{{ doc.version }}</span></td>
                  <td>{{ doc.uploadedByName }}</td>
                  <td class="date-cell">{{ doc.uploadedAt | date:'dd/MM/yyyy' }}</td>
                  <td (click)="$event.stopPropagation()">
                    <div class="row-actions">
                      <button class="icon-btn" (click)="downloadDoc(doc)">⬇</button>
                      <button class="icon-btn danger" (click)="confirmDelete(doc)">🗑</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

        </div>
      </div>

    </div>

    <!-- Modal confirmar eliminación -->
    <div class="modal-overlay" [class.open]="!!docToDelete" (click)="docToDelete = null">
      <div class="modal" (click)="$event.stopPropagation()" *ngIf="docToDelete">
        <div class="modal-header">
          <div class="modal-title">⚠️ Eliminar documento</div>
          <button class="modal-close" (click)="docToDelete = null">✕</button>
        </div>
        <p>¿Eliminar <strong>{{ docToDelete.originalFileName }}</strong>?</p>
        <p class="modal-hint">El archivo se moverá a papelera y se eliminará permanentemente en 90 días.</p>
        <div class="modal-footer">
          <button class="btn btn-outline" (click)="docToDelete = null">Cancelar</button>
          <button class="btn btn-danger" (click)="executeDelete()">Eliminar</button>
        </div>
      </div>
    </div>
  `
})
export class DocumentRepositoryComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  docs$          = this.store.select(selectAllDocuments);
  stats$         = this.store.select(selectStats);
  loading$       = this.store.select(selectLoading);
  error$         = this.store.select(selectError);
  viewMode$      = this.store.select(selectViewMode);
  activeCategory$ = this.store.select(selectActiveCategoryFilter);
  totalElements$ = this.store.select(selectTotalElements);

  searchCtrl = new FormControl('');
  docToDelete: CaseDocument | null = null;

  categories: DocumentCategory[] = [
    'CONTRATO','DEMANDA','SENTENCIA','EVIDENCIA',
    'CORRESPONDENCIA','FACTURA','PODER_NOTARIAL','IDENTIFICACION','OTRO'
  ];

  constructor(private store: Store, private router: Router) {}

  ngOnInit(): void {
    this.store.dispatch(DocumentsActions.loadStats());
    this.store.dispatch(DocumentsActions.loadDocuments({
      filters: { page: 0, size: 24 }
    }));

    this.searchCtrl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(val =>
      this.store.dispatch(DocumentsActions.setFilters({ filters: { search: val ?? undefined } }))
    );
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  setCategory(cat: string | null): void {
    this.store.dispatch(DocumentsActions.setCategoryFilter({ category: cat }));
  }

  setViewMode(mode: 'grid' | 'list'): void {
    this.store.dispatch(DocumentsActions.setViewMode({ mode }));
  }

  setCaseFilter(caseId: string): void {
    this.store.dispatch(DocumentsActions.setFilters({
      filters: { caseId: caseId ? +caseId : undefined }
    }));
  }

  setMimeFilter(mime: string): void {
    // El backend filtraría por mime type
    this.store.dispatch(DocumentsActions.setFilters({ filters: {} }));
  }

  filterRecent(): void {
    this.store.dispatch(DocumentsActions.resetFilters());
  }

  filterMine(): void {
    // TODO: obtener userId desde AuthState
  }

  openDocument(doc: CaseDocument): void {
    this.store.dispatch(DocumentsActions.selectDocument({ id: doc.id }));
    this.router.navigate(['/documents', doc.id]);
  }

  downloadDoc(doc: CaseDocument): void {
    this.store.dispatch(DocumentsActions.getDownloadUrl({ id: doc.id }));
  }

  confirmDelete(doc: CaseDocument): void { this.docToDelete = doc; }

  executeDelete(): void {
    if (this.docToDelete) {
      this.store.dispatch(DocumentsActions.deleteDocument({ id: this.docToDelete.id }));
      this.docToDelete = null;
    }
  }

  exportList(): void { console.log('Export list'); }

  // ── Helpers ─────────────────────────────────────────────────────────
  getCategoryLabel(cat: DocumentCategory): string { return CATEGORY_LABELS[cat] ?? cat; }
  getCategoryIcon(cat: DocumentCategory): string  { return CATEGORY_ICONS[cat]  ?? '📁'; }

  getMimeIcon(mime: string): string {
    if (mime?.includes('pdf'))   return '📄';
    if (mime?.includes('sheet')) return '📊';
    if (mime?.includes('word'))  return '📝';
    if (mime?.includes('image')) return '🖼';
    if (mime?.includes('zip'))   return '📦';
    return '📎';
  }

  getMimeClass(mime: string): string {
    if (mime?.includes('pdf'))   return 'mime-pdf';
    if (mime?.includes('sheet')) return 'mime-xlsx';
    if (mime?.includes('word'))  return 'mime-docx';
    if (mime?.includes('image')) return 'mime-img';
    return '';
  }

  getMimeLabel(mime: string): string {
    if (mime?.includes('pdf'))   return 'PDF';
    if (mime?.includes('sheet')) return 'XLSX';
    if (mime?.includes('word'))  return 'DOCX';
    if (mime?.includes('image')) return 'IMG';
    if (mime?.includes('zip'))   return 'ZIP';
    return mime?.split('/')[1]?.toUpperCase() ?? 'FILE';
  }

  formatSize(bytes: number): string {
    if (!bytes) return '—';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1024*1024)  return `${(bytes/1024).toFixed(0)} KB`;
    return `${(bytes/1024/1024).toFixed(1)} MB`;
  }

  trackById(_: number, doc: CaseDocument): number { return doc.id; }

  // Importar iconos para el template
  get CATEGORY_ICONS() { return CATEGORY_ICONS; }
}

// Importar aquí para usar en el template
import { CATEGORY_ICONS } from '../../../core/models/document.model';
