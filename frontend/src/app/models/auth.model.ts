export interface LoginRequest {
  username: string;
  password: string;
  authMethod: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  roles: string[];
  privileges: string[];
  authMethod: string;
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
