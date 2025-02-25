<div align="center">

[![App Icon](assets/app_icon.png)](#)
<br>
[![App Name](assets/app_name.png)](#)
<br>
A lyrics plugin for [Poweramp](https://powerampapp.com/) <br>
<br>
<a href="https://developer.android.com/tools/releases/platforms#5.0">
<img alt="API 21+" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=for-the-badge&color=FF0800&logo=android" title="Android 6.0 Marshmallow">
</a>
<a href="https://github.com/abhishekabhi789/LyricsForPowerAmp/releases">
<img alt="GitHub release download count" src="https://img.shields.io/github/downloads/abhishekabhi789/LyricsForPowerAmp/total?style=for-the-badge&color=00C853&logo=github" title="GitHub release download count">
</a>
<a href="https://play.google.com/store/apps/details?id=io.github.abhishekabhi789.lyricsforpoweramp&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme%26utm_campaign%3Ddownload_count">
<img alt="Playstore release download count" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dio.github.abhishekabhi789.lyricsforpoweramp%26gl%3DUS%26hl%3Den%26l%3DDOWNLOADS%26m%3D%24totalinstalls&color=4285F4&logo=googleplay&style=for-the-badge" title="Playstore release download count">
</a>
<br>
<a href="https://play.google.com/store/apps/details?id=io.github.abhishekabhi789.lyricsforpoweramp&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme%26utm_campaign%3Dplay_widget" class="btn" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="240" alt="get it on Google Play"/>
</a>

</div>

### :flower_playing_cards: Screenshots

[![App Screenshots](assets/Screenshots_L4PA.png)](#)

### :shield: Permissions

- **Internet**: Required for fetching lyrics from LRCLib.
- **Notification**: Displays status of lyrics requests.

### :wrench: Setup and Usage

No changes are required in Poweramp settings to use this app.

This app is a lyrics plugin, not a standalone lyrics app.
You cannot directly view lyrics in this plugin or select it as a _Preferred Lyrics App_ in Poweramp
settings.

Poweramp automatically sends a lyrics request when it doesn't have lyrics available for a track.

- #### App settings

    - #### Theme
      You can manually change the app's theme.
      Devices running Android 10+ can choose system default mode to follow system theme.

    - #### Lyrics Request.

        - #### Fallback to Search Method
          If the plugin
          cannot [find the best match](https://lrclib.net/docs#:~:text=Get%20lyrics%20with%20a%20track's%20signature),
          it will fallback to
          a [search method](https://lrclib.net/docs#:~:text=Search%20for%20lyrics%20records) which
          may occasionally retrieve incorrect lyrics.

        - #### Notify on Lyrics Request Failure
          If the lyrics request fails, the app posts a notification. From this notification, you can
          launch the plugin prepopulated with track metadata to perform a manual search and update
          the lyrics.

        - #### Replace previous notification
          Enabling this option replaces the previous notification with a new one, preventing the
          notification panel from being flooded with failure notifications.
          However, this means you can perform manual search only for the most recent failed track.
        - #### Preferred Lyrics Type
          Choose whether the app should always try to send synced or plain lyrics to Poweramp.
          If the chosen type is not available for a track, the other type will be sent if available.

    - #### Filters
      You can specify filters for the title, artist, and album fields to remove matches from the
      corresponding search parameter.
      Filters can be strings or regular expressions.  
      For example, to exclude strings like `320kbps` from track title, enter `\d{3}kbps` in the
      title filter box. This will remove any such matches from the title.


- #### Search Modes

  The app offers two search options:

    * **Coarse Search**: Performs a keyword-based search that yields a broad range of results.
      This method sacrifices accuracy for quantity.

    * **Fine Search**: Focuses on specific fields such as track title, artist name, and album name.
      It provides more relevant results but limits the number of matches returned.

### :information_source: Notes

- Lyrics sent to Poweramp by this plugin are cached by Poweramp and will be available offline.
- Poweramp makes a request only when the track doesn't have any lyrics to display.
- When Poweramp initiates a lyrics request, you'll see the message “Searching lyrics via plugin” in
  the Poweramp lyrics UI.
- For every lyrics request received, the plugin will try to get the best match avilable.
  If this fails and the fallback method is enabled, it performs a fine search operation and selects
  the first result, if available.
- To update lyrics set by this plugin, play the track in Poweramp and open the Lyrics UI.
  At the top, you'll find an icon button(:fast_forward:) next to the plugin name.
  Tap on it to launch the plugin and perform a search to retrieve available lyrics.
  Each lyrics result contains a choose icon(:heavy_check_mark:), upon clicking on it will send the
  lyrics to poweramp.
- To reset/delete all lyrics set by this plugin, perform a full rescan
  `Poweramp Settings → Library → Full Rescan`

### :books: References

1. [Poweramp API](https://github.com/maxmpz/powerampapi)
2. [LRCLib](https://lrclib.net/) - Lyrics Source

### :gift_heart: Support

<a href='https://ko-fi.com/X8X1V9VTH' target='_blank'><img style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi1.png?v=3' border='0' alt='Buy Me a Coffee at ko-fi.com'/></a>
