# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-month-validation: month selector is disabled without year selection
- Location: flows/discovery/discovery.spec.ts:111:1

# Error details

```
Error: browserType.launch: Failed to launch the browser process.
Browser logs:

<launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-9CBKOi -juggler-pipe -silent
<launched> pid=77032
[pid=77032][err] *** You are running in headless mode.
[pid=77032][err] Could not find profile folder.
[pid=77032] <process did exit: exitCode=1, signal=null>
[pid=77032] starting temporary directories cleanup
Call log:
  - <launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-9CBKOi -juggler-pipe -silent
  - <launched> pid=77032
  - [pid=77032][err] *** You are running in headless mode.
  - [pid=77032][err] Could not find profile folder.
  - [pid=77032] <process did exit: exitCode=1, signal=null>
  - [pid=77032] starting temporary directories cleanup
  - [pid=77032] <gracefully close start>
  - [pid=77032] <kill>
  - [pid=77032] <skipped force kill spawnedProcess.killed=false processClosed=true>
  - [pid=77032] finished temporary directories cleanup
  - [pid=77032] <gracefully close end>

```