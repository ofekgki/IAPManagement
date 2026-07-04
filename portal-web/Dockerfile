# Build the Vite app, then serve the static bundle with nginx.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
# package-lock.json is optional; install resolves from package.json.
RUN npm install
COPY . .
# Bake the API base URL into the build (compose passes the backend URL).
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
