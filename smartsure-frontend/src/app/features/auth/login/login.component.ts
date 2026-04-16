import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  email = ''; password = '';
  loading = false; showPassword = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {}

  submit(): void {
    if (!this.email || !this.password) { this.toast.warn('Please fill in all fields.'); return; }
    this.loading = true;
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: res => {
        this.toast.success(`Welcome back! Logged in as ${res.role}.`);
        this.router.navigate([res.role === 'ADMIN' ? '/admin/dashboard' : '/customer/dashboard']);
      },
      error: err => {
        this.loading = false;
        this.toast.error(err.error?.message || 'Invalid credentials. Please try again.');
      }
    });
  }
}
