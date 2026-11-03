# Flip Timer

A productivity timer that flips between counting up and counting down without
resetting, alerts you every time it crosses 0:00, tracks tasks with deadlines,
and logs where each day ended. Runs fully offline.

## Updating from version 1
Version 1 builds were signed with a random key each time. This version uses a
fixed key (`app/flip-debug.keystore`), so **uninstall version 1 once**, then install
this. Every future build installs as a normal update and keeps your data.
Keep the repo private if you'd rather not publish the key.

The workflow file lives in the hidden `.github` folder. If you upload through
the GitHub website, check it made it (or recreate it as before).

## Install
Every push to `main` runs the unit tests, builds, and publishes a Release.
On your phone, open the repo's **Releases** page, tap the latest
`FlipTimer.apk`, and install it.

## Build with Android Studio
Open the folder, let Gradle sync, plug in your phone, press Run.

## Code layout
```
core/        Pure Kotlin logic, no Android. Fully unit-tested.
  TimerState    timer as a line: value = base + direction × elapsed
  Task          tasks, progress, completion, list ordering
  Reminders     reminder presets and "next reminder" calculation
  DayLog        which day-end values still need recording
  TimeFormat    all display formatting
data/        Storage. Room database for tasks and day log; prefs for the timer.
system/      Android plumbing: one alarm helper, notifications, broadcast receivers.
controller/  The only places state changes. Each change saves, then refreshes
             alarms and notifications, so those can never drift out of sync.
ui/          Compose screens: Timer, Tasks, History.
FlipTimerApp Wires everything together once (AppGraph).
```

## How it works
- **Timer:** one signed value plus a direction. The notification's live clock is
  drawn by Android, so nothing runs in the background. An exact alarm is set
  for the next zero crossing whenever the timer changes.
- **Day log:** before every timer change (and at midnight), the app records the
  value the timer had at each midnight since the last record. Because the timer
  moves in a straight line between changes, this is exact even if the phone
  was off at midnight.
- **Task reminders:** each task keeps one alarm, for its next reminder. When it
  fires, the next one is scheduled. Defaults: 24h, 6h, 1h before, and at the
  deadline; change them per task under ⋮ → Reminders.
- **Repeating tasks:** a repeating task stays done until its deadline, then resets
  to 0% with the next deadline. It resets at the deadline even if unfinished.
  Deadlines step in local time from a fixed starting point, so "monthly on the
  31st" stays on month-ends and 9 AM stays 9 AM across daylight saving.

## Testing checklist
Timer
- [ ] Start time dialog: set −0:30 (Negative chip), Start, then Flip so it counts up: alert at 0:00
- [ ] Earlier checks still pass: flip without jumping, alerts in the background, notification buttons

Tasks
- [ ] Add a task with no deadline, and one due in ~2 hours
- [ ] The 2-hour task shows on the Timer page with a live countdown; the other doesn't
- [ ] Set a task due in ~70 minutes: 1-hour reminder arrives ~10 minutes later
- [ ] Reminders… menu: turn on "15 minutes before" for a task due in 20 min, it fires
- [ ] Edit a deadline after a reminder was scheduled: old reminder doesn't fire
- [ ] Slide progress to 100%: task moves to Completed, its reminders stop
- [ ] Reopen from the ⋮ menu: back in the active list
- [ ] Clear all completed asks for confirmation, then removes them
- [ ] Overdue task shows red "overdue …" on both pages
- [ ] Restart the phone: task reminders still arrive

Repeating tasks and countdowns
- [ ] Existing tasks survived the update (database migration)
- [ ] Every task with a deadline shows a large live d/h/m/s countdown
- [ ] Make a task repeat daily, due in 3 minutes, mark it complete: it shows "Done · resets in"
- [ ] At the deadline it resets to 0% with tomorrow's deadline
- [ ] Repeating task left unfinished: "due now" reminder, then it resets
- [ ] "Clear all" completed never deletes repeating tasks

History
- [ ] The day after installing, yesterday's ending value appears
- [ ] Leave the timer running overnight: logged value matches what it read at midnight

Please report your phone model and Android version with any bugs.
