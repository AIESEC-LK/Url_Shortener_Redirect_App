# 📑 URL Shortener

A Spring Boot application that shortens and redirects URLs using **Google Sheets** as a backend database.

## 🚀 Features
- Shorten URLs dynamically using Google Sheets
- Retrieve and redirect shortcuts to actual URLs
- Supports **Google OAuth 2.0 Service Account** authentication
- Logs application events using **SLF4J**
- Easily deployable on **AWS EC2 / Azure / Google Cloud Run**

---

## 🛠️ Installation & Deployment

### **1️⃣ Clone the Repository**
```sh
git clone https://github.com/AIESEC-LK/Url_Shortener_Redirect_App.git
cd url-shortener
```

### **2️⃣ Set Up Environment Variables**
Create a **`.env`** file in the root directory and configure:
```properties
# Project Information

# Deployment Configurations
SERVER_HOST="https://signup.aiesec.lk"
SERVER_PORT="4000"
JAR_PATH="${urlshortener_dir}/urlshortener-0.0.1-SNAPSHOT.jar"
LOG_FILE="${urlshortener_dir}/log.out"

# Google API Credentials
GOOGLE_SHEETS_API_KEY=${API_KEY}
GOOGLE_CREDENTIALS_PATH="${urlshortener_dir}/credentials.json"
SERVICE_ACCOUNT_EMAIL="lahirujayathilake@utmlinks-453902.iam.gserviceaccount.com"

# Logging Configuration
LOG_LEVEL="INFO"
```

### **3️⃣ Install Dependencies**
Make sure you have **Java 17** and **Maven** installed:
```sh
sudo apt update
sudo apt install openjdk-17-jdk maven -y
```

### **4️⃣ Build the Application**
```sh
mvn clean package
```
This will generate a **JAR file** in `target/`.

### **5️⃣ Deploy the Application**
Move the JAR file to your server and restart:
```sh
scp target/urlshortener-${version}.jar ubuntu@${your-ec2-ip}:${urlshortener_dir}
ssh ubuntu@${your-ec2-ip}

# Run deployment script
${urlshortener_dir}/deploy.sh
```

---

## 🖥️ API Endpoints

| Endpoint  | Method | Description |
|-----------|--------|-------------|
| `/` or `/**` | `GET` | Redirects to the mapped URL |

---

## 🔍 Logs & Debugging
To view application logs:
```sh
tail -f ${urlshortener_dir}/log.out
```

To debug the application:
```sh
java -jar urlshortener-${version}.jar --debug
```

---

## ⚡ Contributing
1. **Fork the repository**
2. **Create a new branch:** `git checkout -b feature-branch`
3. **Commit your changes:** `git commit -m "Added new feature"`
4. **Push to GitHub:** `git push origin feature-branch`
5. **Create a Pull Request**

---

## 📜 License
This project is licensed under the **MIT License**.

## 🙌 Credits
Developed by **Lahiru Jayathilake**  
📧 Contact: [lahiruthpala@gmail.com](mailto:lahiruthpala@gmail.com)
