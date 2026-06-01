import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from '../models/auth.models';

export const selectAuthState = createFeatureSelector<AuthState>('auth');
export const selectAuthLoading = createSelector(selectAuthState, s => s.loading);
export const selectAuthError = createSelector(selectAuthState, s => s.error);
export const selectIsAuthenticated = createSelector(selectAuthState, s => s.isAuthenticated);
export const selectCurrentUser = createSelector(selectAuthState, s => s.user);

