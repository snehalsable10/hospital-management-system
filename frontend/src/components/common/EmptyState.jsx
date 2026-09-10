import React from 'react';
import { Database } from 'lucide-react';
import Button from './Button';

const EmptyState = ({
  icon: Icon = Database,
  title = 'No data found',
  description = 'No records to display',
  action,
  actionLabel = 'Create New',
}) => {
  return (
    <div className="empty-state py-16">
      <Icon className="empty-state-icon" />
      <h3 className="empty-state-title">{title}</h3>
      <p className="empty-state-text">{description}</p>
      {action && (
        <Button onClick={action} variant="primary" size="md">
          {actionLabel}
        </Button>
      )}
    </div>
  );
};

export default EmptyState;
