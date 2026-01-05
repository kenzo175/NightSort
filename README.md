# 📦 NightSort — Paper Container Autosorter

![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper-blue?style=for-the-badge&logo=paper)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**NightSort** is a lightweight, safe, and highly optimized Paper plugin that automatically sorts container inventories for players. Engineered for high performance and absolute safety against item duplication.

---

## ✨ Features

* **⚡ Smart Sorting** — Automatically organizes inventories when a player opens or closes a container.
* **🚀 High Performance**:
    * **Idle-scheduling**: Groups burst activity into a single optimized sort cycle.
    * **Async Processing**: Offloads heavy sorting logic to asynchronous threads using lightweight snapshots.
    * **Bounded Concurrency**: Uses queuing and semaphores to prevent CPU spikes during high server load.
* **🛡️ Dupe Protection** — Implements multi-set verification and hash-based quick-checks to ensure item safety.
* **🎨 Modern UI** — Native support for **MiniMessage** and Legacy/HEX color codes (`&`, `&#RRGGBB`).
* **📂 Clean Architecture** — Native Paper integration using Lifecycle and Brigadier APIs (no legacy `plugin.yml`).

---

## 🛠 Compatibility

| Requirement | Value |
| :--- | :--- |
| **Server Software** | Paper (or forks) 1.20.x / 1.21.x |
| **Java Version** | Java 17 or higher |
| **API Surface** | Paper-only (Uses native Paper-exclusive APIs) |

---

## 🎮 Commands & Permissions

| Command | Alias | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/nightsort` | `/sort` | Toggle autosorting for yourself | `nightsort.toggle` |

> [!IMPORTANT]
> These commands are player-only. The console will be notified if it attempts to execute them.

---

## 📥 Installation

1. Build the plugin JAR using `mvn clean package`.
2. Drop `NightSort-x.y.z.jar` into your server's `plugins/` folder.
3. Ensure the `paper-plugin.yml` is present in the resources (already pre-configured in the repo).
4. Start or restart your server.
5. On the first run, the plugin will generate:
   * `plugins/NightSort/messages.yml` — Localization and messages.
   * `plugins/NightSort/active.txt` — Simple flat-file storage for active users.

---

## ⚙️ For Developers

The project follows **SOLID** principles and a clean service-oriented architecture:
* **Core**: Handles async sorting logic and job orchestration.
* **Storage**: Modular storage interface for player data.
* **Service Layer**: Decoupled logic for messaging, scheduling, and debouncing.

```bash
# Clone and build
git clone [https://github.com/kenzo175/NightSort.git](https://github.com/kenzo175/NightSort.git)
cd NightSort
mvn clean package
