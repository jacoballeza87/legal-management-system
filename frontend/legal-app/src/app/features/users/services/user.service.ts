import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/v1/users`;

  // Get all users (paginated)
  getUsers(page: number = 0, size: number = 20, sortBy: string = 'createdAt', direction: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
      
    return this.http.get(this.apiUrl, { params });
  }

  // Create a new user (Admin functionality)
  createUser(userData: any): Observable<any> {
    return this.http.post(this.apiUrl, userData);
  }

  // Update user status (e.g., ACTIVE, SUSPENDED)
  updateStatus(userId: number, status: string): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.patch(`${this.apiUrl}/${userId}/status`, null, { params });
  }

  // Assign roles (Replaces all roles, triggers RN-S-004 System Admin limit)
  assignRoles(userId: number, roleIds: number[]): Observable<any> {
    return this.http.put(`${this.apiUrl}/${userId}/roles`, roleIds);
  }
  
  // Get user stats for dashboard
  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }
}
