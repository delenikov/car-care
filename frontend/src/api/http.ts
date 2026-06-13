import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '../types';

const accessTokenKey = 'carcare.accessToken';
const refreshTokenKey = 'carcare.refreshToken';

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

const isApiResponse = <T>(value: T | ApiResponse<T>): value is ApiResponse<T> => {
  return Boolean(value && typeof value === 'object' && 'success' in value && 'message' in value && 'data' in value);
};

export const tokenStorage = {
  get: () => sessionStorage.getItem(accessTokenKey),
  set: (token: string) => sessionStorage.setItem(accessTokenKey, token),
  clear: () => sessionStorage.removeItem(accessTokenKey)
};

export const refreshTokenStorage = {
  get: () => sessionStorage.getItem(refreshTokenKey),
  set: (token: string) => sessionStorage.setItem(refreshTokenKey, token),
  clear: () => sessionStorage.removeItem(refreshTokenKey)
};

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;
    const authUrl = original?.url ?? '';
    const shouldRefresh = error.response?.status === 401
      && original
      && !original._retry
      && !authUrl.includes('/api/auth/change-password')
      && !authUrl.includes('/api/auth/login')
      && !authUrl.includes('/api/auth/refresh');
    if (!shouldRefresh) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      const refreshToken = refreshTokenStorage.get();
      if (!refreshToken) {
        throw new Error('Missing refresh token');
      }
      const { data } = await axios.post<ApiResponse<AuthResponse>>(
        '/api/auth/refresh',
        { refreshToken },
        { baseURL: http.defaults.baseURL, withCredentials: true }
      );
      tokenStorage.set(data.data.accessToken);
      refreshTokenStorage.set(data.data.refreshToken);
      original.headers.Authorization = `Bearer ${data.data.accessToken}`;
      return http(original);
    } catch (refreshError) {
      tokenStorage.clear();
      refreshTokenStorage.clear();
      return Promise.reject(refreshError);
    }
  }
);

export const unwrap = <T>(request: Promise<AxiosResponse<T>>) => request.then((response) => {
  const body = response.data as T | ApiResponse<T>;
  return isApiResponse(body) ? body.data : body;
});
