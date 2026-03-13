import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import * as AuthActions from '../store/auth.actions';
import { selectAuthLoading, selectAuthError } from '../store/auth.selectors';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {

  form!: FormGroup;
  submitted = false;
  loading$ = this.store.select(selectAuthLoading);
  error$   = this.store.select(selectAuthError);

  constructor(private fb: FormBuilder, private store: Store) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.store.dispatch(AuthActions.forgotPassword({ email: this.form.value.email }));
    this.submitted = true;
  }

  get emailCtrl() { return this.form.get('email')!; }
}
