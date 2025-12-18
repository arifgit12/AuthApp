import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User, TwoFactorSetupRequest, TwoFactorSetupResponse, TwoFactorEnableRequest } from '../../models/auth.model';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  
  // 2FA setup properties
  show2FASetup = false;
  selectedMethod = '';
  phoneNumber = '';
  setupResponse: TwoFactorSetupResponse | null = null;
  backupCodes: string[] = [];
  verificationCode = '';
  isLoading = false;
  error2FA = '';
  success2FA = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser.subscribe(user => {
      this.currentUser = user;
    });
  }
  
  selectMethod(method: string): void {
    this.selectedMethod = method;
  }
  
  initiate2FASetup(): void {
    if (!this.selectedMethod) {
      return;
    }
    
    this.isLoading = true;
    this.error2FA = '';
    
    const request: TwoFactorSetupRequest = {
      method: this.selectedMethod,
      phoneNumber: this.selectedMethod === 'SMS' ? this.phoneNumber : undefined
    };
    
    this.authService.setup2FA(request).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.setupResponse = response;
        this.backupCodes = response.backupCodes || [];
        
        if (this.selectedMethod === 'SMS' || this.selectedMethod === 'EMAIL') {
          // For SMS/Email, show message to check phone/email
          this.success2FA = response.message || '';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.error2FA = err.error?.message || err.error || 'Failed to setup 2FA';
      }
    });
  }
  
  enable2FA(): void {
    if (!this.verificationCode) {
      return;
    }
    
    this.isLoading = true;
    this.error2FA = '';
    
    const request: TwoFactorEnableRequest = {
      code: this.verificationCode
    };
    
    this.authService.enable2FA(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.success2FA = 'Two-factor authentication enabled successfully!';
        
        // Reset form after 2 seconds
        setTimeout(() => {
          this.cancel2FASetup();
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.error2FA = err.error?.message || err.error || 'Failed to enable 2FA';
      }
    });
  }
  
  cancel2FASetup(): void {
    this.show2FASetup = false;
    this.selectedMethod = '';
    this.phoneNumber = '';
    this.setupResponse = null;
    this.backupCodes = [];
    this.verificationCode = '';
    this.error2FA = '';
    this.success2FA = '';
  }
}
