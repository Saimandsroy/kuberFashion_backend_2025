# Redis Caching Implementation

## Overview
This document describes the Redis caching implementation for the KuberFashion backend to improve performance and reduce database load.

## Configuration

### Cache Types
The application supports two cache types:
- **Redis** (preferred): Distributed caching using Redis server
- **Simple** (fallback): In-memory caching using Spring's simple cache

### Configuration Properties
```properties
# Cache type: redis (preferred) or simple (fallback)
spring.cache.type=${CACHE_TYPE:simple}

# Redis Configuration (when available)
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.cache.redis.time-to-live=PT10M
spring.cache.redis.key-prefix=kf:
spring.cache.redis.use-key-prefix=true

# Simple Cache Configuration (fallback)
spring.cache.cache-names=products,products_list
```

## Cached Operations

### Product Service Caching
The following operations are cached:

#### Read Operations (Cached)
- `getAllProducts()` - Cache key: `'all'`
- `getProductById(Long id)` - Cache key: `#id`
- `getFeaturedProducts()` - Cache key: `'featured'`
- `getProductsByCategory(String categorySlug)` - Cache key: `'cat:' + #categorySlug`
- `getProductsByCategoryPaginated(...)` - Cache key includes pagination and sorting parameters
- `searchProducts(...)` - Cache key includes search keyword and pagination
- `getProductsByPriceRange(...)` - Cache key includes price range and pagination
- `getTopRatedProducts(int limit)` - Cache key: `'top:' + #limit`
- `getNewestProducts(int limit)` - Cache key: `'new:' + #limit`
- `getTrendingProducts()` - Cache key: `'trending'`
- `getAvailableProducts()` - Cache key: `'available'`

#### Write Operations (Cache Eviction)
- `createProduct(Product product)` - Evicts all `products_list` cache entries
- `updateProduct(Product product)` - Evicts specific product and all `products_list` entries
- `deleteProduct(Long id)` - Evicts specific product and all `products_list` entries

## Cache Management

### Health Check Endpoint
```
GET /api/cache/health
```
Returns cache status, type, and connection information.

### Cache Clear Endpoint
```
POST /api/cache/clear
```
Clears all cached data (requires authentication).

## Redis Setup

### Using Docker (Recommended)
```bash
# Start Redis container
docker run -d --name redis -p 6379:6379 redis:alpine

# Set cache type to redis
export CACHE_TYPE=redis
```

### Using Redis Server
```bash
# Install Redis (Windows - using Chocolatey)
choco install redis-64

# Start Redis server
redis-server

# Set cache type to redis
export CACHE_TYPE=redis
```

## Fallback Behavior
If Redis is not available or `CACHE_TYPE=simple`, the application will:
1. Use Spring's simple in-memory cache
2. Cache data within the application instance only
3. Lose cache data on application restart
4. Still provide performance benefits for repeated requests within the same session

## Performance Benefits
- **Initial Load**: Database queries are cached for subsequent requests
- **Reduced Database Load**: Frequently accessed data served from cache
- **Improved Response Times**: Cache hits return data much faster than database queries
- **Scalability**: Redis allows cache sharing across multiple application instances

## Monitoring
- Check server logs for cache MISS messages (indicates database queries)
- Use cache health endpoint to verify cache status
- Monitor Redis memory usage and hit rates (when using Redis)

## Best Practices
1. Set appropriate TTL values based on data update frequency
2. Use specific cache keys to avoid unnecessary evictions
3. Monitor cache hit rates and adjust caching strategy accordingly
4. Consider cache warming for critical data
5. Implement proper error handling for cache failures
