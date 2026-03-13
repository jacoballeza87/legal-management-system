import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject, takeUntil } from 'rxjs';
import * as AuthActions from '../store/auth.actions';
import { selectAuthError, selectIsAuthenticated } from '../store/auth.selectors';

@Component({
  selector: 'app-oauth-callback',
  templateUrl: './oauth-callback.component.html',
  styleUrls: ['./oauth-callback.component.scss']
})
export class OauthCallbackComponent implements OnInit {

  provider = '';
  status: 'loading' | 'error' = 'loading';
  errorMessage = '';
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.provider = this.route.snapshot.paramMap.get('provider') || '';
    const code    = this.route.snapshot.queryParamMap.get('code');
    const error   = this.route.snapshot.queryParamMap.get('error');

    if (error) {
      this.status = 'error';
      this.errorMessage = 'El proveedor denegó el acceso. Inténtalo de nuevo.';
      return;
    }

    if (!code || !this.provider) {
      this.status = 'error';
      this.errorMessage = 'Parámetros de autenticación inválidos.';
      return;
    }

    this.store.dispatch(AuthActions.oauthCallback({ provider: this.provider, code }));

    // Watch for error
    this.store.select(selectAuthError).pipe(takeUntil(this.destroy$)).subscribe(err => {
      if (err) {
        this.status = 'error';
        this.errorMessage = err;
      }
    });

    // Watch for success
    this.store.select(selectIsAuthenticated).pipe(takeUntil(this.destroy$)).subscribe(auth => {
      if (auth) this.router.navigate(['/dashboard']);
    });
  }

  goToLogin(): void { this.router.navigate(['/auth/login']); }

  get providerName(): string {
    return this.provider === 'google' ? 'Google' : 'GitHub';
  }

  get providerIcon(): string {
    return this.provider === 'google' ? '🔵' : '⚫';
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
