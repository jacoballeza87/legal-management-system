import { createReducer, on } from '@ngrx/store';
import { AuthState } from '../models/auth.models';
import * as AuthActions from './auth.actions';

export const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  deviceId: null,
  loading: false,
  error: null,
  isAuthenticated: false,
};

export const authReducer = createReducer(
  initialState,
  on(AuthActions.login, AuthActions.register, AuthActions.oauthCallback,
    AuthActions.forgotPassword, AuthActions.resetPassword,
    state => ({ ...state, loading: true, error: null })),
  on(AuthActions.loginSuccess, AuthActions.oauthCallbackSuccess,
    (state, { response }) => ({
      ...state,
      loading: false,
      user: response.user,
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      isAuthenticated: true,
      error: null
    })),
  on(AuthActions.loginFailure, AuthActions.registerFailure, AuthActions.oauthCallbackFailure,
    AuthActions.forgotPasswordFailure, AuthActions.resetPasswordFailure,
    (state, { error }) => ({ ...state, loading: false, error })),
  on(AuthActions.registerSuccess, AuthActions.forgotPasswordSuccess, AuthActions.resetPasswordSuccess,
    state => ({ ...state, loading: false, error: null })),
  on(AuthActions.clearError, state => ({ ...state, error: null })),
  on(AuthActions.logout, () => initialState),
);

