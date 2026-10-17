# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-no-episode-controls: title detail has no episode tracking controls
- Location: flows/discovery/discovery.spec.ts:226:1

# Error details

```
Error: browserType.launch: Failed to launch the browser process.
Browser logs:

<launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-Ja791D -juggler-pipe -silent
<launched> pid=77061
[pid=77061][err] *** You are running in headless mode.
[pid=77061][err] Could not find profile folder.
[pid=77061] <process did exit: exitCode=1, signal=null>
[pid=77061] starting temporary directories cleanup
Call log:
  - <launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-Ja791D -juggler-pipe -silent
  - <launched> pid=77061
  - [pid=77061][err] *** You are running in headless mode.
  - [pid=77061][err] Could not find profile folder.
  - [pid=77061] <process did exit: exitCode=1, signal=null>
  - [pid=77061] starting temporary directories cleanup
  - [pid=77061] <gracefully close start>
  - [pid=77061] <kill>
  - [pid=77061] <skipped force kill spawnedProcess.killed=false processClosed=true>
  - [pid=77061] finished temporary directories cleanup
  - [pid=77061] <gracefully close end>

```