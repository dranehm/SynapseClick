# Synapse Click ⚡

**Synapse Click** is a modern, high-performance Android Accessibility Auto-Clicker built to execute massive geometric touch sequences dynamically across heavily fragmented Android devices. It natively interfaces with Android OS Accessibility Service bounds via `WindowManager` protocols. 

## 🚀 Key Features

* **Advanced Layout Overlays:** Features a collapsible, fully draggable HUD. Re-architected in pure codebase logic with dynamic `GradientDrawable` arrays and `RippleDrawable` touch-handlers rendering seamlessly rounded boxes without polluting the standard XML pipelines. 
* **Dynamic Geometrics (Swipes):** Integrates an automated DP sequence calculator natively overriding generic swipe behavior. The app extracts point-to-point Vector pixel distances via the Pythagorean theorem, natively dividing out active screen densities and mapping them out to flawless 800 DP/s humanized Smooth Scrolls. 
* **Isolated Sequencing Delays:** Built around an intelligent Sequence loop logic where users can modify Intra-Node internal "Action Delays" entirely independently of the global Loop-Restart Interval via dynamically bound StateFlow inputs.

## 🛠️ Usage Permissions 

To successfully leverage Synapse Click upon initial setup, the following permissions must be explicitly approved via the Android `Settings` router:

1. **Draw Over Other Apps (`SYSTEM_ALERT_WINDOW`):** Grants the application permission to render the control HUD persistently above fullscreen games or browser bounds. 
2. **Accessibility Service:** Required by Android 8.0+ security specifications to actually execute simulated touch outputs and structural gesture pathing on your behalf. 

## 🔧 Building The Software

Synapse Click actively employs standard `gradlew` architectures. 

To configure a debug build locally or compile raw deployment `.apk` files, you can utilize any CLI wrapper targeting the primary gradle manifest:

```bash
# Debug Deploy
./gradlew assembleDebug

# Secure Keystore Release Deploy
./gradlew assembleRelease
```
