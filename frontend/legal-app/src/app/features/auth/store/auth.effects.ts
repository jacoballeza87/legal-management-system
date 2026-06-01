import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
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
          map(response => AuthActions.loginSuccess({
            user: response.user,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
          })),
          catchError(err => of(AuthActions.loginFailure({ error: err?.error?.message ?? 'Login failed' })))
        )
      )
    )
  );

  loginSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.loginSuccess),
      map(() => { this.router.navigate(['/dashboard']); return { type: '[Auth] Noop' }; })
    )
  );

  oauthCallback$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.oauthCallback),
      switchMap(({ provider, code }) =>
        this.authService.handleOAuthCallback(provider, code).pipe(
          map(response => AuthActions.oauthCallbackSuccess({
            user: response.user,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
          })),
          catchError(err => of(AuthActions.oauthCallbackFailure({ error: err?.error?.message ?? 'OAuth failed' })))
        )
      )
    )
  );
}

