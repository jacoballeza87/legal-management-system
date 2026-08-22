import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../services/user.service';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatSelectModule, MatCardModule, MatPaginatorModule, MatButtonModule, MatChipsModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);

  users: any[] = [];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  
  displayedColumns: string[] = ['name', 'email', 'status', 'roles', 'actions'];

  // Mock list of available roles in the system (these could also be fetched from a RoleController)
  availableRoles = [
    { id: 1, name: 'USER' },
    { id: 2, name: 'SUPERVISOR' },
    { id: 3, name: 'ADMIN' },
    { id: 4, name: 'SYSTEM_ADMIN' }
  ];

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.userService.getUsers(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        // Based on your PageResponse DTO
        this.users = response.content; 
        this.totalElements = response.totalElements;
      },
      error: (err) => console.error('Error loading users:', err)
    });
  }

  onPageChange(event: PageEvent) {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  hasRole(user: any, roleName: string): boolean {
    return user.roles.some((r: any) => r.name === roleName);
  }

  updateUserRole(userId: number, roleId: number) {
    if (confirm('Are you sure you want to assign this role?')) {
      // Sending as an array of IDs to match Set<Long> in backend
      this.userService.assignRoles(userId, [roleId]).subscribe({
        next: () => {
          alert('Role assigned successfully. User is now ACTIVE.');
          this.loadUsers();
        },
        error: (err) => {
          // Captures your RN-S-004 validation error: "Límite máximo de 5 Administradores..."
          alert(err.error?.message || err.error?.error || 'Failed to assign role.');
          this.loadUsers(); // Reset UI selection on failure
        }
      });
    }
  }

  updateStatus(userId: number, newStatus: string) {
    this.userService.updateStatus(userId, newStatus).subscribe({
      next: () => this.loadUsers(),
      error: (err) => alert('Failed to update status')
    });
  }
}
