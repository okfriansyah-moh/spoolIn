FROM mcr.microsoft.com/playwright:v1.49.1-jammy
RUN apt-get update && apt-get install -y --no-install-recommends libnss3-tools && rm -rf /var/lib/apt/lists/*
WORKDIR /work/project
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV ACCOUNT_CERTUTIL=/usr/bin/certutil
ENV ACCOUNT_BROWSER_ENGINE=chromium
