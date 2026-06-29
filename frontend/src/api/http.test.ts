import axios, { type AxiosAdapter, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiErrorMessage, apiFieldErrors, http, normalizeApiError, refreshTokenStorage, tokenStorage } from './http';

const ok = <T>(config: InternalAxiosRequestConfig, data: T): AxiosResponse<T> => ({
  data,
  status: 200,
  statusText: 'OK',
  headers: {},
  config
});

const unauthorized = (config: InternalAxiosRequestConfig) => Promise.reject({
  isAxiosError: true,
  config,
  response: { status: 401, data: { message: 'Unauthorized' }, headers: {}, config }
});

const jwtWithExpiration = (expiresAtSeconds: number) => {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  return `${encode({ alg: 'HS512' })}.${encode({ sub: 'admin@carcare.local', exp: expiresAtSeconds })}.signature`;
};

describe('HTTP token refresh', () => {
  const originalAdapter = http.defaults.adapter;

  afterEach(() => {
    http.defaults.adapter = originalAdapter;
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('shares one refresh request across simultaneous expired access-token responses', async () => {
    tokenStorage.set('expired-access-token');
    refreshTokenStorage.set('valid-refresh-token');
    const refreshSpy = vi.spyOn(axios, 'post').mockResolvedValue({
      data: {
        success: true,
        message: 'Token refreshed',
        data: {
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
          user: {
            id: '1',
            email: 'admin@carcare.local',
            fullName: 'Admin User',
            enabled: true,
            failedLoginAttempts: 0,
            roles: ['ROLE_ADMIN']
          }
        }
      }
    });
    const adapter = vi.fn<AxiosAdapter>(async (config) => {
      if (config.headers?.Authorization === 'Bearer expired-access-token') {
        return unauthorized(config);
      }
      return ok(config, { path: config.url, authorization: config.headers?.Authorization });
    });
    http.defaults.adapter = adapter;

    const [first, second] = await Promise.all([
      http.get('/api/customers'),
      http.get('/api/vehicles')
    ]);

    expect(refreshSpy).toHaveBeenCalledTimes(1);
    expect(first.data).toMatchObject({ authorization: 'Bearer new-access-token' });
    expect(second.data).toMatchObject({ authorization: 'Bearer new-access-token' });
    expect(tokenStorage.get()).toBe('new-access-token');
    expect(refreshTokenStorage.get()).toBe('new-refresh-token');
  });

  it('refreshes an expired access token before sending a protected request', async () => {
    tokenStorage.set(jwtWithExpiration(Math.floor(Date.now() / 1000) - 60));
    refreshTokenStorage.set('valid-refresh-token');
    vi.spyOn(axios, 'post').mockResolvedValue({
      data: {
        success: true,
        message: 'Token refreshed',
        data: {
          accessToken: 'fresh-access-token',
          refreshToken: 'fresh-refresh-token',
          user: {
            id: '1',
            email: 'admin@carcare.local',
            fullName: 'Admin User',
            enabled: true,
            failedLoginAttempts: 0,
            roles: ['ROLE_ADMIN']
          }
        }
      }
    });
    const adapter = vi.fn<AxiosAdapter>(async (config) => ok(config, { authorization: config.headers?.Authorization }));
    http.defaults.adapter = adapter;

    const response = await http.get('/api/customers');

    expect(response.data).toMatchObject({ authorization: 'Bearer fresh-access-token' });
    expect(adapter).toHaveBeenCalledTimes(1);
    expect(tokenStorage.get()).toBe('fresh-access-token');
    expect(refreshTokenStorage.get()).toBe('fresh-refresh-token');
  });
});

describe('HTTP API errors', () => {
  it('normalizes global exception handler responses', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          timestamp: '2026-06-19T10:00:00Z',
          status: 400,
          error: 'VALIDATION_ERROR',
          message: 'Валидацијата не успеа.',
          path: '/api/customers',
          fieldErrors: {
            fullName: 'Полето е задолжително',
            email: 'Внесете валидна е-пошта'
          }
        }
      }
    };

    const normalized = normalizeApiError(error);

    expect(normalized.message).toBe('Валидацијата не успеа.');
    expect(normalized.status).toBe(400);
    expect(normalized.code).toBe('VALIDATION_ERROR');
    expect(normalized.path).toBe('/api/customers');
    expect(normalized.fieldErrors).toEqual({
      fullName: 'Полето е задолжително',
      email: 'Внесете валидна е-пошта'
    });
  });

  it('exposes message and field errors through UI helpers', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 404,
        data: {
          status: 404,
          error: 'NOT_FOUND',
          message: 'Customer not found',
          path: '/api/customers/1',
          fieldErrors: {}
        }
      }
    };

    expect(apiErrorMessage(error, 'Fallback')).toBe('Customer not found');
    expect(apiFieldErrors(error)).toEqual({});
  });

  it('falls back for non API errors', () => {
    expect(apiErrorMessage({ isAxiosError: true, response: { status: 500, data: {} } }, 'Fallback')).toBe('Fallback');
  });
});
