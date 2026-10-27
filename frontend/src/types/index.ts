export type RoleName = 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_USER';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'COMPLETED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  active: boolean;
  roles: RoleName[];
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Team {
  id: number;
  name: string;
  description?: string;
  manager?: User;
  memberCount: number;
  members?: User[];
  createdAt: string;
}

export interface Task {
  id: number;
  title: string;
  description?: string;
  priority: TaskPriority;
  status: TaskStatus;
  creator?: User;
  assignee?: User;
  teamId?: number;
  teamName?: string;
  dueDate?: string;
  completedAt?: string;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
}

export interface Comment {
  id: number;
  taskId: number;
  author: User;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskAuditLog {
  id: number;
  taskId: number;
  action: string;
  fieldChanged?: string;
  oldValue?: string;
  newValue?: string;
  actorId?: number;
  actorEmail: string;
  createdAt: string;
}

export interface DashboardStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  inReviewTasks: number;
  overdueTasks: number;
  statusDistribution: Record<TaskStatus, number>;
  priorityDistribution: Record<TaskPriority, number>;
  recentTasks: Task[];
  totalUsers?: number;
  totalTeams?: number;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
