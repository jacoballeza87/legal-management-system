import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LoginRequest, LoginResponse, RegisterRequest,
  TokenRefreshResponse, UserInfo
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly API = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<UserInfo | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();
  private refreshInProgress = false;

  constructor(private http: HttpClient, private router: Router) {}

  // ─── Login ────────────────────────────────────────────────────────────────

  login(credentials: LoginRequest): Observable<LoginResponse> {
    const payload = {
      ...credentials,
      deviceId: this.getOrCreateDeviceId(),
      deviceName: this.getDeviceName(),
      deviceType: 'WEB'
    };
    return this.http.post<LoginResponse>(`${this.API}/login`, payload).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Register ─────────────────────────────────────────────────────────────

  register(data: RegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/register`, data).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── OAuth2 ───────────────────────────────────────────────────────────────

  initiateOAuth(provider: 'google' | 'github'): void {
    const redirectUri = `${environment.oauthRedirectBase}/auth/oauth/callback/${provider}`;
    if (provider === 'google') {
      const params = new URLSearchParams({
        client_id: environment.googleClientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: 'openid email profile',
        access_type: 'offline',
        prompt: 'select_account'
      });
      window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?${params}`;
    } else {
      const params = new URLSearchParams({
        client_id: environment.githubClientId,
        redirect_uri: redirectUri,
        scope: 'user:email'
      });
      window.location.href = `https://github.com/login/oauth/authorize?${params}`;
    }
  }

  processOAuthCallback(provider: string, code: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/oauth2/callback/${provider}`, {
      code,
      deviceId: this.getOrCreateDeviceId(),
      deviceName: this.getDeviceName(),
      deviceType: 'WEB'
    }).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Token Refresh ────────────────────────────────────────────────────────

  refreshToken(): Observable<TokenRefreshResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return throwError(() => new Error('No refresh token'));

    return this.http.post<TokenRefreshResponse>(`${this.API}/refresh`, {
      refreshToken,
      deviceId: this.getDeviceId()
    }).pipe(
      tap(res => {
        localStorage.setItem(environment.tokenKey, res.accessToken);
        localStorage.setItem(environment.refreshTokenKey, res.refreshToken);
      }),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  // ─── Logout ───────────────────────────────────────────────────────────────

  logout(): void {
    const token = this.getToken();
    if (token) {
      this.http.post(`${this.API}/logout`, {}).pipe(
        catchError(() => of(null))
      ).subscribe();
    }
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  logoutAll(): Observable<any> {
    return this.http.post(`${this.API}/logout-all`, {}).pipe(
      tap(() => this.clearSession()),
      catchError(() => of(null))
    );
  }

  // ─── Password ─────────────────────────────────────────────────────────────

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.API}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API}/reset-password`, { token, newPassword });
  }

  // ─── Getters ──────────────────────────────────────────────────────────────

  get currentUser(): UserInfo | null { return this.currentUserSubject.value; }
  get isAuthenticated(): boolean { return !!this.getToken() && !!this.currentUser; }
  getToken(): string | null { return localStorage.getItem(environment.tokenKey); }
  getRefreshToken(): string | null { return localStorage.getItem(environment.refreshTokenKey); }
  getDeviceId(): string | null { return localStorage.getItem(environment.deviceIdKey); }

  hasRole(role: string): boolean {
    return this.currentUser?.role === role;
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.includes(this.currentUser?.role ?? '');
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private handleAuthSuccess(res: LoginResponse): void {
    localStorage.setItem(environment.tokenKey, res.accessToken);
    localStorage.setItem(environment.refreshTokenKey, res.refreshToken);
    if (res.deviceId) localStorage.setItem(environment.deviceIdKey, res.deviceId);
    this.currentUserSubject.next(res.user);
  }

  private clearSession(): void {
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.refreshTokenKey);
    this.currentUserSubject.next(null);
  }

  private loadUser(): UserInfo | null {
    const token = localStorage.getItem(environment.tokenKey);
    if (!token) return null;
    try {
      // Decode JWT payload
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp * 1000 < Date.now()) {
        localStorage.removeItem(environment.tokenKey);
        return null;
      }
      return {
        id: payload.userId,
        name: payload.name || payload.sub,
        username: payload.username || payload.sub,
        email: payload.sub,
        role: payload.role,
        avatarUrl: payload.avatarUrl
      };
    } catch {
      return null;
    }
  }

  private getOrCreateDeviceId(): string {
    let deviceId = localStorage.getItem(environment.deviceIdKey);
    if (!deviceId) {
      deviceId = crypto.randomUUID();
      localStorage.setItem(environment.deviceIdKey, deviceId);
    }
    return deviceId;
  }

  private getDeviceName(): string {
    const ua = navigator.userAgent;
    if (/iPhone|iPad|iPod/.test(ua)) return 'iOS Device';
    if (/Android/.test(ua)) return 'Android Device';
    if (/Mac/.test(ua)) return 'Mac Browser';
    if (/Windows/.test(ua)) return 'Windows Browser';
    return 'Web Browser';
  }
}
