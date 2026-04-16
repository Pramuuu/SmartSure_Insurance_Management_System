import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { ToastComponent } from './components/toast/toast.component';
import { LoadingComponent } from './components/loading/loading.component';
import { ShellComponent } from './components/layout/shell.component';

@NgModule({
  declarations: [SidebarComponent, ToastComponent, LoadingComponent, ShellComponent],
  imports: [CommonModule, RouterModule, FormsModule],
  exports: [SidebarComponent, ToastComponent, LoadingComponent, ShellComponent, CommonModule, FormsModule, RouterModule]
})
export class SharedModule {}
