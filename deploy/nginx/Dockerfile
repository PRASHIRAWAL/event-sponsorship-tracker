# Nginx reverse proxy for the Event Sponsorship Tracker.
# The config is baked into the image because the Jenkins workspace is deleted after each build.
FROM nginx:1.30.5-alpine
COPY default.conf /etc/nginx/conf.d/default.conf
