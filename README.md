Privacy Protection Pro 🛡️

A simple yet powerful and modern firewall and DNS changer for Android.

Privacy Protection Pro gives you control over your device's network traffic without requiring root access. Block unwanted ads, trackers, and malicious sites with a custom DNS, or take full control and block specific apps from accessing the internet altogether.

✨ Features

    Dual-Mode Operation: Switch between two powerful modes with a single tap.

        DNS-Only Mode: Use any custom DNS provider to block ads, trackers, and phishing sites across your entire device. Works seamlessly with services like AdGuard DNS, Cloudflare, and more.

        Firewall Mode: A robust, per-app internet blocker. By default, all apps are allowed internet access. Simply toggle the switch next to any app to add it to the blocklist and instantly cut its connection.

    Persistent Rules: The app remembers your firewall settings. Your blocklist is saved and automatically reapplied whenever you restart the service or your device.

    Modern & Efficient:

        Built with 100% Kotlin and the latest Jetpack Compose for a smooth, responsive UI.

        Utilizes Android's native VpnService for high performance and low battery impact.

        Targets the latest Android 16 (SDK 36) for modern security and compatibility.

    No Root Required: All functionality works on standard, un-rooted Android devices.

🛠️ How It Works

The app leverages Android's VpnService to create a local tunnel on your device. All traffic is processed locally, and nothing is sent to a remote server.

    In DNS-Only Mode, the service simply tells the Android OS to use your selected custom DNS server. No traffic is actively routed or filtered by the app itself, making it extremely lightweight.

    In Firewall Mode, the service creates a "black hole" by routing all traffic to the local VPN. It then uses the addDisallowedApplication() API to "punch holes" for the apps you want to allow. Any app not on the "allow" list is forced into the VPN and has its traffic dropped, effectively blocking its internet access.

💻 Tech Stack

    Language: Kotlin

    UI: Jetpack Compose with Material 3

    Core: Android VpnService

    Database: Room for saving custom DNS servers

    Asynchronous: Kotlin Coroutines & Flow
