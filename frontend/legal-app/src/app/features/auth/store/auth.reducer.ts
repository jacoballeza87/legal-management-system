import { createReducer, on } from '@ngrx/store';
import { UserInfo } from '../../../core/models/auth.models';
import * as A from './auth.actions';

export interface AuthState {
  user: UserInfo | null;
  loading: boolean;
  error: string | null;
  isAuthenticated: boolean;
}

const initial: AuthState = {
  user: null,
  loading: false,
  error: null,
  isAuthenticated: false
};

export const authReducer = createReducer(
  initial,
  on(A.login, A.register, A.forgotPassword, A.resetPassword, A.oauthCallback, s => ({ ...s, loading: true, error: null })),
  on(A.loginSuccess, A.registerSuccess, A.oauthCallbackSuccess, (s, { user }) => ({
    ...s,
    user,
    isAuthenticated: true,
    loading: false,
    error: null
  })),
  on(A.loginFailure, A.registerFailure, A.oauthCallbackFailure, A.forgotPasswordFailure, A.resetPasswordFailure,
    (s, { error }) => ({ ...s, loading: false, error })),
  on(A.forgotPasswordSuccess, A.resetPasswordSuccess, s => ({ ...s, loading: false, error: null })),
  on(A.clearError, s => ({ ...s, error: null }))
);
