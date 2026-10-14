# Task 13: Configuration Management Specification

## Project
Event Sponsorship Tracker

## Objective
Use Ansible to automate the configuration of server prerequisites in Ubuntu 24.04 LTS running under WSL2.

## Target Environment
- Operating system: Ubuntu 24.04 LTS
- Configuration management tool: Ansible Core 2.16.3
- Inventory target: localhost
- Connection: Local

## Required Packages
- curl
- ca-certificates
- unzip
- nginx

## User and Group
- Service user: eventtracker
- Service group: eventtracker
- User type: System user
- Login shell: /usr/sbin/nologin

## Directories
| Directory | Owner | Group | Permissions |
|---|---|---|---|
| /opt/event-sponsorship-tracker | eventtracker | eventtracker | 0750 |
| /var/log/event-sponsorship-tracker | eventtracker | eventtracker | 0750 |
| /etc/event-sponsorship-tracker | eventtracker | eventtracker | 0750 |

## Deployment Files
- /etc/event-sponsorship-tracker/deployment-spec.txt

The file documents the Docker container names, network, application endpoints and ports.

## Existing Application Deployment
- Spring Boot container: eventtracker-app
- Nginx container: eventtracker-nginx
- MySQL container: eventtracker-mysql
- Docker network: eventtracker-net
- Spring Boot container port: 8080
- Application host port: 8081
- Nginx host port: 80
- MySQL host port: 3306
- Health endpoint: /actuator/health
- Dashboard endpoint: /dashboard

These ports and container details describe the existing Windows Docker deployment. The Ansible playbook does not start, stop, or reconfigure those containers.

## Services
- Nginx package installation is verified.
- Nginx is installed in Ubuntu WSL; this playbook does not configure or start its service.
- Existing Docker application services remain managed by the existing deployment workflow.

## Verification
- Ansible syntax check passed.
- First playbook execution: 8 tasks OK, 5 changed, 0 failed.
- Second playbook execution: 8 tasks OK, 0 changed, 0 failed.
- Service user, directories, packages and deployment specification were verified.
- The second run demonstrated idempotency.
