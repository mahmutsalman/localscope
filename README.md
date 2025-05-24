# LocalScopeLocal

A full-stack application that lets users find nearby places by providing longitude, latitude, and radius parameters.

## Features

- Input form for longitude, latitude, and radius
- Displays nearby places based on the provided parameters
- Visual representation of results
- Caching of previous search results for improved performance
- **Rate limiting** to protect Google API quota from abuse
- Real-time monitoring and analytics
- Automatic IP detection and handling for proxies/load balancers

## Technology Stack

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java 8+ with Spring Boot
- **Database**: H2 (embedded)
- **API**: Google Places API

## Prerequisites

- Java 8 or higher
- Maven
- Google Places API key

## Setup and Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/LocalScopeLocal.git
   cd LocalScopeLocal
   ```

2. Set up Google Places API:
   - Get a Google Places API key from the [Google Cloud Console](https://console.cloud.google.com/)
   - Add your API key to `backend/src/main/resources/application.properties`:
     ```
     google.places.api.key=YOUR_API_KEY
     ```

3. Configure rate limiting (optional):
   ```properties
   # Rate Limiting Configuration
   rate.limit.enabled=true
   rate.limit.ip.requests=10
   rate.limit.ip.window.minutes=1
   rate.limit.global.requests=100
   rate.limit.global.window.minutes=1
   rate.limit.burst.allowance=5
   ```

4. Build and run the backend:
   ```
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

5. Access the application:
   - Open a web browser and navigate to http://localhost:8070

## Usage

1. Enter the longitude, latitude, and radius in the form
2. Click the "Search" button
3. View the list of nearby places
4. (Optional) View the places on the map

## API Endpoints

### Places API
- `GET /api/places?longitude={longitude}&latitude={latitude}&radius={radius}`
  - Returns a list of nearby places based on the provided parameters
  - **Rate limited**: Includes rate limit information in response
  - Returns HTTP 429 if rate limits are exceeded

### Rate Limiting & Monitoring
- `GET /api/places/rate-limit-status`
  - Returns current rate limit status for the requesting IP
- `GET /api/admin/rate-limit-stats`
  - Returns comprehensive rate limiting statistics
- `GET /api/admin/health`
  - Health check endpoint with rate limiting status
- `GET /api/admin/info`
  - System information and metrics

### Example Response (Rate Limited)
```json
{
  "places": [...],
  "count": 15,
  "rateLimitInfo": {
    "remainingIpRequests": 8,
    "remainingGlobalRequests": 85
  }
}
```

### Rate Limit Exceeded Response (HTTP 429)
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests from your IP address. Please try again later.",
  "remainingIpRequests": 0,
  "remainingGlobalRequests": 50
}
```

## Rate Limiting

This application includes comprehensive rate limiting to protect your Google Places API quota:

- **Per-IP limits**: Prevents individual IPs from overwhelming the service
- **Global limits**: Overall protection for your API quota
- **Burst protection**: Allows short spikes beyond normal limits
- **Automatic cleanup**: Memory-efficient with automatic counter cleanup

For detailed configuration and usage information, see [RATE_LIMITING_GUIDE.md](RATE_LIMITING_GUIDE.md).

### Recommended VPS Settings
```properties
# Conservative settings for VPS deployment
rate.limit.ip.requests=5
rate.limit.ip.window.minutes=1
rate.limit.global.requests=50
rate.limit.global.window.minutes=1
rate.limit.burst.allowance=2
```

## Development

### Backend
The backend is built with Spring Boot and provides a REST API for the frontend to consume. It includes:
- Google Places API integration
- H2 database for caching
- Comprehensive rate limiting
- Real-time monitoring endpoints

### Frontend
The frontend is built with HTML, CSS, and JavaScript, providing a simple and intuitive user interface.

## Monitoring

Monitor your application using these endpoints:
- `/api/admin/health` - Overall health status
- `/api/admin/rate-limit-stats` - Rate limiting statistics
- `/api/admin/info` - System information

## Security Considerations

- Rate limiting protects against API abuse
- IP addresses are properly extracted from proxy headers
- Admin endpoints should be secured in production
- Google API key should be kept secure

## License

[MIT](https://choosealicense.com/licenses/mit/) 