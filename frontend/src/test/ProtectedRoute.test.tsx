import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ProtectedRoute } from '../routes/ProtectedRoute';
import * as AuthContext from '../context/AuthContext';
import { MemoryRouter } from 'react-router-dom';

describe('ProtectedRoute', () => {
  it('renders loading state when auth is loading', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: null,
      token: null,
      isAuthenticated: false,
      isAdmin: false,
      isManager: false,
      isUser: false,
      loading: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Authenticating session...')).toBeInTheDocument();
  });

  it('renders access denied when user lacks required role', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 1,
        email: 'user@test.com',
        fullName: 'Test User',
        active: true,
        roles: ['ROLE_USER'],
        createdAt: new Date().toISOString(),
      },
      token: 'valid-token',
      isAuthenticated: true,
      isAdmin: false,
      isManager: false,
      isUser: true,
      loading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
          <div>Admin Only Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(screen.queryByText('Admin Only Content')).not.toBeInTheDocument();
  });

  it('renders protected children when user possesses required role', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 1,
        email: 'admin@test.com',
        fullName: 'Admin User',
        active: true,
        roles: ['ROLE_ADMIN'],
        createdAt: new Date().toISOString(),
      },
      token: 'valid-token',
      isAuthenticated: true,
      isAdmin: true,
      isManager: false,
      isUser: false,
      loading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter>
        <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
          <div>Admin Only Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Admin Only Content')).toBeInTheDocument();
  });
});
