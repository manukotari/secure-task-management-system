import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiClient, getErrorMessage } from '../api/client';
import { DashboardStats, TaskPriority, TaskStatus } from '../types';
import { PriorityBadge, StatusBadge } from '../components/common/Badge';
import {
  CheckCircle2,
  Clock,
  AlertTriangle,
  ListTodo,
  Layers,
  Users,
  Shield,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user, isAdmin, isManager } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await apiClient.get<DashboardStats>('/dashboard/stats');
        setStats(res.data);
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (error || !stats) {
    return (
      <div className="p-6">
        <div className="bg-rose-50 border border-rose-200 text-rose-700 p-4 rounded-xl text-sm font-medium">
          {error || 'Unable to load dashboard metrics.'}
        </div>
      </div>
    );
  }

  const completionRate = stats.totalTasks > 0
    ? Math.round((stats.completedTasks / stats.totalTasks) * 100)
    : 0;

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Welcome back, {user?.fullName}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {isAdmin && 'System Administrator Portal — Global cross-team oversight.'}
            {isManager && !isAdmin && 'Team Manager Dashboard — Overseeing authorized team deliverables.'}
            {!isAdmin && !isManager && 'Personal Task Workspace — Your assigned tasks and active progress.'}
          </p>
        </div>
        <Link
          to="/tasks"
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold shadow-sm transition-all"
        >
          <span>View All Tasks</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Tasks</div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.totalTasks}</div>
            <div className="text-xs text-indigo-600 font-medium flex items-center gap-1 mt-0.5">
              <TrendingUp className="w-3 h-3" /> {completionRate}% completed
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Completed</div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.completedTasks}</div>
            <div className="text-xs text-emerald-600 font-medium mt-0.5">Verified deliverable</div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">In Progress</div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.inProgressTasks}</div>
            <div className="text-xs text-blue-600 font-medium mt-0.5">Active development</div>
          </div>
        </div>

        <div className={`bg-white rounded-2xl p-5 border shadow-sm flex items-center gap-4 ${
          stats.overdueTasks > 0 ? 'border-rose-200 bg-rose-50/20' : 'border-slate-200'
        }`}>
          <div className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 ${
            stats.overdueTasks > 0 ? 'bg-rose-100 text-rose-600' : 'bg-slate-50 text-slate-400'
          }`}>
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Overdue</div>
            <div className={`text-2xl font-bold mt-0.5 ${stats.overdueTasks > 0 ? 'text-rose-600' : 'text-slate-900'}`}>
              {stats.overdueTasks}
            </div>
            <div className="text-xs font-medium mt-0.5 text-slate-500">
              {stats.overdueTasks > 0 ? 'Requires attention' : 'All on schedule'}
            </div>
          </div>
        </div>
      </div>

      {/* Admin Specific Metric Cards */}
      {isAdmin && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="bg-white rounded-2xl p-5 border border-purple-100 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0">
              <Users className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Registered Accounts</div>
              <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.totalUsers ?? 0}</div>
              <div className="text-xs text-purple-600 font-medium mt-0.5">Active enterprise directory</div>
            </div>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-indigo-100 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-400">Functional Teams</div>
              <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.totalTeams ?? 0}</div>
              <div className="text-xs text-indigo-600 font-medium mt-0.5">Configured project units</div>
            </div>
          </div>
        </div>
      )}

      {/* Breakdown Section: Status & Priority Distributions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Status Distribution */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
          <h3 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
            <ListTodo className="w-5 h-5 text-indigo-600" />
            <span>Task Status Breakdown</span>
          </h3>
          <div className="space-y-3.5">
            {(['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'COMPLETED'] as TaskStatus[]).map((status) => {
              const count = stats.statusDistribution[status] || 0;
              const percent = stats.totalTasks > 0 ? Math.round((count / stats.totalTasks) * 100) : 0;
              const barColors: Record<TaskStatus, string> = {
                TODO: 'bg-slate-400',
                IN_PROGRESS: 'bg-indigo-600',
                IN_REVIEW: 'bg-purple-600',
                COMPLETED: 'bg-emerald-500',
              };

              return (
                <div key={status} className="space-y-1">
                  <div className="flex justify-between text-xs font-medium">
                    <span className="text-slate-600">{status.replace('_', ' ')}</span>
                    <span className="text-slate-900 font-semibold">{count} ({percent}%)</span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full transition-all duration-500 ${barColors[status]}`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Priority Distribution */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
          <h3 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-amber-500" />
            <span>Task Priority Breakdown</span>
          </h3>
          <div className="space-y-3.5">
            {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as TaskPriority[]).map((priority) => {
              const count = stats.priorityDistribution[priority] || 0;
              const percent = stats.totalTasks > 0 ? Math.round((count / stats.totalTasks) * 100) : 0;
              const barColors: Record<TaskPriority, string> = {
                CRITICAL: 'bg-rose-500',
                HIGH: 'bg-amber-500',
                MEDIUM: 'bg-blue-500',
                LOW: 'bg-slate-400',
              };

              return (
                <div key={priority} className="space-y-1">
                  <div className="flex justify-between text-xs font-medium">
                    <span className="text-slate-600">{priority}</span>
                    <span className="text-slate-900 font-semibold">{count} ({percent}%)</span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full transition-all duration-500 ${barColors[priority]}`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Recent Tasks List */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-bold text-slate-900">Recent Tasks</h3>
          <Link to="/tasks" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">
            View all →
          </Link>
        </div>

        {stats.recentTasks && stats.recentTasks.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {stats.recentTasks.map((task) => (
              <div key={task.id} className="py-3.5 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <Link
                    to={`/tasks/${task.id}`}
                    className="text-sm font-semibold text-slate-900 hover:text-indigo-600 transition-colors truncate block"
                  >
                    {task.title}
                  </Link>
                  <div className="flex items-center gap-2 mt-1 text-xs text-slate-500">
                    {task.teamName && <span>Team: {task.teamName}</span>}
                    {task.assignee && <span>Assignee: {task.assignee.fullName}</span>}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <PriorityBadge priority={task.priority} />
                  <StatusBadge status={task.status} overdue={task.overdue} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-sm text-slate-500">
            No recent tasks found.
          </div>
        )}
      </div>
    </div>
  );
};
