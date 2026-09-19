import React from 'react';
import {
  PieChart as RechartPieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

const PieChart = ({
  data = [],
  title = 'Chart',
  dataKey = 'value',
  nameKey = 'name',
  height = 300,
}) => {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
      {title && <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>}

      <ResponsiveContainer width="100%" height={height}>
        <RechartPieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            labelLine={false}
            label={(entry) => `${entry[nameKey]}: ${entry[dataKey]}`}
            outerRadius={80}
            fill="#8884d8"
            dataKey={dataKey}
            animationDuration={800}
          >
            {data.map((entry, index) => (
              // stroke="none": the default white stroke draws a visible radius
              // seam across a slice that covers the whole circle.
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="none" />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              backgroundColor: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
            }}
          />
          <Legend />
        </RechartPieChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PieChart;
