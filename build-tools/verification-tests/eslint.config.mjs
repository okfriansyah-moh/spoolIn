import js from '@eslint/js';
import svelte from 'eslint-plugin-svelte';
import ts from 'typescript-eslint';

export default [
  {
    ignores: [
      '**/build/**',
      '**/dist/**',
      '**/.svelte-kit/**',
      '**/generated/**',
      '**/node_modules/**',
    ],
  },
  js.configs.recommended,
  ...ts.configs.recommended,
  ...svelte.configs['flat/recommended'],
  {
    languageOptions: {
      globals: { console: 'readonly', process: 'readonly', module: 'readonly' },
    },
    rules: {
      complexity: ['error', 10],
      'max-depth': ['error', 3],
      'max-lines-per-function': [
        'error',
        { max: 50, skipBlankLines: true, skipComments: true },
      ],
    },
  },
  {
    files: ['**/*.svelte'],
    languageOptions: { parserOptions: { parser: ts.parser } },
  },
];
