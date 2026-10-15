import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('starts signed out', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token).toBeNull();
  });

  it('stores the session and roles after login, and clears them on logout', () => {
    service.login('omar', 'demo-pass').subscribe();

    const req = http.expectOne('/api/auth/token');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'omar', password: 'demo-pass' });
    req.flush({ accessToken: 'abc.def.ghi', tokenType: 'Bearer', expiresIn: 3600, username: 'omar', roles: ['ADMIN', 'APPROVER'] });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token).toBe('abc.def.ghi');
    expect(service.hasRole('ADMIN')).toBeTrue();
    expect(service.hasRole('INITIATOR')).toBeFalse();

    service.logout();
    expect(service.isAuthenticated()).toBeFalse();
    expect(sessionStorage.length).toBe(0);
  });

  it('treats an expired session as signed out', () => {
    service.login('raj', 'demo-pass').subscribe();
    http.expectOne('/api/auth/token').flush({
      accessToken: 't', tokenType: 'Bearer', expiresIn: -1, username: 'raj', roles: ['APPROVER'],
    });
    expect(service.isAuthenticated()).toBeFalse();
  });
});
