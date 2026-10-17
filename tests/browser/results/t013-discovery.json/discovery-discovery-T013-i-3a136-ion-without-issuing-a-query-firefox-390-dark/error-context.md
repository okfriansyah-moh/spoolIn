# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-invalid: 201-code-point text retains full input and shows correction without issuing a query
- Location: flows/discovery/discovery.spec.ts:62:1

# Error details

```
Error: browserType.launch: Failed to launch the browser process.
Browser logs:

<launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-YnB7rD -juggler-pipe -silent
<launched> pid=77014
[pid=77014][err] *** You are running in headless mode.
[pid=77014][err] Could not find profile folder.
[pid=77014] <process did exit: exitCode=1, signal=null>
[pid=77014] starting temporary directories cleanup
Call log:
  - <launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-YnB7rD -juggler-pipe -silent
  - <launched> pid=77014
  - [pid=77014][err] *** You are running in headless mode.
  - [pid=77014][err] Could not find profile folder.
  - [pid=77014] <process did exit: exitCode=1, signal=null>
  - [pid=77014] starting temporary directories cleanup
  - [pid=77014] <gracefully close start>
  - [pid=77014] <kill>
  - [pid=77014] <skipped force kill spawnedProcess.killed=false processClosed=true>
  - [pid=77014] finished temporary directories cleanup
  - [pid=77014] <gracefully close end>

```