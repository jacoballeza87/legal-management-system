import { createAction, props } from '@ngrx/store';
import { LoginRequest, RegisterRequest, LoginResponse, UserInfo } from '../models/auth.models';

export const login = createAction('[Auth] Login', props<{ credentials: LoginRequest }>());
export const loginSuccess = createAction('[Auth] Login Success', props<{ response: LoginResponse }>());
export const loginFailure = createAction('[Auth] Login Failure', props<{ error: string }>());

export const register = createAction('[Auth] Register', props<{ request: RegisterRequest }>());
export const registerSuccess = createAction('[Auth] Register Success');
export const registerFailure = createAction('[Auth] Register Failure', props<{ error: string }>());

export const oauthCallback = createAction('[Auth] OAuth Callback', props<{ provider: string; code: string }>());
export const oauthCallbackSuccess = createAction('[Auth] OAuth Callback Success', props<{ response: LoginResponse }>());
export const oauthCallbackFailure = createAction('[Auth] OAuth Callback Failure', props<{ error: string }>());

export const forgotPassword = createAction('[Auth] Forgot Password', props<{ email: string }>());
export const forgotPasswordSuccess = createAction('[Auth] Forgot Password Success');
export const forgotPasswordFailure = createAction('[Auth] Forgot Password Failure', props<{ error: string }>());

export const resetPassword = createAction('[Auth] Reset Password', props<{ token: string; password: string }>());
export const resetPasswordSuccess = createAction('[Auth] Reset Password Success');
export const resetPasswordFailure = createAction('[Auth] Reset Password Failure', props<{ error: string }>());

export const clearError = createAction('[Auth] Clear Error');
export const logout = createAction('[Auth] Logout');

