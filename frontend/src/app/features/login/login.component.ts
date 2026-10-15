import { NgFor, NgIf } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/payments-api.service';

interface DemoUser {
  username: string;
  description: string;
}

@Component({
  selector: 'pf-login',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor],
  template: `
    <section class="wrap">
      <div class="card panel">
        <h1>Sign in</h1>
        <p class="muted">Initiate ACH and wire payments and route them through maker-checker approval.</p>

        <form (ngSubmit)="submit()" #f="ngForm" novalidate>
          <div class="field">
            <label for="username">Username</label>
            <input id="username" name="username" [(ngModel)]="username" required autocomplete="username" />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" [(ngModel)]="password" required
                   autocomplete="current-password" />
          </div>
          <div class="alert" *ngIf="error" role="alert">{{ error }}</div>
          <button class="btn primary" type="submit" [disabled]="busy || !f.form.valid">
            {{ busy ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      </div>

      <aside class="card panel demo">
        <h2>Demo users</h2>
        <p class="muted">Password for all: <span class="mono">demo-pass</span></p>
        <ul>
          <li *ngFor="let u of demoUsers">
            <button type="button" class="pick" (click)="pick(u.username)">
              <strong>{{ u.username }}</strong>
              <span class="muted">{{ u.description }}</span>
            </button>
          </li>
        </ul>
      </aside>
    </section>
  `,
  styles: [
    `
      .wrap {
        display: grid;
        grid-template-columns: minmax(0, 1fr) minmax(0, 0.8fr);
        gap: 20px;
        max-width: 860px;
        margin: 32px auto;
      }
      @media (max-width: 720px) {
        .wrap {
          grid-template-columns: 1fr;
        }
      }
      .panel {
        padding: 24px;
      }
      h1 {
        font-size: 24px;
        margin-bottom: 6px;
      }
      h2 {
        font-size: 16px;
        margin-bottom: 4px;
      }
      form {
        display: flex;
        flex-direction: column;
        gap: 14px;
        margin-top: 18px;
      }
      ul {
        list-style: none;
        padding: 0;
        margin: 12px 0 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .pick {
        width: 100%;
        text-align: left;
        display: flex;
        flex-direction: column;
        gap: 2px;
        padding: 10px 12px;
        border: 1px solid var(--border);
        border-radius: 8px;
        background: #fff;
        font: inherit;
        cursor: pointer;
      }
      .pick:hover {
        border-color: var(--brand);
        background: var(--brand-soft);
      }
      .pick span {
        font-size: 13px;
      }
    `,
  ],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly demoUsers: DemoUser[] = [
    { username: 'maya', description: 'Initiator: creates and submits payments' },
    { username: 'raj', description: 'Approver' },
    { username: 'lena', description: 'Approver' },
    { username: 'omar', description: 'Approver and admin: can release approved payments' },
  ];

  username = '';
  password = '';
  busy = false;
  error: string | null = null;

  pick(username: string): void {
    this.username = username;
    this.password = 'demo-pass';
    this.error = null;
  }

  submit(): void {
    if (!this.username || !this.password || this.busy) {
      return;
    }
    this.busy = true;
    this.error = null;
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => {
        this.busy = false;
        void this.router.navigate(['/payments']);
      },
      error: (err: unknown) => {
        this.busy = false;
        this.error = errorMessage(err);
      },
    });
  }
}
