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
ARG APP_NAME
COPY --from=build /app/apps/${APP_NAME}/dist /usr/share/nginx/html
COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
