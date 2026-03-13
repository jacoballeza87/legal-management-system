import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CaseStats, Case, Notification, PageResponse } from '../../core/models/index';

export interface DashboardData {
  stats: CaseStats;
  recentCases: Case[];
  overdueCount: number;
  dueSoonCount: number;
  unreadNotifications: number;
}

export interface ActivityItem {
  id: number;
  type: 'case_created' | 'case_updated' | 'version_added' | 'collaborator_added' | 'status_changed';
  caseNumber: string;
  caseTitle: string;
  description: string;
  userName: string;
  userAvatar?: string;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private readonly CASES_API = `${environment.apiUrl}/cases`;
  private readonly NOTIF_API = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<CaseStats> {
    return this.http.get<CaseStats>(`${this.CASES_API}/stats`);
  }

  getRecentCases(size = 5): Observable<Case[]> {
    const params = new HttpParams()
      .set('page', 0).set('size', size)
      .set('sortBy', 'updatedAt').set('direction', 'desc');
    return this.http.get<PageResponse<Case>>(this.CASES_API, { params })
      .pipe(map(r => r.content));
  }

  getMyCases(ownerId: number, size = 8): Observable<Case[]> {
    const params = new HttpParams()
      .set('page', 0).set('size', size)
      .set('sortBy', 'updatedAt').set('direction', 'desc');
    return this.http.get<PageResponse<Case>>(this.CASES_API, { params })
      .pipe(map(r => r.content));
  }

  getUnreadNotificationsCount(): Observable<number> {
    return this.http.get<{ count: number }>(`${this.NOTIF_API}/unread/count`)
      .pipe(map(r => r.count));
  }

  getRecentNotifications(size = 5): Observable<Notification[]> {
    const params = new HttpParams().set('page', 0).set('size', size);
    return this.http.get<PageResponse<Notification>>(this.NOTIF_API, { params })
      .pipe(map(r => r.content));
  }

  getActivityFeed(size = 10): Observable<ActivityItem[]> {
    return this.http.get<ActivityItem[]>(`${this.CASES_API}/activity?size=${size}`);
  }
}
