import React from 'react';
import { TaskPriority, TaskStatus, RoleName } from '../../types';

export const PriorityBadge: React.FC<{ priority: TaskPriority }> = ({ priority }) => {
  const styles: Record<TaskPriority, string> = {
    LOW: 'bg-slate-100 text-slate-700 border-slate-200',
    MEDIUM: 'bg-blue-50 text-blue-700 border-blue-200',
    HIGH: 'bg-amber-50 text-amber-700 border-amber-200',
    CRITICAL: 'bg-rose-50 text-rose-700 border-rose-200 font-semibold',
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${styles[priority]}`}>
      {priority}
    </span>
  );
};

export const StatusBadge: React.FC<{ status: TaskStatus; overdue?: boolean }> = ({ status, overdue }) => {
  const styles: Record<TaskStatus, string> = {
    TODO: 'bg-slate-100 text-slate-700 border-slate-200',
    IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    IN_REVIEW: 'bg-purple-50 text-purple-700 border-purple-200',
    COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  };

  const labels: Record<TaskStatus, string> = {
    TODO: 'To Do',
    IN_PROGRESS: 'In Progress',
    IN_REVIEW: 'In Review',
    COMPLETED: 'Completed',
  };

  return (
    <div className="inline-flex items-center gap-1.5">
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${styles[status]}`}>
        {labels[status]}
      </span>
      {overdue && status !== 'COMPLETED' && (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-rose-600 text-white animate-pulse">
          OVERDUE
        </span>
      )}
    </div>
  );
};

export const RoleBadge: React.FC<{ role: RoleName }> = ({ role }) => {
  const styles: Record<RoleName, string> = {
    ROLE_ADMIN: 'bg-purple-100 text-purple-800 border-purple-300 font-bold',
    ROLE_MANAGER: 'bg-indigo-100 text-indigo-800 border-indigo-300 font-semibold',
    ROLE_USER: 'bg-emerald-100 text-emerald-800 border-emerald-300',
  };

  const labels: Record<RoleName, string> = {
    ROLE_ADMIN: 'ADMIN',
    ROLE_MANAGER: 'MANAGER',
    ROLE_USER: 'USER',
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-xs border ${styles[role] || 'bg-slate-100 text-slate-700'}`}>
      {labels[role] || role}
    </span>
  );
};
