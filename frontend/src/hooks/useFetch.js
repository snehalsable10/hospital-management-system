import { useState, useEffect, useCallback } from 'react';
import { useNotification } from './useNotification';

export const useFetch = (fetchFn) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { error: showError } = useNotification();

  const fetch = useCallback(async (...args) => {
    try {
      setLoading(true);
      setError(null);
      const result = await fetchFn(...args);
      setData(result.data);
      return result;
    } catch (err) {
      const message = err.response?.data?.message || err.message || 'An error occurred';
      setError(message);
      showError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchFn, showError]);

  const refetch = useCallback(() => {
    return fetch();
  }, [fetch]);

  return { data, loading, error, fetch, refetch };
};
