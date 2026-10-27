import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, AuthResponse, RoleName } from '../types';
import { apiClient } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isManager: boolean;
  isUser: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('access_token');
  });
  const [loading, setLoading] = useState(true);

  const fetchCurrentUser = async () => {
    try {
      if (localStorage.getItem('access_token')) {
        const res = await apiClient.get<User>('/auth/me');
        setUser(res.data);
        localStorage.setItem('user', JSON.stringify(res.data));
      }
    } catch {
      setUser(null);
      setToken(null);
      localStorage.removeItem('user');
      localStorage.removeItem('access_token');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCurrentUser();
  }, []);

  const login = async (email: string, password: string) => {
    const res = await apiClient.post<AuthResponse>('/auth/login', { email, password });
    const { accessToken, user: authUser } = res.data;
    setToken(accessToken);
    setUser(authUser);
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('user', JSON.stringify(authUser));
  };

  const register = async (fullName: string, email: string, password: string) => {
    const res = await apiClient.post<AuthResponse>('/auth/register', { fullName, email, password });
    const { accessToken, user: authUser } = res.data;
    setToken(accessToken);
    setUser(authUser);
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('user', JSON.stringify(authUser));
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore network errors on logout
    } finally {
      setUser(null);
      setToken(null);
      localStorage.removeItem('access_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
  };

  const hasRole = (role: RoleName) => {
    return user?.roles?.includes(role) ?? false;
  };

  const isAdmin = hasRole('ROLE_ADMIN');
  const isManager = hasRole('ROLE_MANAGER');
  const isUser = hasRole('ROLE_USER');
  const isAuthenticated = !!token && !!user;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
        isManager,
        isUser,
        loading,
        login,
        register,
        logout,
        refreshProfile: fetchCurrentUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
