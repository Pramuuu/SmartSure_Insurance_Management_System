import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CustomerGuard } from '../../core/guards/guards';
import { ShellComponent } from '../../shared/components/layout/shell.component';

import { CustomerDashboardComponent } from './dashboard/dashboard.component';
import { CustomerPoliciesComponent } from './policies/policies.component';
import { PurchaseComponent } from './purchase/purchase.component';
import { CustomerClaimsComponent } from './claims/claims.component';
import { CustomerPaymentsComponent } from './payments/payments.component';

const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [CustomerGuard],
    children: [
      { path: 'dashboard', component: CustomerDashboardComponent },
      { path: 'policies',  component: CustomerPoliciesComponent },
      { path: 'purchase',  component: PurchaseComponent },
      { path: 'claims',    component: CustomerClaimsComponent },
      { path: 'payments',  component: CustomerPaymentsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];

@NgModule({
  declarations: [
    CustomerDashboardComponent,
    CustomerPoliciesComponent,
    PurchaseComponent,
    CustomerClaimsComponent,
    CustomerPaymentsComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CustomerModule {}
