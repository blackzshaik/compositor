# Step 02: Multi-Device Bezel Mockups & Form Factors

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Blocked by Step 01  
*Target Module*: `web-viewer`

---

## 1. Objective
Expand the single-device frame component into a modular device frame suite supporting multiple realistic hardware form factors (Pixel, Samsung Galaxy, Foldable, Tablet) and responsive viewport scaling.

---

## 2. Functional Requirements
* **Device Profiles**: Support distinct hardware bezels:
  * Google Pixel 8 (Compact, punchhole camera, rounded edges).
  * Samsung Galaxy S24 Ultra (Sharp corners, edge display styling).
  * Foldable (Galaxy Fold unfolded wide aspect ratio).
  * Android Tablet (10-inch landscape/portrait tablet frame).
* **Frame Switcher Bar**: Dropdown or segmented control to select the active device profile.
* **Canvas Zoom & Pan**:
  * Zoom controls (50%, 75%, 100%, 125%, Fit-to-Screen).
  * Mouse wheel / touchpad zoom and canvas drag-pan.
* **Aspect-Ratio & Density Mapping**: Scale screen dimensions accurately based on standard Android dp/px aspect ratios.

---

## 3. High-Level Architectural Guidance
* Create pure CSS / SVG-based device frames for crisp rendering at all zoom levels without heavy raster asset overhead.
* Support a "Frameless" mode that displays only the rendered UI canvas with a subtle border for minimalists.
* Store the user's preferred device profile in `localStorage`.

---

## 4. Verification & Quality Gates
* **Component Tests**: Test rendering each device profile, verifying bezel dimensions, camera cutout placement, and zoom scaling transforms.
* **Regression Check**: Ensure rendered preview images fit cleanly within all bezels without clipping or overflow.
