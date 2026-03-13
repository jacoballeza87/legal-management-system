// src/app/features/documents/document-upload/document-upload.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { v4 as uuidv4 } from 'uuid';
import { DocumentsActions } from '../../../store/documents/actions/documents.actions';
import {
  selectUploadQueue, selectIsUploading, selectAllDone, selectError
} from '../../../store/documents/selectors/documents.selectors';
import {
  UploadQueueItem, DocumentCategory, CATEGORY_LABELS,
  ALLOWED_MIME_TYPES, MAX_FILE_SIZE_BYTES
} from '../../../core/models/document.model';

@Component({
  selector: 'app-document-upload',
  template: `
    <div class="upload-page">
      <button class="back-btn" routerLink="/documents">← Volver al repositorio</button>

      <div class="page-header">
        <div>
          <h1>Subir Documentos</h1>
          <p class="subtitle">Drag & drop o selecciona — máximo 50 MB por archivo · validación MIME con Apache Tika</p>
        </div>
      </div>

      <!-- Zona de drop -->
      <app-file-upload (filesSelected)="onFilesSelected($event)"></app-file-upload>

      <!-- Formulario de metadatos (aparece al agregar archivos) -->
      <div class="card" *ngIf="(queue$ | async)?.length">
        <div class="card-header">
          <div class="card-title">📋 Metadatos del lote</div>
          <span class="meta-hint">Se aplica a todos los archivos — puedes cambiar individualmente</span>
        </div>
        <form [formGroup]="metaForm" class="meta-grid">
          <div class="form-group">
            <label>Caso asociado *</label>
            <select formControlName="caseId">
              <option value="">Seleccionar caso...</option>
              <option value="3">CASE-2024-00003 — Disputa laboral</option>
              <option value="2">CASE-2024-00002 — Fraude financiero</option>
              <option value="8">CASE-2024-00008 — Estafa inmobiliaria</option>
            </select>
          </div>
          <div class="form-group">
            <label>Categoría *</label>
            <select formControlName="category">
              <option *ngFor="let c of categories" [value]="c">{{ getCategoryLabel(c) }}</option>
            </select>
          </div>
          <div class="form-group full">
            <label>Descripción (opcional)</label>
            <input type="text" formControlName="description"
                   placeholder="Ej: Documentos de la audiencia del 15/01/2025">
          </div>
          <div class="form-group full">
            <button type="button" class="btn btn-outline btn-sm"
                    (click)="applyMetaToAll()">
              Aplicar a todos los archivos
            </button>
          </div>
        </form>
      </div>

      <!-- Cola de archivos -->
      <div class="upload-queue" *ngIf="(queue$ | async) as queue">
        <div *ngFor="let item of queue; trackBy: trackById" class="upload-item">
          <span class="item-icon">{{ getFileIcon(item.file.name) }}</span>

          <div class="item-info">
            <div class="item-name">{{ item.file.name }}</div>
            <div class="item-size">{{ formatSize(item.file.size) }} · {{ getFileExt(item.file.name) }}</div>

            <!-- Selects individuales -->
            <div class="item-meta" *ngIf="item.status === 'pending'">
              <select [value]="item.caseId || ''"
                      (change)="setItemMeta(item.localId, 'caseId', $any($event.target).value)"
                      class="item-select">
                <option value="">Caso...</option>
                <option value="3">CASE-2024-00003</option>
                <option value="2">CASE-2024-00002</option>
                <option value="8">CASE-2024-00008</option>
              </select>
              <select [value]="item.category || 'OTRO'"
                      (change)="setItemMeta(item.localId, 'category', $any($event.target).value)"
                      class="item-select">
                <option *ngFor="let c of categories" [value]="c">{{ getCategoryLabel(c) }}</option>
              </select>
            </div>

            <!-- Barra de progreso -->
            <div class="progress-wrap" *ngIf="item.status !== 'pending'">
              <div class="progress-bar">
                <div class="progress-fill"
                     [ngClass]="item.status"
                     [style.width.%]="item.progress">
                </div>
              </div>
              <span class="progress-label" [ngClass]="'label-' + item.status">
                {{ getStatusLabel(item) }}
              </span>
            </div>
          </div>

          <button class="remove-btn"
                  [disabled]="item.status === 'uploading' || item.status === 'done'"
                  (click)="removeItem(item.localId)">✕</button>
        </div>
      </div>

      <!-- Error global -->
      <div class="error-banner" *ngIf="error$ | async as error">⚠ {{ error }}</div>

      <!-- Acciones -->
      <div class="upload-actions" *ngIf="(queue$ | async)?.length">
        <button class="btn btn-outline" (click)="clearAll()">Limpiar todo</button>
        <div style="flex:1"></div>

        <!-- Todos terminaron -->
        <ng-container *ngIf="allDone$ | async">
          <span class="all-done-badge">✓ Todos los archivos subidos</span>
          <button class="btn btn-gold" routerLink="/documents">Ver repositorio →</button>
        </ng-container>

        <!-- Aún hay pendientes -->
        <ng-container *ngIf="!(allDone$ | async)">
          <span class="queue-summary" *ngIf="queue$ | async as q">
            {{ q.filter(i=>i.status==='pending').length }} archivos pendientes
          </span>
          <button class="btn btn-gold"
                  [disabled]="isUploading$ | async"
                  (click)="uploadAll()">
            <span *ngIf="isUploading$ | async">⬆ Subiendo...</span>
            <span *ngIf="!(isUploading$ | async)">⬆ Subir Todo</span>
          </button>
        </ng-container>
      </div>

    </div>
  `
})
export class DocumentUploadComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  queue$       = this.store.select(selectUploadQueue);
  isUploading$ = this.store.select(selectIsUploading);
  allDone$     = this.store.select(selectAllDone);
  error$       = this.store.select(selectError);

  categories: DocumentCategory[] = [
    'CONTRATO','DEMANDA','SENTENCIA','EVIDENCIA',
    'CORRESPONDENCIA','FACTURA','PODER_NOTARIAL','IDENTIFICACION','OTRO'
  ];

  metaForm = new FormGroup({
    caseId:      new FormControl('', [Validators.required]),
    category:    new FormControl<DocumentCategory>('OTRO', [Validators.required]),
    description: new FormControl(''),
  });

  constructor(private store: Store, private router: Router) {}

  ngOnInit(): void {}
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  onFilesSelected(files: File[]): void {
    const items: UploadQueueItem[] = files.map(file => ({
      localId:  uuidv4(),
      file,
      caseId:   this.metaForm.value.caseId ? +this.metaForm.value.caseId : undefined,
      category: (this.metaForm.value.category as DocumentCategory) || 'OTRO',
      description: this.metaForm.value.description || undefined,
      progress: 0,
      status:   'pending',
    }));
    this.store.dispatch(DocumentsActions.addToQueue({ items }));
  }

  applyMetaToAll(): void {
    if (this.metaForm.invalid) return;
    this.store.select(selectUploadQueue).pipe(takeUntil(this.destroy$)).subscribe(queue => {
      queue.filter(i => i.status === 'pending').forEach(i => {
        this.store.dispatch(DocumentsActions.setQueueMeta({
          localId:     i.localId,
          caseId:      +this.metaForm.value.caseId!,
          category:    this.metaForm.value.category as string,
          description: this.metaForm.value.description || undefined,
        }));
      });
    });
  }

  setItemMeta(localId: string, field: string, value: string): void {
    if (field === 'caseId') {
      this.store.dispatch(DocumentsActions.setQueueMeta({
        localId, caseId: +value, category: 'OTRO'
      }));
    } else if (field === 'category') {
      this.store.dispatch(DocumentsActions.setQueueMeta({
        localId, caseId: 0, category: value
      }));
    }
  }

  removeItem(localId: string): void {
    this.store.dispatch(DocumentsActions.removeFromQueue({ localId }));
  }

  clearAll(): void {
    this.store.dispatch(DocumentsActions.clearQueue());
  }

  uploadAll(): void {
    this.store.dispatch(DocumentsActions.uploadAll());
  }

  // ── Helpers ─────────────────────────────────────────────────────────
  getCategoryLabel(cat: DocumentCategory): string { return CATEGORY_LABELS[cat] ?? cat; }

  getFileIcon(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase() ?? '';
    const icons: Record<string, string> = {
      pdf:'📄', docx:'📝', doc:'📝', xlsx:'📊', xls:'📊',
      png:'🖼', jpg:'🖼', jpeg:'🖼', zip:'📦', rar:'📦', txt:'📋'
    };
    return icons[ext] ?? '📎';
  }

  getFileExt(filename: string): string {
    return filename.split('.').pop()?.toUpperCase() ?? 'FILE';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1024*1024)  return `${(bytes/1024).toFixed(0)} KB`;
    return `${(bytes/1024/1024).toFixed(1)} MB`;
  }

  getStatusLabel(item: UploadQueueItem): string {
    switch (item.status) {
      case 'uploading':   return `Subiendo... ${item.progress}%`;
      case 'done':        return '✓ Completado';
      case 'error':       return `✗ ${item.errorMessage ?? 'Error'}`;
      case 'validating':  return 'Validando tipo MIME...';
      default:            return 'Pendiente';
    }
  }

  trackById(_: number, item: UploadQueueItem): string { return item.localId; }
}
