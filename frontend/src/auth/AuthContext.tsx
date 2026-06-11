import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useMutation } from '@tanstack/react-query';
import { authApi, type LoginPayload } from '../api/modules';
import { refreshTokenStorage, tokenStorage } from '../api/http';
import type { User } from '../types';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  isReady: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  logout: () => Promise<void>;
}

const userKey = 'carcare.user';
const AuthContext = createContext<AuthContextValue | null>(null);

const readStoredUser = () => {
  const value = sessionStorage.getItem(userKey);
  return value ? (JSON.parse(value) as User) : null;
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    if (tokenStorage.get()) {
      setUser(readStoredUser());
    }
    setIsReady(true);
  }, []);

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (response) => {
      tokenStorage.set(response.accessToken);
      refreshTokenStorage.set(response.refreshToken);
      sessionStorage.setItem(userKey, JSON.stringify(response.user));
      setUser(response.user);
    }
  });

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user && tokenStorage.get()),
      isReady,
      login: async (payload) => {
        await loginMutation.mutateAsync(payload);
      },
      logout: async () => {
        await authApi.logout();
        sessionStorage.removeItem(userKey);
        setUser(null);
      }
    }),
    [isReady, loginMutation, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
