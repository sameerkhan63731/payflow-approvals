import { AsyncPipe, NgIf } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'pf-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, NgIf, AsyncPipe],
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/payments">
        <span class="logo" aria-hidden="true">PF</span>
        <span>PayFlow <span class="sub">Approvals</span></span>
      </a>
      <div class="user" *ngIf="auth.user$ | async as user">
        <span class="who">
          <strong>{{ user.username }}</strong>
          <span class="roles">{{ user.roles.join(' · ') }}</span>
        </span>
        <button type="button" class="btn" (click)="logout()">Sign out</button>
      </div>
    </header>
    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [
    `
      .topbar {
        background: #0a2540;
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 24px;
        gap: 16px;
        flex-wrap: wrap;
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
        color: #fff;
        text-decoration: none;
        font-weight: 700;
        font-size: 18px;
      }
      .sub {
        font-weight: 400;
        opacity: 0.75;
      }
      .logo {
        display: grid;
        place-items: center;
        width: 32px;
        height: 32px;
        border-radius: 8px;
        background: #2f80ed;
        font-size: 13px;
        letter-spacing: 0.04em;
      }
      .user {
        display: flex;
        align-items: center;
        gap: 14px;
      }
      .who {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        line-height: 1.2;
      }
      .roles {
        font-size: 12px;
        opacity: 0.75;
      }
      .user .btn {
        background: transparent;
        color: #fff;
        border-color: rgba(255, 255, 255, 0.35);
      }
    `,
  ],
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
