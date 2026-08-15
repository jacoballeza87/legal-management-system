# Stage 1: Build the Angular application
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Replace 'legal-management-frontend' with your actual Angular project name from angular.json
RUN npm run build --configuration=production

# Stage 2: Serve the application with Nginx
FROM nginx:alpine
# Copy the compiled files from the build stage to the Nginx html directory
COPY --from=build /app/dist/legal-management-frontend/browser /usr/share/nginx/html
# Copy custom Nginx config (listens on 8080, handles Angular routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Cloud Run expects the container to listen on port 8080
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
