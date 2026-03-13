import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';

import { authReducer } from './store/auth.reducer';
import { AuthEffects } from './store/auth.effects';

import { LoginComponent }           from './login/login.component';
import { RegisterComponent }        from './register/register.component';
import { ForgotPasswordComponent }  from './forgot-password/forgot-password.component';
import { ResetPasswordComponent }   from './reset-password/reset-password.component';
import { OauthCallbackComponent }   from './oauth-callback/oauth-callback.component';
import { GuestGuard }               from '../../core/guards/auth.guard';

const routes: Routes = [
  { path: '',                redirectTo: 'login', pathMatch: 'full' },
  { path: 'login',           component: LoginComponent,          canActivate: [GuestGuard] },
  { path: 'register',        component: RegisterComponent,       canActivate: [GuestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent, canActivate: [GuestGuard] },
  { path: 'reset-password',  component: ResetPasswordComponent },
  { path: 'oauth/callback/:provider', component: OauthCallbackComponent }
];

@NgModule({
  declarations: [
    LoginComponent,
    RegisterComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    OauthCallbackComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    StoreModule.forFeature('auth', authReducer),
    EffectsModule.forFeature([AuthEffects])
  ]
})
export class AuthModule {}
