import React from 'react';

const Card = ({
  title,
  value,
  icon: Icon,
  trend,
  trendLabel,
  className = '',
  onClick,
  children
}) => {
  const isPositive = trend > 0;

  return (
    <div
      onClick={onClick}
      className={`card bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow ${
        onClick ? 'cursor-pointer' : ''
      } ${className}`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <p className="text-gray-600 text-sm font-medium mb-1">{title}</p>
          <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
        </div>
        {Icon && (
          <div className="p-3 bg-primary-50 rounded-lg">
            <Icon className="w-6 h-6 text-primary-600" />
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

      {children}
    </div>
  );
};

export default Card;
