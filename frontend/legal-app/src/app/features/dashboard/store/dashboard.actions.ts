import { createAction, props } from '@ngrx/store';
import { CaseStats, Case, Notification } from '../../../core/models/index';
import { ActivityItem } from '../dashboard.service';

export const loadDashboard    = createAction('[Dashboard] Load');
export const refreshDashboard = createAction('[Dashboard] Refresh');
export const loadActivityFeed = createAction('[Dashboard] Load Activity');

export const loadDashboardSuccess = createAction(
  '[Dashboard] Load Success',
  props<{ stats: CaseStats; recentCases: Case[]; notifications: Notification[] }>()
);

export const loadDashboardFailure = createAction(
  '[Dashboard] Load Failure',
  props<{ error: string }>()
);

export const loadActivityFeedSuccess = createAction(
  '[Dashboard] Activity Success',
  props<{ items: ActivityItem[] }>()
);