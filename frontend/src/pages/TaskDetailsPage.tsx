import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiClient, getErrorMessage } from '../api/client';
import { Task, Comment, TaskAuditLog, TaskStatus, TaskPriority, Team, User, PagedResponse } from '../types';
import { PriorityBadge, StatusBadge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import {
  ArrowLeft,
  Calendar,
  User as UserIcon,
  Shield,
  MessageSquare,
  History,
  Send,
  Edit3,
  Clock,
  AlertCircle,
} from 'lucide-react';

export const TaskDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, isAdmin, isManager } = useAuth();

  const [task, setTask] = useState<Task | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [auditLogs, setAuditLogs] = useState<TaskAuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // New Comment state
  const [commentText, setCommentText] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  // Active Tab: Comments or Audit History
  const [activeTab, setActiveTab] = useState<'comments' | 'history'>('comments');

  // Edit Task Modal state
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const [editTitle, setEditTitle] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editPriority, setEditPriority] = useState<TaskPriority>('MEDIUM');
  const [editStatus, setEditStatus] = useState<TaskStatus>('TODO');
  const [editTeamId, setEditTeamId] = useState<string>('');
  const [editAssigneeId, setEditAssigneeId] = useState<string>('');
  const [editDueDate, setEditDueDate] = useState('');

  const [teams, setTeams] = useState<Team[]>([]);
  const [assignableUsers, setAssignableUsers] = useState<User[]>([]);

  const fetchTaskDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const [taskRes, commentsRes, historyRes] = await Promise.all([
        apiClient.get<Task>(`/tasks/${id}`),
        apiClient.get<Comment[]>(`/tasks/${id}/comments`),
        apiClient.get<TaskAuditLog[]>(`/tasks/${id}/history`),
      ]);
      setTask(taskRes.data);
      setComments(commentsRes.data);
      setAuditLogs(historyRes.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchTaskDetails();
  }, [fetchTaskDetails]);

  const handleStatusChange = async (newStatus: TaskStatus) => {
    if (!task) return;
    try {
      const res = await apiClient.patch<Task>(`/tasks/${task.id}/status`, { status: newStatus });
      setTask(res.data);
      // Refresh audit logs
      const historyRes = await apiClient.get<TaskAuditLog[]>(`/tasks/${task.id}/history`);
      setAuditLogs(historyRes.data);
    } catch (err) {
      alert(getErrorMessage(err));
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim() || !task) return;
    setCommentLoading(true);
    try {
      const res = await apiClient.post<Comment>(`/tasks/${task.id}/comments`, {
        content: commentText.trim(),
      });
      setComments((prev) => [...prev, res.data]);
      setCommentText('');
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setCommentLoading(false);
    }
  };

  const openEditModal = async () => {
    if (!task) return;
    setIsEditOpen(true);
    setEditError(null);
    setEditTitle(task.title);
    setEditDesc(task.description || '');
    setEditPriority(task.priority);
    setEditStatus(task.status);
    setEditTeamId(task.teamId ? task.teamId.toString() : '');
    setEditAssigneeId(task.assignee?.id ? task.assignee.id.toString() : '');
    setEditDueDate(task.dueDate ? task.dueDate.split('T')[0] : '');

    try {
      const [teamsRes, usersRes] = await Promise.all([
        apiClient.get<PagedResponse<Team>>('/teams?size=100'),
        apiClient.get<User[]>('/users/team-eligible'),
      ]);
      setTeams(teamsRes.data.content);
      setAssignableUsers(usersRes.data);
    } catch {
      // Fallback
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!task) return;
    setEditLoading(true);
    setEditError(null);
    try {
      const payload: Record<string, unknown> = {
        title: editTitle,
        description: editDesc,
        priority: editPriority,
        status: editStatus,
        teamId: editTeamId ? parseInt(editTeamId) : null,
        assigneeId: editAssigneeId ? parseInt(editAssigneeId) : null,
        dueDate: editDueDate ? new Date(editDueDate).toISOString() : null,
      };

      const res = await apiClient.put<Task>(`/tasks/${task.id}`, payload);
      setTask(res.data);
      setIsEditOpen(false);
      // Refresh audit logs
      const historyRes = await apiClient.get<TaskAuditLog[]>(`/tasks/${task.id}/history`);
      setAuditLogs(historyRes.data);
    } catch (err) {
      setEditError(getErrorMessage(err));
    } finally {
      setEditLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (error || !task) {
    return (
      <div className="p-6 max-w-xl mx-auto">
        <div className="bg-rose-50 border border-rose-200 text-rose-700 p-6 rounded-2xl text-center space-y-3">
          <AlertCircle className="w-10 h-10 mx-auto text-rose-600" />
          <h2 className="text-lg font-bold">Access Denied or Not Found</h2>
          <p className="text-sm">{error || 'This task does not exist or you do not have permission to view it.'}</p>
          <Link
            to="/tasks"
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-sm font-semibold hover:bg-indigo-700"
          >
            <ArrowLeft className="w-4 h-4" /> Return to Tasks
          </Link>
        </div>
      </div>
    );
  }

  const isAssignedToMe = task.assignee?.id === user?.id;
  const canModify = isAdmin || isManager;
  const canUpdateStatus = isAdmin || isManager || isAssignedToMe;

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Top Breadcrumb & Actions */}
      <div className="flex items-center justify-between">
        <Link
          to="/tasks"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Task List</span>
        </Link>

        {canModify && (
          <button
            onClick={openEditModal}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-semibold shadow-sm transition-all"
          >
            <Edit3 className="w-3.5 h-3.5" />
            <span>Edit Task</span>
          </button>
        )}
      </div>

      {/* Main Task Header Card */}
      <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <PriorityBadge priority={task.priority} />
              <StatusBadge status={task.status} overdue={task.overdue} />
              {task.teamName && (
                <span className="text-xs font-medium px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                  {task.teamName}
                </span>
              )}
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              {task.title}
            </h1>
          </div>

          {/* Quick Status Dropdown */}
          {canUpdateStatus && (
            <div className="flex items-center gap-2 self-start sm:self-auto bg-slate-50 p-2 rounded-xl border border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider pl-1">
                Status:
              </span>
              <select
                value={task.status}
                onChange={(e) => handleStatusChange(e.target.value as TaskStatus)}
                className="text-xs font-bold bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="IN_REVIEW">In Review</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
          )}
        </div>

        {/* Task Description */}
        <div className="prose prose-slate max-w-none text-sm text-slate-700 leading-relaxed bg-slate-50/50 p-4 rounded-xl border border-slate-100">
          {task.description || <span className="italic text-slate-400">No description provided.</span>}
        </div>

        {/* Meta Attributes Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-slate-100 text-xs">
          <div>
            <div className="text-slate-400 font-medium mb-1">Assignee</div>
            <div className="font-semibold text-slate-800 flex items-center gap-1.5">
              <UserIcon className="w-3.5 h-3.5 text-slate-400" />
              <span>{task.assignee?.fullName || 'Unassigned'}</span>
            </div>
          </div>

          <div>
            <div className="text-slate-400 font-medium mb-1">Created By</div>
            <div className="font-semibold text-slate-800 flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-slate-400" />
              <span>{task.creator?.fullName || 'System'}</span>
            </div>
          </div>

          <div>
            <div className="text-slate-400 font-medium mb-1">Due Date</div>
            <div className={`font-semibold flex items-center gap-1.5 ${task.overdue ? 'text-rose-600' : 'text-slate-800'}`}>
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              <span>{task.dueDate ? new Date(task.dueDate).toLocaleDateString() : 'None'}</span>
            </div>
          </div>

          <div>
            <div className="text-slate-400 font-medium mb-1">Completion</div>
            <div className="font-semibold text-slate-800 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              <span>
                {task.completedAt ? new Date(task.completedAt).toLocaleDateString() : 'Incomplete'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs: Discussion vs Audit Log */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="flex border-b border-slate-200 bg-slate-50/50 px-6">
          <button
            onClick={() => setActiveTab('comments')}
            className={`py-3.5 px-4 text-xs font-bold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'comments'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            <span>Comments ({comments.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`py-3.5 px-4 text-xs font-bold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'history'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <History className="w-4 h-4" />
            <span>Activity Trail ({auditLogs.length})</span>
          </button>
        </div>

        <div className="p-6">
          {activeTab === 'comments' ? (
            <div className="space-y-6">
              {/* Add Comment Input */}
              <form onSubmit={handleAddComment} className="space-y-3">
                <textarea
                  rows={3}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Leave a project comment or update..."
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:bg-white text-slate-900"
                />
                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={commentLoading || !commentText.trim()}
                    className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>{commentLoading ? 'Posting...' : 'Post Comment'}</span>
                  </button>
                </div>
              </form>

              {/* Comments Timeline */}
              {comments.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  No comments yet. Start the conversation above!
                </div>
              ) : (
                <div className="space-y-4 divide-y divide-slate-100">
                  {comments.map((comment) => (
                    <div key={comment.id} className="pt-4 first:pt-0 space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-slate-800">
                          {comment.author?.fullName || 'Anonymous'}
                        </span>
                        <span className="text-slate-400">
                          {new Date(comment.createdAt).toLocaleString()}
                        </span>
                      </div>
                      <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">
                        {comment.content}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : (
            /* Audit Log History */
            <div className="space-y-4">
              {auditLogs.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  No audit history records available.
                </div>
              ) : (
                <div className="space-y-3">
                  {auditLogs.map((log) => (
                    <div
                      key={log.id}
                      className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs flex items-start justify-between gap-4"
                    >
                      <div className="space-y-0.5">
                        <div className="font-bold text-slate-800">
                          {log.action} {log.fieldChanged && `(${log.fieldChanged})`}
                        </div>
                        {log.oldValue && log.newValue && (
                          <div className="text-slate-500">
                            Changed from <span className="font-semibold text-slate-700">{log.oldValue}</span> to{' '}
                            <span className="font-semibold text-indigo-600">{log.newValue}</span>
                          </div>
                        )}
                        <div className="text-slate-400 text-[11px]">Actor: {log.actorEmail}</div>
                      </div>
                      <span className="text-slate-400 shrink-0">
                        {new Date(log.createdAt).toLocaleString()}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Edit Task Modal */}
      <Modal isOpen={isEditOpen} onClose={() => setIsEditOpen(false)} title="Edit Task Details">
        {editError && (
          <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-xl text-xs font-medium">
            {editError}
          </div>
        )}

        <form onSubmit={handleEditSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Title *
            </label>
            <input
              type="text"
              required
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-900"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Description
            </label>
            <textarea
              rows={3}
              value={editDesc}
              onChange={(e) => setEditDesc(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-900"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Priority
              </label>
              <select
                value={editPriority}
                onChange={(e) => setEditPriority(e.target.value as TaskPriority)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Status
              </label>
              <select
                value={editStatus}
                onChange={(e) => setEditStatus(e.target.value as TaskStatus)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="IN_REVIEW">In Review</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Team
              </label>
              <select
                value={editTeamId}
                onChange={(e) => setEditTeamId(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
              >
                <option value="">None (Personal)</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Assignee
              </label>
              <select
                value={editAssigneeId}
                onChange={(e) => setEditAssigneeId(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
              >
                <option value="">Unassigned</option>
                {assignableUsers.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.fullName} ({u.email})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Due Date
            </label>
            <input
              type="date"
              value={editDueDate}
              onChange={(e) => setEditDueDate(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsEditOpen(false)}
              className="px-4 py-2 border border-slate-200 rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={editLoading}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold shadow-sm disabled:opacity-50"
            >
              {editLoading ? 'Saving...' : 'Update Task'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
