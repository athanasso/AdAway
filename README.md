# ![AdAway logo](app/src/main/res/mipmap-mdpi/icon.png) AdAway Enhanced (@athanasso fork)

[![GitHub Release](https://img.shields.io/github/v/release/athanasso/AdAway?logo=github)](https://github.com/athanasso/AdAway/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/athanasso/AdAway/total?logo=github)](https://github.com/athanasso/AdAway/releases)
[![Get in Obtainium](https://img.shields.io/badge/Get%20in-Obtainium-blue)](obtainium://add/https://github.com/athanasso/AdAway)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-athanasso-yellow?logo=buy-me-a-coffee)](https://buymeacoffee.com/athanasso)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](/LICENSE.md)
[![Upstream: AdAway/AdAway](https://img.shields.io/badge/Upstream-AdAway%2FAdAway-lightgrey.svg)](https://github.com/AdAway/AdAway)

> [!NOTE]
> **AdAway Enhanced:**
> This repository is an enhanced, actively maintained fork of the official [AdAway](https://github.com/AdAway/AdAway) project, developed by [@athanasso](https://github.com/athanasso).
> In compliance with the GNU General Public License v3 (§5a), this modified version documents key enhancements, features, and fixes applied on top of upstream AdAway.

AdAway Enhanced is an open source ad blocker for Android using the hosts file and local VPN.

### Key Fork Enhancements & Features

- **Custom Upstream DNS**: Pick your preferred DNS resolver in VPN mode (Cloudflare 1.1.1.1, Quad9 9.9.9.9, AdGuard DNS, or custom IP address).
- **Curated Blocklist Presets**: Built-in 1-tap blocklist catalog in Sources (Hâgezi Multi Light, OISD, StevenBlack, AdGuard DNS filter, Dan Pollock).
- **Temporary Snooze & Notification Color Fix**: 5-minute pause action in notification with automatic resumption alarm; status-aware notification tinting (green when active, amber when paused).
- **1-Tap DNS Log Quick Actions**: Tap any DNS log entry for quick Block/Allow dialog and instant undo snackbar feedback.
- **Subdomain & Wildcard VPN Matching**: Full support for wildcard patterns (`*` and `?`) and parent domain matching in VPN mode.
- **Material 3 Modernized UI**: Clean rounded card layout, integrated metrics iconography, high-contrast typography, and seamless status bar styling.
- **Real-Time DNS Log Search**: Search bar filter in DNS Request Log activity with instant matching across domain names.
- **DNS Request Log Export**: One-tap share and export action in DNS log to easily pipe domains to blocklists or clipboard.
- **Launcher App Shortcuts with Back Stack**: Quick-access app launcher shortcuts (DNS Logs, Your Lists, Hosts Sources, Preferences) with proper backstack navigation.
- **Dynamic Filter Download**: Bundled `hosts.txt` asset removed. Rules are downloaded dynamically on startup, guaranteeing up-to-date hosts.
- **Android 14+ Receiver Compatibility**: Fixed download complete `BroadcastReceiver` registration by explicitly specifying `RECEIVER_EXPORTED` on API 34+.
- **VPN State Broadcast Sync**: Added `VpnStatusBroadcastReceiver` to `VpnModel` so the home UI reflects active VPN state changes in real time.
- **Performance Optimization**: Eliminated synchronous SharedPreferences disk reads from Application.onCreate on the main thread.

[<img src="metadata/en-US/phoneScreenshots/screenshot1.png"
    alt="Home screen"
    height="256">](metadata/en-US/phoneScreenshots/screenshot1.png)
[<img src="metadata/en-US/phoneScreenshots/screenshot2.png"
    alt="Preferences screen"
    height="256">](metadata/en-US/phoneScreenshots/screenshot2.png)
[<img src="metadata/en-US/phoneScreenshots/screenshot3.png"
    alt="Root based ad blocker screen"
    height="256">](metadata/en-US/phoneScreenshots/screenshot3.png)
[<img src="metadata/en-US/phoneScreenshots/screenshot4.png"
    alt="Backup and restore screen"
    height="256">](metadata/en-US/phoneScreenshots/screenshot4.png)
[<img src="metadata/en-US/phoneScreenshots/screenshot5.png"
    alt="Help screen"
    height="256">](metadata/en-US/phoneScreenshots/screenshot5.png)

For more information visit https://adaway.org

## Installing

### Fork Releases (Recommended)

Pre-built signed release APKs are available directly on [GitHub Releases](https://github.com/athanasso/AdAway/releases).

[![Get in Obtainium](https://img.shields.io/badge/Get%20in-Obtainium-blue?style=for-the-badge)](obtainium://add/https://github.com/athanasso/AdAway)

### Upstream Releases

There are two kinds of release:
* The preview builds: on the bleeding edge of development - for testers or adventurous
* The stable builds: ready for every day usage - for end users

### Preview builds

**Requirements:** Android 8 _Oreo_ or above

For users with bugs, there may be preview builds available from the [XDA development thread](https://forum.xda-developers.com/showthread.php?t=2190753) and [AdAway official website](https://app.adaway.org/beta.apk).
It is recommended to try those builds to see if your issue is resolved before creating an issue.
The preview builds may contain bug fixes or new features for new android versions.

[<img src="Resources/get-it-on-adaway.png"
      alt="Get it on official AdAway website"
      height="80">](https://app.adaway.org/beta.apk)
[<img src="Resources/XDADevelopers.png"
      raw="true"
      alt="Get it on XDA forum"
      height="60">](https://forum.xda-developers.com/showthread.php?t=2190753)

### Stable builds

**Requirements:**
* Android Android 8 _Oreo_ or above

After preview builds have been tested by the more technical or responsive community within the forums, we will then post the stable build to F-Droid.

[<img src="Resources/get-it-on-adaway.png"
    alt="Get it on official AdAway website"
    height="80">](https://app.adaway.org/adaway.apk)
[<img src="Resources/get-it-on-fdroid.png"
      raw="true"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/app/org.adaway)

For devices older than Android 8 _Oreo_, use the version 4 of AdAway.

## Get Host File Sources

See the [Wiki](https://github.com/AdAway/AdAway/wiki), in particular the page [HostsSources](https://github.com/AdAway/AdAway/wiki/HostsSources) for an assorted list of sources you can use in AdAway.
Add the ones you like to the AdAway "Hosts sources" section.

## Getting Help

For issues specific to this fork, please open an issue on [athanasso/AdAway Issues](https://github.com/athanasso/AdAway/issues).
For upstream discussions, you can post [Upstream Issues](https://github.com/AdAway/AdAway/issues) or obtain community support via the [XDA developer thread](http://forum.xda-developers.com/showthread.php?t=2190753).

## Contributing

You want to be involved in the project? Welcome onboard!  
Check [the contributing guide](CONTRIBUTING.md) to learn how to report bugs, suggest features and make you first code contribution :+1:

If you are looking for translating the application in your language, [the translating guide](TRANSLATING.md) is for you.

## Project Status & Credits

This fork is maintained by:
* Manos Athanassoglou ([@athanasso](https://github.com/athanasso)) - Fork maintainer | [Buy Me a Coffee](https://buymeacoffee.com/athanasso)

The upstream project is actively developed by:
* Bruce Bujon ([@PerfectSlayer](https://github.com/PerfectSlayer)) - Developer  
[PayPal](https://paypal.me/BruceBUJON) | [GitHub Sponsorship](https://github.com/sponsors/PerfectSlayer)
* Daniel Mönch ([@Vankog](https://github.com/Vankog)) - Translations
* Jawz101 ([@jawz101](https://github.com/jawz101)) - Hosts list
* Anxhelo Lushka ([@AnXh3L0](https://github.com/AnXh3L0)) - Web site

We do not forget the past maintainers:
* Dāvis Mošenkovs ([@DavisNT](https://github.com/DavisNT)) - Developer  
[Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=5GUHNXYE58RZS&lc=US&item_name=AdAway%20Donation&no_note=0&no_shipping=1)
* [@0-kaladin](https://github.com/0-kaladin) - Developer and XDA OP
* Sanjay Govind ([@sanjay900](https://github.com/sanjay900)) - Developer

And we thank a lot to the original author:
* Dominik Schürmann ([@dschuermann](https://github.com/dschuermann)) - Original developer  
[Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=android%40schuermann.eu&lc=US&item_name=AdAway%20Donation&no_note=0&no_shipping=1&currency_code=EUR) | [Flattr](https://flattr.com/thing/369138/AdAway-Ad-blocker-for-Android) | BTC: `173kZxbkKuvnF5fa5b7t21kqU5XfEvvwTs`

## Permissions

AdAway requires the following permissions:

* `INTERNET` to download hosts files and application updates. It can send bug reports and telemetry [if the user wants to (opt-in only)](https://github.com/AdAway/AdAway/wiki/Telemetry)
* `ACCESS_NETWORK_STATE` to restart VPN on network connection change
* `RECEIVE_BOOT_COMPLETED` to start the VPN on boot
* `FOREGROUND_SERVICE` to run the VPN service in foreground
* `POST_NOTIFICATIONS` to post notifications about hosts source update, application update and VPN controls. All notifications can be enabled or disabled independently.
* `REQUEST_INSTALL_PACKAGES` to update the application using the builtin updater
* `QUERY_ALL_PACKAGES` to let the user pick the applications to exclude from VPN

## Licenses

AdAway is licensed under the GPLv3+.  
The file LICENSE includes the full license text.
For more details, check [the license notes](LICENSE.md).
