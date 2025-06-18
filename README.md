# 🔍 StuffFinder

A community-driven platform for identifying mysterious objects. Upload photos of unknown items and get help from the community to discover what they are.

## Features

- **Mystery Object Posts** - Upload images with detailed attributes (material, color, shape, location, etc.)
- **Sub-component Analysis** - Break down complex objects into detailed sub-parts
- **Interactive Community** - Comment system with Q&A, voting, and threaded discussions
- **Smart Tagging** - Wikidata integration for semantic search and categorization
- **Advanced Search** - Filter by attributes, resolution status, and semantic similarity
- **User Profiles** - Follow users, earn badges, track contributions
- **Real-time Notifications** - Stay updated on post activity and interactions
- **Resolution System** - Mark posts as solved with contributing answers
- **Cross-platform** - Web app with mobile support via Capacitor

## Technology Stack

| Component | Technology |
|-----------|------------|
| **Frontend** | SvelteKit + Tailwind CSS |
| **Mobile** | Capacitor (iOS/Android) |
| **Backend** | Java Spring Boot |
| **Database** | AWS RDS MySQL |
| **Storage** | Google Cloud Storage |
| **Deployment** | Google Cloud Run |
| **Auth** | JWT + BCrypt |

## Quick Start

### Prerequisites
- Node.js 20+
- Java 17+
- Maven 3.9+
- MySQL 8.0+

### Backend Setup
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

### Mobile Development
```bash
cd frontend
npm run build
npx cap sync
npx cap open android  # or ios
```

## Architecture

StuffFinder follows a modern cloud-native architecture:

- **Microservices**: Fully containerized with Docker
- **Serverless Deployment**: Google Cloud Run for auto-scaling
- **Semantic Search**: Wikidata API integration for intelligent tagging
- **Real-time Features**: WebSocket-like notifications and live updates
- **Responsive Design**: Mobile-first UI with cross-platform mobile apps

*Built for SWE574 - Boğaziçi University Software Engineering Master's Degree Program*
