import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const required = route.data?.['roles'] as string[];
    const user = this.auth.currentUser;
    if (!user) { this.router.navigate(['/auth/login']); return false; }
    if (!required || required.length === 0) return true;
    const hasRole = required.some(r => user.roles?.includes(r));
    if (!hasRole) { this.router.navigate(['/dashboard']); return false; }
    return true;
  }
}