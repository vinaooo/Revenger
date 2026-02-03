# Multi-Slot Save System - Testing Guide

## Status: Ready for Testing
**Branch:** `feature/multi-slot-save-implementation`  
**Commits:** 4  
**Build Status:** ✅ BUILD SUCCESSFUL

---

## Phase Completion Summary

### ✅ Phase 1-5: Core Implementation (COMPLETE)
- SaveSlotData model
- SaveStateManager with CRUD operations
- ScreenshotCaptureUtil for game screenshots
- UI layouts (3x3 grid, dialogs, drawables)
- Fragment classes (SaveStateGridFragment, SaveSlotsFragment, LoadSlotsFragment, ManageSavesFragment)

### ✅ Phase 6: ProgressFragment Integration (COMPLETE)
- Added "Manage Saves" menu item to Progress menu
- Implemented navigation methods and listener callbacks
- Dynamic menuItems list (3-4 items based on save state)

### ✅ Phase 7: Navigation & Screenshot Support (COMPLETE)
- LEFT/RIGHT navigation for 2D grid traversal
- Screenshot capture enabled using ScreenshotCaptureUtil
- Complete D-PAD/keyboard support for grid navigation

---

## Testing Checklist

### 1. Build and Install
```bash
cd /home/vina/Projects/Emuladores/Revenger
./gradlew clean assembleDebug installDebug
```

Expected result: APK installed on device/emulator

---

### 2. Basic Menu Navigation

#### 2.1 Access Progress Menu
1. Launch game (any ROM)
2. Press START button to open main menu
3. Navigate to "Progress" option
4. Press A/Enter to confirm

**Expected:**
- Progress menu shows 3 items: Save State, Manage Saves, Back
- If a save exists, Load State appears (4 items total)

#### 2.2 Navigate to Manage Saves
1. In Progress menu, navigate DOWN to "Manage Saves"
2. Press A/Enter

**Expected:**
- ManageSavesFragment appears with 3x3 grid
- Grid shows 9 empty slots initially
- Back button at bottom

---

### 3. Save Functionality Testing

#### 3.1 Save to Slot 1 (First Save)
1. In Progress menu, select "Save State" (quick save)
2. Press A/Enter

**Expected:**
- Game pauses (or continues if configured)
- Save completes silently
- Progress menu refreshes to show Load State option (now 4 items)

#### 3.2 Save to Multiple Slots
1. In Progress menu, navigate to "Manage Saves"
2. Press A/Enter to open save slots grid
3. Navigate to slot 1 (top-left) using D-PAD
4. Press A/Enter
5. Enter custom name (e.g., "Level 1-1 Start")
6. Confirm save

**Expected:**
- Naming dialog appears with EditText
- Custom name is accepted
- Slot 1 now shows screenshot + name + timestamp
- Grid refreshes to show occupied slot

7. Repeat for slots 2, 3, etc. with different names

**Test Cases:**
- Save with empty name → should use default "Save Slot X"
- Save with long name (50+ chars) → should truncate
- Save to occupied slot → should show overwrite confirmation dialog

---

### 4. Load Functionality Testing

#### 4.1 Load from Slot (via Manage Saves)
1. In Progress menu, select "Manage Saves"
2. Open Load Slots grid
3. Navigate to occupied slot (e.g., slot 1)
4. Press A/Enter

**Expected:**
- Load confirmation (if implemented)
- Game state restored to saved state
- Menu closes automatically
- Game resumes from saved point

#### 4.2 Load from Empty Slot (Error Handling)
1. Open Load Slots grid
2. Navigate to empty slot
3. Press A/Enter

**Expected:**
- Error message: "Cannot load from empty slot"
- No state change
- User remains in Load Slots grid

---

### 5. Grid Navigation Testing

#### 5.1 2D Navigation
Test grid layout (slots 1-9):
```
[1] [2] [3]
[4] [5] [6]
[7] [8] [9]
[Back Button]
```

**D-PAD/Keyboard Controls:**
- UP: move up (bounded, stops at row 1)
- DOWN: move down (bounded, wraps to Back button from row 3)
- LEFT: move left (bounded, stops at column 1)
- RIGHT: move right (bounded, stops at column 3)
- A/Enter: confirm selection
- B/Escape: back to Progress menu

**Test Scenarios:**
1. From slot 1, press LEFT → should stay at slot 1 (bounded)
2. From slot 3, press RIGHT → should stay at slot 3 (bounded)
3. From slot 1, press UP → should stay at slot 1 (bounded)
4. From slot 7, press DOWN → should move to Back button
5. From Back button, press UP → should move to slot 7
6. From slot 5 (center), navigate all directions → verify correct movement

---

### 6. Save Management Operations

#### 6.1 Copy Slot
1. In Manage Saves grid, navigate to occupied slot (source)
2. Press A/Enter
3. Select "Copy" from operations dialog
4. Navigate to destination slot (empty or occupied)
5. Press A/Enter
6. Confirm overwrite if destination is occupied

**Expected:**
- Source slot remains unchanged
- Destination slot now has identical save (same screenshot, name, but new timestamp)

#### 6.2 Move Slot
1. Select occupied slot (source)
2. Choose "Move" operation
3. Select destination slot
4. Confirm

**Expected:**
- Source slot becomes empty
- Destination slot has the moved save
- Timestamp updated

#### 6.3 Delete Slot
1. Select occupied slot
2. Choose "Delete" operation
3. Confirm deletion

**Expected:**
- Slot becomes empty
- Screenshot and metadata files deleted from storage
- Grid refreshes

#### 6.4 Rename Slot
1. Select occupied slot
2. Choose "Rename" operation
3. Enter new name
4. Confirm

**Expected:**
- Slot name updated
- Metadata.json updated
- Screenshot and state data unchanged

---

### 7. Screenshot Verification

#### 7.1 Screenshot Capture
1. Play game to a visually distinct scene (e.g., title screen, specific level)
2. Open menu (game pauses, screenshot captured)
3. Save to new slot
4. Close menu, navigate to different scene
5. Open menu again, view Manage Saves → Load Slots

**Expected:**
- Saved slot shows captured screenshot (120dp x 100dp)
- Screenshot reflects game state at time of save
- No black borders (cropped automatically)

#### 7.2 Screenshot Persistence
1. Save to slot with screenshot
2. Close game completely
3. Relaunch game
4. Open Manage Saves → Load Slots

**Expected:**
- Screenshot still visible in slot
- File exists at: `/files/saves/slot_X/screenshot.webp`

---

### 8. Legacy Save Migration

#### 8.1 First Launch with Legacy Save
**Setup:**
1. Manually place legacy save file at:
   `/data/data/com.vinaooo.revenger.{config_id}/files/state`
2. Launch game

**Expected:**
- SaveStateManager.migrateLegacySaveIfNeeded() executes
- Legacy save migrated to `/files/saves/slot_1/state.bin`
- Metadata generated with default name "Migrated Save"
- Legacy file deleted
- Load State appears in Progress menu (hasSaveState = true)

#### 8.2 Migration with Existing Slot 1
**Setup:**
1. Save to slot 1 normally
2. Manually place legacy save at `/files/state`
3. Restart game

**Expected:**
- Migration skipped (preserves newer slot_1 data)
- Legacy file remains or deleted (check implementation)

---

### 9. Performance Testing

#### 9.1 Save/Load Speed
1. Create large save states (long gameplay session)
2. Measure time from confirm to completion

**Acceptance Criteria:**
- Save completes in < 2 seconds
- Load completes in < 2 seconds
- Screenshot capture < 500ms

#### 9.2 Grid Rendering
1. Fill all 9 slots with saves + screenshots
2. Open Load Slots grid

**Expected:**
- Grid renders instantly (< 100ms)
- No lag when navigating between slots
- No memory leaks (verify with Android Profiler)

---

### 10. Edge Cases & Error Handling

#### 10.1 Disk Space
1. Fill device storage to near capacity
2. Attempt to save

**Expected:**
- Error message if save fails
- No partial saves (atomic operation)

#### 10.2 Corrupted Metadata
1. Manually edit `/files/saves/slot_1/metadata.json` with invalid JSON
2. Open Load Slots

**Expected:**
- Slot shows "Empty" or error indicator
- App doesn't crash

#### 10.3 Missing Screenshot
1. Delete `/files/saves/slot_1/screenshot.webp`
2. Open Load Slots

**Expected:**
- Slot shows placeholder or default icon
- Metadata still displays (name, timestamp)

#### 10.4 State/Metadata Mismatch
1. Delete `state.bin` but keep `metadata.json`
2. Attempt to load slot

**Expected:**
- Load fails gracefully
- Error message displayed

---

## Automated Testing Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Check for compilation errors
./gradlew compileDebugKotlin

# Generate test coverage report
./gradlew jacocoTestReport
```

---

## Known Limitations

1. **Screenshot Timing:** Screenshot captured when menu opens, not when save button pressed
   - Workaround: Screenshot reflects current game state accurately
   
2. **No Cloud Sync:** Saves stored locally only
   
3. **No Export/Import:** Cannot transfer saves between devices (future feature)

4. **9 Slot Limit:** Grid hardcoded to 3x3 (can be expanded in future)

---

## Troubleshooting

### Issue: "No cached screenshot available"
**Solution:** Screenshot capture requires menu to open first (triggers PixelCopy). Save immediately after opening menu.

### Issue: Grid navigation doesn't respond to D-PAD
**Solution:** Verify KeyboardInputAdapter is sending LEFT/RIGHT events (check logcat for `[KEY_DOWN] Arrow LEFT`).

### Issue: Save fails silently
**Solution:** Check logcat for errors. Verify RetroView.serializeState() returns non-null data.

### Issue: Load doesn't restore state
**Solution:** Verify slot has valid `state.bin` file. Check SaveStateManager logs.

---

## Success Criteria

- ✅ All 9 slots can save/load successfully
- ✅ Screenshots captured and displayed correctly
- ✅ Copy/move/delete/rename operations work
- ✅ 2D grid navigation smooth and intuitive
- ✅ Legacy migration works on first launch
- ✅ No crashes, no data corruption
- ✅ Performance acceptable (< 2s save/load)

---

## Next Steps After Testing

1. **Fix Bugs:** Address any issues found during testing
2. **Optimization:** Improve screenshot capture if lag detected
3. **Documentation:** Update user manual with new features
4. **PR Creation:** Merge `feature/multi-slot-save-implementation` → `develop`
5. **Release Notes:** Document new multi-slot save system for changelog

---

**Testing Status:** 🟡 PENDING MANUAL VALIDATION  
**Last Updated:** 2025-02-02  
**Tester:** [Your Name]
