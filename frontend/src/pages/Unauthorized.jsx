import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock } from 'lucide-react';
import Button from '../components/common/Button';

const Unauthorized = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-danger-50 to-danger-100">
      <div className="text-center">
        <Lock className="w-20 h-20 text-danger-600 mx-auto mb-6" />
        <h1 className="text-5xl font-bold text-gray-900 mb-2">403</h1>
        <h2 className="text-2xl font-semibold text-gray-700 mb-4">Unauthorized</h2>
        <p className="text-gray-600 mb-8 max-w-md">
          You don't have permission to access this resource.
        </p>
        <div className="flex gap-4 justify-center">
          <Button variant="primary" onClick={() => navigate('/dashboard')}>
            Go to Dashboard
          </Button>
          <Button variant="outline" onClick={() => navigate(-1)}>
            Go Back
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Unauthorized;
