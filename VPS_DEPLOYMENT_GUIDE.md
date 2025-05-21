# VPS Deployment Guide: Full-Stack Application on Ubuntu 25.04

This guide will walk you through the process of deploying your full-stack application (Java Spring backend and JavaScript frontend) to a new Ubuntu 25.04 VPS. It also includes considerations for hosting multiple projects.

## Table of Contents

1.  [Prerequisites](#prerequisites)
2.  [Initial Server Setup](#initial-server-setup)
    *   [Connect to Your VPS](#connect-to-your-vps)
    *   [Update System Packages](#update-system-packages)
    *   [Create a Non-Root User](#create-a-non-root-user)
    *   [Basic Firewall Setup (UFW)](#basic-firewall-setup-ufw)
3.  [Software Installation](#software-installation)
    *   [Install Java (OpenJDK)](#install-java-openjdk)
    *   [Install Build Tool (Maven/Gradle)](#install-build-tool-mavengradle)
    *   [Install Node.js and npm/yarn (for Frontend)](#install-nodejs-and-npmyarn-for-frontend)
    *   [Install Web Server (Nginx)](#install-web-server-nginx)
    *   [Install Database (Optional)](#install-database-optional)
4.  [Project Deployment](#project-deployment)
    *   [Uploading Your Project Files](#uploading-your-project-files)
    *   [Backend Deployment (Spring Boot)](#backend-deployment-spring-boot)
        *   [Building the Application](#building-the-application)
        *   [Running the Spring Boot Application](#running-the-spring-boot-application)
        *   [Setting up with Systemd](#setting-up-with-systemd)
    *   [Frontend Deployment (JavaScript)](#frontend-deployment-javascript)
        *   [Building the Frontend](#building-the-frontend)
        *   [Configuring Nginx to Serve Frontend](#configuring-nginx-to-serve-frontend)
5.  [Nginx Configuration for Reverse Proxy and SSL](#nginx-configuration-for-reverse-proxy-and-ssl)
    *   [Configuring Nginx as a Reverse Proxy](#configuring-nginx-as-a-reverse-proxy)
    *   [Securing Nginx with Let's Encrypt (SSL)](#securing-nginx-with-lets-encrypt-ssl)
6.  [Tomcat Installation (Alternative for WAR deployments)](#tomcat-installation-alternative-for-war-deployments)
7.  [Tips for Hosting Multiple Projects](#tips-for-hosting-multiple-projects)
    *   [Nginx Server Blocks (Virtual Hosts)](#nginx-server-blocks-virtual-hosts)
    *   [Directory Structure](#directory-structure)
8.  [Troubleshooting](#troubleshooting)

---

## 1. Prerequisites

*   A newly provisioned VPS running Ubuntu 25.04.
*   SSH access to your VPS with root or sudo privileges.
*   Your project code (backend and frontend) ready for deployment.
*   A domain name pointed to your VPS's IP address (recommended for online accessibility and SSL).

---

## 2. Initial Server Setup

### Connect to Your VPS

Use SSH to connect to your server. Replace `your_vps_ip` with your server's actual IP address.

```bash
ssh root@your_vps_ip
```

### Update System Packages

It's crucial to start with an up-to-date system.

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y build-essential git curl wget unzip
```

### Create a Non-Root User

Operating as root is risky. Create a new user and grant sudo privileges.

```bash
# Replace 'your_username' with your desired username
adduser your_username
usermod -aG sudo your_username
```

Log out from the root user and log back in as the new user:

```bash
exit
ssh your_username@your_vps_ip
```

From now on, use `sudo` for commands requiring root privileges.

### Basic Firewall Setup (UFW)

UFW (Uncomplicated Firewall) is an easy-to-use interface for `iptables`.

```bash
sudo ufw allow OpenSSH  # Allows SSH connections
sudo ufw allow http     # Allows HTTP traffic (port 80)
sudo ufw allow https    # Allows HTTPS traffic (port 443)
# If your Spring Boot app runs on a specific port (e.g., 8080) and you need direct access temporarily:
# sudo ufw allow 8080/tcp
sudo ufw enable         # Enable the firewall
sudo ufw status         # Check the status
```
Ensure OpenSSH is allowed before enabling UFW to avoid locking yourself out.

---

## 3. Software Installation

### Install Java (OpenJDK)

Spring Boot applications require a Java Runtime Environment (JRE) or Java Development Kit (JDK). Let's install OpenJDK 17 (a recent LTS version, adjust if your project needs a specific version).

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version # Verify installation
```

You can also set the `JAVA_HOME` environment variable if needed, though it's often set automatically. Add to `~/.bashrc` or `/etc/environment`:
```bash
# Example for /etc/environment (system-wide):
# sudo nano /etc/environment
# Add this line:
# JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
# source /etc/environment # to apply immediately or re-login
```

### Install Build Tool (Maven/Gradle)

You'll likely need Maven or Gradle to build your Java project on the server or if you plan to pull source code and build directly.

**For Maven:**
```bash
sudo apt install -y maven
mvn -version # Verify installation
```

**For Gradle:**
Gradle is often best installed via SDKMAN or by downloading the binary.
Using SDKMAN:
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle
gradle -v # Verify installation
```
Alternatively, download the latest Gradle binary from the official website and configure it manually.

### Install Node.js and npm/yarn (for Frontend)

For your JavaScript frontend, you'll need Node.js and a package manager (npm comes with Node.js, yarn is an alternative). We'll use NodeSource repositories for a recent version.

```bash
# Check NodeSource for the latest LTS version if needed (e.g., Node 20.x)
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt-get install -y nodejs
node -v  # Verify Node.js installation
npm -v   # Verify npm installation
```

**Optional: Install Yarn**
```bash
sudo npm install -g yarn
yarn --version # Verify yarn installation
```

### Install Web Server (Nginx)

Nginx is a high-performance web server that we'll use as a reverse proxy for your Spring Boot application and to serve your static frontend files.

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx # Start Nginx on boot
sudo systemctl status nginx # Check status
```
You should be able to see the default Nginx welcome page by navigating to `http://your_vps_ip` in your browser.

### Install Database (Optional)

If your application requires a database (e.g., PostgreSQL, MySQL), install it now.

**Example: Install PostgreSQL**
```bash
sudo apt update
sudo apt install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
# Create a database and user for your application
# sudo -u postgres psql
# CREATE DATABASE myappdb;
# CREATE USER myappuser WITH ENCRYPTED PASSWORD 'your_password';
# GRANT ALL PRIVILEGES ON DATABASE myappdb TO myappuser;
# \q
```

**Example: Install MySQL**
```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
# sudo mysql_secure_installation # Secure your installation
# Create a database and user:
# sudo mysql
# CREATE DATABASE myappdb;
# CREATE USER 'myappuser'@'localhost' IDENTIFIED BY 'your_password';
# GRANT ALL PRIVILEGES ON myappdb.* TO 'myappuser'@'localhost';
# FLUSH PRIVILEGES;
# EXIT;
```
Remember to update your Spring Boot application's `application.properties` or `application.yml` with the database credentials.

---

## 4. Project Deployment

### Uploading Your Project Files

You have several ways to get your project files onto the VPS:

1.  **Git Clone (Recommended for ongoing development):**
    If your project is in a Git repository (e.g., GitHub, GitLab).
    ```bash
    # Navigate to where you want to store your projects, e.g., /var/www or ~/projects
    mkdir -p ~/projects
    cd ~/projects
    git clone your_repository_url
    ```

2.  **SCP (Secure Copy):**
    To copy files/folders from your local machine to the VPS.
    *   **To copy a file:**
        ```bash
        scp /path/to/local/file.jar your_username@your_vps_ip:/path/to/remote/directory
        ```
    *   **To copy a directory (e.g., your built frontend):**
        ```bash
        scp -r /path/to/local/frontend_dist your_username@your_vps_ip:/path/to/remote/directory
        ```

3.  **SFTP:**
    Use an SFTP client like FileZilla or Cyberduck.

**Recommended Directory Structure for Projects:**
A common practice is to store web application files in `/var/www/`.
```bash
# For your first project, e.g., myapp
sudo mkdir -p /var/www/myapp
sudo chown -R your_username:your_username /var/www/myapp # Give your user ownership
cd /var/www/myapp
# Now clone or copy your project files here
```

### Backend Deployment (Spring Boot)

#### Building the Application

If you haven't built your Spring Boot application into an executable JAR or WAR file, do it now.

*   **Locally:** Build the JAR/WAR on your development machine and then upload it.
    *   Using Maven: `mvn clean package`
    *   Using Gradle: `gradle clean build`
    The resulting file (e.g., `target/your-app-name.jar` or `libs/your-app-name.jar`) is what you'll deploy.

*   **On the Server (if you cloned the source):**
    Navigate to your project's backend directory on the VPS.
    ```bash
    cd /var/www/myapp/backend # Or your project's backend path
    # Using Maven
    mvn clean package -DskipTests # Skip tests if not needed for deployment build
    # Using Gradle
    gradle clean build -x test # Skip tests
    ```
    Your JAR file will typically be in the `target/` (Maven) or `build/libs/` (Gradle) directory. Let's assume it's `your-app.jar`.

#### Running the Spring Boot Application

Navigate to the directory containing your JAR file.
```bash
# Example:
# cd /var/www/myapp/backend/target
java -jar your-app.jar
```
Your Spring Boot application should now be running, typically on port 8080 (or as configured in `application.properties`). You can test it locally on the server:
```bash
curl http://localhost:8080
```
If you allowed port 8080 through UFW, you could temporarily test it via `http://your_vps_ip:8080`.

#### Setting up with Systemd

To ensure your Spring Boot application runs continuously (restarts on failure or server reboot), use `systemd`.

1.  **Create a systemd service file:**
    Replace `your_app_name` with a descriptive name for your service (e.g., `myapp-spring`).
    Replace `your_username` with the user you created.
    Replace `/var/www/myapp/backend/target/your-app.jar` with the actual path to your JAR file.
    Replace `YOUR_APP_DESCRIPTION` with a short description.
    If your app uses a specific profile, add it to `ExecStart` e.g. `--spring.profiles.active=prod`

    ```bash
    sudo nano /etc/systemd/system/your_app_name.service
    ```

    Paste the following content, adjusting paths and names:

    ```ini
    [Unit]
    Description=YOUR_APP_DESCRIPTION (e.g., My Spring Boot Application)
    After=syslog.target network.target

    [Service]
    User=your_username
    Group=your_username # Or a relevant group
    # ExecStartPre=/path/to/your/pre-start-script (optional)
    ExecStart=/usr/bin/java -jar /var/www/myapp/backend/target/your-app.jar
    # Example with profile: ExecStart=/usr/bin/java -jar /var/www/myapp/backend/target/your-app.jar --spring.profiles.active=prod
    SuccessExitStatus=143
    TimeoutStopSec=10
    Restart=on-failure
    RestartSec=5
    # WorkingDirectory=/var/www/myapp/backend/target/ (optional, if your app needs it)
    # Environment="SPRING_PROFILES_ACTIVE=prod" (alternative way to set profile)

    [Install]
    WantedBy=multi-user.target
    ```

2.  **Reload systemd, enable, and start your service:**
    ```bash
    sudo systemctl daemon-reload
    sudo systemctl enable your_app_name.service
    sudo systemctl start your_app_name.service
    sudo systemctl status your_app_name.service # Check status
    ```

    To see logs:
    ```bash
    sudo journalctl -u your_app_name.service -f
    ```

### Frontend Deployment (JavaScript)

#### Building the Frontend

If your frontend is a Single Page Application (SPA) built with a framework like React, Angular, or Vue, you need to build it for production.

*   **Locally:** Build the frontend on your development machine.
    ```bash
    # Example for a React app (Create React App)
    # npm run build  or  yarn build
    ```
    This usually creates a `build` or `dist` folder with static assets (HTML, CSS, JS). Upload this folder to your VPS (e.g., using `scp`).

*   **On the Server (if you cloned the source):**
    Navigate to your project's frontend directory.
    ```bash
    cd /var/www/myapp/frontend # Or your project's frontend path
    npm install # or yarn install
    npm run build # or yarn build
    ```
    Let's assume the build output is in `/var/www/myapp/frontend/dist`.

#### Configuring Nginx to Serve Frontend

Nginx will serve your static frontend files. We'll create a server block for this later. For now, ensure your built frontend files are in a known location (e.g., `/var/www/myapp/frontend/dist` or `/var/www/myapp/public_html`).

---

## 5. Nginx Configuration for Reverse Proxy and SSL

Nginx will act as a reverse proxy, forwarding requests to your Spring Boot application (running, for example, on `localhost:8080`) and serving your static frontend assets.

### Configuring Nginx as a Reverse Proxy

1.  **Create an Nginx server block configuration file:**
    Replace `your_domain.com` with your actual domain name. If you don't have a domain yet, you can use your VPS IP address for testing, but a domain is needed for SSL.
    ```bash
    sudo nano /etc/nginx/sites-available/your_app_name
    ```

    Paste and adapt the following configuration. This example assumes:
    *   Your Spring Boot app runs on `http://localhost:8080`.
    *   Your frontend static files are in `/var/www/myapp/frontend/dist`.
    *   Requests to `/api/` should go to the backend, others to the frontend.

    ```nginx
    server {
        listen 80;
        listen [::]:80;

        server_name your_domain.com www.your_domain.com; # Replace with your domain or IP

        root /var/www/myapp/frontend/dist; # Path to your frontend's build files
        index index.html index.htm;

        location / {
            try_files $uri $uri/ /index.html; # For SPAs, ensures client-side routing works
        }

        location /api/ { # Or any other base path for your backend
            proxy_pass http://localhost:8080/; # Assuming Spring Boot runs on 8080
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # WebSocket support (if your Spring app uses WebSockets)
            # proxy_http_version 1.1;
            # proxy_set_header Upgrade $http_upgrade;
            # proxy_set_header Connection "upgrade";
        }

        # Optional: Add error pages, logs, etc.
        # access_log /var/log/nginx/your_app_name.access.log;
        # error_log /var/log/nginx/your_app_name.error.log;
    }
    ```

2.  **Enable the server block by creating a symbolic link:**
    ```bash
    sudo ln -s /etc/nginx/sites-available/your_app_name /etc/nginx/sites-enabled/
    ```

3.  **Test Nginx configuration and reload:**
    ```bash
    sudo nginx -t
    # If successful:
    sudo systemctl reload nginx
    ```

Your application should now be accessible via `http://your_domain.com` (or your IP). Frontend requests will be served directly, and requests to `/api/` will be proxied to your Spring Boot backend.

### Securing Nginx with Let's Encrypt (SSL)

It's highly recommended to use HTTPS. Let's Encrypt provides free SSL certificates.

1.  **Install Certbot (Let's Encrypt client):**
    ```bash
    sudo apt update
    sudo apt install -y certbot python3-certbot-nginx
    ```

2.  **Obtain and install an SSL certificate:**
    Make sure your domain name is correctly pointing to your VPS's IP address.
    ```bash
    # Replace your_domain.com and www.your_domain.com
    sudo certbot --nginx -d your_domain.com -d www.your_domain.com
    ```
    Certbot will guide you through the process, including automatically updating your Nginx configuration for SSL and setting up automatic renewal. Choose the option to redirect HTTP traffic to HTTPS.

3.  **Verify auto-renewal:**
    Certbot usually sets up a cron job or systemd timer for renewal.
    ```bash
    sudo systemctl status certbot.timer
    sudo certbot renew --dry-run # Test the renewal process
    ```

Your site should now be accessible via `https://your_domain.com`.

---

## 6. Tomcat Installation (Alternative for WAR deployments)

If your Spring Boot application is packaged as a WAR file and you prefer to deploy it to a standalone Tomcat server (instead of using the embedded server in an executable JAR):

1.  **Install Tomcat:**
    Check the official Tomcat website for the latest version (e.g., Tomcat 10).
    ```bash
    sudo apt update
    # Create a tomcat user and group
    sudo groupadd tomcat
    sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat

    # Download and extract Tomcat (adjust version number)
    cd /tmp
    TOMCAT_VERSION=10.1.19 # Check latest Tomcat 10 version
    wget https://dlcdn.apache.org/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz
    sudo mkdir /opt/tomcat
    sudo tar xzvf apache-tomcat-*.tar.gz -C /opt/tomcat --strip-components=1

    # Set permissions
    sudo chown -R tomcat:tomcat /opt/tomcat
    sudo sh -c 'chmod +x /opt/tomcat/bin/*.sh'

    # Create a systemd service file for Tomcat
    sudo nano /etc/systemd/system/tomcat.service
    ```

    Paste the following into `tomcat.service`, ensuring `JAVA_HOME` is correct (or remove the Environment line if Java is in PATH):
    ```ini
    [Unit]
    Description=Apache Tomcat Web Application Container
    After=network.target

    [Service]
    Type=forking

    User=tomcat
    Group=tomcat

    Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" # Adjust if needed
    Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
    Environment="CATALINA_HOME=/opt/tomcat"
    Environment="CATALINA_BASE=/opt/tomcat"
    Environment='CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC'
    Environment='JAVA_OPTS=-Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom'

    ExecStart=/opt/tomcat/bin/startup.sh
    ExecStop=/opt/tomcat/bin/shutdown.sh

    Restart=on-failure

    [Install]
    WantedBy=multi-user.target
    ```

    ```bash
    sudo systemctl daemon-reload
    sudo systemctl start tomcat
    sudo systemctl enable tomcat
    sudo systemctl status tomcat
    ```
    You may need to open port 8080 in UFW if you want to access Tomcat directly (`sudo ufw allow 8080/tcp`).

2.  **Deploy your WAR file:**
    Copy your `your-app.war` file to Tomcat's `webapps` directory:
    ```bash
    sudo cp /path/to/your-app.war /opt/tomcat/webapps/
    ```
    Tomcat will automatically deploy it. Your app will usually be available at `http://your_vps_ip:8080/your-app/`.

3.  **Nginx Reverse Proxy for Tomcat:**
    If using Tomcat, your Nginx `proxy_pass` directive would change to:
    ```nginx
    # In your /etc/nginx/sites-available/your_app_name
    location /api/ { # Or your app's context path if deployed to /
        proxy_pass http://localhost:8080/your-app/; # Note the context path of your WAR
        # ... other proxy_set_header directives ...
    }
    ```
    If your WAR is deployed as ROOT.war, it will be accessible at `/`.

---

## 7. Tips for Hosting Multiple Projects

As you plan to host more projects, here's how to manage them:

### Nginx Server Blocks (Virtual Hosts)

For each new project/domain, create a new Nginx server block configuration file in `/etc/nginx/sites-available/` and enable it with a symbolic link in `/etc/nginx/sites-enabled/`.

Example structure:
*   `/etc/nginx/sites-available/project1.com`
*   `/etc/nginx/sites-available/project2.org`

Each file will look similar to the one created in section 5, but with different `server_name`, `root` (for frontend), and `proxy_pass` directives (if they have backends).

Make sure each `server_name` is unique and corresponds to the domain/subdomain for that project. You'll need to obtain SSL certificates for each new domain/subdomain as well.

### Directory Structure

Organize your project files logically. A common approach:
```
/var/www/
├── project1.com/
│   ├── backend/      # Spring Boot app for project1
│   │   └── target/
│   │       └── project1.jar
│   ├── frontend/     # Frontend build for project1 (e.g., dist folder)
│   └── logs/         # (Optional) specific logs for project1
│
├── project2.org/
│   ├── backend/
│   │   └── target/
│   │       └── project2.jar
│   ├── frontend/
│   └── logs/
│
└── shared/           # (Optional) for shared static assets if any
```
Adjust permissions for each project directory so that the respective user (if you create different users per app) or your primary non-root user can manage them.

For systemd services, create a unique `.service` file for each backend application (e.g., `project1.service`, `project2.service`), ensuring they listen on different ports if they are not all proxied through Nginx via unique location blocks or server names. If Nginx handles routing to the correct backend based on domain name or path, they can often run on their default ports (e.g., 8080, 8081, etc.) internally.

---

## 8. Troubleshooting

*   **Check Logs:**
    *   Nginx: `/var/log/nginx/error.log`, `/var/log/nginx/access.log`, and custom logs defined in server blocks.
    *   Your Spring Boot app (via systemd): `sudo journalctl -u your_app_name.service -f`
    *   Tomcat: `/opt/tomcat/logs/catalina.out` and other logs in that directory.
    *   UFW: `sudo ufw status verbose` (to check rules), `sudo less /var/log/ufw.log` (for denied packets).
    *   Syslog: `sudo less /var/log/syslog`

*   **Nginx Configuration:**
    Always run `sudo nginx -t` after making changes to Nginx configuration files before reloading.

*   **Port Conflicts:**
    Ensure no other services are using the ports your applications or Nginx need. Use `sudo ss -tulnp | grep LISTEN` or `sudo netstat -tulnp | grep LISTEN` to see listening ports.

*   **Permissions:**
    File and directory permissions can often cause issues. Ensure Nginx can read frontend files and your application user can execute JARs and write to log directories if needed.

*   **Firewall:**
    Double-check UFW rules to ensure HTTP (80), HTTPS (443), and any other necessary ports (e.g., your backend's direct port if testing) are open.

*   **Spring Boot Application Fails to Start:**
    *   Check Java version compatibility.
    *   Ensure database is running and accessible with correct credentials in `application.properties/yml`.
    *   Look for stack traces in the `journalctl` output.
    *   Test running the JAR directly (`java -jar your-app.jar`) to see immediate console output.

This guide provides a comprehensive starting point. Depending on the specifics of your applications, you might need to adjust certain steps or configurations. Good luck! 