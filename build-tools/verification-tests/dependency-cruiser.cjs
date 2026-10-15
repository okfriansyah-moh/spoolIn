module.exports = {
  forbidden: [
    { name: 'no-cycles', severity: 'error', from: {}, to: { circular: true } },
    {
      name: 'no-service-internals',
      severity: 'error',
      from: {},
      to: { path: '^services/' },
    },
    {
      name: 'no-database-drivers',
      severity: 'error',
      from: {},
      to: { path: 'node_modules/(pg|postgres|mysql|mysql2|better-sqlite3)/' },
    },
  ],
  options: { doNotFollow: { path: 'node_modules' } },
};
