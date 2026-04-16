import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}
  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn()) return true;
    return this.router.createUrlTree(['/auth/login']);
  }
}

@Injectable({ providedIn: 'root' })
export class CustomerGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}
  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn() && this.auth.isCustomer()) return true;
    if (!this.auth.isLoggedIn()) return this.router.createUrlTree(['/auth/login']);
    return this.router.createUrlTree(['/access-denied']);
  }
}

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}
  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn() && this.auth.isAdmin()) return true;
    if (!this.auth.isLoggedIn()) return this.router.createUrlTree(['/auth/login']);
    return this.router.createUrlTree(['/access-denied']);
  }
}

@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}
  canActivate(): boolean | UrlTree {
    if (!this.auth.isLoggedIn()) return true;
    return this.auth.isAdmin()
      ? this.router.createUrlTree(['/admin/dashboard'])
      : this.router.createUrlTree(['/customer/dashboard']);
  }
}
