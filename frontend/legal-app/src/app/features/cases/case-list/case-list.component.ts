// src/app/features/cases/case-list/case-list.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject, combineLatest } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FormControl } from '@angular/forms';
import { CasesActions } from '../../../store/cases/actions/cases.actions';
import {
  selectAllCases, selectStats, selectLoading, selectFilters,
  selectTotalElements, selectTotalPages, selectViewMode, selectError
} from '../../../store/cases/selectors/cases.selectors';
import { CaseStatus, CasePriority, CaseType } from '../../../core/models/case.model';

@Component({
  selector: 'app-case-list',
  template: `
    <div class="cases-page">

      <!-- Stat chips -->
      <div class="stat-chips" *ngIf="stats$ | async as stats">
        <div class="stat-chip">
          <span class="chip-icon">⚖️</span>
          <div><div class="chip-val">{{ stats.totalCases }}</div><div class="chip-lbl">Total</div></div>
        </div>
        <div class="stat-chip chip-open">
          <span class="chip-icon">🔵</span>
          <div><div class="chip-val">{{ stats.openCases }}</div><div class="chip-lbl">Abiertos</div></div>
        </div>
        <div class="stat-chip chip-review">
          <span class="chip-icon">🟡</span>
          <div><div class="chip-val">{{ stats.inReviewCases }}</div><div class="chip-lbl">En revisión</div></div>
        </div>
        <div class="stat-chip chip-urgent">
          <span class="chip-icon">🔴</span>
          <div><div class="chip-val">{{ stats.urgentCases }}</div><div class="chip-lbl">Urgentes</div></div>
        </div>
        <div class="stat-chip chip-closed">
          <span class="chip-icon">✅</span>
          <div><div class="chip-val">{{ stats.closedCases }}</div><div class="chip-lbl">Cerrados</div></div>
        </div>
      </div>

      <!-- Header -->
      <div class="page-header">
        <div>
          <h1>Casos</h1>
          <p class="subtitle">{{ (totalElements$ | async) || 0 }} expedientes en el sistema</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline" (click)="exportPdf()">⬇ Exportar</button>
          <button class="btn btn-gold" routerLink="/cases/new">+ Nuevo Caso</button>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input [formControl]="searchCtrl" placeholder="Buscar por número, título, cliente..." />
        </div>

        <select (change)="setFilter('status', $any($event.target).value)">
          <option value="">Todos los estados</option>
          <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
        </select>

        <select (change)="setFilter('priority', $any($event.target).value)">
          <option value="">Toda prioridad</option>
          <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
        </select>

        <select (change)="setFilter('caseType', $any($event.target).value)">
          <option value="">Todos los tipos</option>
          <option *ngFor="let t of types" [value]="t">{{ t }}</option>
        </select>

        <!-- Vista toggle -->
        <div class="view-toggle">
          <button [class.active]="(viewMode$ | async) === 'list'"
                  (click)="setViewMode('list')" title="Lista">☰</button>
          <button [class.active]="(viewMode$ | async) === 'kanban'"
                  (click)="setViewMode('kanban')" title="Kanban">⊞</button>
        </div>
      </div>

      <!-- Error state -->
      <div class="error-banner" *ngIf="error$ | async as error">
        ⚠ {{ error }}
        <button (click)="reload()">Reintentar</button>
      </div>

      <!-- Table -->
      <div class="table-wrap" *ngIf="(viewMode$ | async) === 'list'">

        <div class="loading-bar" *ngIf="loading$ | async"></div>

        <table>
          <thead>
            <tr>
              <th (click)="sort('caseNumber')">N° Caso <span class="sort-icon">↕</span></th>
              <th (click)="sort('title')">Título / Cliente <span class="sort-icon">↕</span></th>
              <th>Tipo</th>
              <th>Estado</th>
              <th (click)="sort('priority')">Prioridad <span class="sort-icon">↕</span></th>
              <th>Abogado</th>
              <th (click)="sort('dueDate')">Vencimiento <span class="sort-icon">↕</span></th>
              <th>Versión</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of cases$ | async; trackBy: trackById"
                (click)="goToDetail(c.id)"
                class="case-row">
              <td class="case-num">{{ c.caseNumber }}</td>
              <td class="case-title">
                {{ c.title }}
                <small>{{ c.clientName }}</small>
              </td>
              <td><span class="pill type-{{ c.caseType | lowercase }}">{{ c.caseType }}</span></td>
              <td><span class="pill status-{{ c.status | lowercase | replace:'_':'-' }}">
                <span class="dot"></span>{{ c.status }}
              </span></td>
              <td><span class="pill priority-{{ c.priority | lowercase }}">{{ c.priority }}</span></td>
              <td class="lawyer-cell">{{ c.assignedLawyerName }}</td>
              <td [class.overdue]="isOverdue(c.dueDate)" [class.near-due]="isNearDue(c.dueDate)">
                {{ c.dueDate | date:'dd/MM/yyyy' }}
              </td>
              <td><span class="tag">{{ c.currentVersion }}</span></td>
              <td class="actions-cell" (click)="$event.stopPropagation()">
                <button class="icon-btn" (click)="goToDetail(c.id)" title="Ver detalle">👁</button>
                <button class="icon-btn" (click)="editCase(c.id)" title="Editar">✏️</button>
                <button class="icon-btn danger"
                        *ngIf="canDelete()"
                        (click)="deleteCase(c.id, c.caseNumber)" title="Eliminar">🗑</button>
              </td>
            </tr>

            <!-- Empty state -->
            <tr *ngIf="(cases$ | async)?.length === 0 && !(loading$ | async)">
              <td colspan="9" class="empty-state">
                <div class="empty-icon">⚖️</div>
                <div>No se encontraron casos</div>
                <button class="btn btn-outline btn-sm" (click)="resetFilters()">Limpiar filtros</button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination">
          <span class="page-info">
            Mostrando {{ currentPageStart }} - {{ currentPageEnd }}
            de {{ (totalElements$ | async) || 0 }} casos
          </span>
          <div class="page-btns">
            <button class="page-btn" [disabled]="currentPage === 0"
                    (click)="goToPage(currentPage - 1)">‹</button>
            <button *ngFor="let p of pageNumbers"
                    class="page-btn" [class.current]="p === currentPage"
                    (click)="goToPage(p)">{{ p + 1 }}</button>
            <button class="page-btn" [disabled]="currentPage >= (totalPages$ | async)! - 1"
                    (click)="goToPage(currentPage + 1)">›</button>
          </div>
        </div>
      </div>

      <!-- Kanban redirect -->
      <app-case-kanban *ngIf="(viewMode$ | async) === 'kanban'"></app-case-kanban>

    </div>
  `
})
export class CaseListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  searchCtrl = new FormControl('');

  cases$         = this.store.select(selectAllCases);
  stats$         = this.store.select(selectStats);
  loading$       = this.store.select(selectLoading);
  error$         = this.store.select(selectError);
  totalElements$ = this.store.select(selectTotalElements);
  totalPages$    = this.store.select(selectTotalPages);
  viewMode$      = this.store.select(selectViewMode);

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  statuses: CaseStatus[]   = ['ABIERTO','EN_REVISION','ACTIVO','CERRADO'];
  priorities: CasePriority[] = ['URGENTE','ALTA','MEDIA','BAJA'];
  types: CaseType[]        = ['CIVIL','PENAL','LABORAL','MERCANTIL','FAMILIAR','ADMINISTRATIVO'];

  constructor(private store: Store, private router: Router) {}

  ngOnInit(): void {
    this.store.dispatch(CasesActions.loadStats());
    this.load();

    // Search con debounce
    this.searchCtrl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(val =>
      this.store.dispatch(CasesActions.setFilters({ filters: { search: val ?? undefined } }))
    );

    // Sincronizar paginación desde store
    this.store.select(selectFilters).pipe(takeUntil(this.destroy$))
      .subscribe(f => { this.currentPage = f.page; this.pageSize = f.size; });

    this.totalElements$.pipe(takeUntil(this.destroy$))
      .subscribe(t => this.totalElements = t);

    this.totalPages$.pipe(takeUntil(this.destroy$))
      .subscribe(t => this.totalPages = t);
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.store.select(selectFilters).pipe(takeUntil(this.destroy$)).subscribe(filters =>
      this.store.dispatch(CasesActions.loadCases({ filters }))
    );
  }

  reload(): void { this.load(); }

  setFilter(key: string, value: string): void {
    this.store.dispatch(CasesActions.setFilters({
      filters: { [key]: value || undefined } as any
    }));
  }

  resetFilters(): void {
    this.searchCtrl.setValue('');
    this.store.dispatch(CasesActions.resetFilters());
  }

  sort(field: string): void {
    this.store.select(selectFilters).pipe(takeUntil(this.destroy$)).subscribe(f => {
      const dir = f.sortBy === field && f.sortDir === 'asc' ? 'desc' : 'asc';
      this.store.dispatch(CasesActions.setFilters({ filters: { sortBy: field, sortDir: dir } }));
    });
  }

  goToPage(page: number): void {
    this.store.dispatch(CasesActions.setFilters({ filters: { page } }));
  }

  setViewMode(mode: 'list' | 'kanban'): void {
    this.store.dispatch(CasesActions.setViewMode({ mode }));
  }

  goToDetail(id: number): void { this.router.navigate(['/cases', id]); }
  editCase(id: number): void   { this.router.navigate(['/cases', id, 'edit']); }

  deleteCase(id: number, caseNumber: string): void {
    if (confirm(`¿Eliminar el caso ${caseNumber}? Esta acción no se puede deshacer.`)) {
      this.store.dispatch(CasesActions.deleteCase({ id }));
    }
  }

  exportPdf(): void {
    // Implementar exportación
    console.log('Exportar PDF');
  }

  canDelete(): boolean {
    // Verificar rol del usuario actual
    return true; // TODO: conectar con AuthState
  }

  isOverdue(dueDate?: string): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  isNearDue(dueDate?: string): boolean {
    if (!dueDate) return false;
    const due = new Date(dueDate);
    const now = new Date();
    const diff = due.getTime() - now.getTime();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000;
  }

  trackById(_: number, c: any): number { return c.id; }

  get pageNumbers(): number[] {
    const range = 5;
    const start = Math.max(0, this.currentPage - Math.floor(range / 2));
    const end = Math.min(this.totalPages, start + range);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  get currentPageStart(): number { return this.currentPage * this.pageSize + 1; }
  get currentPageEnd(): number   { return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements); }
}
