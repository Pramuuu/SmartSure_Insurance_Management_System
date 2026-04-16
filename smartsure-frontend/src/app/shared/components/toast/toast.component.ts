import { Component, OnInit } from '@angular/core';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  standalone: false,
  selector: 'app-toast',
  template: `
    <div class="toast-container">
      <div *ngFor="let t of toasts" class="toast" [class]="'toast-' + t.type">
        <span class="toast-icon">{{ icons[t.type] }}</span>
        <span class="toast-msg">{{ t.message }}</span>
        <button class="toast-close" (click)="remove(t.id)">×</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container { position: fixed; bottom: 24px; right: 24px; display: flex; flex-direction: column; gap: 8px; z-index: 9999; }
    .toast { background: var(--surface2); border: 1px solid var(--border2); border-radius: var(--r2); padding: 12px 16px; font-size: 13px; color: var(--text); display: flex; align-items: center; gap: 10px; min-width: 280px; max-width: 380px; box-shadow: 0 8px 32px rgba(0,0,0,0.4); animation: fadeUp 0.3s ease; }
    .toast-success { border-color: rgba(0,229,180,0.3); }
    .toast-error   { border-color: rgba(255,107,107,0.3); }
    .toast-warn    { border-color: rgba(255,209,102,0.3); }
    .toast-info    { border-color: rgba(0,153,255,0.2); }
    .toast-icon { font-size: 16px; flex-shrink: 0; }
    .toast-msg  { flex: 1; }
    .toast-close { margin-left: auto; background: none; border: none; color: var(--dim); cursor: pointer; font-size: 18px; line-height: 1; padding: 0 2px; }
    .toast-close:hover { color: var(--text); }
    @keyframes fadeUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ToastComponent implements OnInit {
  toasts: Toast[] = [];
  icons: Record<string, string> = { success: '✅', error: '❌', warn: '⚠️', info: 'ℹ️' };

  constructor(private toastService: ToastService) {}
  ngOnInit(): void { this.toastService.toasts$.subscribe(t => this.toasts = t); }
  remove(id: string): void { this.toastService.remove(id); }
}
