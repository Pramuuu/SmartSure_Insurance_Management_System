import { Component } from '@angular/core';
@Component({
  standalone: false,
  selector: 'app-loading',
  template: `<div class="loading-wrap"><span class="loading-spinner"></span><span class="loading-text">Loading…</span></div>`,
  styles: [`.loading-wrap{display:flex;align-items:center;gap:10px;padding:40px;justify-content:center;color:var(--muted);font-size:13px}.loading-text{color:var(--muted)}`]
})
export class LoadingComponent {}
