# Multi-stage build for React apps
FROM node:22-alpine AS build
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
ARG APP_NAME
COPY apps/${APP_NAME}/package.json apps/${APP_NAME}/
COPY packages/ packages/
RUN pnpm install --frozen-lockfile
COPY apps/${APP_NAME} apps/${APP_NAME}
RUN pnpm --filter ${APP_NAME} build

FROM nginx:alpine
RUN addgroup -S appuser && adduser -S -G appuser -u 1001 appuser && \
    chown -R appuser:appuser /var/cache/nginx /var/log/nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && chown appuser:appuser /var/run/nginx.pid
ARG APP_NAME
COPY --from=build /app/apps/${APP_NAME}/dist /usr/share/nginx/html
COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
USER appuser
EXPOSE 80
