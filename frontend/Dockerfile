FROM node:22-alpine AS build

WORKDIR /app

RUN npm install --global npm@10.8.3

COPY package.json package-lock.json ./

RUN npm install

COPY . .

RUN npm run build


FROM caddy:2-alpine

COPY Caddyfile /etc/caddy/Caddyfile

COPY --from=build /app/dist/frontend/browser /srv

EXPOSE 8080