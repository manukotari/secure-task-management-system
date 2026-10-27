import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  CheckSquare,
  Users,
  Shield,
  History,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { isAdmin, isManager } = useAuth();

  const navItems = [
    {
      label: 'Dashboard',
      path: '/dashboard',
      icon: LayoutDashboard,
      show: true,
    },
    {
      label: 'Tasks',
      path: '/tasks',
      icon: CheckSquare,
      show: true,
    },
    {
      label: 'Teams',
      path: '/teams',
      icon: Users,
      show: isAdmin || isManager,
    },
    {
      label: 'User Management',
      path: '/users',
      icon: Shield,
      show: isAdmin,
    },
    {
      label: 'System Audit Logs',
      path: '/audit-logs',
      icon: History,
      show: isAdmin,
    },
  ];

  return (
    <aside className="w-64 bg-white border-r border-slate-200 min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between shrink-0">
      <nav className="space-y-1.5">
        <div className="px-3 py-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Workspace
        </div>
        {navItems
          .filter((item) => item.show)
          .map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-200'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
      </nav>

      <div className="bg-slate-50 rounded-xl p-3.5 border border-slate-100 text-xs text-slate-500">
        <div className="font-semibold text-slate-700 mb-1">Security Standard</div>
        <p className="leading-relaxed">
          Zero-trust authorization verified server-side on every request.
        </p>
      </div>
    </aside>
  );
};
