# ChestSorter

![Fabric API](https://img.shields.io/badge/Mod%20Loader-Fabric-blue?style=flat-square)
![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2-green?style=flat-square)

**ChestSorter** is a lightweight logistics mod that helps you organize chests and items in-game by creating connected chest networks.

---

## Features

* **Visualizing & Gizmos**: In-world box highlights and dynamic arrow path rendering for active sender/receiver networks using a custom client gizmo manager.
* **Dynamic Panel GUI Layout**: Automatically resizes and re-centers slot layouts based on active configuration states and filters.
* **Filter Modes & Quantities**: Support for multiple filter modes (`ONLY`, `EXCEPT`, `BURN`) with scroll-wheel limit adjustments, shift-scrolling, and direct keyboard entry.
* **Authority & Permissions**: Fine-grained player access control for receiver networks, allowing owners to assign granular permissions (`Gizmos`, `Filter`, `Settings`, `Authority`, `Connect`) to guests.
* **Intuitive Tool Controls**:
* **Crouch + RMB**: Target a sender chest to begin network editing or manage links.
* **RMB**: Select receivers during linking, open receiver configurations, or manage security settings.
* **LMB on Filter Slot**: Set, clear, or cycle item filter rules inside the configuration GUI.
* **Scroll Wheel / Ctrl + Click**: Adjust item stack limits or cycle advanced filter rules (including Burn mode protection warnings).


* **Server Synchronized**: Fully multiplayer compatible with optimized custom network payloads for server-side state, sync, and permission validations.

---

## How to Use

**1. Connecting Receivers to a Sender**

1. Hold the Chest Linker and **Crouch + RMB** on your **Sender Chest** to start the connection session.
2. **RMB** on each **Receiver Chest** you want to link to this sender.
3. **RMB** on the **Sender Chest** again to finalize and save the connection.

**2. Configuring Receiver Filters**

1. **RMB** directly on any **Receiver Chest** to open its filter configuration GUI.
2. Inside the GUI:
* **LMB** with an item on a slot to set a filter rule (or click with an empty hand to clear it).
* **Scroll Wheel** or type numbers over a filter slot to adjust item limits.
* **Ctrl + Left-Click** to cycle filter behavior modes (`ONLY`, `EXCEPT`, `BURN`).



**3. Managing Authority & Settings**

1. Open a receiver's configuration GUI and click the **Settings (⚙)** button.
2. Access the **Authority** menu to view trusted players and toggle specific permission flags per user.

---

## Author's Note

This is my very first Minecraft mod!

I designed the core mechanics, UI layouts, and user interactions.
I used AI in a development process, writing the Java code and navigating the Fabric API ecosystem.

Because the mod is currently in **Alpha**, your feedback and bug reports are hugely appreciated! If you run into any issues or have feature suggestions, please feel free to open an issue on the [GitHub Issues](https://www.google.com/search?q=../../issues) tab.

**P.S.** If you check out the code and spot anything inefficient, un-idiomatic, or broken, constructive feedback is always welcome! I'm actively learning Java and Fabric development as I go, so advice, suggestions, and pull requests are more than welcome. :)

---

## License

This project is licensed under the MIT License - feel free to build upon it or include it in your modpacks!