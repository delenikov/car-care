import axios, { type AxiosAdapter, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { http, refreshTokenStorage, tokenStorage } from './http';

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
});
