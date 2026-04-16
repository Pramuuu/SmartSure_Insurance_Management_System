import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models';

interface NavItem {
  label: string; icon: string; route: string;
  badge?: string; badgeClass?: string; adminOnly?: boolean; customerOnly?: boolean;
}

@Component({
  standalone: false,
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {
  currentUser: User | null = null;
  currentRoute = '';

  customerNav: NavItem[] = [
    { label: 'Dashboard',      icon: '◈', route: '/customer/dashboard' },
    { label: 'My Policies',    icon: '◉', route: '/customer/policies',  badge: '', badgeClass: 'green' },
    { label: 'Buy Insurance',  icon: '◎', route: '/customer/purchase' },
    { label: 'My Claims',      icon: '⬡', route: '/customer/claims' },
    { label: 'Payments',       icon: '₹', route: '/customer/payments' },
  ];

  adminNav: NavItem[] = [
    { label: 'Dashboard',      icon: '◈', route: '/admin/dashboard' },
    { label: 'Claims Queue',   icon: '⬡', route: '/admin/claims',    badge: '', badgeClass: 'red' },
    { label: 'All Policies',   icon: '◉', route: '/admin/policies' },
    { label: 'Audit Logs',     icon: '≡', route: '/admin/audit-logs' },
  ];

  constructor(public auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.auth.currentUser$.subscribe(u => this.currentUser = u);
    this.router.events.pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.currentRoute = e.urlAfterRedirects);
    this.currentRoute = this.router.url;
  }

  get navItems(): NavItem[] {
    return this.auth.isAdmin() ? this.adminNav : this.customerNav;
  }

  isActive(route: string): boolean {
    return this.currentRoute.startsWith(route);
  }

  getUserInitials(): string {
    const u = this.currentUser;
    if (!u) return '?';
    return ((u.firstName?.[0] ?? '') + (u.lastName?.[0] ?? '')).toUpperCase() || u.email[0].toUpperCase();
  }

  logout(): void { this.auth.logout(); }
}
