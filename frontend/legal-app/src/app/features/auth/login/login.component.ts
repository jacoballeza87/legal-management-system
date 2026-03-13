import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject, takeUntil } from 'rxjs';
import * as AuthActions from '../store/auth.actions';
import { selectAuthLoading, selectAuthError } from '../store/auth.selectors';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {

  form!: FormGroup;
  showPassword = false;
  loading$ = this.store.select(selectAuthLoading);
  error$   = this.store.select(selectAuthError);
  resetSuccess = false;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private store: Store,
    private route: ActivatedRoute,
    public  authService: AuthService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      email:       ['', [Validators.required, Validators.email]],
      password:    ['', [Validators.required, Validators.minLength(6)]],
      rememberMe:  [false]
    });

    // Pre-fill email if coming from register
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(p => {
      if (p['email']) this.form.patchValue({ email: p['email'] });
      if (p['reset'] === 'success') this.resetSuccess = true;
    });

    // Clear error on input
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.store.dispatch(AuthActions.clearError());
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { email, password } = this.form.value;
    this.store.dispatch(AuthActions.login({ credentials: { email, password } }));
  }

  loginWithGoogle(): void  { this.authService.initiateOAuth('google'); }
  loginWithGitHub(): void  { this.authService.initiateOAuth('github'); }

  get emailCtrl()    { return this.form.get('email')!; }
  get passwordCtrl() { return this.form.get('password')!; }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
