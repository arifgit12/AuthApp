export interface LoginRequest {
  username: string;
  password: string;
  authMethod: string;
  twoFactorCode?: string;
  recaptchaToken?: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  roles: string[];
  privileges: string[];
  authMethod: string;
  twoFactorRequired?: boolean;
  twoFactorMethod?: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  privileges: string[];
}

export interface TwoFactorSetupRequest {
  method: string; // TOTP, SMS, EMAIL
  phoneNumber?: string;
}

export interface TwoFactorSetupResponse {
  method: string;
  secret?: string;
  qrCodeUrl?: string;
  backupCodes: string[];
  message?: string;
}

export interface TwoFactorEnableRequest {
  code: string;
}

export interface TwoFactorVerifyRequest {
  username: string;
  code: string;
  useBackupCode: boolean;
}
