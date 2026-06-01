import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import * as AuthActions from './auth.actions';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private router: Router
  ) {}

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      switchMap(({ credentials }) =>
        this.authService.login(credentials).pipe(
          map(response => AuthActions.loginSuccess({ response })),
          catchError(err => of(AuthActions.loginFailure({ error: err?.error?.message ?? 'Login failed' })))
        )
      )
    )
  );

  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }
  );

  register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.register),
      switchMap(({ request }) =>
        this.authService.register(request).pipe(
          map(() => AuthActions.registerSuccess()),
          catchError(err => of(AuthActions.registerFailure({ error: err?.error?.message ?? 'Registration failed' })))
        )
      )
    )
  );

  oauthCallback$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.oauthCallback),
      switchMap(({ provider, code }) =>
        this.authService.handleOAuthCallback(provider, code).pipe(
          map(response => AuthActions.oauthCallbackSuccess({ response })),
          catchError(err => of(AuthActions.oauthCallbackFailure({ error: err?.error?.message ?? 'OAuth failed' })))
        )
      )
    )
  );

  oauthCallbackSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.oauthCallbackSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }
  );

  forgotPassword$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.forgotPassword),
      switchMap(({ email }) =>
        this.authService.forgotPassword(email).pipe(
          map(() => AuthActions.forgotPasswordSuccess()),
          catchError(err => of(AuthActions.forgotPasswordFailure({ error: err?.error?.message ?? 'Request failed' })))
        )
      )
    )
  );

  resetPassword$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.resetPassword),
      switchMap(({ token, password }) =>
        this.authService.resetPassword(token, password).pipe(
          map(() => AuthActions.resetPasswordSuccess()),
          catchError(err => of(AuthActions.resetPasswordFailure({ error: err?.error?.message ?? 'Reset failed' })))
        )
      )
    )
  );

  resetPasswordSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.resetPasswordSuccess),
        tap(() => this.router.navigate(['/auth/login'], { queryParams: { reset: 'success' } }))
      ),
    { dispatch: false }
  );
}

