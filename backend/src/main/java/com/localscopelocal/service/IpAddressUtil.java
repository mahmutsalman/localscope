package com.localscopelocal.service;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting client IP addresses from HTTP requests
 * Handles common proxy headers and load balancer configurations
 */
public class IpAddressUtil {

    private static final Logger log = LoggerFactory.getLogger(IpAddressUtil.class);

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Extract the real client IP address from the HTTP request
     * This method handles various proxy headers that might contain the original client IP
     *
     * @param request The HTTP servlet request
     * @return The client IP address as a string
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = null;

        // Check each possible header for the IP address
        for (String header : IP_HEADER_CANDIDATES) {
            ipAddress = request.getHeader(header);
            if (isValidIpAddress(ipAddress)) {
                log.debug("Found client IP {} in header {}", ipAddress, header);
                break;
            }
        }

        // If no valid IP found in headers, use the remote address
        if (!isValidIpAddress(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            log.debug("Using remote address as client IP: {}", ipAddress);
        }

        // Handle X-Forwarded-For header which may contain multiple IPs
        if (ipAddress != null && ipAddress.contains(",")) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // The first one is usually the original client IP
            ipAddress = ipAddress.split(",")[0].trim();
            log.debug("Extracted first IP from comma-separated list: {}", ipAddress);
        }

        // Handle IPv6 to IPv4 mapping (::ffff:192.168.1.1 -> 192.168.1.1)
        if (ipAddress != null && ipAddress.startsWith("::ffff:") && ipAddress.length() > 7) {
            String mappedIp = ipAddress.substring(7);
            if (isValidIpv4Address(mappedIp)) {
                ipAddress = mappedIp;
                log.debug("Converted IPv6 mapped address to IPv4: {}", ipAddress);
            }
        }

        // Default to localhost if we still don't have a valid IP
        if (!isValidIpAddress(ipAddress)) {
            ipAddress = "127.0.0.1";
            log.debug("No valid IP found, defaulting to localhost");
        }

        return ipAddress;
    }

    /**
     * Check if the IP address is valid and not a placeholder
     *
     * @param ipAddress The IP address to validate
     * @return true if the IP address is valid, false otherwise
     */
    private static boolean isValidIpAddress(String ipAddress) {
        return ipAddress != null 
                && !ipAddress.isEmpty() 
                && !ipAddress.equalsIgnoreCase("unknown")
                && !ipAddress.equalsIgnoreCase("localhost")
                && !ipAddress.equals("0:0:0:0:0:0:0:1")  // IPv6 localhost
                && ipAddress.length() > 6;  // Minimum valid IP length
    }

    /**
     * Check if the address is a valid IPv4 address
     *
     * @param ipAddress The IP address to validate
     * @return true if it's a valid IPv4 address, false otherwise
     */
    private static boolean isValidIpv4Address(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get a normalized IP address for rate limiting purposes
     * This can be used to group similar IPs or handle special cases
     *
     * @param request The HTTP servlet request
     * @return A normalized IP address string
     */
    public static String getNormalizedIpAddress(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        
        // For localhost development, return a consistent identifier
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "localhost";
        }
        
        // You could add additional normalization logic here, such as:
        // - Masking the last octet for privacy: 192.168.1.* -> 192.168.1.0
        // - Grouping private networks
        // - Handling known proxy networks
        
        return ipAddress;
    }
} 