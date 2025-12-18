import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest, LoginResponse } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginRequest: LoginRequest = {
    username: '',
    password: '',
    authMethod: 'JWT'
  };
  
  errorMessage = '';
  isLoading = false;
  authMethods = ['JWT', 'BASIC', 'LDAP', 'KEYCLOAK'];
  
  // 2FA related properties
  twoFactorRequired = false;
  twoFactorMethod = '';
  twoFactorCode = '';
  savedLoginRequest: LoginRequest | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.isLoading = true;

    this.authService.login(this.loginRequest).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading = false;
        if (response.twoFactorRequired) {
          // Show 2FA input
          this.twoFactorRequired = true;
          this.twoFactorMethod = response.twoFactorMethod || '';
          this.savedLoginRequest = { ...this.loginRequest };
          
          // If method is SMS or EMAIL, code has been sent
          if (this.twoFactorMethod === 'SMS' || this.twoFactorMethod === 'EMAIL') {
            this.errorMessage = '';
          }
        } else {
          // Login successful
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Login failed. Please try again.';
      }
    });
  }
  
  onSubmit2FA(): void {
    if (!this.savedLoginRequest) {
      return;
    }
    
    this.errorMessage = '';
    this.isLoading = true;
    
    // Add 2FA code to login request
    const requestWith2FA: LoginRequest = {
      ...this.savedLoginRequest,
      twoFactorCode: this.twoFactorCode
    };
    
    this.authService.login(requestWith2FA).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading = false;
        if (response.token) {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || '2FA verification failed.';
      }
    });
  }
  
  resendCode(): void {
    if (this.twoFactorMethod === 'SMS' || this.twoFactorMethod === 'EMAIL') {
      this.isLoading = true;
      this.authService.sendCode().subscribe({
        next: () => {
          this.isLoading = false;
          this.errorMessage = 'Verification code sent!';
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = 'Failed to send code.';
        }
      });
    }
  }
  
  cancel2FA(): void {
    this.twoFactorRequired = false;
    this.twoFactorCode = '';
    this.savedLoginRequest = null;
  }

  loginWithKeycloak(): void {
    // Keycloak login will be handled by Keycloak adapter
    alert('Keycloak SSO integration - To be configured with your Keycloak server');
  }
}
