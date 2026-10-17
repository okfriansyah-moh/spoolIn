import { renderReview } from './review-state.js';

const root = document.querySelector('#app');
if (root) renderReview(root, { revision: 0, values: {}, sources: [], unknowns: [] });
