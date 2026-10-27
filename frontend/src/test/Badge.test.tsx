import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PriorityBadge, StatusBadge, RoleBadge } from '../components/common/Badge';

describe('Badge Components', () => {
  it('renders PriorityBadge with correct label', () => {
    render(<PriorityBadge priority="CRITICAL" />);
    expect(screen.getByText('CRITICAL')).toBeInTheDocument();
  });

  it('renders StatusBadge with overdue indicator when overdue', () => {
    render(<StatusBadge status="IN_PROGRESS" overdue={true} />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('OVERDUE')).toBeInTheDocument();
  });

  it('renders RoleBadge with human-readable label', () => {
    render(<RoleBadge role="ROLE_ADMIN" />);
    expect(screen.getByText('ADMIN')).toBeInTheDocument();
  });
});
