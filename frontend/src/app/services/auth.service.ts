import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { LoginRequest, LoginResponse, RegisterRequest, User, TwoFactorSetupRequest, TwoFactorSetupResponse, TwoFactorEnableRequest } from '../models/auth.model';

const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;

  constructor(private http: HttpClient) {
    this.currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
    this.currentUser = this.currentUserSubject.asObservable();
  }

  login(loginRequest: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, loginRequest)
      .pipe(
        tap(response => {
          // Only save token if 2FA is not required
          if (response.token && !response.twoFactorRequired) {
            this.saveToken(response.token);
            const user: User = {
              id: 0,
              username: response.username,
              email: response.email,
              fullName: response.username,
              roles: response.roles,
              privileges: response.privileges
            };
            this.saveUser(user);
            this.currentUserSubject.next(user);
          }
        })
      );
  }

  register(registerRequest: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, registerRequest);
  }

  logout(): Observable<any> {
    this.removeToken();
    this.removeUser();
    this.currentUserSubject.next(null);
    return this.http.post(`${this.apiUrl}/logout`, {});
  }

  // 2FA methods
  setup2FA(request: TwoFactorSetupRequest): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>(`${this.apiUrl}/2fa/setup`, request);
  }

  enable2FA(request: TwoFactorEnableRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/2fa/enable`, request);
  }

  disable2FA(): Observable<any> {
    return this.http.post(`${this.apiUrl}/2fa/disable`, {});
  }

  sendCode(): Observable<any> {
    return this.http.post(`${this.apiUrl}/2fa/send-code`, {});
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getToken(): string | null {
    return window.sessionStorage.getItem(TOKEN_KEY);
  }

  private saveToken(token: string): void {
    window.sessionStorage.removeItem(TOKEN_KEY);
    window.sessionStorage.setItem(TOKEN_KEY, token);
  }

  private removeToken(): void {
    window.sessionStorage.removeItem(TOKEN_KEY);
  }

  private saveUser(user: User): void {
    window.sessionStorage.removeItem(USER_KEY);
    window.sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  private removeUser(): void {
    window.sessionStorage.removeItem(USER_KEY);
  }

  private getUserFromStorage(): User | null {
    const user = window.sessionStorage.getItem(USER_KEY);
    if (user) {
      return JSON.parse(user);
    }
    return null;
  }

  hasRole(role: string): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.includes(role) : false;
  }

  hasPrivilege(privilege: string): boolean {
    const user = this.currentUserValue;
    return user ? user.privileges.includes(privilege) : false;
  }
}
