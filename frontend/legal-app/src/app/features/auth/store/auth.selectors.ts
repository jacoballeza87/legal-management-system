import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from '../../core/models/auth.models';

export const selectAuthState  = createFeatureSelector<AuthState>('auth');
export const selectCurrentUser     = createSelector(selectAuthState, s => s.user);
export const selectIsAuthenticated = createSelector(selectAuthState, s => s.isAuthenticated);
export const selectAuthLoading     = createSelector(selectAuthState, s => s.loading);
export const selectAuthError       = createSelector(selectAuthState, s => s.error);
export const selectAccessToken     = createSelector(selectAuthState, s => s.accessToken);
export const selectUserRole        = createSelector(selectCurrentUser, u => u?.role);
export const selectUserName        = createSelector(selectCurrentUser, u => u?.name);
export const selectUserAvatar      = createSelector(selectCurrentUser, u => u?.avatarUrl);
