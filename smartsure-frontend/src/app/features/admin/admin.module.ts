import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AdminGuard } from '../../core/guards/guards';
import { ShellComponent } from '../../shared/components/layout/shell.component';

import { AdminDashboardComponent } from './dashboard/dashboard.component';
import { AdminClaimsComponent } from './claims/claims.component';
import { AdminPoliciesComponent, AuditLogsComponent } from './policies/admin-policies.component';

const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [AdminGuard],
    children: [
      { path: 'dashboard',  component: AdminDashboardComponent },
      { path: 'claims',     component: AdminClaimsComponent },
      { path: 'policies',   component: AdminPoliciesComponent },
      { path: 'audit-logs', component: AuditLogsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];

@NgModule({
  declarations: [
    AdminDashboardComponent,
    AdminClaimsComponent,
    AdminPoliciesComponent,
    AuditLogsComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
