#!/bin/bash

# LocalScopeLocal Build Script

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to display messages
print_message() {
    echo -e "${GREEN}[LocalScopeLocal]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 8 or higher."
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print_message "Found Java version: $java_version"
}

# Check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven."
        exit 1
    fi
    
    mvn_version=$(mvn --version | awk '/Apache Maven/ {print $3}')
    print_message "Found Maven version: $mvn_version"
}

# Build the backend
build_backend() {
    print_message "Building backend..."
    
    cd backend
    
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found in backend directory!"
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        print_error "Backend build failed!"
        exit 1
    fi
    
    print_message "Backend built successfully!"
    cd ..
}

# Copy frontend to static resources
copy_frontend() {
    print_message "Copying frontend files to backend static resources..."
    
    # Create static directory if it doesn't exist
    mkdir -p backend/src/main/resources/static
    
    # Copy frontend files
    cp -r frontend/* backend/src/main/resources/static/
    
    print_message "Frontend files copied successfully!"
}

# Run the application
run_app() {
    print_message "Running the application..."
    
    cd backend
    mvn spring-boot:run
}

# Main execution
main() {
    print_message "Starting build process..."
    
    # Check prerequisites
    check_java
    check_maven
    
    # Build and run
    build_backend
    copy_frontend
    
    print_message "Build completed successfully!"
    
    # Ask if user wants to run the application
    read -p "Do you want to run the application now? (y/n): " choice
    case "$choice" in 
        y|Y ) run_app;;
        * ) print_message "To run the application later, use: cd backend && mvn spring-boot:run";;
    esac
}

# Execute main function
main 