import { Component } from '@angular/core';
@Component({
  standalone: false,
  selector: 'app-access-denied',
  template: `
    <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:16px;text-align:center;padding:40px">
      <div style="font-size:64px">🔒</div>
      <h1 style="font-family:var(--font-head);font-size:28px;font-weight:700">Access Denied</h1>
      <p style="color:var(--muted);font-size:15px">You don't have permission to access this page.</p>
      <a href="/auth/login" class="btn btn-primary">Return to Login</a>
    </div>
  `
})
export class AccessDeniedComponent {}
