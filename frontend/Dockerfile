# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage
# ---------------------------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /build

# Copy the manifests first so a source change does not reinstall every
# dependency. npm ci installs exactly what the lockfile pins and fails if the
# lockfile and package.json have drifted apart.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite inlines env vars at build time, so the API base has to be decided here
# rather than at run time. The default points at this image's own nginx, which
# proxies /api to the backend - same origin, no CORS.
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ---------------------------------------------------------------------------
# Runtime stage
#
# The output is static files. Nothing from the build stage - Node, the
# toolchain, node_modules - ships in the final image.
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --quiet --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
