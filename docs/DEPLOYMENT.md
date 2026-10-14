# Deployment (Pipeline as Code)

The `Jenkinsfile` builds, tests and packages the application, then (when `DEPLOY` is ticked) builds Docker
images and deploys the app behind an Nginx reverse proxy on this machine.

```
Browser ──► :80   eventtracker-nginx  ──┐   (Docker network: eventtracker-net)
Browser ──► :8081 eventtracker-app   ◄──┘ :8080
                   │
                   └──► host.docker.internal:3306 ──► eventtracker-mysql (existing container, unchanged)
```

| Component | Image | Host port | Notes |
|---|---|---|---|
| `eventtracker-app` | `event-sponsorship-tracker:<build>` (from `eclipse-temurin:21.0.12.1_1-jre-noble`) | `APP_PORT` (default 8081) | Runs the JAR that passed the pipeline's tests |
| `eventtracker-nginx` | `eventtracker-nginx:<build>` (from `nginx:1.30.5-alpine`) | `NGINX_PORT` (default 80) | Config: `deploy/nginx/default.conf` |
| `eventtracker-mysql` | `mysql:8.4` | 3306 | Existing container and volume; the pipeline never runs commands against it |

The app reaches MySQL through the port MySQL already publishes on the host, so the MySQL container is not
reconnected, restarted or recreated. Database credentials come only from Jenkins.

## Pipeline stages

| Stage | Runs when | What it does |
|---|---|---|
| Checkout, Build, Test, E2E Tests, Package | Always | Unchanged CI stages |
| Docker Image | `DEPLOY` ticked | Builds both images, tagged with the Jenkins build number and `latest` |
| Deploy | `DEPLOY` ticked | Validates ports, creates `eventtracker-net` if missing, replaces `eventtracker-app` and `eventtracker-nginx` |
| Verify Deployment | `DEPLOY` ticked | Waits for `/actuator/health` = `UP` directly and through Nginx, checks `/dashboard` through Nginx, archives `health-app.json` and `health-nginx.json` |

Parameters (shown under **Build with Parameters** after the first run):

| Parameter | Default | Purpose |
|---|---|---|
| `APP_PORT` | `8081` | Host port for the app container. 8040 (Jenkins) and 3306 (MySQL) are rejected. |
| `NGINX_PORT` | `80` | Host port for Nginx |
| `DEPLOY` | ticked | Untick to run CI only (no Docker stages) |

## Prerequisites

- Docker Desktop running; the Docker CLI is expected at
  `C:\Users\Windows\AppData\Local\Programs\DockerDesktop\resources\bin` (set in the Jenkinsfile as `DOCKER_BIN`).
- `eventtracker-mysql` running (its restart policy is `no`, so start it after a reboot:
  `docker start eventtracker-mysql`).
- Host ports 80 and 8081 free (see [Port checks](#port-checks)).
- Internet access on the first deploy to pull the two base images.

## One-time setup

The commands below use PowerShell with:

```powershell
$docker = "C:\Users\Windows\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe"
```

### 1. Find the source host MySQL sees

The `eventtracker` MySQL account should only accept connections from the address the app container actually
connects from. Find that address with a one-off client container on the same network the app will use
(the `mysql:8.4` image is already present locally; `--rm` deletes the container afterwards):

```powershell
& $docker network create eventtracker-net    # skip if it already exists
& $docker run --rm -it --network eventtracker-net mysql:8.4 `
    mysql -h host.docker.internal -P 3306 -u root -p `
    -e "SELECT USER() AS seen_as, CURRENT_USER() AS matched_account;"
```

Enter the MySQL root password at the prompt. `seen_as` shows `root@<source-host>`; note the `<source-host>` part.

**Limitation.** With Docker Desktop, traffic to `host.docker.internal:3306` goes through Docker's port
forwarding, so MySQL usually sees a Docker gateway address (for example `172.17.0.1`), not the app container's
own IP. Every connection that arrives through the published port 3306 (from the Windows host, other containers
or the network) can appear with that same address. Restricting the account to it therefore blocks direct
container-to-container connections, but does not distinguish the app from other clients using the published
port. The gateway address can also change if Docker Desktop's network is reset; the app then fails with
`Access denied` and the account host must be updated. Only if the observed address turns out to vary between
connections should a wider pattern be considered (for example the subnet `'172.17.0.%'`), never `'%'` by default.

### 2. Create the dedicated MySQL user

Open a MySQL session as root (prompts for the password, so it is not stored in shell history):

```powershell
& $docker exec -it eventtracker-mysql mysql -u root -p
```

Then run, replacing `<source-host>` with the value from step 1 and choosing a strong password:

```sql
CREATE USER 'eventtracker'@'<source-host>' IDENTIFIED BY '<choose-a-strong-password>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON eventsponsorship_db.* TO 'eventtracker'@'<source-host>';
SHOW GRANTS FOR 'eventtracker'@'<source-host>';
```

These privileges are what `spring.jpa.hibernate.ddl-auto=update` needs (create/alter tables, indexes and
foreign keys). The account has no `DROP`, no `GRANT` and no access to other databases. Existing data is untouched.

### 3. Add the Jenkins credential

1. Open `http://localhost:8040` → **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**.
2. **Kind:** Username with password; **Scope:** Global.
3. **Username:** `eventtracker`; **Password:** the password chosen in step 2.
4. **ID:** `eventtracker-db` (must match the Jenkinsfile); **Description:** MySQL user for Event Sponsorship Tracker deployment.
5. Save.

The Jenkinsfile reads it with `withCredentials`. Docker receives the values through `-e DB_USERNAME -e DB_PASSWORD`
(no value on the command line), and Jenkins masks the password in the console log.

## Deploying

1. Push the branch, then open the job in Jenkins.
2. The first build after this change registers the parameters (it deploys with the defaults).
3. Later builds: **Build with Parameters** → set `APP_PORT` / `NGINX_PORT` / `DEPLOY` → **Build**.

### Confirm the MySQL source host after the first deploy

While the app is running, check which host the `eventtracker` connections come from:

```sql
SELECT user, host FROM information_schema.processlist WHERE user = 'eventtracker';
```

The host column should match the `<source-host>` used in step 2 (it may include a client port, e.g.
`172.17.0.1:53422`; compare the address part only).

## Port checks

```powershell
Get-NetTCPConnection -State Listen -LocalPort 80,8081 -ErrorAction SilentlyContinue   # no output = free
& $docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Stopped practice containers that are configured for these ports (for example `custom-nginx` on 8081) would
conflict if they were started again. If a port is taken, `docker run` fails with `port is already allocated`;
pick another port with the build parameters.

## Verifying a deployment

The **Verify Deployment** stage does this automatically. Manual checks:

```powershell
curl.exe -fsS http://localhost:8081/actuator/health        # app directly: {"status":"UP",...,"db":{"status":"UP"...}}
curl.exe -fsS http://localhost/actuator/health             # through Nginx
curl.exe -fsS -o NUL -w "%{http_code}`n" http://localhost/dashboard   # 200
& $docker ps --filter "name=eventtracker" --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
& $docker images event-sponsorship-tracker
& $docker logs --tail 50 eventtracker-app
```

Browser: `http://localhost/dashboard`, `http://localhost/events`, `http://localhost/sponsors` (through Nginx) and
`http://localhost:8081/dashboard` (direct).

## Evidence checklist

1. Jenkins **Build with Parameters** page showing `APP_PORT`, `NGINX_PORT` and `DEPLOY`.
2. Stage View with all stages green (Checkout → … → Package → Docker Image → Deploy → Verify Deployment).
3. Console log of **Deploy** (the `docker run` lines; the password is never shown) and **Verify Deployment**
   (the `{"status":"UP"...}` output and the "Deployed build" line).
4. Build artifacts: `health-app.json`, `health-nginx.json`, the JAR; **Test Result** page with the JUnit reports.
5. `docker ps` showing `eventtracker-app`, `eventtracker-nginx` and `eventtracker-mysql` running.
6. `docker images event-sponsorship-tracker` showing build-number tags.
7. Browser screenshots of the dashboard, events and sponsors pages through Nginx (`http://localhost/...`).
8. Persistence: create an event, run the pipeline again, and show the event is still listed.
9. `SHOW GRANTS FOR 'eventtracker'@'<source-host>';` and the processlist query above.

## Rollback

Every deploy keeps its images, tagged with the Jenkins build number. To go back to build `<N>`:

```powershell
& $docker images event-sponsorship-tracker       # list available build numbers

$cred = Get-Credential -UserName eventtracker -Message "MySQL password for eventtracker"
$env:DB_USERNAME = $cred.UserName
$env:DB_PASSWORD = $cred.GetNetworkCredential().Password
$env:DB_URL = "jdbc:mysql://host.docker.internal:3306/eventsponsorship_db"

& $docker rm -f eventtracker-nginx eventtracker-app
& $docker run -d --name eventtracker-app --network eventtracker-net --restart unless-stopped `
    -p 8081:8080 -e DB_URL -e DB_USERNAME -e DB_PASSWORD event-sponsorship-tracker:<N>
& $docker run -d --name eventtracker-nginx --network eventtracker-net --restart unless-stopped `
    -p 80:80 eventtracker-nginx:<N>

Remove-Item Env:DB_PASSWORD, Env:DB_USERNAME, Env:DB_URL
curl.exe -fsS http://localhost/actuator/health
```

Rollback only replaces the app and Nginx containers; the database is not rolled back. Old images can be
removed with `docker image rm event-sponsorship-tracker:<N> eventtracker-nginx:<N>` when no longer needed.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `docker: command not found` / `'docker' is not recognized` in Jenkins | `DOCKER_BIN` in the Jenkinsfile does not match the Docker install path |
| `port is already allocated` | Another process or container uses the port; choose another `APP_PORT`/`NGINX_PORT` |
| Health stays `DOWN` / curl gives up after ~90 s | `eventtracker-mysql` stopped (`docker start eventtracker-mysql`), or see `docker logs eventtracker-app` |
| `Access denied for user 'eventtracker'@'<host>'` in the app log | Account host does not match the source host; compare `<host>` with step 1 |
| `Could not find credentials entry with ID 'eventtracker-db'` | Jenkins credential missing or ID misspelled |
| Nginx returns `502 Bad Gateway` | App container not running or still starting; check `docker ps` and the app log |

## Security notes

- Credentials exist only in Jenkins; nothing secret is stored in the repository or images.
- The password is visible through `docker inspect eventtracker-app` to anyone with Docker access on this machine.
- MySQL's port 3306 is published on all host interfaces (existing setup); keep the `eventtracker` password strong.
- `/actuator/health` with details is reachable through Nginx as well as on the app port.
