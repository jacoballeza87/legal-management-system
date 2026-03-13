import {
  Component, OnInit, OnDestroy, ChangeDetectionStrategy
} from '@angular/core';
import { Store } from '@ngrx/store';
import { Subject, interval, takeUntil } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import * as A from './store/dashboard.actions';
import {
  selectStats, selectRecentCases, selectNotifications,
  selectActivity, selectLoading, selectError, selectCasesByStatus
} from './store/dashboard.reducer';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit, OnDestroy {

  // Streams
  stats$         = this.store.select(selectStats);
  recentCases$   = this.store.select(selectRecentCases);
  notifications$ = this.store.select(selectNotifications);
  activity$      = this.store.select(selectActivity);
  loading$       = this.store.select(selectLoading);
  error$         = this.store.select(selectError);
  byStatus$      = this.store.select(selectCasesByStatus);

  currentUser    = this.authService.currentUser;
  greeting       = this.getGreeting();
  currentDate    = new Date();

  private destroy$ = new Subject<void>();

  constructor(
    private store: Store,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.store.dispatch(A.loadDashboard());
    this.store.dispatch(A.loadActivityFeed());

    // Auto-refresh every 5 minutes
    interval(5 * 60 * 1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.store.dispatch(A.refreshDashboard()));
  }

  refresh(): void {
    this.store.dispatch(A.refreshDashboard());
    this.store.dispatch(A.loadActivityFeed());
  }

  private getGreeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 18) return 'Buenas tardes';
    return 'Buenas noches';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'Abierto', IN_PROGRESS: 'En progreso',
      PENDING_REVIEW: 'En revisión', ON_HOLD: 'En espera',
      CLOSED: 'Cerrado', ARCHIVED: 'Archivado', CANCELLED: 'Cancelado'
    };
    return map[status] ?? status;
  }

  getPriorityLabel(p: string): string {
    const map: Record<string, string> = {
      LOW: 'Baja', MEDIUM: 'Media', HIGH: 'Alta',
      URGENT: 'Urgente', CRITICAL: 'Crítico'
    };
    return map[p] ?? p;
  }

  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      OPEN: '#3b82f6', IN_PROGRESS: '#d4af37', PENDING_REVIEW: '#a855f7',
      ON_HOLD: '#94a3b8', CLOSED: '#22c55e', CANCELLED: '#ef4444'
    };
    return map[status] ?? '#64748b';
  }

  getPriorityColor(p: string): string {
    const map: Record<string, string> = {
      LOW: '#22c55e', MEDIUM: '#3b82f6', HIGH: '#f59e0b',
      URGENT: '#f97316', CRITICAL: '#ef4444'
    };
    return map[p] ?? '#94a3b8';
  }

  getActivityIcon(type: string): string {
    const map: Record<string, string> = {
      case_created: '📁', case_updated: '✏️',
      version_added: '📄', collaborator_added: '👤', status_changed: '🔄'
    };
    return map[type] ?? '•';
  }

  sidebarCollapsed = false;
  notifOpen = false;

  onLogout(): void {
    this.authService.logout();
  }

  // ─── Donut chart segments ─────────────────────────────────────────────────
  private readonly STATUS_COLORS: Record<string, string> = {
    OPEN: '#3b82f6', IN_PROGRESS: '#d4af37', PENDING_REVIEW: '#a855f7',
    ON_HOLD: '#94a3b8', CLOSED: '#22c55e', ARCHIVED: '#64748b', CANCELLED: '#ef4444'
  };

  private readonly STATUS_LABELS: Record<string, string> = {
    OPEN: 'Abierto', IN_PROGRESS: 'En progreso', PENDING_REVIEW: 'Revisión',
    ON_HOLD: 'En espera', CLOSED: 'Cerrado', ARCHIVED: 'Archivado', CANCELLED: 'Cancelado'
  };

  buildDonut(byStatus: Record<string, number> | undefined): any[] {
    if (!byStatus) return [];
    const total = Object.values(byStatus).reduce((a, b) => a + b, 0);
    if (total === 0) return [];
    const circumference = 2 * Math.PI * 45; // r=45
    let offset = 0;
    return Object.entries(byStatus)
      .filter(([, v]) => v > 0)
      .map(([key, value]) => {
        const pct  = value / total;
        const dash = `${pct * circumference} ${circumference}`;
        const seg  = { id: key, color: this.STATUS_COLORS[key] ?? '#64748b', dash, offset: -offset * circumference, rotate: '' };
        offset += pct;
        return seg;
      });
  }

  getLegend(byStatus: Record<string, number> | undefined): any[] {
    if (!byStatus) return [];
    const total = Object.values(byStatus).reduce((a, b) => a + b, 0);
    return Object.entries(byStatus)
      .filter(([, v]) => v > 0)
      .map(([key, value]) => ({
        id: key,
        label: this.STATUS_LABELS[key] ?? key,
        color: this.STATUS_COLORS[key] ?? '#64748b',
        value,
        pct: total > 0 ? Math.round((value / total) * 100) : 0
      }));
  }

  getPriorityBars(byPriority: Record<string, number> | undefined): any[] {
    if (!byPriority) return [];
    const colors: Record<string, string> = {
      CRITICAL: '#ef4444', URGENT: '#f97316', HIGH: '#f59e0b',
      MEDIUM: '#3b82f6', LOW: '#22c55e'
    };
    const labels: Record<string, string> = {
      CRITICAL: 'Crítico', URGENT: 'Urgente', HIGH: 'Alta', MEDIUM: 'Media', LOW: 'Baja'
    };
    const max = Math.max(...Object.values(byPriority), 1);
    const order = ['CRITICAL', 'URGENT', 'HIGH', 'MEDIUM', 'LOW'];
    return order
      .filter(k => (byPriority[k] ?? 0) >= 0)
      .map(k => ({
        id: k, label: labels[k], color: colors[k],
        value: byPriority[k] ?? 0,
        pct: Math.round(((byPriority[k] ?? 0) / max) * 100)
      }));
  }

  trackById(_: number, item: any): number { return item.id; }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
