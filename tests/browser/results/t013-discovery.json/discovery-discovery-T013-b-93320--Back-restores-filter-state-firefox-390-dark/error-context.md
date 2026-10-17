# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-back-restores-criteria: browser Back restores filter state
- Location: flows/discovery/discovery.spec.ts:273:1

# Error details

```
Error: browserType.launch: Failed to launch the browser process.
Browser logs:

<launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-pNSDaY -juggler-pipe -silent
<launched> pid=77086
[pid=77086][err] *** You are running in headless mode.
[pid=77086][err] Could not find profile folder.
[pid=77086] <process did exit: exitCode=1, signal=null>
[pid=77086] starting temporary directories cleanup
Call log:
  - <launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-pNSDaY -juggler-pipe -silent
  - <launched> pid=77086
  - [pid=77086][err] *** You are running in headless mode.
  - [pid=77086][err] Could not find profile folder.
  - [pid=77086] <process did exit: exitCode=1, signal=null>
  - [pid=77086] starting temporary directories cleanup
  - [pid=77086] <gracefully close start>
  - [pid=77086] <kill>
  - [pid=77086] <skipped force kill spawnedProcess.killed=false processClosed=true>
  - [pid=77086] finished temporary directories cleanup
  - [pid=77086] <gracefully close end>

```