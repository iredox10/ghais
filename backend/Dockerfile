FROM node:22-alpine
WORKDIR /app
COPY backend/package*.json ./backend/
RUN cd backend && npm ci
COPY backend/ ./backend/
ENV DATABASE_SSL_CA=/app/backend/certs/rds-global-bundle.pem
RUN cd backend && npm run build
CMD ["node", "backend/dist/index.js"]
