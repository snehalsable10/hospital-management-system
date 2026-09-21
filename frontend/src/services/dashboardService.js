import api from './api';

/**
 * One call for everything the dashboard draws, counted in the database.
 *
 * The response carries a `sections` map saying which parts the caller's role
 * was allowed to see. A section set to false has null totals rather than
 * zeros, so the page can hide that panel instead of showing it empty.
 */
const dashboardService = {
  getSummary: async () => {
    const response = await api.get('/dashboard/summary');
    return response.data;
  },
};

export default dashboardService;
