# XML to JSON Refactoring Plan

## Planning
- [x] Explore all XML files in [values/](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values), [values-port/](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values-port), [values-v31/](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/res/values-v31)
- [x] Map how each XML resource is consumed (Kotlin code, layouts, build.gradle)
- [x] Classify files: migratable vs must-stay-XML
- [x] Analyze existing JSON patterns ([optimal_settings.json](file:///home/vina/Projects/Emuladores/Revenger/app/src/main/assets/optimal_settings.json))
- [x] Check existing test structure
- [x] Write implementation plan
- [x] Get user approval on plan

## Execution (after approval)
- [x] Phase 1: Config files → JSON
- [ ] Phase 2: Gamepad config → JSON
- [ ] Phase 3: RetroMenu3 config → JSON
- [ ] Phase 4: RetroMenu3 strings → JSON
