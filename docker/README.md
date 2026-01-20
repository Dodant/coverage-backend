# Docker Development Environment

This directory contains Docker configuration for the Book Cover Archive development environment.

## Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Caching and rate limiting |
| Elasticsearch | 9200, 9300 | Full-text search |
| MinIO | 9000 (API), 9001 (Console) | S3-compatible object storage |

## Quick Start

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Service Access

### PostgreSQL
- Host: `localhost`
- Port: `5432`
- Database: `bookcover_dev`
- Username: `postgres`
- Password: `postgres`

### Redis
- Host: `localhost`
- Port: `6379`

### Elasticsearch
- URL: `http://localhost:9200`
- No authentication required (development mode)

### MinIO
- API Endpoint: `http://localhost:9000`
- Console: `http://localhost:9001`
- Access Key: `minioadmin`
- Secret Key: `minioadmin`
- Default Bucket: `book-covers`

## Health Checks

All services include health checks. You can verify service health with:

```bash
docker-compose ps
```

## Troubleshooting

### Elasticsearch fails to start
Elasticsearch requires increased virtual memory. On Linux:
```bash
sudo sysctl -w vm.max_map_count=262144
```

To make it permanent, add to `/etc/sysctl.conf`:
```
vm.max_map_count=262144
```

### MinIO bucket not created
The `minio-init` service creates the default bucket. If it fails, manually create:
```bash
docker exec -it bookcover-minio mc alias set myminio http://localhost:9000 minioadmin minioadmin
docker exec -it bookcover-minio mc mb myminio/book-covers
```
