import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import * as AuthActions from '../store/auth.actions';
import { selectAuthLoading, selectAuthError } from '../store/auth.selectors';

function passwordsMatch(ctrl: AbstractControl): ValidationErrors | null {
  return ctrl.get('password')?.value === ctrl.get('confirmPassword')?.value
    ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {

  form!: FormGroup;
  token = '';
  showPassword = false;
  loading$ = this.store.select(selectAuthLoading);
  error$   = this.store.select(selectAuthError);

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private store: Store
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(8),
                      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordsMatch });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.token) { this.form.markAllAsTouched(); return; }
    this.store.dispatch(AuthActions.resetPassword({
      token: this.token,
      newPassword: this.form.value.password
    }));
  }

  get f() { return this.form.controls; }

  get passwordStrength() {
    const pw = this.f['password'].value || '';
    let score = 0;
    if (pw.length >= 8)  score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[@$!%*?&]/.test(pw)) score++;
    if (pw.length >= 12) score++;
    const map = [
      { label: '', color: 'transparent', width: '0%' },
      { label: 'Débil',   color: '#ef4444', width: '25%' },
      { label: 'Regular', color: '#f59e0b', width: '50%' },
      { label: 'Buena',   color: '#3b82f6', width: '75%' },
      { label: 'Fuerte',  color: '#22c55e', width: '90%' },
      { label: '¡Excelente!', color: '#10b981', width: '100%' },
    ];
    return { score, ...map[Math.min(score, 5)] };
  }
}
