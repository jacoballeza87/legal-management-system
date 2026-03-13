import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Router } from '@angular/router';
import { switchMap, map, catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import * as AuthActions from './auth.actions';

@Injectable()
export class AuthEffects {

  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private router: Router
  ) {}

  login$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.login),
    switchMap(({ credentials }) =>
      this.authService.login(credentials).pipe(
        map(response => AuthActions.loginSuccess({ response })),
        catchError(err => of(AuthActions.loginFailure({
          error: err.error?.message || 'Credenciales incorrectas'
        })))
      )
    )
  ));

  loginSuccess$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.loginSuccess, AuthActions.registerSuccess, AuthActions.oauthCallbackSuccess),
    tap(() => this.router.navigate(['/dashboard']))
  ), { dispatch: false });

  register$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.register),
    switchMap(({ data }) =>
      this.authService.register(data).pipe(
        map(response => AuthActions.registerSuccess({ response })),
        catchError(err => of(AuthActions.registerFailure({
          error: err.error?.message || 'Error al crear la cuenta'
        })))
      )
    )
  ));

  oauthCallback$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.oauthCallback),
    switchMap(({ provider, code }) =>
      this.authService.processOAuthCallback(provider, code).pipe(
        map(response => AuthActions.oauthCallbackSuccess({ response })),
        catchError(err => of(AuthActions.oauthCallbackFailure({
          error: err.error?.message || 'Error en la autenticación OAuth'
        })))
      )
    )
  ));

  logout$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.logout),
    tap(() => {
      this.authService.logout();
    })
  ), { dispatch: false });

  forgotPassword$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.forgotPassword),
    switchMap(({ email }) =>
      this.authService.forgotPassword(email).pipe(
        map(() => AuthActions.forgotPasswordSuccess()),
        catchError(err => of(AuthActions.forgotPasswordFailure({
          error: err.error?.message || 'Error al enviar el correo'
        })))
      )
    )
  ));

  resetPassword$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.resetPassword),
    switchMap(({ token, newPassword }) =>
      this.authService.resetPassword(token, newPassword).pipe(
        map(() => AuthActions.resetPasswordSuccess()),
        catchError(err => of(AuthActions.resetPasswordFailure({
          error: err.error?.message || 'Error al restablecer la contraseña'
        })))
      )
    )
  ));

  resetPasswordSuccess$ = createEffect(() => this.actions$.pipe(
    ofType(AuthActions.resetPasswordSuccess),
    tap(() => this.router.navigate(['/auth/login'], {
      queryParams: { reset: 'success' }
    }))
  ), { dispatch: false });
}
