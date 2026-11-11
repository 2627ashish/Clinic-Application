/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        clinic: {
          ink: '#183630',
          green: '#176c5c',
          mint: '#e5f2ed',
          paper: '#f5f8f6',
          gold: '#dca75a',
        },
      },
      boxShadow: {
        soft: '0 20px 60px rgba(24, 54, 48, 0.09)',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
