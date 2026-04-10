export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface TokenResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
