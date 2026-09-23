# Flip Timer

A timer that flips between counting up and counting down without resetting,
and alerts you every time it crosses 0:00. Runs fully offline.

## Build with GitHub (no install needed)
1. Create a new GitHub repo and push this folder to it:
   ```
   git init && git add . && git commit -m "Flip Timer"
   git branch -M main
   git remote add origin https://github.com/<you>/flip-timer.git
   git push -u origin main
   ```
2. Open the repo's **Actions** tab and wait for "Build APK" to finish (~5 min).
3. Open the run, download **FlipTimer-apk**, and unzip it.
4. Get `app-debug.apk` onto your phone and open it. Allow "install unknown apps" when asked.

Each CI build is signed with a fresh debug key, so to install a new build
you'll need to uninstall the old one first.

## Build with Android Studio
Open this folder, let Gradle sync, plug in your phone with USB debugging on, and press Run.

## How it works
- The timer is one signed value plus a direction. Flip reverses the direction without resetting.
- It keeps going past zero; negative time shows in red.
- The live time is shown in a notification drawn by Android itself, so nothing
  needs to keep running in the background.
- An exact alarm is scheduled for the next zero crossing and rescheduled on every
  Start, Pause or Flip. Timer state is saved, and it's restored after a reboot.

## Testing checklist
Basics
- [ ] Set 0:10, Start: counts down, alert at 0:00, keeps going into red negative time
- [ ] Flip mid-countdown: direction changes, number doesn't jump
- [ ] Flip in negative time: counts back up, alerts again at 0:00
- [ ] Pause, then Resume: continues from the same value
- [ ] Flip while paused, then Resume: goes the new direction
- [ ] Reset: back to start time, notification disappears

In the background
- [ ] Start, go to home screen: notification shows live time and buttons
- [ ] Pause / Flip / Reset from the notification: app matches when reopened
- [ ] Lock the phone with ~30s left: alert sounds and vibrates at 0:00
- [ ] Leave the screen off for 5+ minutes before a crossing: alert still on time
- [ ] Swipe the app out of recents mid-run: notification and alert still work
- [ ] Restart the phone mid-run: notification comes back after unlock, time is correct

Edge cases
- [ ] Flip rapidly around zero: alert each time it actually crosses, no duplicates
- [ ] Deny notifications: app shows a warning with a button to turn them on
- [ ] Silent / Do Not Disturb mode: note what happens (alerts use the alarm sound stream)

Please report your phone model and Android version with any bugs.
