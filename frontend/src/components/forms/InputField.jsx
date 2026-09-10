import React from 'react';

const InputField = ({
  label,
  type = 'text',
  placeholder,
  error,
  touched,
  disabled = false,
  required = false,
  icon: Icon,
  ...props
}) => {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-2">
          {label}
          {required && <span className="text-danger-600">*</span>}
        </label>
      )}

      <div className="relative">
        {Icon && (
          <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
            <Icon className="w-5 h-5" />
          </div>
        )}

        <input
          type={type}
          placeholder={placeholder}
          disabled={disabled}
          className={`w-full px-4 py-2 ${
            Icon ? 'pl-10' : 'pl-4'
          } border rounded-lg outline-none transition-colors focus:ring-2 focus:ring-offset-2 ${
            error && touched
              ? 'border-danger-500 focus:ring-danger-500'
              : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500'
          } ${disabled ? 'bg-gray-100 cursor-not-allowed' : 'bg-white'}`}
          {...props}
        />
      </div>

      {error && touched && (
        <p className="mt-1 text-sm text-danger-600">{error}</p>
      )}
    </div>
  );
};

export default InputField;
