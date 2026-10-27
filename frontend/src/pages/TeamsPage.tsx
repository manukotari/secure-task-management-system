import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient, getErrorMessage } from '../api/client';
import { Team, User, PagedResponse } from '../types';
import { Modal } from '../components/common/Modal';
import { Users, Plus, Shield, UserPlus, Trash2, AlertCircle } from 'lucide-react';

export const TeamsPage: React.FC = () => {
  const { user, isAdmin, isManager } = useAuth();

  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create Team Modal (Admin only)
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createManagerId, setCreateManagerId] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Add Member Modal
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);
  const [addUserId, setAddUserId] = useState('');
  const [addLoading, setAddLoading] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  // Available users for dropdowns
  const [availableUsers, setAvailableUsers] = useState<User[]>([]);

  const fetchTeams = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<PagedResponse<Team>>('/teams?size=50');
      setTeams(res.data.content);
      if (res.data.content.length > 0 && !selectedTeam) {
        fetchTeamDetails(res.data.content[0].id);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [selectedTeam]);

  useEffect(() => {
    fetchTeams();
  }, [fetchTeams]);

  const fetchTeamDetails = async (teamId: number) => {
    try {
      const res = await apiClient.get<Team>(`/teams/${teamId}`);
      setSelectedTeam(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadUsersForDropdown = async () => {
    try {
      const res = await apiClient.get<PagedResponse<User>>('/users?size=100');
      setAvailableUsers(res.data.content);
      if (res.data.content.length > 0) {
        setCreateManagerId(res.data.content[0].id.toString());
        setAddUserId(res.data.content[0].id.toString());
      }
    } catch {
      // Fallback
    }
  };

  const handleCreateTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    setCreateLoading(true);
    try {
      await apiClient.post('/teams', {
        name: createName,
        description: createDesc,
        managerId: parseInt(createManagerId),
      });
      setIsCreateOpen(false);
      setCreateName('');
      setCreateDesc('');
      fetchTeams();
    } catch (err) {
      setCreateError(getErrorMessage(err));
    } finally {
      setCreateLoading(false);
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTeam) return;
    setAddError(null);
    setAddLoading(true);
    try {
      const res = await apiClient.post<Team>(`/teams/${selectedTeam.id}/members`, {
        userId: parseInt(addUserId),
      });
      setSelectedTeam(res.data);
      setIsAddMemberOpen(false);
      fetchTeams();
    } catch (err) {
      setAddError(getErrorMessage(err));
    } finally {
      setAddLoading(false);
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!selectedTeam) return;
    if (!window.confirm('Remove this user from the team?')) return;
    try {
      const res = await apiClient.delete<Team>(`/teams/${selectedTeam.id}/members/${userId}`);
      setSelectedTeam(res.data);
      fetchTeams();
    } catch (err) {
      alert(getErrorMessage(err));
    }
  };

  const canManageSelectedTeam =
    isAdmin || (isManager && selectedTeam?.manager?.id === user?.id);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Teams</h1>
          <p className="text-sm text-slate-500 mt-1">
            Functional working groups and member allocations
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={() => {
              setIsCreateOpen(true);
              loadUsersForDropdown();
            }}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold shadow-sm transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Create Team</span>
          </button>
        )}
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-700 p-4 rounded-xl text-sm font-medium flex items-center gap-2">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Grid: Team List and Team Detail View */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Teams List */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 space-y-2">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider px-2 py-1">
            Available Teams ({teams.length})
          </h2>

          {loading ? (
            <div className="p-8 text-center text-sm text-slate-400">Loading teams...</div>
          ) : teams.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-400">No teams found.</div>
          ) : (
            <div className="space-y-1">
              {teams.map((t) => (
                <button
                  key={t.id}
                  onClick={() => fetchTeamDetails(t.id)}
                  className={`w-full text-left p-3 rounded-xl transition-all flex items-center justify-between ${
                    selectedTeam?.id === t.id
                      ? 'bg-indigo-50 border border-indigo-200 text-indigo-900 font-semibold shadow-sm'
                      : 'hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="min-w-0">
                    <div className="text-sm truncate">{t.name}</div>
                    <div className="text-xs text-slate-500 truncate mt-0.5">
                      Manager: {t.manager?.fullName || 'None'}
                    </div>
                  </div>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
                    {t.memberCount} members
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Selected Team Details */}
        <div className="lg:col-span-2">
          {selectedTeam ? (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div>
                  <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
                    {selectedTeam.name}
                  </h2>
                  <p className="text-sm text-slate-600 mt-1">
                    {selectedTeam.description || 'No description provided.'}
                  </p>
                  <div className="flex items-center gap-2 mt-3 text-xs text-indigo-700 font-semibold bg-indigo-50 px-3 py-1 rounded-lg w-fit border border-indigo-200">
                    <Shield className="w-3.5 h-3.5" />
                    <span>Manager: {selectedTeam.manager?.fullName} ({selectedTeam.manager?.email})</span>
                  </div>
                </div>

                {canManageSelectedTeam && (
                  <button
                    onClick={() => {
                      setIsAddMemberOpen(true);
                      loadUsersForDropdown();
                    }}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition-all"
                  >
                    <UserPlus className="w-3.5 h-3.5" />
                    <span>Add Member</span>
                  </button>
                )}
              </div>

              {/* Members Table */}
              <div className="pt-4 border-t border-slate-100 space-y-3">
                <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
                  <Users className="w-4 h-4 text-indigo-600" />
                  <span>Team Members ({selectedTeam.members?.length || 0})</span>
                </h3>

                {selectedTeam.members && selectedTeam.members.length > 0 ? (
                  <div className="divide-y divide-slate-100">
                    {selectedTeam.members.map((member) => (
                      <div
                        key={member.id}
                        className="py-3 flex items-center justify-between gap-4"
                      >
                        <div>
                          <div className="text-sm font-semibold text-slate-800">
                            {member.fullName}
                          </div>
                          <div className="text-xs text-slate-500">{member.email}</div>
                        </div>

                        {canManageSelectedTeam && (
                          <button
                            onClick={() => handleRemoveMember(member.id)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
                            title="Remove Member"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-6 text-xs text-slate-400">
                    No members assigned to this team yet.
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-12 text-center text-slate-400 text-sm">
              Select a team on the left to inspect its details and members.
            </div>
          )}
        </div>
      </div>

      {/* Create Team Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Team">
        {createError && (
          <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-xl text-xs font-medium">
            {createError}
          </div>
        )}

        <form onSubmit={handleCreateTeam} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Team Name *
            </label>
            <input
              type="text"
              required
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              placeholder="e.g. Infrastructure Platform"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-900"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Description
            </label>
            <textarea
              rows={2}
              value={createDesc}
              onChange={(e) => setCreateDesc(e.target.value)}
              placeholder="Brief team purpose..."
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-900"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Designate Team Manager *
            </label>
            <select
              value={createManagerId}
              onChange={(e) => setCreateManagerId(e.target.value)}
              required
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
            >
              {availableUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.email})
                </option>
              ))}
            </select>
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsCreateOpen(false)}
              className="px-4 py-2 border border-slate-200 rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createLoading}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold shadow-sm disabled:opacity-50"
            >
              {createLoading ? 'Creating...' : 'Create Team'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Member Modal */}
      <Modal isOpen={isAddMemberOpen} onClose={() => setIsAddMemberOpen(false)} title="Add Team Member">
        {addError && (
          <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-xl text-xs font-medium">
            {addError}
          </div>
        )}

        <form onSubmit={handleAddMember} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Select User *
            </label>
            <select
              value={addUserId}
              onChange={(e) => setAddUserId(e.target.value)}
              required
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700"
            >
              {availableUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.email})
                </option>
              ))}
            </select>
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsAddMemberOpen(false)}
              className="px-4 py-2 border border-slate-200 rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={addLoading}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold shadow-sm disabled:opacity-50"
            >
              {addLoading ? 'Adding...' : 'Add Member'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
