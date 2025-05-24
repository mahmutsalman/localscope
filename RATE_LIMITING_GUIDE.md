# Rate Limiting Implementation Guide

## Overview

This application now includes comprehensive rate limiting to protect your Google Places API quota from abuse. The implementation provides multiple layers of protection:

1. **Per-IP Rate Limiting**: Limits requests per IP address
2. **Global Rate Limiting**: Overall API call limits across all users
3. **Burst Protection**: Allows short bursts beyond normal limits
4. **Monitoring**: Real-time statistics and health checks

## Configuration

### Application Properties

Add these properties to `backend/src/main/resources/application.properties`:

```properties
# Rate Limiting Configuration
# Per IP limits (requests per time window)
rate.limit.ip.requests=10
rate.limit.ip.window.minutes=1

# Global API limits (total requests per time window)
rate.limit.global.requests=100
rate.limit.global.window.minutes=1

# Burst allowance (allow short bursts beyond the rate limit)
rate.limit.burst.allowance=5

# Rate limit enabled/disabled
rate.limit.enabled=true
```

### Configuration Options Explained

| Property | Default | Description |
|----------|---------|-------------|
| `rate.limit.enabled` | `true` | Enable/disable rate limiting |
| `rate.limit.ip.requests` | `10` | Max requests per IP per time window |
| `rate.limit.ip.window.minutes` | `1` | Time window for IP-based limiting (minutes) |
| `rate.limit.global.requests` | `100` | Max total requests per time window |
| `rate.limit.global.window.minutes` | `1` | Time window for global limiting (minutes) |
| `rate.limit.burst.allowance` | `5` | Extra requests allowed for short bursts |

## How It Works

### Request Flow

1. **IP Extraction**: The system extracts the real client IP from headers (handling proxies/load balancers)
2. **Rate Limit Check**: Both IP-specific and global limits are checked
3. **Request Processing**: If limits are not exceeded, the request is processed
4. **Counter Updates**: Request counters are incremented
5. **Response**: Rate limit information is included in the response

### Rate Limiting Logic

- **IP-based**: Each IP address has its own counter that resets every time window
- **Global**: All requests count towards a global counter
- **Burst Allowance**: Allows temporary spikes beyond normal limits
- **Automatic Cleanup**: Expired counters are automatically cleaned up to prevent memory leaks

## API Endpoints

### Places API (Rate Limited)

```
GET /api/places?longitude={lon}&latitude={lat}&radius={radius}
```

**Rate Limited Response Format:**
```json
{
  "places": [...],
  "count": 5,
  "rateLimitInfo": {
    "remainingIpRequests": 8,
    "remainingGlobalRequests": 95
  }
}
```

**Rate Limit Exceeded Response (HTTP 429):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests from your IP address. Please try again later.",
  "remainingIpRequests": 0,
  "remainingGlobalRequests": 50
}
```

### Monitoring Endpoints

#### Check Rate Limit Status
```
GET /api/places/rate-limit-status
```

Response:
```json
{
  "clientIp": "192.168.1.100",
  "rateLimitEnabled": true,
  "globalRequests": 25,
  "globalLimit": 100,
  "activeIpAddresses": 5,
  "ipLimit": 10
}
```

#### Admin Statistics
```
GET /api/admin/rate-limit-stats
```

Response:
```json
{
  "rateLimitEnabled": true,
  "global": {
    "currentRequests": 25,
    "limit": 100,
    "utilizationPercentage": 25
  },
  "perIp": {
    "activeIpAddresses": 5,
    "ipLimit": 10
  },
  "timestamp": 1703123456789
}
```

#### Health Check
```
GET /api/admin/health
```

Response:
```json
{
  "status": "UP",
  "timestamp": 1703123456789,
  "service": "LocalScopeLocal API",
  "rateLimiting": {
    "enabled": true,
    "healthy": true
  }
}
```

## Recommended Settings

### For VPS Deployment

#### Conservative (Recommended)
```properties
rate.limit.ip.requests=5
rate.limit.ip.window.minutes=1
rate.limit.global.requests=50
rate.limit.global.window.minutes=1
rate.limit.burst.allowance=2
```

#### Moderate
```properties
rate.limit.ip.requests=10
rate.limit.ip.window.minutes=1
rate.limit.global.requests=100
rate.limit.global.window.minutes=1
rate.limit.burst.allowance=5
```

#### Relaxed (for high-traffic sites)
```properties
rate.limit.ip.requests=20
rate.limit.ip.window.minutes=1
rate.limit.global.requests=200
rate.limit.global.window.minutes=1
rate.limit.burst.allowance=10
```

### Google Places API Considerations

- Google Places API has daily quotas and per-second rate limits
- Nearby Search requests cost more than simple place details
- Consider your Google Cloud billing limits
- Monitor your Google Cloud Console for API usage

## Advanced Configuration

### Proxy/Load Balancer Setup

The system automatically handles common proxy headers:
- `X-Forwarded-For`
- `X-Real-IP`
- `Proxy-Client-IP`
- And many others...

### Custom IP Normalization

You can modify `IpAddressUtil.getNormalizedIpAddress()` to:
- Mask IP addresses for privacy
- Group IP ranges
- Handle special cases (VPN, CDN networks)

### Database Persistence (Future Enhancement)

Currently, rate limiting uses in-memory storage. For production scaling, consider:
- Redis for distributed rate limiting
- Database persistence for audit trails
- Cluster-aware rate limiting

## Monitoring and Alerting

### Key Metrics to Monitor

1. **Global Request Rate**: `currentGlobalRequests / globalLimit`
2. **Active IP Addresses**: Number of unique IPs making requests
3. **Rate Limit Violations**: HTTP 429 responses
4. **Google API Usage**: Monitor your Google Cloud Console

### Log Messages

- `INFO`: Successful requests with IP and response count
- `WARN`: Rate limit violations with IP and details
- `DEBUG`: Detailed rate limiting decisions

### Sample Log Entries

```
INFO  - Received request for places from IP: 192.168.1.100 at lon: -122.4194, lat: 37.7749, radius: 1000
INFO  - Successfully returned 15 places for IP: 192.168.1.100
WARN  - Rate limit exceeded for IP: 203.0.113.1 - Too many requests from your IP address. Please try again later.
WARN  - Global rate limit exceeded: 105 requests in 1 minutes
```

## Testing Rate Limits

### Manual Testing

1. Make rapid requests to `/api/places` endpoint
2. Check response headers for rate limit info
3. Verify HTTP 429 responses when limits exceeded
4. Monitor `/api/admin/rate-limit-stats` for statistics

### Load Testing

Use tools like Apache Bench or curl scripts:

```bash
# Test IP rate limiting
for i in {1..15}; do
  curl "http://localhost:8070/api/places?longitude=-122.4194&latitude=37.7749&radius=1000"
  echo ""
done
```

## Troubleshooting

### Common Issues

1. **Rate limits too restrictive**: Increase limits or burst allowance
2. **Memory usage**: Monitor active IP addresses, implement cleanup
3. **Proxy IP issues**: Check IP extraction logic in logs
4. **Google API errors**: Check your API key and quotas

### Debugging

Enable debug logging:
```properties
logging.level.com.localscopelocal.service.RateLimitService=DEBUG
logging.level.com.localscopelocal.service.IpAddressUtil=DEBUG
```

## Security Considerations

1. **Admin Endpoints**: Consider restricting `/api/admin/*` endpoints to authorized users
2. **IP Spoofing**: The system handles common proxy headers but sophisticated attacks may bypass IP detection
3. **Memory Usage**: Rate limiting counters are cleaned up automatically but monitor memory usage
4. **API Key Protection**: Ensure your Google API key is properly secured

## Performance Impact

- **Memory**: Each active IP address uses minimal memory for counters
- **CPU**: Rate limiting checks are very fast (O(1) operations)
- **Network**: Minimal overhead, adds small amount of data to responses
- **Cleanup**: Automatic cleanup runs on each request but is optimized

## Future Enhancements

1. **Redis Integration**: For distributed rate limiting across multiple server instances
2. **User Authentication**: Per-user rate limiting instead of IP-based
3. **Dynamic Limits**: Adjust limits based on server load or time of day
4. **Whitelist/Blacklist**: Allow certain IPs unlimited access or block malicious IPs
5. **Advanced Analytics**: Detailed usage patterns and abuse detection 