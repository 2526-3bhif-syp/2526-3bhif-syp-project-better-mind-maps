## 1. Selection Styling

- [x] 1.1 Update `MainController.createNodeView` to change the selection highlight (`isCurrent`) to use a 2px stroke with `#8b5cf6` (violet) and a hard `DropShadow` (e.g., `radius=0`, `offsetY=4`).

## 2. Mouse Event Remapping

- [x] 2.1 Update `MainController.createNodeView` so double-click on a node calls `promptEditNode` instead of `showDescriptionPopup`.

## 3. Keyboard Remapping & Sibling Creation

- [x] 3.1 In `MainController.java`, implement `promptAddSibling` which creates a new node attached to the current node's parent.
- [x] 3.2 Update `handleKeyPress` to map `KeyCode.TAB` to `promptAddChild`.
- [x] 3.3 Update `handleKeyPress` to map `KeyCode.ENTER` and `KeyCode.INSERT` to `promptAddSibling`.
- [x] 3.4 Update `handleKeyPress` to map `KeyCode.SPACE` to `showEditDescriptionDialog`.

## 4. Deletion Confirmation

- [x] 4.1 Extract the deletion logic into a reusable method `confirmAndDeleteNode(MindMap map, Pane canvas, Node node)` that shows an `Alert` (`AlertType.CONFIRMATION`) before deleting.
- [x] 4.2 Update `handleKeyPress` so that both `KeyCode.DELETE` and `KeyCode.BACK_SPACE` trigger `confirmAndDeleteNode`.
- [x] 4.3 Update the `deleteNode` ContextMenu action in `createNodeView` to use `confirmAndDeleteNode`.
