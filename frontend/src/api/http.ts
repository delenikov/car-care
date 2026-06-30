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

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiClientError extends Error {
  readonly status?: number;
  readonly code?: string;
  readonly path?: string;
  readonly fieldErrors: Record<string, string>;
  readonly originalError: unknown;

  constructor(message: string, options: { status?: number; code?: string; path?: string; fieldErrors?: Record<string, string>; originalError?: unknown } = {}) {
    super(message);
    this.name = 'ApiClientError';
    this.status = options.status;
    this.code = options.code;
    this.path = options.path;
    this.fieldErrors = options.fieldErrors ?? {};
    this.originalError = options.originalError;
  }
}

const isApiResponse = <T>(value: T | ApiResponse<T>): value is ApiResponse<T> => {
  return Boolean(value && typeof value === 'object' && 'success' in value && 'message' in value && 'data' in value);
};

const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object');

const stringRecord = (value: unknown): Record<string, string> => {
  if (!isRecord(value)) {
    return {};
  }
  return Object.entries(value).reduce<Record<string, string>>((result, [key, item]) => {
    if (typeof item === 'string') {
      result[key] = item;
    }
    return result;
  }, {});
};

const isApiErrorResponse = (value: unknown): value is ApiErrorResponse => {
  return isRecord(value)
    && typeof value.status === 'number'
    && typeof value.error === 'string'
    && typeof value.message === 'string';
};

export const normalizeApiError = (error: unknown, fallback = 'Барањето не успеа.'): ApiClientError => {
  if (error instanceof ApiClientError) {
    return error;
  }

  if (isRecord(error) && isRecord(error.response)) {
    const response = error.response;
    const data = response.data;
    if (isApiErrorResponse(data)) {
      return new ApiClientError(data.message || fallback, {
        status: data.status,
        code: data.error,
        path: data.path,
        fieldErrors: stringRecord(data.fieldErrors),
        originalError: error
      });
    }
    if (isRecord(data) && typeof data.message === 'string') {
      return new ApiClientError(data.message || fallback, {
        status: typeof response.status === 'number' ? response.status : undefined,
        fieldErrors: stringRecord(data.fieldErrors),
        originalError: error
      });
    }
  }

  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (isApiErrorResponse(data)) {
      return new ApiClientError(data.message || fallback, {
        status: data.status,
        code: data.error,
        path: data.path,
        fieldErrors: stringRecord(data.fieldErrors),
        originalError: error
      });
    }
    if (isRecord(data) && typeof data.message === 'string') {
      return new ApiClientError(data.message || fallback, {
        status: error.response?.status,
        fieldErrors: stringRecord(data.fieldErrors),
        originalError: error
      });
    }
    if (!error.response) {
      return new ApiClientError(error.message || fallback, { originalError: error });
    }
    return new ApiClientError(fallback, { status: error.response.status, originalError: error });
  }

  if (error instanceof Error) {
    return new ApiClientError(error.message || fallback, { originalError: error });
  }

  return new ApiClientError(fallback, { originalError: error });
};

export const apiErrorMessage = (error: unknown, fallback = 'Барањето не успеа.') => normalizeApiError(error, fallback).message;

export const apiFieldErrors = (error: unknown) => normalizeApiError(error).fieldErrors;

const authEndpointsWithoutAccessToken = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];
const accessTokenRefreshSkewSeconds = 30;

const shouldSendAccessToken = (url = '') => {
  return !authEndpointsWithoutAccessToken.some((endpoint) => url.includes(endpoint));
};

const accessTokenExpiresSoon = (token: string) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    if (!payload.exp) {
      return true;
    }
    return payload.exp * 1000 <= Date.now() + accessTokenRefreshSkewSeconds * 1000;
  } catch {
    return true;
  }
};

export const tokenStorage = {
  get: () => localStorage.getItem(accessTokenKey),
  set: (token: string) => localStorage.setItem(accessTokenKey, token),
  clear: () => localStorage.removeItem(accessTokenKey)
};

export const refreshTokenStorage = {
  get: () => localStorage.getItem(refreshTokenKey),
  set: (token: string) => localStorage.setItem(refreshTokenKey, token),
  clear: () => localStorage.removeItem(refreshTokenKey)
};

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
});

let refreshRequest: Promise<AuthResponse> | null = null;

export const refreshAccessToken = () => {
  if (!refreshRequest) {
    refreshRequest = (async () => {
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
      return data.data;
    })().finally(() => {
      refreshRequest = null;
    });
  }
  return refreshRequest;
};

http.interceptors.request.use(async (config) => {
  const url = config.url ?? '';
  if (!shouldSendAccessToken(url)) {
    delete config.headers.Authorization;
    return config;
  }

  let token = tokenStorage.get();
  if (token && accessTokenExpiresSoon(token)) {
    try {
      token = (await refreshAccessToken()).accessToken;
    } catch {
      tokenStorage.clear();
      refreshTokenStorage.clear();
      token = null;
    }
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
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
      return Promise.reject(normalizeApiError(error));
    }

    original._retry = true;
    try {
      const response = await refreshAccessToken();
      original.headers.Authorization = `Bearer ${response.accessToken}`;
      return http(original);
    } catch (refreshError) {
      tokenStorage.clear();
      refreshTokenStorage.clear();
      return Promise.reject(normalizeApiError(refreshError));
    }
  }
);

export const unwrap = <T>(request: Promise<AxiosResponse<T>>) => request.then((response) => {
  const body = response.data as T | ApiResponse<T>;
  return isApiResponse(body) ? body.data : body;
});
