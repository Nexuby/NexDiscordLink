# NexDiscordLink

[![Build](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml/badge.svg)](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml)

🌐 [English documentation](README.md)

NexDiscordLink; Minecraft oyuncularının Discord hesaplarını güvenli biçimde eşleştiren, rol ve kullanıcı adı senkronizasyonu sağlayan, iki aşamalı doğrulama, sohbet köprüsü, ödül sistemi ve proxy ağı desteği sunan Spigot/Paper eklentisidir.

## Özellikler

- Discord DM, etkileşimli modal veya slash komutuyla hesap eşleme
- Türkçe ve İngilizce Minecraft komutları
- Türkçe ve İngilizce Discord slash komutları
- Tek Minecraft hesabı ↔ tek Discord hesabı politikası
- Süreli, tek kullanımlık ve hız sınırlamalı eşleme kodları
- TOTP tabanlı 2FA ve oyun içi QR haritası
- IP değişikliğinde bağlı Discord hesabından giriş doğrulaması
- 2FA veya Discord düğmesiyle güvenli eşleme kaldırma
- Minecraft/Vault → Discord, Discord → Vault veya çift yönlü rol senkronizasyonu
- Girişte, belirli aralıklarla veya yönetici komutuyla rol senkronizasyonu
- Discord kullanıcı adı ve Minecraft kullanıcı adı senkronizasyonu
- İlk eşleme, yeniden eşleme ve Discord rolüne özel ödüller
- Çevrimiçi bağlı oyunculara periyodik maaş ödülü
- Discord boost ödülleri
- Çift yönlü Minecraft–Discord sohbet köprüsü
- Katılma, ayrılma, ölüm ve başarım bildirimleri
- Mesaj bazında Discord embed görünümü, kanal yönlendirme, görsel ve olay açma/kapatma ayarları
- Hassas verileri maskeleyen Discord denetim kayıtları
- İsteğe bağlı, izin listeli Discord konsol komutu
- SQLite, MySQL ve ortak MySQL kullanan Velocity/Bungee ağı desteği
- PlaceholderAPI entegrasyonu
- Türkçe ve İngilizce dil dosyaları

## Gereksinimler

- Java 17 veya üzeri
- Spigot/Paper 1.16.5 veya üzeri
- Discord botu
- Rol senkronizasyonu için isteğe bağlı Vault ve uyumlu bir yetki eklentisi
- Placeholder kullanımı için isteğe bağlı PlaceholderAPI
- Proxy modu için bütün backend sunucularının erişebildiği MySQL veritabanı

Discord Developer Portal üzerinden bot için `Server Members Intent` ve `Message Content Intent` seçeneklerini etkinleştirin.

Kullanılan özelliklere göre bot rolünde `View Channels`, `Send Messages`, `Embed Links`, `Manage Roles`, `Manage Nicknames` ve ban senkronizasyonu açıksa `Ban Members` izinleri bulunmalıdır. Bot rolü, yöneteceği rollerin üzerinde olmalıdır.

## Kurulum

1. `NexDiscordLink-<sürüm>.jar` dosyasını sunucunun `plugins` klasörüne kopyalayın.
2. Sunucuyu bir kez başlatıp yapılandırma dosyalarının oluşmasını bekleyin.
3. Sunucuyu tamamen durdurun.
4. `plugins/NexDiscordLink/config.yml` içindeki bot tokenini, kanal ve rol kimliklerini yapılandırın.
5. Türkçe kullanım için `settings.language: "tr"` ayarlayın.
6. Discord Developer Portal üzerinden gerekli intent ve bot izinlerini etkinleştirin.
7. Sunucuyu yeniden başlatın.
8. Modal eşleme kullanıyorsanız Discord üzerinde `/setup-link` komutunu çalıştırın.

Canlı bot tokenini, veritabanı parolasını, webhook adresini, `secret.key` dosyasını veya çalışan sunucunun yapılandırmasını GitHub'a göndermeyin.

## Hızlı yapılandırma

### Tek sunucu

Varsayılan SQLite kurulumu tek sunucu için yeterlidir:

```yaml
settings:
  language: "tr"

proxy:
  enabled: false

database-settings:
  type: "sqlite"

link-system:
  type: "BOTH"
  code-length: 6
  code-expiry-minutes: 5
```

`link-system.type` seçenekleri:

- `DM`: Kod Discord botuna özel mesajla gönderilir.
- `MODAL`: Kod, Discord bağlantı penceresine girilir.
- `BOTH`: İki yöntem de kullanılabilir.

### MySQL

```yaml
database-settings:
  type: "mysql"
  host: "127.0.0.1"
  port: 3306
  database: "nexdiscordlink"
  username: "nexdiscordlink"
  password: "GUCLU_BIR_PAROLA"
```

Veritabanı tabloları ve gerekli yeni sütunlar başlangıç sırasında otomatik oluşturulur. Veritabanı bağlantısı kurulamazsa eklenti tutarsız veri üretmemek için kendisini devre dışı bırakır.

## Proxy ağı kurulumu

Velocity veya BungeeCord ağında tüm backend sunucularında aynı MySQL bilgilerini kullanın. SQLite proxy modunda kabul edilmez.

Botun çalışacağı ana node:

```yaml
proxy:
  enabled: true
  server-id: "lobby-1"
  bot-enabled: true
  link-check-interval-ticks: 40
```

Diğer backend node'ları:

```yaml
proxy:
  enabled: true
  server-id: "survival-1"
  bot-enabled: false
  link-check-interval-ticks: 40
```

Her `server-id` benzersiz olmalıdır. Eşleme kodları ortak MySQL tablosunda süreli tutulur. Bot başka bir node'da kodu çözdüğünde kodu oluşturan backend, çevrimiçi oyuncuya bildirimi, ödülü ve rol senkronizasyonunu varsayılan olarak iki saniye içinde uygular.

IP değişikliği doğrulaması ve Discord üzerinden eşleme kaldırma onayı gibi anlık DM gerektiren özellikler botun çalıştığı node'a ihtiyaç duyar. Bot kapalı worker node'larında 2FA kullanın veya bu güvenlik akışlarını ağ mimarinize göre yapılandırın.

## Minecraft komutları

| Ana komut | Türkçe alternatifler | Açıklama |
| --- | --- | --- |
| `/link` | `/eşle`, `/hesapeşle`, `/esle`, `/hesapesle` | Süreli hesap eşleme kodu oluşturur. |
| `/unlink [2FA kodu]` | `/eşlemesil`, `/eslemesil`, `/hesapayır`, `/hesapayir` | Hesap eşlemesini güvenli biçimde kaldırır. |
| `/linkstatus` | `/eşledurum`, `/esledurum`, `/hesabım`, `/hesabim` | Discord hesabını, eşleme tarihini ve 2FA durumunu gösterir. |
| `/linkreward` | `/eşleödül`, `/esleodul`, `/ödül`, `/odul` | Eşleme ödüllerini ve kalan ödül hakkını gösterir. |
| `/2fa setup` | `/ikifaktör kur`, `/ikifaktor kur` | 2FA kurulumunu başlatır ve QR haritası verir. |
| `/2fa login <kod>` | `/ikifaktör giriş <kod>`, `/ikifaktor giris <kod>` | Giriş doğrulamasını tamamlar. |
| `/2fa disable <kod>` | `/ikifaktör kapat <kod>`, `/ikifaktor kapat <kod>` | 2FA'yı doğrulama koduyla kapatır. |
| `/nexdiscord reload` | `/discordyönet yenile`, `/discordyonet yenile` | Yapılandırmayı, veritabanını, zamanlayıcıları ve botu yeniler. |
| `/nexdiscord status` | `/discordyönet durum`, `/discordyonet kontrol` | Veritabanı, bot, Discord sunucusu ve güvenlik durumunu gösterir. |
| `/nexdiscord sync [oyuncu\|all]` | `/discordyönet senkronize [oyuncu\|tümü]` | Rol senkronizasyonunu hemen başlatır. |
| `/nexdiscord resetreward <oyuncu\|all>` | `/discordyönet ödülsıfırla <oyuncu\|all>` | Eşleme ödülü sayaçlarını sıfırlar. |

## Discord komutları

| Komut | Açıklama |
| --- | --- |
| `/eşle kod:<kod>` / `/link code:<code>` | Minecraft hesabını eşler. |
| `/hesap` / `/profile` | Eşlenen Minecraft profilini gösterir. |
| `/eşlemeyi-kaldır` / `/unlink` | Hesap eşlemesini Discord tarafından kaldırır. |
| `/yardım` / `/help` | Kullanılabilir hesap komutlarını gösterir. |
| `/setup-link` | Modal eşleme mesajını seçili kanalda yayımlar. |
| `/console command:<komut>` | Yalnızca açık ve izinli olduğunda sunucu komutu çalıştırır. |

Discord hesap komutlarının yanıtları gizli/ephemeral olarak gönderilir.

## Yetkiler

| Yetki | Varsayılan | Açıklama |
| --- | --- | --- |
| `nexdiscord.link` | Herkes | Hesap eşleme kodu oluşturma |
| `nexdiscord.unlink` | Herkes | Hesap eşlemesini kaldırma |
| `nexdiscord.2fa` | Herkes | 2FA yönetimi |
| `nexdiscord.status` | Herkes | Kendi hesap durumunu görüntüleme |
| `nexdiscord.reward.preview` | Herkes | Eşleme ödüllerini görüntüleme |
| `nexdiscord.reload` | OP | Yapılandırmayı yenileme |
| `nexdiscord.admin` | OP | Durum, senkronizasyon ve ödül yönetimi |
| `nexdiscord.update.notify` | OP | Güncelleme bildirimi alma |

## Rol senkronizasyonu

```yaml
sync:
  linked-role:
    enabled: true
    role-id: "DISCORD_ROL_ID"
  role-sync:
    enabled: true
    direction: "MINECRAFT_TO_DISCORD"
    interval-minutes: 5
    vault-groups:
      vip: "VIP_DISCORD_ROL_ID"
      mvp: "MVP_DISCORD_ROL_ID"
```

`direction` seçenekleri:

- `MINECRAFT_TO_DISCORD`: Vault grubu Discord rolünü belirler.
- `DISCORD_TO_MINECRAFT`: Discord rolü Vault grubunu belirler.
- `BIDIRECTIONAL`: Eşlenen Discord rolü varsa Discord tarafı önceliklidir; eşlenen rol yoksa Minecraft grubu Discord'a uygulanır.

Eklenti yalnızca `vault-groups` altında tanımlanan grupları ve rolleri yönetir.

## Ödül sistemi

Ödüller konsol komutlarıyla tanımlandığı için ekonomi, eşya, XP veya başka eklentilerin komutları birlikte kullanılabilir:

```yaml
rewards:
  link-rewards:
    enabled: true
    limit: 1
    commands:
      - "eco give {player} 2000"
      - "give {player} golden_apple 2"
    first-link-commands:
      - "experience add {player} 250 points"
    relink-commands: []
    discord-role-commands:
      "DESTEKCI_DISCORD_ROL_ID":
        - "eco give {player} 1000"
```

- `commands`: Her başarılı ve ödüle uygun eşlemede çalışır.
- `first-link-commands`: Oyuncunun ilk ödüllü eşlemesinde çalışır.
- `relink-commands`: Sonraki ödüllü eşlemelerde çalışır.
- `discord-role-commands`: Oyuncunun sahip olduğu Discord rolüne göre ek komut çalıştırır.
- `limit: 0`: Ödül alma sınırını kaldırır.

## Discord mesajlarını özelleştirme

Mesaj metinleri `plugins/NexDiscordLink/lang/messages_tr.yml` ve `messages_en.yml` dosyalarından değiştirilmeye devam eder. `config.yml` içindeki `discord-messages` bölümü; katılma, ayrılma, ölüm, başarım, eşleme paneli, giriş güvenliği, profil, denetim kaydı ve Minecraft'tan Discord'a sohbet mesajlarının görünümünü ve kanalını yönetir.

```yaml
discord-messages:
  defaults:
    timestamp: true
    footer:
      enabled: true
      text: "oyna.ornek.net"
      icon-url: "https://ornek.net/icon.png"

  events:
    join:
      enabled: true
      channel-id: "" # Boşsa channels.log-channel-id kullanılır
      title: "Sunucuya hoş geldin"
      description: "**{player}** aramıza katıldı!"
      color: "#2F80ED"
      author:
        enabled: true
        text: "{player}"
        icon-url: "https://mc-heads.net/avatar/{player}"
      thumbnail:
        enabled: true
        url: "https://mc-heads.net/avatar/{player}"
      image:
        enabled: false
        url: "https://ornek.net/karsilama.png"
```

Renkler `GREEN` gibi isimle, `#2F80ED` gibi HEX koduyla veya `47, 128, 237` biçiminde RGB olarak yazılabilir. Yalnızca HTTP/HTTPS görsel adresleri kabul edilir. Geçersiz renklerde olayın yerleşik rengi kullanılır, geçersiz görsel adresleri yok sayılır ve metinler Discord alan sınırlarına güvenli biçimde kısaltılır.

Ortak yer tutucular `{player}` ve `{uuid}` değerleridir. Ölüm mesajında `{death_message}`, başarımda `{advancement}`, özel giriş doğrulama mesajında `{ip}`, sohbet köprüsünde `{message}` kullanılabilir. Denetim şablonlarının başlık veya açıklamasında `{event}`, `{actor}` ve `{detail}` kullanılabilir.

Boş bırakılan `title`, `description`, düğme etiketi ve alan etiketleri etkin dil dosyasındaki metni korur. Her olay ayrı bir `channel-id` değerine yönlendirilebilir; boş değer mevcut log kanalı ayarını kullanır. Webhook dahil bütün sohbet mesajlarında Discord mention çözümleme güvenlik amacıyla kapalıdır.

Yapılandırma ve dil değişikliklerini `/nexdiscord reload` veya `/discordyönet yenile` ile uygulayın. Daha önce yayımlanan eşleme paneli Discord üzerinde geriye dönük değiştirilmediği için panel görünümünü değiştirdikten sonra `/setup-link` komutunu yeniden çalıştırın.

## PlaceholderAPI

PlaceholderAPI kuruluysa genişletme otomatik kaydolur:

| Placeholder | Değer |
| --- | --- |
| `%nexdiscord_linked%` | Hesabın bağlı olup olmadığı (`true`/`false`) |
| `%nexdiscord_discord_id%` | Bağlı Discord kullanıcı kimliği |
| `%nexdiscord_discord_username%` | Önbellekteki Discord kullanıcı adı |
| `%nexdiscord_2fa_enabled%` | 2FA durumu (`true`/`false`) |
| `%nexdiscord_linked_at%` | Eşleme tarihi |
| `%nexdiscord_reward_count%` | Alınan eşleme ödülü sayısı |

Placeholder değerleri sık kullanılan TAB ve scoreboard sistemlerinin veritabanını yormaması için asenkron olarak yenilenen 30 saniyelik önbellekten sunulur. İlk sorguda değer kısa süreliğine boş veya varsayılan olabilir.

## Güvenlik

### Hassas veri koruması

- TOTP gizli anahtarları AES-GCM ile şifrelenir.
- IP adresleri geri döndürülemeyen, anahtarlı HMAC parmak izi olarak saklanır.
- Denetim kayıtlarında IP, token, secret ve key değerleri otomatik maskelenir.
- Eşleme kodu ve 2FA doğrulamaları hız sınırına tabidir.

İlk başlangıçta `plugins/NexDiscordLink/secret.key` oluşturulur. Bu dosyayı güvenli biçimde yedekleyin. Kaybolması veya değiştirilmesi mevcut şifreli 2FA anahtarlarının okunamamasına neden olur.

Yönetilen kurulumlarda dosya yerine Base64 kodlu 32 baytlık anahtar kullanılabilir:

```text
NEXDISCORDLINK_MASTER_KEY=<BASE64_KODLU_32_BAYTLIK_ANAHTAR>
```

Anahtarı veya gerçek değerini GitHub'a göndermeyin.

### Güvenli eşleme kaldırma

- 2FA etkinse `/unlink <kod>` kullanılması gerekir.
- 2FA etkin değilse bağlı Discord hesabına iki dakika geçerli onay düğmesi gönderilir.
- Onay tokeni tek kullanımlıktır ve yalnızca bağlı Discord hesabı tarafından kullanılabilir.
- Discord slash komutundan gelen `/unlink`, bağlı Discord hesabının doğrudan onayı sayılır.

### Denetim kayıtları

Eşleme, eşleme kaldırma, 2FA, başarısız doğrulama, IP doğrulaması, rol değişimi ve yönetici işlemleri Discord kanalına kaydedilebilir:

```yaml
audit-log:
  enabled: true
  channel-id: "" # Boşsa channels.log-channel-id kullanılır
  console: false
```

### Discord konsol komutu

`/console` tam sunucu erişimi sağlayabildiği için varsayılan olarak kapalıdır:

```yaml
console-command:
  enabled: false
  whitelist:
    - "say"
    - "list"
    - "tps"
```

Yalnızca kök komutu izin listesinde bulunan komutlar çalıştırılır. Boş izin listesi bütün komutları reddeder. Güvenilir Discord yöneticileri olmayan sunucularda bu özelliği açmayın.

## Derleme ve test

Windows:

```powershell
.\gradlew.bat clean test shadowJar
```

Linux/macOS:

```bash
./gradlew clean test shadowJar
```

Dağıtıma hazır gölgeli JAR şu konumda oluşturulur:

```text
build/libs/NexDiscordLink-<sürüm>.jar
```

Her push için GitHub Actions üzerinde temiz derleme ve test çalıştırılır.

## Güncelleme

1. Sunucuyu tamamen durdurun.
2. Mevcut JAR, `config.yml`, `database.db` veya MySQL veritabanı ve `secret.key` dosyasını yedekleyin.
3. Eski JAR'ı yeni sürümle değiştirin.
4. Sunucuyu yeniden başlatın ve başlangıç kayıtlarını kontrol edin.
5. `/discordyönet durum` komutuyla veritabanı ve bot durumunu doğrulayın.

JAR güncellemelerinde `/reload` veya benzeri genel sunucu yenileme komutlarını kullanmayın. Eklentinin kendi `/discordyönet yenile` komutu yapılandırma değişiklikleri içindir.

Mevcut dil dosyaları silinmek zorunda değildir; yeni mesaj anahtarları JAR içindeki güncel varsayılanlardan otomatik alınır.

## Sorun giderme

- **Bot başlamıyor:** Tokeni, privileged intent seçeneklerini ve ağ erişimini kontrol edin.
- **Slash komutları görünmüyor:** Önce eklentinin başarıyla başladığını ve botun doğru uygulama hesabına ait olduğunu doğrulayın.
- **Roller değişmiyor:** Bot rol sırasını, `Manage Roles` iznini, Vault bağlantısını ve rol kimliklerini kontrol edin.
- **Proxy modu başlamıyor:** Proxy modunda `database-settings.type` mutlaka `mysql` olmalıdır.
- **Eşleme kodu kabul edilmiyor:** Kodun süresi dolmuş olabilir; Minecraft üzerinde yeniden `/eşle` çalıştırın.
- **Discord DM gelmiyor:** Kullanıcının sunucu üyelerinden özel mesaj kabul ettiğini ve node üzerinde botun etkin olduğunu kontrol edin.
- **2FA verileri okunamıyor:** Doğru `secret.key` veya `NEXDISCORDLINK_MASTER_KEY` değerinin kullanıldığını doğrulayın.

## Lisans

NexDiscordLink [MIT Lisansı](LICENSE) ile yayımlanır.
