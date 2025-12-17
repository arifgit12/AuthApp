import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

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

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.isLoading = true;

    this.authService.login(this.loginRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Login failed. Please try again.';
      }
    });
  }

  loginWithKeycloak(): void {
    // Keycloak login will be handled by Keycloak adapter
    alert('Keycloak SSO integration - To be configured with your Keycloak server');
  }
}
