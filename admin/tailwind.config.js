/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        noir: {
          black: '#050506',
          top: '#0C0C0E',
          surface: '#111114',
          elevated: '#17171C',
        },
        ghais: {
          emerald: '#10B981',
          gold: '#F59E0B',
          cyan: '#06B6D4',
        }
      },
      backgroundColor: {
        'glass-wash': 'rgba(255, 255, 255, 0.04)',
        'glass-card': 'rgba(255, 255, 255, 0.06)',
        'glass-active': 'rgba(255, 255, 255, 0.12)',
        'glass-inset': 'rgba(0, 0, 0, 0.45)',
      },
      borderColor: {
        'glass-ghost': 'rgba(255, 255, 255, 0.06)',
        'glass-card': 'rgba(255, 255, 255, 0.10)',
        'glass-specular': 'rgba(255, 255, 255, 0.22)',
      },
      boxShadow: {
        'specular': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.22)',
        'specular-soft': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.12)',
        'ambient-glow': '0 0 50px -10px rgba(255, 255, 255, 0.05)',
        'emerald-glow': '0 0 25px -5px rgba(16, 185, 129, 0.3)',
      },
      backgroundImage: {
        'card-gradient': 'linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
        'card-gradient-soft': 'linear-gradient(180deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%)',
        'card-gradient-active': 'linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.06) 100%)',
        'chrome-cta': 'linear-gradient(180deg, #F5F5F7 0%, #C7C7CC 100%)',
        'well-dome': 'linear-gradient(180deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.04) 100%)',
      }
    },
  },
  plugins: [],
}
