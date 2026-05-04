import { createReducer, on, createFeatureSelector, createSelector } from '@ngrx/store';
import * as A from './dashboard.actions';

export interface DashboardState {
  stats:         any | null;
  recentCases:   any[];
  notifications: any[];
  activity:      any[];
  casesByStatus: Record<string, number>;
  loading:       boolean;
  error:         string | null;
}

const initialState: DashboardState = {
  stats:         null,
  recentCases:   [],
  notifications: [],
  activity:      [],
  casesByStatus: {},
  loading:       false,
  error:         null,
};

export const dashboardReducer = createReducer(
  initialState,
  on(A.loadDashboard, A.refreshDashboard, state => ({ ...state, loading: true, error: null })),
  on(A.loadDashboardSuccess, (state, { stats, recentCases, notifications, casesByStatus }) => ({
    ...state, loading: false, stats, recentCases, notifications, casesByStatus
  })),
  on(A.loadActivityFeedSuccess, (state, { activity }) => ({ ...state, activity })),
  on(A.loadDashboardFailure, (state, { error }) => ({ ...state, loading: false, error })),
);

// ── Selectors ─────────────────────────────────────────────────────────────────
const selectFeature = createFeatureSelector<DashboardState>('dashboard');

export const selectStats         = createSelector(selectFeature, s => s.stats);
export const selectRecentCases   = createSelector(selectFeature, s => s.recentCases);
export const selectNotifications = createSelector(selectFeature, s => s.notifications);
export const selectActivity      = createSelector(selectFeature, s => s.activity);
export const selectLoading       = createSelector(selectFeature, s => s.loading);
export const selectError         = createSelector(selectFeature, s => s.error);
export const selectCasesByStatus = createSelector(selectFeature, s => s.casesByStatus);
