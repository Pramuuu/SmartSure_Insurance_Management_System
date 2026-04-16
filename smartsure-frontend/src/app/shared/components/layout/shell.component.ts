import { Component } from '@angular/core';
@Component({
  standalone: false,
  selector: 'app-shell',
  template: `
    <div class="app-shell">
      <app-sidebar></app-sidebar>
      <div class="shell-main">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .app-shell { display: flex; min-height: 100vh; }
    .shell-main { flex: 1; min-width: 0; overflow-y: auto; height: 100vh; }
  `]
})
export class ShellComponent {}
