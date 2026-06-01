import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, mergeMap, of, tap } from 'rxjs';
import * as A from './auth.actions';
import { AuthService } from '../../../core/services/auth.service';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private router: Router
  ) {}

  login$ = createEffect(() => this.actions$.pipe(
    ofType(A.login),
    mergeMap(({ credentials }) =>
      this.authService.login(credentials).pipe(
        map(res => A.loginSuccess({
          user: res.user,
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
          deviceId: res.deviceId ?? null
        })),
        catchError(err => of(A.loginFailure({ error: err?.error?.message || err?.message || 'Login failed' })))
      )
    )
  ));

  register$ = createEffect(() => this.actions$.pipe(
    ofType(A.register),
    mergeMap(({ data }) =>
      this.authService.register(data).pipe(
        map(res => A.registerSuccess({
          user: res.user,
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
          deviceId: res.deviceId ?? null
        })),
        catchError(err => of(A.registerFailure({ error: err?.error?.message || err?.message || 'Registration failed' })))
      )
    )
  ));

  oauthCallback$ = createEffect(() => this.actions$.pipe(
    ofType(A.oauthCallback),
    mergeMap(({ provider, code }) =>
      this.authService.processOAuthCallback(provider, code).pipe(
        map(res => A.oauthCallbackSuccess({
          user: res.user,
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
          deviceId: res.deviceId ?? null
        })),
        catchError(err => of(A.oauthCallbackFailure({ error: err?.error?.message || err?.message || 'OAuth login failed' })))
      )
    )
  ));

  forgotPassword$ = createEffect(() => this.actions$.pipe(
    ofType(A.forgotPassword),
    mergeMap(({ email }) =>
      this.authService.forgotPassword(email).pipe(
        map(() => A.forgotPasswordSuccess()),
        catchError(err => of(A.forgotPasswordFailure({ error: err?.error?.message || err?.message || 'Password reset request failed' })))
      )
    )
  ));

  resetPassword$ = createEffect(() => this.actions$.pipe(
    ofType(A.resetPassword),
    mergeMap(({ token, newPassword }) =>
      this.authService.resetPassword(token, newPassword).pipe(
        map(() => A.resetPasswordSuccess()),
        catchError(err => of(A.resetPasswordFailure({ error: err?.error?.message || err?.message || 'Password reset failed' })))
      )
    )
  ));

  authSuccess$ = createEffect(() => this.actions$.pipe(
    ofType(A.loginSuccess, A.registerSuccess, A.oauthCallbackSuccess),
    tap(() => this.router.navigate(['/dashboard']))
  ), { dispatch: false });
}
