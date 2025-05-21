# LocalScopeLocal

A full-stack application that lets users find nearby places by providing longitude, latitude, and radius parameters.

## Features

- Input form for longitude, latitude, and radius
- Displays nearby places based on the provided parameters
- Visual representation of results
- Caching of previous search results for improved performance

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

3. Build and run the backend:
   ```
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

4. Access the application:
   - Open a web browser and navigate to http://localhost:8070

## Usage

1. Enter the longitude, latitude, and radius in the form
2. Click the "Search" button
3. View the list of nearby places
4. (Optional) View the places on the map

## API Endpoints

- `GET /api/places?longitude={longitude}&latitude={latitude}&radius={radius}`
  - Returns a list of nearby places based on the provided parameters

## Development

### Backend
The backend is built with Spring Boot and provides a REST API for the frontend to consume.

### Frontend
The frontend is built with HTML, CSS, and JavaScript, providing a simple and intuitive user interface.

## License

[MIT](https://choosealicense.com/licenses/mit/) 