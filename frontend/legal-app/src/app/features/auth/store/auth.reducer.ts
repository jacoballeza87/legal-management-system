import { createReducer, on } from '@ngrx/store';
import { AuthState } from '../../core/models/auth.models';
import * as AuthActions from './auth.actions';

const initialState: AuthState = {
  user:            null,
  accessToken:     localStorage.getItem('legal_access_token'),
  refreshToken:    localStorage.getItem('legal_refresh_token'),
  deviceId:        localStorage.getItem('legal_device_id'),
  loading:         false,
  error:           null,
  isAuthenticated: !!localStorage.getItem('legal_access_token')
};

export const authReducer = createReducer(
  initialState,

  // Login
  on(AuthActions.login, s => ({ ...s, loading: true, error: null })),
  on(AuthActions.loginSuccess, (s, { response }) => ({
    ...s, loading: false, error: null,
    user: response.user,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    deviceId: response.deviceId,
    isAuthenticated: true
  })),
  on(AuthActions.loginFailure, (s, { error }) => ({ ...s, loading: false, error })),

  // Register
  on(AuthActions.register, s => ({ ...s, loading: true, error: null })),
  on(AuthActions.registerSuccess, (s, { response }) => ({
    ...s, loading: false, error: null,
    user: response.user,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    deviceId: response.deviceId,
    isAuthenticated: true
  })),
  on(AuthActions.registerFailure, (s, { error }) => ({ ...s, loading: false, error })),

  // OAuth
  on(AuthActions.oauthCallback, s => ({ ...s, loading: true, error: null })),
  on(AuthActions.oauthCallbackSuccess, (s, { response }) => ({
    ...s, loading: false,
    user: response.user, accessToken: response.accessToken,
    refreshToken: response.refreshToken, isAuthenticated: true
  })),
  on(AuthActions.oauthCallbackFailure, (s, { error }) => ({ ...s, loading: false, error })),

  // Forgot / Reset
  on(AuthActions.forgotPassword, s => ({ ...s, loading: true, error: null })),
  on(AuthActions.forgotPasswordSuccess, s => ({ ...s, loading: false })),
  on(AuthActions.forgotPasswordFailure, (s, { error }) => ({ ...s, loading: false, error })),
  on(AuthActions.resetPassword, s => ({ ...s, loading: true, error: null })),
  on(AuthActions.resetPasswordSuccess, s => ({ ...s, loading: false })),
  on(AuthActions.resetPasswordFailure, (s, { error }) => ({ ...s, loading: false, error })),

  // Logout & clear
  on(AuthActions.logout, AuthActions.logoutAll, () => ({
    user: null, accessToken: null, refreshToken: null,
    deviceId: null, loading: false, error: null, isAuthenticated: false
  })),
  on(AuthActions.clearError, s => ({ ...s, error: null }))
);
