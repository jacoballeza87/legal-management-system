// src/app/features/cases/case-detail/case-detail.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { CasesActions } from '../../../store/cases/actions/cases.actions';
import {
  selectSelectedCase, selectLoadingDetail, selectError,
  selectVersionsByCaseId
} from '../../../store/cases/selectors/cases.selectors';
import { CreateVersionRequest } from '../../../core/models/case.model';

@Component({
  selector: 'app-case-detail',
  template: `
    <div class="detail-page" *ngIf="!(loadingDetail$ | async); else loader">

      <button class="back-btn" routerLink="/cases">← Volver a casos</button>

      <ng-container *ngIf="case$ | async as c">

        <!-- Header -->
        <div class="page-header">
          <div>
            <div class="case-num-label">{{ c.caseNumber }}</div>
            <h1>{{ c.title }}</h1>
            <div class="header-pills">
              <span class="pill status-{{ c.status | lowercase | replace:'_':'-' }}">
                <span class="dot"></span>{{ c.status }}
              </span>
              <span class="pill priority-{{ c.priority | lowercase }}">
                🔺 {{ c.priority }}
              </span>
              <span class="pill type-{{ c.caseType | lowercase }}">{{ c.caseType }}</span>
              <span class="tag">📎 Documentos</span>
              <span class="tag">👥 {{ c.collaborators?.length || 0 }} colaboradores</span>
            </div>
          </div>
          <div class="header-actions">
            <button class="icon-btn" title="Compartir">🔗</button>
            <button class="icon-btn" title="Exportar PDF" (click)="exportPdf(c.id)">🖨️</button>
            <button class="btn btn-outline" (click)="showVersionModal = true">+ Nueva versión</button>
            <button class="btn btn-gold" [routerLink]="['/cases', c.id, 'edit']">✏️ Editar</button>
          </div>
        </div>

        <div class="detail-grid">

          <!-- MAIN COLUMN -->
          <div class="detail-main">

            <!-- Info general -->
            <div class="card">
              <div class="card-header">
                <div class="card-title">📋 Información del Caso</div>
                <span class="meta-date">Creado: {{ c.createdAt | date:'dd/MM/yyyy' }}</span>
              </div>
              <div class="info-grid">
                <div class="info-item"><label>Cliente</label><span>{{ c.clientName }}</span></div>
                <div class="info-item"><label>Abogado Principal</label><span>{{ c.assignedLawyerName }}</span></div>
                <div class="info-item"><label>Juzgado</label><span>{{ c.courtName || '—' }}</span></div>
                <div class="info-item"><label>Fecha apertura</label><span>{{ c.createdAt | date:'dd/MM/yyyy' }}</span></div>
                <div class="info-item">
                  <label>Vencimiento</label>
                  <span [class.overdue]="isOverdue(c.dueDate)" [class.near-due]="isNearDue(c.dueDate)">
                    {{ c.dueDate ? (c.dueDate | date:'dd/MM/yyyy') : '—' }}
                  </span>
                </div>
                <div class="info-item"><label>Expediente externo</label><span>{{ c.externalExpedientNumber || '—' }}</span></div>
              </div>
              <div class="description-block">
                <label>Descripción</label>
                <p>{{ c.description }}</p>
              </div>
            </div>

            <!-- Versiones -->
            <div class="card">
              <div class="card-header">
                <div class="card-title">🕐 Historial de Versiones</div>
                <button class="btn btn-outline btn-sm" (click)="showVersionModal = true">+ Nueva versión</button>
              </div>
              <div class="timeline">
                <div *ngFor="let v of versions$ | async; trackBy: trackById" class="tl-item">
                  <div class="tl-dot" [ngClass]="v.status | lowercase">
                    {{ v.status === 'APPROVED' ? '✓' : v.status === 'REVIEW' ? '⟳' : '○' }}
                  </div>
                  <div class="tl-content">
                    <div class="tl-header">
                      <span class="tl-version">Versión {{ v.versionNumber }}</span>
                      <span class="pill" [ngClass]="'status-' + (v.status | lowercase)">{{ v.status }}</span>
                      <span class="tl-date">{{ v.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                    </div>
                    <div class="tl-author">👤 {{ v.createdByName }}</div>
                    <div class="tl-changes">{{ v.changes }}</div>
                    <div class="tl-comment" *ngIf="v.lawyerComment">
                      💬 "{{ v.lawyerComment }}"
                    </div>
                  </div>
                </div>
                <div class="empty-state" *ngIf="(versions$ | async)?.length === 0">
                  No hay versiones registradas aún
                </div>
              </div>
            </div>

            <!-- Relaciones -->
            <div class="card" *ngIf="c.relations?.length">
              <div class="card-header">
                <div class="card-title">🔗 Casos Relacionados</div>
              </div>
              <div class="relations-list">
                <div *ngFor="let r of c.relations" class="relation-item"
                     [routerLink]="['/cases', r.targetCaseId]">
                  <span class="rel-num">{{ r.targetCaseNumber }}</span>
                  <span class="rel-title">{{ r.targetCaseTitle }}</span>
                  <span class="rel-type tag">{{ r.relationType }}</span>
                  <span class="pill status-{{ r.targetCaseStatus?.toLowerCase() | replace:'_':'-' }}">
                    {{ r.targetCaseStatus }}
                  </span>
                </div>
              </div>
            </div>

          </div>

          <!-- SIDEBAR -->
          <div class="detail-side">

            <!-- QR Code -->
            <div class="card qr-card">
              <div class="card-title" style="text-align:center;margin-bottom:14px">📱 QR del Caso</div>
              <div class="qr-wrap">
                <img *ngIf="c.qrCodeBase64" [src]="'data:image/png;base64,' + c.qrCodeBase64"
                     alt="QR Code" class="qr-image" />
                <div *ngIf="!c.qrCodeBase64" class="qr-placeholder">
                  <span>QR</span>
                </div>
              </div>
              <p class="qr-hint">Escanea para acceso rápido</p>
              <div class="qr-actions">
                <button class="btn btn-outline btn-sm" (click)="downloadQr(c)">⬇ Descargar</button>
                <button class="btn btn-outline btn-sm">🖨 Imprimir</button>
              </div>
            </div>

            <!-- Colaboradores -->
            <div class="card">
              <div class="card-header">
                <div class="card-title">👥 Colaboradores</div>
              </div>
              <div class="collab-list">
                <div *ngFor="let col of c.collaborators" class="collab-item">
                  <div class="collab-avatar">{{ getInitials(col.userName) }}</div>
                  <div class="collab-info">
                    <div class="collab-name">{{ col.userName }}</div>
                    <div class="collab-role">{{ col.userRole }} · {{ col.collaboratorRole }}</div>
                  </div>
                </div>
                <div class="empty-state" *ngIf="!c.collaborators?.length">
                  Sin colaboradores asignados
                </div>
              </div>
            </div>

          </div>
        </div>

      </ng-container>

      <!-- Error state -->
      <div class="error-state" *ngIf="error$ | async as error">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button class="btn btn-outline" routerLink="/cases">Volver a la lista</button>
      </div>

    </div>

    <!-- Loading skeleton -->
    <ng-template #loader>
      <div class="loading-state">
        <div class="skeleton-header"></div>
        <div class="skeleton-body"></div>
      </div>
    </ng-template>

    <!-- MODAL nueva versión -->
    <div class="modal-overlay" [class.open]="showVersionModal" (click)="closeVersionModal($event)">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="modal-title">Nueva Versión</div>
          <button class="modal-close" (click)="showVersionModal = false">✕</button>
        </div>
        <form [formGroup]="versionForm" (ngSubmit)="submitVersion()">
          <div class="form-group">
            <label>Estado</label>
            <select formControlName="status">
              <option value="DRAFT">BORRADOR</option>
              <option value="REVIEW">EN REVISIÓN</option>
              <option value="APPROVED">APROBADA</option>
            </select>
          </div>
          <div class="form-group">
            <label>Cambios en esta versión *</label>
            <textarea formControlName="changes" placeholder="Describe qué cambió..."></textarea>
          </div>
          <div class="form-group">
            <label>Comentario (opcional)</label>
            <textarea formControlName="lawyerComment" placeholder="Observaciones..."></textarea>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline" (click)="showVersionModal = false">Cancelar</button>
            <button type="submit" class="btn btn-gold"
                    [disabled]="versionForm.invalid">Guardar Versión</button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CaseDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private caseId!: number;

  case$          = this.store.select(selectSelectedCase);
  loadingDetail$ = this.store.select(selectLoadingDetail);
  error$         = this.store.select(selectError);
  versions$      = this.store.select(selectVersionsByCaseId(0)); // se actualiza en ngOnInit

  showVersionModal = false;

  versionForm = new FormGroup({
    status:         new FormControl('DRAFT', [Validators.required]),
    changes:        new FormControl('',      [Validators.required, Validators.minLength(10)]),
    lawyerComment:  new FormControl('')
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.caseId = +params['id'];
      this.store.dispatch(CasesActions.loadCase({ id: this.caseId }));
      this.store.dispatch(CasesActions.loadVersions({ caseId: this.caseId }));
      this.versions$ = this.store.select(selectVersionsByCaseId(this.caseId));
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  submitVersion(): void {
    if (this.versionForm.invalid) return;
    const request: CreateVersionRequest = {
      status:        this.versionForm.value.status as any,
      changes:       this.versionForm.value.changes!,
      lawyerComment: this.versionForm.value.lawyerComment || undefined
    };
    this.store.dispatch(CasesActions.createVersion({ caseId: this.caseId, request }));
    this.showVersionModal = false;
    this.versionForm.reset({ status: 'DRAFT' });
  }

  closeVersionModal(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.showVersionModal = false;
    }
  }

  exportPdf(id: number): void {
    console.log('Exportar PDF del caso', id);
  }

  downloadQr(c: any): void {
    if (!c.qrCodeBase64) return;
    const a = document.createElement('a');
    a.href = 'data:image/png;base64,' + c.qrCodeBase64;
    a.download = `QR_${c.caseNumber}.png`;
    a.click();
  }

  isOverdue(dueDate?: string): boolean {
    return !!dueDate && new Date(dueDate) < new Date();
  }

  isNearDue(dueDate?: string): boolean {
    if (!dueDate) return false;
    const diff = new Date(dueDate).getTime() - Date.now();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000;
  }

  getInitials(name?: string): string {
    if (!name) return '?';
    return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
  }

  trackById(_: number, item: any): number { return item.id; }
}
