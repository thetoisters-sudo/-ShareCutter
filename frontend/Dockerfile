FROM node:22-alpine AS build

WORKDIR /app

RUN npm install --global npm@11.6.2

COPY package.json package-lock.json ./

RUN npm ci

COPY . .

RUN npm run build


FROM caddy:2-alpine

COPY Caddyfile /etc/caddy/Caddyfile

COPY --from=build /app/dist/frontend/browser /srv

EXPOSE 8080