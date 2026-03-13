import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Subject, takeUntil } from 'rxjs';
import * as AuthActions from '../store/auth.actions';
import { selectAuthLoading, selectAuthError } from '../store/auth.selectors';
import { AuthService } from '../../../core/services/auth.service';

// Custom validator: passwords must match
function passwordsMatch(ctrl: AbstractControl): ValidationErrors | null {
  const pass    = ctrl.get('password')?.value;
  const confirm = ctrl.get('confirmPassword')?.value;
  return pass === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit, OnDestroy {

  form!: FormGroup;
  showPassword = false;
  showConfirm  = false;
  step = 1;            // 1 = personal info, 2 = credentials
  loading$ = this.store.select(selectAuthLoading);
  error$   = this.store.select(selectAuthError);
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private store: Store,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name:            ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      username:        ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
      email:           ['', [Validators.required, Validators.email]],
      password:        ['', [Validators.required, Validators.minLength(8),
                             Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/)]],
      confirmPassword: ['', Validators.required],
      acceptTerms:     [false, Validators.requiredTrue]
    }, { validators: passwordsMatch });

    this.form.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.store.dispatch(AuthActions.clearError()));
  }

  nextStep(): void {
    const step1Fields = ['name', 'username', 'email'];
    step1Fields.forEach(f => this.form.get(f)?.markAsTouched());
    const step1Valid = step1Fields.every(f => this.form.get(f)?.valid);
    if (step1Valid) this.step = 2;
  }

  prevStep(): void { this.step = 1; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { name, username, email, password } = this.form.value;
    this.store.dispatch(AuthActions.register({ data: { name, username, email, password } }));
  }

  registerWithGoogle(): void { this.authService.initiateOAuth('google'); }
  registerWithGitHub(): void { this.authService.initiateOAuth('github'); }

  get passwordStrength(): { score: number; label: string; color: string; width: string } {
    const pw = this.form.get('password')?.value || '';
    let score = 0;
    if (pw.length >= 8)  score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[@$!%*?&]/.test(pw)) score++;
    if (pw.length >= 12) score++;

    const map = [
      { label: '',        color: 'transparent', width: '0%' },
      { label: 'Débil',   color: '#ef4444',     width: '25%' },
      { label: 'Regular', color: '#f59e0b',     width: '50%' },
      { label: 'Buena',   color: '#3b82f6',     width: '75%' },
      { label: 'Fuerte',  color: '#22c55e',     width: '90%' },
      { label: '¡Excelente!', color: '#10b981', width: '100%' },
    ];
    return { score, ...map[Math.min(score, 5)] };
  }

  get f() { return this.form.controls; }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
