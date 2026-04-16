import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../core/services/api.services';
import { DashboardMetrics, AuditLog } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-admin-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  metrics: DashboardMetrics | null = null;
  recentLogs: AuditLog[] = [];
  loading = true;

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void {
    this.adminService.getDashboard().subscribe({
      next: m => {
        this.metrics = m;
        this.recentLogs = m.recentActivity || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        // Show mock data if backend unavailable
        this.metrics = { totalUsers: 0, totalPolicies: 0, totalClaims: 0, pendingClaims: 0 };
      }
    });
    this.adminService.getRecentActivity(10).subscribe({
      next: logs => this.recentLogs = logs,
      error: () => {}
    });
  }
}
