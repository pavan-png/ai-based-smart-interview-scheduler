# 🚀 AI-Based Smart Interview Scheduler

An intelligent interview scheduling system that automates interview planning using Google Calendar integration, Gmail notifications, and AI-powered enhancements.

---

## 📌 Overview

The **AI-Based Smart Interview Scheduler** helps automate the interview process by integrating with Google services and AI APIs.
It allows users to schedule interviews, manage availability, and automatically create calendar events with notifications.

---

## ✨ Features

* 📅 Google Calendar Integration
* 🔐 Google OAuth 2.0 Authentication
* 📩 Email Notifications using Gmail SMTP
* 🤖 AI Integration using Groq API
* 👥 Candidate & Interviewer Management
* ⏰ Smart Interview Scheduling

---

## 🏗️ Tech Stack

### Backend

* Java
* Spring Boot
* Spring Data JPA
* Hibernate

### Frontend

* HTML
* CSS
* JavaScript
* Bootstrap

### Integrations

* Google Calendar API
* Google OAuth 2.0
* Gmail SMTP
* Groq API

---

## 📂 Important Project Structure

```text
src/main/java → Controllers, Services, Repositories
src/main/resources/static → Frontend (HTML, CSS, JS, Bootstrap)
src/main/resources/static/goole-credentials.json → Google OAuth file
tokens/ → Stores OAuth tokens (auto-generated)
```

---

## 🔐 Required Configurations (MANDATORY)

Before running the project, configure the following:

---

### 1️⃣ Google OAuth Credentials

* Download credentials from Google Cloud Console
* Place the JSON file here:

```text
src/main/resources/static/goole-credentials.json
```

---

### 2️⃣ Gmail SMTP Setup

You must configure:

* Gmail ID
* 16-digit App Password (generated via Google Account)

```properties
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_digit_app_password
```

---

### 3️⃣ Groq API Configuration

* Generate API Key from Groq
* Add it in your configuration file or environment

Example:

```properties
groq.api.key=your_groq_api_key
```

---

## ⚙️ How to Run the Project

### 🔹 Step 1: Clone Repository

```bash
git clone https://github.com/your-username/ai-based-smart-interview-scheduler.git
cd ai-based-smart-interview-scheduler
```

---

### 🔹 Step 2: Add Required Files

* Add Google credentials JSON in `static/`
* Configure Gmail App Password
* Add Groq API key

---

### 🔹 Step 3: Run Application

```bash
mvn clean install
mvn spring-boot:run
```

---

### 🔹 Step 4: Access Application

Open browser:

👉 http://localhost:8080/

---

## 🔄 Execution Flow

1. User opens application at `localhost:8080`
2. Authenticates via Google OAuth
3. Credentials are read from `static` folder
4. OAuth tokens are generated and stored in `tokens/`
5. User schedules interview
6. System:

   * Checks availability
   * Creates Google Calendar event
   * Sends email via Gmail SMTP
   * Uses Groq API for AI-based enhancements
7. Interview is scheduled successfully 🎉

---

## ⚠️ Important Notes

* ❌ Do NOT upload:

  * `tokens/`
  * `goole-credentials.json`
  * API keys

* ✅ Always include them in `.gitignore`

---

## 🛡️ Security Best Practices

* Store secrets outside code (env variables)
* Never push credentials to GitHub
* Rotate keys if exposed

---

## 📈 Future Enhancements

* AI-based candidate evaluation
* Dashboard analytics
* Interview feedback system
* Multi-user roles

---

## 👨‍💻 Author

**Mohan Pavan Kalyan**
Java Developer | Spring Boot

---

## ⭐ Support

If you like this project, give it a ⭐ on GitHub!
