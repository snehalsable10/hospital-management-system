import React from 'react';

const SelectField = ({
  label,
  options = [],
  placeholder = 'Select an option',
  error,
  touched,
  disabled = false,
  required = false,
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

      <select
        disabled={disabled}
        className={`w-full px-4 py-2 border rounded-lg outline-none transition-colors focus:ring-2 focus:ring-offset-2 ${
          error && touched
            ? 'border-danger-500 focus:ring-danger-500'
            : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500'
        } ${disabled ? 'bg-gray-100 cursor-not-allowed' : 'bg-white'}`}
        {...props}
      >
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option
            key={option.value || option}
            value={option.value || option}
          >
            {option.label || option}
          </option>
        ))}
      </select>

      {error && touched && (
        <p className="mt-1 text-sm text-danger-600">{error}</p>
      )}
    </div>
  );
};

export default SelectField;
