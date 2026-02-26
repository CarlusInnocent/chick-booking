# Google Cloud Run Deployment Guide for Chick Booking System

## Prerequisites
1. Google Cloud account with billing enabled
2. Install Google Cloud SDK: https://cloud.google.com/sdk/docs/install
3. Docker Desktop installed (for local testing)

---

## Step 1: Set Up Google Cloud

### 1.1 Create a Google Cloud Project
```bash
# Login to Google Cloud
gcloud auth login

# Create a new project (or use existing)
gcloud projects create chicke-booking --name="Chicke Booking"

# Set as active project
gcloud config set project chicke-booking

# Enable required APIs
gcloud services enable run.googleapis.com
gcloud services enable sqladmin.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

---

## Step 2: Set Up PostgreSQL Database

### Option A: Cloud SQL (Recommended - Managed PostgreSQL)
```bash
# Create Cloud SQL PostgreSQL instance
gcloud sql instances create chicke-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=europe-west1 \
  --root-password=YOUR_SECURE_PASSWORD

# Create database
gcloud sql databases create chick_booking --instance=chicke-db

# Create user
gcloud sql users create app_user \
  --instance=chicke-db \
  --password=YOUR_APP_PASSWORD
```

**Cost Warning**: Cloud SQL db-f1-micro costs ~$7-10/month. It does NOT scale to zero.

### Option B: Free PostgreSQL Options
1. **Supabase** (Free tier): https://supabase.com
2. **Neon** (Free tier): https://neon.tech
3. **ElephantSQL** (Free tier): https://www.elephantsql.com

For these, you'll get a connection URL like:
```
postgresql://user:password@host:5432/database
```

---

## Step 3: Build and Deploy to Cloud Run

### 3.1 Build the Docker Image
```bash
# Navigate to project directory
cd chicke-booking/chicke-booking

# Build image using Cloud Build (no local Docker needed)
gcloud builds submit --tag gcr.io/chicke-booking/chicke-booking

# OR build locally and push
docker build -t gcr.io/chicke-booking/chicke-booking .
docker push gcr.io/chicke-booking/chicke-booking
```

### 3.2 Deploy to Cloud Run

#### If using Cloud SQL:
```bash
gcloud run deploy chicke-booking \
  --image gcr.io/chicke-booking/chicke-booking \
  --platform managed \
  --region europe-west1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 2 \
  --set-env-vars "DATABASE_URL=jdbc:postgresql:///chick_booking?cloudSqlInstance=chicke-booking:europe-west1:chicke-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
  --set-env-vars "DATABASE_USERNAME=app_user" \
  --set-env-vars "DATABASE_PASSWORD=YOUR_APP_PASSWORD" \
  --set-env-vars "JWT_SECRET=your-production-jwt-secret-here" \
  --add-cloudsql-instances chicke-booking:europe-west1:chicke-db
```

#### If using external PostgreSQL (Supabase/Neon):
```bash
gcloud run deploy chicke-booking \
  --image gcr.io/chicke-booking/chicke-booking \
  --platform managed \
  --region europe-west1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 2 \
  --set-env-vars "DATABASE_URL=jdbc:postgresql://your-host:5432/your-database" \
  --set-env-vars "DATABASE_USERNAME=your-username" \
  --set-env-vars "DATABASE_PASSWORD=your-password" \
  --set-env-vars "JWT_SECRET=your-production-jwt-secret-here"
```

---

## Step 4: Post-Deployment

### Get your app URL
```bash
gcloud run services describe chicke-booking --region europe-west1 --format="value(status.url)"
```

### View logs
```bash
gcloud run logs read --service chicke-booking --region europe-west1
```

### Update deployment
```bash
# After code changes, rebuild and redeploy
gcloud builds submit --tag gcr.io/chicke-booking/chicke-booking
gcloud run deploy chicke-booking --image gcr.io/chicke-booking/chicke-booking --region europe-west1
```

---

## Cost Estimates

| Component | Free Tier | Beyond Free Tier |
|-----------|-----------|------------------|
| Cloud Run | 2M requests/month | ~$0.00002/request |
| Cloud Run (min-instances=0) | $0 when idle | Pay per second used |
| Cloud Run (min-instances=1) | N/A | ~$5-11/month |
| Cloud SQL | None | ~$7-10/month (db-f1-micro) |
| Supabase/Neon DB | Free tier | Varies |

### To Minimize Costs:
1. Use `--min-instances 0` (cold starts but free when idle)
2. Use external free PostgreSQL (Supabase/Neon) instead of Cloud SQL
3. Set billing alerts in Google Cloud Console

---

## Budget Alerts (Important!)

```bash
# Set a budget alert to avoid surprises
gcloud billing budgets create \
  --billing-account=YOUR_BILLING_ACCOUNT_ID \
  --display-name="Chicke Booking Budget" \
  --budget-amount=10USD \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90 \
  --threshold-rule=percent=100
```

---

## Delete Everything (Stop All Billing)

```bash
# Delete the Cloud Run service
gcloud run services delete chicke-booking --region europe-west1

# Delete Cloud SQL (if used)
gcloud sql instances delete chicke-db

# Delete container images
gcloud container images delete gcr.io/chicke-booking/chicke-booking --force-delete-tags
```

---

## Troubleshooting

### Cold Start Too Slow?
Add to application-prod.properties:
```properties
spring.main.lazy-initialization=true
```

### Out of Memory?
Increase memory allocation:
```bash
gcloud run deploy chicke-booking --memory 1Gi ...
```

### Database Connection Issues?
- Check firewall rules for external DBs
- Verify connection string format
- Check Cloud SQL proxy is properly configured

---

## Quick Start Summary

1. `gcloud auth login`
2. `gcloud config set project chicke-booking`
3. `gcloud services enable run.googleapis.com`
4. `gcloud builds submit --tag gcr.io/chicke-booking/chicke-booking`
5. `gcloud run deploy chicke-booking --image gcr.io/chicke-booking/chicke-booking ...`
