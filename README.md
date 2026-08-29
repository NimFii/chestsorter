# ChestSorter

![Fabric API](https://img.shields.io/badge/Mod%20Loader-Fabric-blue?style=flat-square)
![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2-green?style=flat-square)

**ChestSorter** is a lightweight, procedural item logistics mod for Minecraft. It introduces customizable receiver filter configurations, allowing players to precise control how items are sorted and handled across their chest networks.

---

## Features

* **Dynamic Panel GUI Layout**: Automatically resizes and re-centers slot layouts based on the total configured filter slots (from single-row to multi-row setups).
* **Intuitive Tool Controls**:
    * **Crouch + Scroll Wheel**: Toggle modes while holding the Chest Linker (`CONNECT`, `DISCONNECT`, `CONFIGURE`).
    * **Crouch + RMB**: Target a sender/input chest to begin a linking or unlinking session.
    * **RMB**: Select receiver chests during linking/unlinking, or finish a linking session on the sender chest.
    * **LMB on Filter Slot**: Set or clear item filter rules inside the configuration GUI.
    * **Scroll Wheel on Filter Slot**: Adjust item stack limits (+1 / -1, or ±16 while holding Shift).
* **Server Synchronized**: Fully multiplayer compatible with optimized custom network payloads for server-side filter validation.

---

## How to Use

**1. Connecting Receivers to a Sender (One-to-Many Sorting)**
1. Hold the Chest Linker and **Crouch + Scroll Wheel** to select **`CONNECT`** mode.
2. **Crouch + RMB** on your **Sender (Input) Chest** to start the connection session.
3. **RMB** on each **Receiver Chest** you want to link to this sender (you can connect multiple receivers at once).
4. **RMB** on the **Sender Chest** again to finalize and save the connection.

**2. Disconnecting Receivers**
1. Hold the Chest Linker and **Crouch + Scroll Wheel** to select **`DISCONNECT`** mode.
2. **Crouch + RMB** on the **Sender Chest** to start the disconnection session.
3. **RMB** on the **Receiver Chest(s)** you wish to unlink.
4. **RMB** on the **Sender Chest** again to finalize the disconnection.

**3. Configuring Receiver Filters**
* *No need to interact with the sender chest first!*
1. Switch to **`CONFIGURE`** mode using **Crouch + Scroll Wheel**.
2. **RMB** directly on any **Receiver Chest** to open its filter configuration GUI.
3. Inside the GUI:
    * **LMB** with an item on a slot to set a filter rule (or click with an empty hand to clear it).
    * **Scroll Wheel** over a filter slot to increase or decrease item limits.
    * **Shift + Scroll Wheel** to adjust stack limits in steps of 16.
---

## Author's Note

This is my very first Minecraft mod!

I designed the core mechanics, UI layouts, and user interactions.
I used AI in a development process, writing the Java code and navigating the Fabric API ecosystem.

Because the mod is currently in **Alpha**, your feedback and bug reports are hugely appreciated! If you run into any issues or have feature suggestions, please feel free to open an issue on the [GitHub Issues](../../issues) tab.

**P.S.** If you check out the code and spot anything inefficient, un-idiomatic, or broken, constructive feedback is always welcome! I'm actively learning Java and Fabric development as I go, so advice, suggestions, and pull requests are more than welcome. :)

---

## License

This project is licensed under the MIT License - feel free to build upon it or include it in your modpacks!