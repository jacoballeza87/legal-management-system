// src/app/features/cases/case-kanban/case-kanban.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { CasesActions } from '../../../store/cases/actions/cases.actions';
import { selectCasesByStatus, selectLoading } from '../../../store/cases/selectors/cases.selectors';
import { Case, CaseStatus } from '../../../core/models/case.model';

interface KanbanColumn {
  id: CaseStatus;
  label: string;
  icon: string;
  color: string;
  cases: Case[];
}

@Component({
  selector: 'app-case-kanban',
  template: `
    <div class="kanban-wrap">
      <div *ngFor="let col of columns"
           class="kanban-col"
           [id]="'col-' + col.id"
           (dragover)="onDragOver($event)"
           (dragleave)="onDragLeave($event)"
           (drop)="onDrop($event, col.id)">

        <!-- Column header -->
        <div class="kanban-header">
          <div class="col-title" [style.color]="col.color">
            {{ col.icon }} {{ col.label }}
          </div>
          <span class="col-count">{{ col.cases.length }}</span>
        </div>

        <!-- Cards -->
        <div class="kanban-body">
          <div *ngFor="let c of col.cases; trackBy: trackById"
               class="kanban-card"
               [class.dragging]="draggingId === c.id"
               draggable="true"
               (dragstart)="onDragStart($event, c)"
               (dragend)="onDragEnd($event)"
               (click)="goToDetail(c.id)">

            <!-- Priority bar -->
            <div class="priority-bar priority-{{ c.priority | lowercase }}"></div>

            <div class="card-num">{{ c.caseNumber }}</div>
            <div class="card-title">{{ c.title }}</div>
            <div class="card-client">{{ c.clientName }}</div>

            <div class="card-tags">
              <span class="tag type-{{ c.caseType | lowercase }}">{{ c.caseType }}</span>
              <span class="tag priority-{{ c.priority | lowercase }}">{{ c.priority }}</span>
            </div>

            <div class="card-footer">
              <div class="card-avatar" [title]="c.assignedLawyerName || ''">
                {{ getInitials(c.assignedLawyerName) }}
              </div>
              <span class="card-due"
                    [class.overdue]="isOverdue(c.dueDate)"
                    [class.near-due]="isNearDue(c.dueDate)">
                {{ c.dueDate | date:'dd/MM' }}
              </span>
            </div>
          </div>

          <!-- Drop zone indicator -->
          <div class="drop-zone" [class.visible]="isDragOver(col.id)">
            Soltar aquí
          </div>

          <!-- Empty column -->
          <div class="col-empty" *ngIf="col.cases.length === 0">
            No hay casos {{ col.label | lowercase }}
          </div>
        </div>

        <button class="add-card-btn" routerLink="/cases/new">
          + Agregar caso
        </button>
      </div>
    </div>
  `
})
export class CaseKanbanComponent implements OnInit {
  columns: KanbanColumn[] = [];
  draggingId: number | null = null;
  draggingCase: Case | null = null;
  dragOverColId: CaseStatus | null = null;

  private readonly colDefs = [
    { id: 'ABIERTO'      as CaseStatus, label: 'Abierto',     icon: '📂', color: '#4A90D9' },
    { id: 'EN_REVISION'  as CaseStatus, label: 'En Revisión', icon: '🔄', color: '#F39C12' },
    { id: 'ACTIVO'       as CaseStatus, label: 'Activo',      icon: '⚡', color: '#2ECC71' },
    { id: 'CERRADO'      as CaseStatus, label: 'Cerrado',     icon: '✅', color: '#6B7A94' },
  ];

  constructor(private store: Store, private router: Router) {}

  ngOnInit(): void {
    this.store.select(selectCasesByStatus).subscribe(grouped => {
      this.columns = this.colDefs.map(def => ({
        ...def,
        cases: (grouped as any)[def.id] ?? []
      }));
    });
  }

  // ── Drag & Drop ────────────────────────────────────────────────────

  onDragStart(event: DragEvent, c: Case): void {
    this.draggingId = c.id;
    this.draggingCase = c;
    event.dataTransfer?.setData('text/plain', String(c.id));
    (event.currentTarget as HTMLElement).classList.add('dragging');
  }

  onDragEnd(event: DragEvent): void {
    this.draggingId = null;
    this.draggingCase = null;
    this.dragOverColId = null;
    (event.currentTarget as HTMLElement).classList.remove('dragging');
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    const col = this.getColumnFromEvent(event);
    if (col) this.dragOverColId = col;
  }

  onDragLeave(event: DragEvent): void {
    const related = event.relatedTarget as HTMLElement;
    const colEl = (event.currentTarget as HTMLElement);
    if (!colEl.contains(related)) {
      this.dragOverColId = null;
    }
  }

  onDrop(event: DragEvent, newStatus: CaseStatus): void {
    event.preventDefault();
    this.dragOverColId = null;
    if (!this.draggingCase || this.draggingCase.status === newStatus) return;

    this.store.dispatch(CasesActions.moveCaseStatus({
      id: this.draggingCase.id,
      newStatus
    }));
  }

  isDragOver(colId: CaseStatus): boolean {
    return this.dragOverColId === colId && this.draggingCase?.status !== colId;
  }

  private getColumnFromEvent(event: DragEvent): CaseStatus | null {
    const el = (event.currentTarget as HTMLElement);
    const colId = el.id.replace('col-', '');
    return this.colDefs.find(c => c.id === colId)?.id ?? null;
  }

  // ── Helpers ────────────────────────────────────────────────────────

  goToDetail(id: number): void {
    if (!this.draggingId) this.router.navigate(['/cases', id]);
  }

  getInitials(name?: string): string {
    if (!name) return '?';
    return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
  }

  isOverdue(dueDate?: string): boolean {
    return !!dueDate && new Date(dueDate) < new Date();
  }

  isNearDue(dueDate?: string): boolean {
    if (!dueDate) return false;
    const diff = new Date(dueDate).getTime() - Date.now();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000;
  }

  trackById(_: number, c: Case): number { return c.id; }
}
