/**
 * LocalScopeLocal - Find Nearby Places
 * Main JavaScript file for handling UI interactions and API calls
 */

// Backend API URL
// For production deployment, change this to '/api/places' (relative URL)
// For local development, use 'http://localhost:8070/api/places'
const API_URL = '/api/places';

// DOM elements
const searchForm = document.getElementById('search-form');
const currentLocationBtn = document.getElementById('current-location');
const latitudeInput = document.getElementById('latitude');
const longitudeInput = document.getElementById('longitude');
const radiusInput = document.getElementById('radius');
const resultsSection = document.getElementById('results-section');
const placesList = document.getElementById('places-list');
const mapContainer = document.getElementById('map-container');
const loader = document.getElementById('loader');

// Global variables
let map;
let markers = [];

/**
 * Initialize the application
 */
function init() {
    // Event listeners
    searchForm.addEventListener('submit', handleSearch);
    currentLocationBtn.addEventListener('click', getCurrentLocation);
}

/**
 * Handle form submission
 * @param {Event} e - Form submit event
 */
function handleSearch(e) {
    e.preventDefault();
    
    const latitude = parseFloat(latitudeInput.value);
    const longitude = parseFloat(longitudeInput.value);
    const radius = parseInt(radiusInput.value);
    
    // Validate inputs
    if (isNaN(latitude) || isNaN(longitude) || isNaN(radius)) {
        showError('Please enter valid values for latitude, longitude, and radius.');
        return;
    }
    
    // Call API to get nearby places
    fetchNearbyPlaces(longitude, latitude, radius);
}

/**
 * Get user's current location
 */
function getCurrentLocation() {
    if (!navigator.geolocation) {
        showError('Geolocation is not supported by your browser.');
        return;
    }
    
    showLoader();
    
    navigator.geolocation.getCurrentPosition(
        position => {
            hideLoader();
            latitudeInput.value = position.coords.latitude;
            longitudeInput.value = position.coords.longitude;
        },
        error => {
            hideLoader();
            let errorMessage;
            
            switch (error.code) {
                case error.PERMISSION_DENIED:
                    errorMessage = 'User denied the request for Geolocation.';
                    break;
                case error.POSITION_UNAVAILABLE:
                    errorMessage = 'Location information is unavailable.';
                    break;
                case error.TIMEOUT:
                    errorMessage = 'The request to get user location timed out.';
                    break;
                default:
                    errorMessage = 'An unknown error occurred.';
            }
            
            showError(errorMessage);
        }
    );
}

/**
 * Fetch nearby places from the backend API
 * @param {number} longitude - Longitude coordinate
 * @param {number} latitude - Latitude coordinate
 * @param {number} radius - Search radius in meters
 */
function fetchNearbyPlaces(longitude, latitude, radius) {
    showLoader();
    
    // Clear previous results
    placesList.innerHTML = '';
    clearMap();
    
    // Build the API URL with query parameters
    const url = `${API_URL}?longitude=${longitude}&latitude=${latitude}&radius=${radius}`;
    
    fetch(url)
        .then(response => {
            if (!response.ok) {
                // Handle rate limiting specifically
                if (response.status === 429) {
                    return response.json().then(errorData => {
                        throw new Error(`Rate limit exceeded: ${errorData.message || 'Too many requests. Please try again later.'}`);
                    });
                }
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideLoader();
            
            // Handle new response format with rate limiting info
            let places;
            let rateLimitInfo = null;
            
            // Check if response is the new format (object with places array) or old format (direct array)
            if (data && typeof data === 'object' && data.places && Array.isArray(data.places)) {
                // New format with rate limiting
                places = data.places;
                rateLimitInfo = data.rateLimitInfo;
                
                // Log rate limiting info for debugging
                if (rateLimitInfo) {
                    console.log('Rate limit info:', rateLimitInfo);
                }
            } else if (Array.isArray(data)) {
                // Fallback for old format (direct array)
                places = data;
            } else {
                // Handle unexpected format
                console.error('Unexpected response format:', data);
                places = [];
            }
            
            if (!places || places.length === 0) {
                placesList.innerHTML = '<p class="no-results">No places found in this area.</p>';
                return;
            }
            
            // Display places in list and on map
            displayPlaces(places);
            initMap(latitude, longitude, places);
            
            // Display rate limiting info if available
            displayRateLimitInfo(rateLimitInfo);
        })
        .catch(error => {
            hideLoader();
            showError(`Error fetching places: ${error.message}`);
        });
}

/**
 * Display places in the results list
 * @param {Array} places - Array of place objects
 */
function displayPlaces(places) {
    places.forEach(place => {
        const placeCard = document.createElement('div');
        placeCard.className = 'place-card';
        
        const rating = place.rating ? `<p class="rating">Rating: ${place.rating} ⭐</p>` : '';
        // Use a default icon as place.icon is no longer directly available from the new API response structure
        const iconUrl = 'https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/generic_business-71.png';
        
        placeCard.innerHTML = `
            <div class="place-card-image" style="background-image: url('${iconUrl}')"></div>
            <div class="place-card-content">
                <h3>${place.displayName || 'N/A'}</h3>
                <p>${place.formattedAddress || 'No address available'}</p>
                ${rating}
                ${place.primaryType ? `<p>Type: ${place.primaryType}</p>` : ''}
                ${place.websiteUri ? `<p><a href="${place.websiteUri}" target="_blank">Website</a></p>` : ''}
            </div>
        `;
        
        placesList.appendChild(placeCard);
    });
}

/**
 * Initialize Google Map and add markers
 * @param {number} latitude - Center latitude
 * @param {number} longitude - Center longitude
 * @param {Array} places - Array of place objects
 */
function initMap(latitude, longitude, places) {
    // Show map container
    mapContainer.classList.remove('hidden');
    
    // Create map centered at the search location
    const mapOptions = {
        center: { lat: latitude, lng: longitude },
        zoom: 14,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    
    map = new google.maps.Map(mapContainer, mapOptions);
    
    // Add center marker (search location)
    const centerMarker = new google.maps.Marker({
        position: { lat: latitude, lng: longitude },
        map: map,
        title: 'Search Location',
        icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: 10,
            fillColor: '#3498db',
            fillOpacity: 0.5,
            strokeWeight: 1,
            strokeColor: '#2980b9'
        }
    });
    
    markers.push(centerMarker);
    
    // Add search radius circle
    const radiusCircle = new google.maps.Circle({
        strokeColor: '#3498db',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#3498db',
        fillOpacity: 0.1,
        map: map,
        center: { lat: latitude, lng: longitude },
        radius: parseInt(radiusInput.value)
    });
    
    // Add markers for each place
    places.forEach(place => {
        const marker = new google.maps.Marker({
            position: { lat: place.latitude, lng: place.longitude },
            map: map,
            title: place.displayName || 'N/A' // Use displayName here
        });
        
        // Add info window with place details
        const infoWindow = new google.maps.InfoWindow({
            content: `
                <div class="info-window">
                    <h3>${place.displayName || 'N/A'}</h3>
                    <p>${place.formattedAddress || 'No address available'}</p>
                    ${place.rating ? `<p>Rating: ${place.rating} ⭐</p>` : ''}
                    ${place.primaryType ? `<p>Type: ${place.primaryType}</p>` : ''}
                </div>
            `
        });
        
        marker.addListener('click', () => {
            infoWindow.open(map, marker);
        });
        
        markers.push(marker);
    });
}

/**
 * Clear map markers
 */
function clearMap() {
    if (map) {
        markers.forEach(marker => marker.setMap(null));
        markers = [];
        mapContainer.classList.add('hidden');
    }
}

/**
 * Show error message
 * @param {string} message - Error message to display
 */
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    
    // Remove any existing error messages
    const existingError = document.querySelector('.error-message');
    if (existingError) {
        existingError.remove();
    }
    
    // Add error message before the form
    searchForm.parentNode.insertBefore(errorDiv, searchForm);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

/**
 * Display rate limiting information to users
 * @param {Object} rateLimitInfo - Rate limiting information from the API
 */
function displayRateLimitInfo(rateLimitInfo) {
    // Remove any existing rate limit info
    const existingInfo = document.querySelector('.rate-limit-info');
    if (existingInfo) {
        existingInfo.remove();
    }
    
    // Only display if rate limit info is available
    if (!rateLimitInfo) {
        return;
    }
    
    const infoDiv = document.createElement('div');
    infoDiv.className = 'rate-limit-info';
    
    const ipRemaining = rateLimitInfo.remainingIpRequests;
    const globalRemaining = rateLimitInfo.remainingGlobalRequests;
    
    let message = 'Rate limit status: ';
    if (ipRemaining !== undefined && globalRemaining !== undefined) {
        message += `${ipRemaining} requests remaining for your IP, ${globalRemaining} globally`;
    } else if (ipRemaining !== undefined) {
        message += `${ipRemaining} requests remaining for your IP`;
    } else if (globalRemaining !== undefined) {
        message += `${globalRemaining} requests remaining globally`;
    } else {
        return; // No useful info to display
    }
    
    infoDiv.innerHTML = `
        <div style="background-color: #e8f4fd; border: 1px solid #bee5eb; color: #0c5460; padding: 10px; margin: 10px 0; border-radius: 4px; font-size: 14px;">
            ℹ️ ${message}
        </div>
    `;
    
    // Add info after the form
    if (resultsSection) {
        resultsSection.parentNode.insertBefore(infoDiv, resultsSection);
    } else {
        searchForm.parentNode.insertBefore(infoDiv, searchForm.nextSibling);
    }
    
    // Auto-remove after 10 seconds
    setTimeout(() => {
        if (infoDiv.parentNode) {
            infoDiv.remove();
        }
    }, 10000);
}

/**
 * Show loader
 */
function showLoader() {
    loader.classList.remove('hidden');
}

/**
 * Hide loader
 */
function hideLoader() {
    loader.classList.add('hidden');
}

// Initialize the application when the DOM is loaded
document.addEventListener('DOMContentLoaded', init); 