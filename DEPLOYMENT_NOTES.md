# Deployment Notes

## VPS Server Configuration Changes

### Date: May 22, 2025

#### Nginx Configuration Updates
- **Server**: VPS with IP 31.97.34.175
- **Configuration File**: `/etc/nginx/sites-available/your_app_name`
- **Changes Made**:
  - Updated `proxy_pass` directive from `http://localhost:8070/api/` to `http://localhost:8070` (removed trailing slash)
  - Added CORS headers for browser JavaScript requests
  - Added OPTIONS request handling for CORS preflight
  - Spring Boot application runs on port 8070
  - Nginx serves frontend static files and proxies API requests to backend

#### Key Configuration Block:
```nginx
location /api/ {
    proxy_pass http://localhost:8070;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # CORS headers
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS' always;
    add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range' always;
}
```

#### Issue Resolved:
- Fixed 404 errors when frontend JavaScript tried to make API calls to `/api/places`
- The trailing slash in `proxy_pass` was causing path duplication (nginx was forwarding to `/api/api/places`)

#### Frontend Configuration:
- Updated `API_URL` in `frontend/js/app.js` from `http://localhost:8070/api/places` to `/api/places` for relative URLs

## Deployment Commands Used

### Starting/Stopping Spring Boot Application:
```bash
# Stop existing instance if running
pkill -f localscope.jar || true

# Start application in background
nohup java -jar localscope.jar > app.log 2>&1 &
```

### Nginx Commands:
```bash
# Test configuration
sudo nginx -t

# Reload nginx after config changes
sudo systemctl reload nginx

# Check nginx status
systemctl status nginx.service
```

### Testing Commands:
```bash
# Test Spring Boot directly
curl "http://localhost:8070/api/places?longitude=-74.006&latitude=40.7128&radius=1000"

# Test through nginx proxy
curl "http://localhost/api/places?longitude=-74.006&latitude=40.7128&radius=1000"
``` 