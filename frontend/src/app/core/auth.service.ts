import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { Role, Session } from './models';

interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: Role[];
}

const STORAGE_KEY = 'payflow.session';

/**
 * Holds the signed-in user's session. The token lives in sessionStorage so it
 * is gone when the tab closes; nothing is written to localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session$ = new BehaviorSubject<Session | null>(AuthService.restore());

  readonly user$: Observable<Session | null> = this.session$.asObservable();

  get session(): Session | null {
    const current = this.session$.value;
    if (current && current.expiresAt <= Date.now()) {
      this.logout();
      return null;
    }
    return current;
  }

  get token(): string | null {
    return this.session?.token ?? null;
  }

  isAuthenticated(): boolean {
    return this.session !== null;
  }

  hasRole(role: Role): boolean {
    return this.session?.roles.includes(role) ?? false;
  }

  login(username: string, password: string): Observable<Session> {
    return this.http.post<TokenResponse>('/api/auth/token', { username, password }).pipe(
      map((res) => ({
        token: res.accessToken,
        username: res.username,
        roles: res.roles,
        expiresAt: Date.now() + res.expiresIn * 1000,
      })),
      tap((session) => {
        AuthService.persist(session);
        this.session$.next(session);
      }),
    );
  }

  logout(): void {
    AuthService.persist(null);
    this.session$.next(null);
  }

  private static restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as Session;
      return parsed.expiresAt > Date.now() ? parsed : null;
    } catch {
      return null;
    }
  }

  private static persist(session: Session | null): void {
    try {
      if (session) {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // Storage can be unavailable (private mode); the in-memory session still works.
    }
  }
}
