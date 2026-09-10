import React from 'react';

const KPICard = ({ title, value, icon: Icon, trend, trendLabel, color = 'primary' }) => {
  const isPositive = trend > 0;
  const colorVariants = {
    primary: 'bg-primary-50 text-primary-600',
    success: 'bg-success-50 text-success-600',
    danger: 'bg-danger-50 text-danger-600',
    warning: 'bg-warning-50 text-warning-600',
    info: 'bg-info-50 text-info-600',
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <p className="text-gray-600 text-sm font-medium mb-1">{title}</p>
          <h3 className="text-3xl font-bold text-gray-900">{value}</h3>
        </div>
        {Icon && (
          <div className={`p-3 rounded-lg ${colorVariants[color]}`}>
            <Icon className="w-6 h-6" />
          </div>
        )}
      </div>

      {trend !== undefined && (
        <div className="flex items-center gap-2">
          <span
            className={`text-sm font-semibold ${
              isPositive ? 'text-success-600' : 'text-danger-600'
            }`}
          >
            {isPositive ? '↑' : '↓'} {Math.abs(trend)}%
          </span>
          {trendLabel && (
            <span className="text-xs text-gray-500">{trendLabel}</span>
          )}
        </div>
      )}
    </div>
  );
};

export default KPICard;
