FROM node:20-alpine AS build
WORKDIR /app

# Copy package.json files and install dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the app
COPY . .

# Install adapter-node (compatible with Node 20)
RUN npm install @sveltejs/adapter-node

# Modify svelte.config.js to use adapter-node
RUN sed -i "s/import adapter from '@sveltejs\/adapter-static';/import adapter from '@sveltejs\/adapter-node';/" svelte.config.js
RUN sed -i "s/adapter({/adapter({/" svelte.config.js

# Build the app
RUN npm run build

# Production stage
FROM node:20-alpine
WORKDIR /app

# Copy built app from the build stage
COPY --from=build /app/package*.json ./
COPY --from=build /app/build ./build
COPY --from=build /app/node_modules ./node_modules

# Expose the port your app runs on (cloud platforms often use PORT env variable)
EXPOSE 4200
ENV PORT=4200
ENV HOST=0.0.0.0

# Start the app
CMD ["node", "build"]