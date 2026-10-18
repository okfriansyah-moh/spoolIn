# Run SpoolIn locally

Use this guide if you're starting from a computer that doesn't have development tools installed. Commands go in a terminal, one block at a time. Don't type the ``` marks around a block. When `sudo` asks for your computer password, the terminal may show nothing as you type; press Enter when you're done.

## Contents

- [Before you start](#before-you-start)
- [Windows](#windows)
- [macOS](#macos)
- [Ubuntu Linux](#ubuntu-linux)
- [Download SpoolIn](#download-spoolin)
- [Start and sign in](#start-and-sign-in)
- [Everyday commands](#everyday-commands)
- [Troubleshooting](#troubleshooting)
- [Source development](#source-development)
- [Verification limits](#verification-limits)

## Before you start

You need an internet connection for the first download, permission to install software, and enough free storage for container images and build dependencies. Docker runs Linux containers, including on macOS and Windows. Check Docker's linked requirements for your operating system and hardware before installing it. The documentation work used a Docker environment with 8 GiB of memory and 8 CPUs; it had about 15 GiB free before builds. These are observed development resources, not established minimum requirements. Container builds can need additional disk space as caches grow.

| Tool | Why you need it |
| --- | --- |
| Git | Downloads the repository and later updates |
| Docker and Compose | Builds and runs the database and application services |
| Make | Gives the setup commands short, consistent names |
| Python 3.10 or newer | Runs the local setup helper |
| A browser | Opens the application |

Java, Node.js, npm, an editor, and Mermaid preview tools are optional for this container workflow. Docker builds the required application toolchains inside containers and starts PostgreSQL, so you don't need a separate database installation.

## Windows

Use Windows PowerShell for the first step, then Ubuntu for all project commands. WSL2 provides the Linux terminal that the setup scripts expect.

1. Open Start, search for **PowerShell**, and select **Run as administrator**. Install Ubuntu in WSL:

   ```powershell
   wsl --install -d Ubuntu
   ```

   Restart Windows if prompted. Open **Ubuntu** from Start and create the Linux username and password it requests. This is a separate account from your Windows account. Microsoft's [WSL installation guide](https://learn.microsoft.com/en-us/windows/wsl/install) covers installation failures and virtualization requirements.

2. In PowerShell, update WSL and check the installed distribution:

   ```powershell
   wsl --update
   wsl --list --verbose
   ```

   Ubuntu should show version `2`. If it shows `1`, run `wsl --set-version Ubuntu 2`.

3. Download and run [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/). Select the WSL2 backend when offered, launch Docker Desktop, and wait until the engine is running. In **Settings > Resources > WSL Integration**, enable Ubuntu and apply the change. See [Docker's WSL guide](https://docs.docker.com/desktop/features/wsl/).

4. Open **Ubuntu**, then install the remaining tools:

   ```bash
   sudo apt update
   sudo apt install -y git make python3 ca-certificates curl
   ```

5. In that same Ubuntu terminal, check the tools:

   ```bash
   git --version
   make --version
   python3 --version
   docker version
   docker compose version
   ```

   `docker version` must show both Client and Server. Keep Docker Desktop running. Continue to [Download SpoolIn](#download-spoolin) inside Ubuntu. Store the checkout under your Ubuntu home directory, not `/mnt/c`, to avoid Windows file permission and filesystem performance problems.

## macOS

1. Open **Terminal** from Applications > Utilities. Install Apple's command line tools:

   ```bash
   xcode-select --install
   ```

   Complete the installation dialog. If macOS says the tools are already installed, continue.

2. Install Homebrew, which installs command line software. This command downloads and runs Homebrew's installer; inspect [the Homebrew installation instructions](https://brew.sh/) first:

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

   Follow the installer's **Next steps**, including its `brew shellenv` instructions, then reopen Terminal. The exact Homebrew path differs between Apple silicon and Intel Macs.

3. Install the tools and Docker Desktop:

   ```bash
   brew install git make python
   brew install --cask docker
   open -a Docker
   ```

   Finish Docker Desktop's first-run setup and wait for the engine. Docker's [Mac installation guide](https://docs.docker.com/desktop/setup/install/mac-install/) explains supported hardware and system requirements. Homebrew installs GNU Make as `gmake`; the macOS `make` command also supports this project's targets.

4. Check the tools:

   ```bash
   git --version
   make --version
   python3 --version
   docker version
   docker compose version
   ```

   `docker version` must include a Server section. Continue to [Download SpoolIn](#download-spoolin).

## Ubuntu Linux

These steps use Ubuntu 22.04 or newer with Docker Engine and Docker's Compose plugin. If you already use Docker Desktop on Ubuntu, keep that installation and verify its active context before continuing; don't install a second engine just for SpoolIn.

1. Open **Terminal** and install basic tools:

   ```bash
   sudo apt update
   sudo apt install -y git make python3 ca-certificates curl
   ```

2. If Docker is already installed, check [Docker's Ubuntu instructions](https://docs.docker.com/engine/install/ubuntu/) for conflicting packages before changing it. For a fresh machine, add Docker's package signing key and repository:

   ```bash
   sudo install -m 0755 -d /etc/apt/keyrings
   sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
   sudo chmod a+r /etc/apt/keyrings/docker.asc
   . /etc/os-release
   sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF_DOCKER
   Types: deb
   URIs: https://download.docker.com/linux/ubuntu
   Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
   Components: stable
   Architectures: $(dpkg --print-architecture)
   Signed-By: /etc/apt/keyrings/docker.asc
   EOF_DOCKER
   sudo apt update
   sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   sudo systemctl enable --now docker
   ```

   When copying the heredoc block, keep `EOF_DOCKER` on its own line without leading spaces. The package setup follows [Docker's official Ubuntu installation guide](https://docs.docker.com/engine/install/ubuntu/).

3. Allow your account to use Docker, then log out of Ubuntu and log back in:

   ```bash
   sudo usermod -aG docker "$USER"
   ```

   Membership in the Docker group grants privileges comparable to root. Use this on a machine where you control that access. See [Docker's Linux post-installation guide](https://docs.docker.com/engine/install/linux-postinstall/).

4. In a new terminal, check the tools:

   ```bash
   git --version
   make --version
   python3 --version
   docker version
   docker compose version
   docker run --rm hello-world
   ```

   Resolve any permission error before continuing. Run SpoolIn's commands as your normal user so generated files belong to your account.

## Download SpoolIn

Use Ubuntu on Windows, Terminal on macOS, or your Linux terminal:

```bash
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/okfriansyah-moh/spoolIn.git
cd spoolIn
git switch codex/remote-acceptance-closure
```

The local helpers are currently on PR #10's `codex/remote-acceptance-closure` branch, so switch to it before setup. `git clone` creates the `spoolIn` folder. You only need it once. If you already have this repository, open its folder with `cd` and continue. Every `make local-*` command below runs from the folder containing `Makefile`.

## Start and sign in

1. Prepare the local environment:

   ```bash
   make local-setup
   ```

   Setup generates protected local configuration, certificates, and account secrets. Repeating setup preserves existing secrets and data.

2. Start the services:

   ```bash
   make local-start
   ```

   Wait for readiness checks to finish. Open `https://localhost:8443` after startup succeeds. Initial credentials are stored in `.local/accounts.txt`: `maintainer@example.invalid` can edit the catalog, and `viewer@example.invalid` has Viewer access. Their random passwords persist across repeated setup. Startup checks artwork codec isolation too. A codec isolation error means the environment isn't ready, even if a catalog page responds.

3. Trust the generated public local certificate using [the Firefox steps below](#trust-the-local-certificate), then open `https://localhost:8443`. Don't import a private key or turn off certificate validation.

4. In the project terminal, read your initial account details:

   ```bash
   cat .local/accounts.txt
   ```

   Choose **Sign in** and enter the Maintainer email and generated password from that file. Keep the terminal output private and out of screenshots, issues, and commits. Registration creates a Viewer; it doesn't grant catalog editing permissions.

### Trust the local certificate

Firefox provides the same certificate import route on Windows, macOS, and Linux. You can install it from [Mozilla's download page](https://www.mozilla.org/firefox/new/) if your usual browser's certificate settings are unfamiliar. This changes trust in that Firefox profile.

1. Locate `.local/local-ca.crt` inside your project. `.local` is a hidden folder. From the project terminal, open it with the command for your platform:

   Windows, inside Ubuntu:

   ```bash
   explorer.exe "$(wslpath -w .local)"
   ```

   macOS:

   ```bash
   open .local
   ```

   Linux:

   ```bash
   xdg-open .local
   ```

   In a Linux file picker, press `Ctrl+H` to show hidden folders. On macOS use `Command+Shift+G` and paste the project folder path followed by `/.local` if needed. In Windows, copy the folder address from Explorer into the import dialog.

2. In Firefox open **Settings**, search for **certificates**, and choose **View Certificates**. Select **Authorities**, then **Import**.
3. Select only the generated public `local-ca.crt` file. Confirm trust for identifying websites when Firefox asks. Never import `.local` private key files or share the folder. Mozilla documents [manual certificate import and trust settings](https://wiki.mozilla.org/CA/Changing_Trust_Settings).
4. Open `https://localhost:8443` again. If a warning remains, check that you imported the certificate from this checkout and that the address is exactly `localhost`. Resolve the warning before signing in.

Local certificates expire after 365 days. Renewal needs manual attention; don't delete valued data just to renew a certificate. If you reset the installation, it creates a new local CA, so remove the old SpoolIn local authority from Firefox and repeat the import. Remove that authority when you're finished using this local installation.

Seed data provides 100 movies and 100 TV entries, plus people, genres, and credits for browsing. It doesn't bundle poster image files; records can show artwork placeholders until you upload an image. Changes you make locally stay in Docker volumes across normal stops and restarts. The [stakeholder guide](stakeholder-guide.md) explains the first journeys to try.

## Everyday commands

```bash
make local-status
make local-logs
make local-stop
make local-start
make local-test
```

`make local-test` runs the setup-helper regression suite, builds the app, runs npm type checks, frontend and MCP coverage tests, and Gradle test tasks. Some backend tests use isolated database fixtures. The separately named `integrationTest` tasks and browser acceptance suites are not part of this command.

Container builds keep Linux dependencies in dedicated Docker volumes, so they preserve any Node dependencies installed on your host. Reset removes these build caches too.

Its temporary backend test container receives access to your local Docker engine so Testcontainers can start and remove isolated PostgreSQL fixtures. Application containers do not receive this socket. Remote Docker endpoints are not supported by this helper.

Status checks service readiness and codec isolation as well as container state. Logs print the most recent 200 lines per service, then return to your terminal. Stop retains the database and artwork. Start brings the same installation back.

To update this PR branch in a checkout without local source edits:

```bash
make local-stop
git pull --ff-only
make local-setup
make local-start
```

If Git reports local modifications, preserve or commit them before updating. Don't use a forced reset to fix an update error.

### Delete local data and start again

`make local-reset` removes this local installation's stored data after its confirmation prompt. This includes accounts, catalog edits, and uploaded artwork. It isn't necessary for a normal restart.

```bash
make local-reset
make local-setup
make local-start
```

Type `DELETE` at the prompt only when you intend to discard the local installation. Keep a backup if the data matters to you.

## Troubleshooting

| What you see | What to check |
| --- | --- |
| `make: command not found` | Install Make for your platform and reopen the terminal. On Windows use Ubuntu. |
| `python3: command not found` | Install Python 3 using your platform steps above. |
| Cannot connect to Docker daemon | Start Docker Desktop, or run `sudo systemctl start docker` on Ubuntu Engine. Check `docker version`. |
| Docker permission denied on Ubuntu | Finish the Docker group step, then log out and back in. |
| `docker` missing inside WSL | Enable Docker Desktop's Ubuntu integration and reopen Ubuntu. |
| Port already in use | Stop the other service using the printed port, then start SpoolIn again. |
| Another local command is running | Wait for the current setup, start, or test command. After an interruption, confirm no local command remains before removing only the empty `.local/action.lock` directory with `rmdir .local/action.lock`. |
| Build download fails | Check your connection, proxy, free disk space, and registry access; retry setup. |
| Page can't be reached | Run `make local-status`, inspect `make local-logs`, and use the exact printed HTTPS address. |
| Artwork codec isolation fails | Preserve the diagnostic. Check Docker's Linux security support and the repository's Media policy; don't disable the sandbox to force a successful start. |
| Sign-in fails | Use the generated local credentials. Registration creates a Viewer, while editing requires a Maintainer. |
| Save outcome is uncertain | Use the page's original-result check before submitting again. |

The [Media isolation policy](release/media-sandbox-policy.md) describes deployment enforcement. The local Media profile uses `systempaths=unconfined` to let Bubblewrap mount a fresh private `/proc`: Docker Desktop's inherited process-filesystem masks blocked that mount in the macOS check. This setting applies only to Media. It retains non-root execution, dropped capabilities, a read-only filesystem, seccomp, and the codec worker's network, PID, user, and mount isolation. Local startup must still verify the codec boundary.

The local profile does not load the production AppArmor policy and is not equivalent to the production environment. Use trusted sample images on your development computer. Local helpers keep deployment configuration separate; don't substitute deployment `make start` for `make local-start`.

## Source development

You can run the whole application in Docker while editing source files. Rerun `make local-start` to build source changes and start the updated services. `make local-setup` prepares configuration; it doesn't build source. For checks directly on the host, install the pinned Node.js `24.21.0`, npm `11.19.0`, and Java 21 toolchains. The repository helper `ops/bin/toolchain-env.sh` recognizes the documented local tool locations; it doesn't install missing toolchains.

From the repository root:

```bash
npm ci
npm run generate
npm run check
npm run build
npm run test:coverage --workspace frontend
./gradlew test
```

Contract generation requires Java and Node; generated interfaces must match the checked-in schemas. `npm run dev --workspace frontend` starts only Vite, not the backend or database. Use the Docker workflow for the complete application.

`make test-unit` and `make test-integration` use the repository's task-aware verification runners. Integration checks require isolated fixture configuration; follow the [verification workflow](07-harness-workflow.md) and never point test helpers at data you want to keep. Running the application doesn't require installing AI clients, workflow hooks, or external chat integrations.

## Verification limits

The local workflow and browser walkthrough were exercised on macOS with Docker Desktop and Docker Engine `29.8.2`, running Linux ARM64 containers. Installer commands were checked against the linked vendor guides; this was not a fresh operating-system installation. Windows/WSL2 and native Ubuntu instructions received source inspection only.

Platform instructions follow the linked vendor installation guides. Successful tool installation depends on your OS version, virtualization settings, and Docker security support. The delivery report records which platforms and application journeys were actually run. Historical release evidence and remaining acceptance work are retained in [the release handoff](release/handoff.md); screenshots alone don't establish every acceptance case.
