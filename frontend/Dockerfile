# Stage 1: Build the Angular application
FROM node:20-alpine AS build
WORKDIR /app
COPY legal-app/package*.json ./
RUN npm install
COPY legal-app/. .
RUN npm run build --configuration=production

# Stage 2: Serve the application with Nginx
FROM nginx:alpine
COPY --from=build /app/dist/legal-management-app/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
