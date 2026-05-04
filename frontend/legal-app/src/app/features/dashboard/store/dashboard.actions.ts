import { createAction, props } from '@ngrx/store';

export const loadDashboard = createAction('[Dashboard] Load Dashboard');
export const refreshDashboard = createAction('[Dashboard] Refresh Dashboard');
export const loadActivityFeed = createAction('[Dashboard] Load Activity Feed');

export const loadDashboardSuccess = createAction(
  '[Dashboard] Load Dashboard Success',
  props<{
    stats: any;
    recentCases: any[];
    notifications: any[];
    casesByStatus: Record<string, number>;
  }>()
);

export const loadActivityFeedSuccess = createAction(
  '[Dashboard] Load Activity Feed Success',
  props<{ activity: any[] }>()
);

export const loadDashboardFailure = createAction(
  '[Dashboard] Load Dashboard Failure',
  props<{ error: string }>()
);
