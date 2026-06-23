## 1. Localization & Keys

- [x] 1.1 Simplify German translation for `sync.label.PENDING` key in `LanguageManager.java` to `"Sync ausstehend"`.
- [x] 1.2 Simplify English translation for `sync.label.PENDING` key in `LanguageManager.java` to `"Pending Sync"`.
- [x] 1.3 Add new statusbar and HUD translation keys in `LanguageManager.java` (`sb.addsibling`, `sb.editdesc`, `hud.shape`).

## 2. Statusbar View & Controller Binding

- [x] 2.1 Correct layout and keys in the bottom `<HBox fx:id="statusbar">` of `main-view.fxml` (Tab ➔ Add child, Enter ➔ Add Sibling, Space ➔ Description).
- [x] 2.2 Update `@FXML` statusbar label declarations in `MainController.java` (replace `sbCycle` with `sbAddSibling`, add `sbEditDesc`).
- [x] 2.3 Set localized texts for the updated statusbar labels in `applyLanguage()` inside `MainController.java`.

## 3. HUD Color and Shape Grid

- [x] 3.1 Replace color swatch `HBox` with a wrapping `FlowPane` in the `MainController.java` HUD builder.
- [x] 3.2 Add a shape selector using `FlowPane` in the `MainController.java` HUD builder, showing all 8 standard shapes and mapping click events to node updates.
- [x] 3.3 Polish slide-out toggle tab visual borders and hover text-fill styles.
