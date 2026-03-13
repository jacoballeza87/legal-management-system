import { createReducer, on, createFeatureSelector, createSelector } from '@ngrx/store';
import { CaseStats, Case, Notification } from '../../../core/models/index';
import { ActivityItem } from '../dashboard.service';
import * as A from './dashboard.actions';

export interface DashboardState {
  stats:         CaseStats | null;
  recentCases:   Case[];
  notifications: Notification[];
  activity:      ActivityItem[];
  loading:       boolean;
  error:         string | null;
  lastLoaded:    string | null;
}

const initial: DashboardState = {
  stats: null, recentCases: [], notifications: [],
  activity: [], loading: false, error: null, lastLoaded: null
};

export const dashboardReducer = createReducer(
  initial,
  on(A.loadDashboard, A.refreshDashboard, s => ({ ...s, loading: true, error: null })),
  on(A.loadDashboardSuccess, (s, { stats, recentCases, notifications }) => ({
    ...s, loading: false, stats, recentCases, notifications,
    lastLoaded: new Date().toISOString()
  })),
  on(A.loadDashboardFailure, (s, { error }) => ({ ...s, loading: false, error })),
  on(A.loadActivityFeedSuccess, (s, { items }) => ({ ...s, activity: items }))
);

// ── Selectors ─────────────────────────────────────────────────────────────────
const selectState     = createFeatureSelector<DashboardState>('dashboard');
export const selectStats         = createSelector(selectState, s => s.stats);
export const selectRecentCases   = createSelector(selectState, s => s.recentCases);
export const selectNotifications = createSelector(selectState, s => s.notifications);
export const selectActivity      = createSelector(selectState, s => s.activity);
export const selectLoading       = createSelector(selectState, s => s.loading);
export const selectError         = createSelector(selectState, s => s.error);
export const selectLastLoaded    = createSelector(selectState, s => s.lastLoaded);

// Computed: casos por status para el kanban mini
export const selectCasesByStatus = createSelector(selectRecentCases, cases => ({
  OPEN:           cases.filter(c => c.status === 'OPEN').length,
  IN_PROGRESS:    cases.filter(c => c.status === 'IN_PROGRESS').length,
  PENDING_REVIEW: cases.filter(c => c.status === 'PENDING_REVIEW').length,
  CLOSED:         cases.filter(c => c.status === 'CLOSED').length,
}));
