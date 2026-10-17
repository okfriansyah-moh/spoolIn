# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-empty-catalog: empty catalog state shows browse-again; does not claim failure
- Location: flows/discovery/discovery.spec.ts:172:1

# Error details

```
Error: browserType.launch: Failed to launch the browser process.
Browser logs:

<launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-LAaRlc -juggler-pipe -silent
<launched> pid=77047
[pid=77047][err] *** You are running in headless mode.
[pid=77047][err] Could not find profile folder.
[pid=77047] <process did exit: exitCode=1, signal=null>
[pid=77047] starting temporary directories cleanup
Call log:
  - <launching> /Users/mekari/Library/Caches/ms-playwright/firefox-1555/firefox/Nightly.app/Contents/MacOS/firefox -no-remote -headless -profile /var/folders/f7/yc_mc5ln3mldc644sdg50_680000gn/T/playwright_firefoxdev_profile-LAaRlc -juggler-pipe -silent
  - <launched> pid=77047
  - [pid=77047][err] *** You are running in headless mode.
  - [pid=77047][err] Could not find profile folder.
  - [pid=77047] <process did exit: exitCode=1, signal=null>
  - [pid=77047] starting temporary directories cleanup
  - [pid=77047] <gracefully close start>
  - [pid=77047] <kill>
  - [pid=77047] <skipped force kill spawnedProcess.killed=false processClosed=true>
  - [pid=77047] finished temporary directories cleanup
  - [pid=77047] <gracefully close end>

```