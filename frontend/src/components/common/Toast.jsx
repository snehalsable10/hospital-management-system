import React from 'react';
import { CheckCircle, AlertCircle, AlertTriangle, Info, X } from 'lucide-react';
import { useNotification } from '../../hooks/useNotification';

const Toast = ({ id, message, type = 'info' }) => {
  const { removeToast } = useNotification();

  const icons = {
    success: <CheckCircle className="w-5 h-5" />,
    error: <AlertCircle className="w-5 h-5" />,
    warning: <AlertTriangle className="w-5 h-5" />,
    info: <Info className="w-5 h-5" />,
  };

  const styles = {
    success: 'bg-success-50 border-success-200 text-success-900',
    error: 'bg-danger-50 border-danger-200 text-danger-900',
    warning: 'bg-warning-50 border-warning-200 text-warning-900',
    info: 'bg-info-50 border-info-200 text-info-900',
  };

  const iconColors = {
    success: 'text-success-600',
    error: 'text-danger-600',
    warning: 'text-warning-600',
    info: 'text-info-600',
  };

  return (
    <div
      className={`animate-fadeIn flex items-start gap-3 px-4 py-3 rounded-lg border ${styles[type]} shadow-lg max-w-md`}
    >
      <span className={iconColors[type]}>{icons[type]}</span>
      <div className="flex-1">
        <p className="text-sm font-medium">{message}</p>
      </div>
      <button
        onClick={() => removeToast(id)}
        className="p-1 hover:bg-white hover:bg-opacity-50 rounded transition-colors"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

export default Toast;
