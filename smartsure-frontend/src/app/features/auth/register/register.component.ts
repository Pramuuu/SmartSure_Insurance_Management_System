import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['../login/login.component.scss']
})
export class RegisterComponent {
  firstName = ''; lastName = ''; email = ''; password = ''; role = 'CUSTOMER';
  loading = false; showPassword = false;

  constructor(private auth: AuthService, private router: Router, private toast: ToastService) {}

  submit(): void {
    if (!this.firstName || !this.email || !this.password) { this.toast.warn('Please fill in required fields.'); return; }
    this.loading = true;
    this.auth.register({ firstName: this.firstName, lastName: this.lastName, email: this.email, password: this.password, role: this.role }).subscribe({
      next: () => { this.toast.success('Account created! Please sign in.'); this.router.navigate(['/auth/login']); },
      error: err => { this.loading = false; this.toast.error(err.error?.message || 'Registration failed.'); }
    });
  }
}
