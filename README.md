<p align="center">
  <!-- <img src="logo.png" alt="Turkish Profanity Detection Logo" width="200"/> -->
  <h1 align="center">Turkish Profanity Detection Plugin</h1>
  <p align="center">
    <b>Minecraft sunucunuz için gelişmiş Türkçe küfür ve hakaret tespit sistemi</b><br>
    <small>Versiyon: 0.0.3 | Minecraft 1.16+</small>
  </p>
</p>

<p align="center">
  <a href="#-özellikler">Özellikler</a> •
  <a href="#-kurulum">Kurulum</a> •
  <a href="#-yapılandırma">Yapılandırma</a> •
  <a href="#-komutlar">Komutlar</a> •
  <a href="#-izinler">İzinler</a> •
  <a href="#-api-kullanımı">API Kullanımı</a> •
  <a href="#-sık-sorulan-sorular">SSS</a> •
  <a href="#-sürüm-notları">Sürüm Notları</a>
</p>

---

## 📋 Genel Bakış

Turkish Profanity Detection, Minecraft sunucunuzda Türkçe küfür ve hakaret içeren mesajları yapay zeka destekli bir API ile tespit edip filtreleyebilen gelişmiş bir eklentidir. Oyuncularınıza temiz ve güvenli bir oyun ortamı sağlarken, karmaşık ve gizlenmiş küfürleri de tespit edebilir.

## ✨ Özellikler

### Temel Özellikler
- **Türkçe Küfür Tespiti**: Türkçe metinlerde küfür ve hakaret içeriklerini yüksek doğrulukla tespit eder
- **Yapay Zeka Desteği**: Gelişmiş AI modelleri kullanarak karmaşık veya gizlenmiş küfürleri tespit edebilir
- **Şiddet Seviyesi Sınıflandırması**: Tespit edilen küfürleri şiddet seviyesine göre 1-5 arası sınıflandırır

### Moderasyon Araçları
- **Yapılandırılabilir Eylemler**: Küfür tespit edildiğinde yapılacak işlemleri özelleştirebilme
- **Otomatik Cezalandırma**: Şiddet seviyesine göre otomatik uyarı, susturma veya yasaklama yapabilme
- **Admin Bildirimleri**: Yetkililere küfür tespitlerini anında bildirebilme
- **Admin GUI**: Küfür tespit istatistiklerini görüntülemek için kullanıcı dostu arayüz

### Entegrasyon ve Depolama
- **Discord Webhook Entegrasyonu**: Küfür tespitlerini Discord kanalınıza iletebilme
- **Veritabanı Desteği**: İstatistiklerin MySQL veritabanında kalıcı olarak saklanması
- **PlaceholderAPI Desteği**: Küfür istatistiklerini diğer eklentilerde kullanabilme

### Performans ve Güvenlik
- **Düşük Gecikme**: Sohbet mesajlarını minimum gecikme ile işleme
- **Hız Sınırlaması**: Spam koruma sistemi ile hızlı mesaj gönderimi engelleme
- **İzin Sistemi**: Kapsamlı izin sistemi ile eklenti özelliklerinin kontrolü

## 🔧 Kurulum

### Gereksinimler
- Minecraft 1.16 veya üzeri
- Spigot, Paper veya uyumlu bir sunucu yazılımı
- PlaceholderAPI (isteğe bağlı)
- MySQL veritabanı (istatistiklerin kalıcı olması için önerilir)

### Adımlar
1. Eklenti dosyasını (`TurkishProfanityDetection.jar`) sunucunuzun `plugins` klasörüne yükleyin
2. Sunucuyu yeniden başlatın veya `/reload` komutunu çalıştırın
3. İlk çalıştırmada otomatik olarak oluşturulan `config.yml` dosyasını ihtiyaçlarınıza göre düzenleyin
4. MySQL kullanmak istiyorsanız, veritabanı bağlantı bilgilerini yapılandırın
5. Discord webhook entegrasyonu için webhook URL'sini yapılandırın (isteğe bağlı)
6. Eklentiyi `/tpd reload` komutu ile yeniden yükleyin

## ⚙️ Yapılandırma

Eklenti ilk yüklendiğinde otomatik olarak bir `config.yml` dosyası oluşturulur. Ana yapılandırma bölümleri şunlardır:

### API Ayarları

```yaml
api:
  url: "https://api.kynux.cloud/api/swear/detect"  # Küfür tespit API'sinin URL'si
  timeout: 30000  # API isteği zaman aşımı (milisaniye)
  
  # Yapay zeka ayarları
  ai:
    use: true  # Yapay zeka kullanılsın mı?
    model: "gpt-4.5"  # Kullanılacak AI modeli
    confidence: 0.1  # Minimum güven seviyesi (0.0-1.0)
```

### Eylem Ayarları

```yaml
actions:
  cancel-message: true  # Küfür içeren mesajları engellesin mi?
  
  # Log işlemleri
  log:
    enabled: true  # Küfür tespitlerini loglasın mı?
    console: true  # Konsola yazdırsın mı?
    file: true  # Dosyaya yazdırsın mı?
    file-path: "logs/profanity.log"  # Log dosyası yolu
  
  # Discord webhook entegrasyonu
  discord:
    enabled: false
    webhook-url: ""  # Discord webhook URL'si
    username: "Küfür Koruması"
    avatar-url: ""  # Webhook avatar URL'si
    embed-color: "#FF0000"
    notify-severity-level: 3  # Bu seviye ve üzeri küfürler Discord'a bildirilecek (1-5)
  
  # Küfür tespit edildiğinde çalıştırılacak komutlar
  commands:
    enabled: true
    list:
      - "warn %player% Uygunsuz dil kullanımı"
      # - "tempban %player% 1h Küfür/Hakaret"
```

### Şiddet Seviyesi Eylemler

```yaml
severity-actions:
  enabled: true
  levels:
    1:  # Hafif küfürler
      commands:
        - "warn %player% Hafif uygunsuz dil kullanımı"
    # ... 2, 3, 4 seviyeleri
    5:  # Çok ağır küfürler
      commands:
        - "warn %player% Çok ağır küfür kullanımı"
        - "tempban %player% 1h Küfür/Hakaret"
```

### Veritabanı Ayarları

```yaml
statistics:
  enabled: true
  storage-type: "mysql"  # mysql, sqlite veya file
  data-retention-days: 30  # 0 = sınırsız
  
  # MySQL ayarları
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "password"
    table-prefix: "tpd_"
```

Daha fazla yapılandırma seçeneği için `config.yml` dosyasını inceleyebilirsiniz.

## 🔍 Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/turkishprofanity` veya `/tpd` | Ana komut | `turkishprofanitydetection.commands` |
| `/tpd help` | Komut yardımını gösterir | `turkishprofanitydetection.commands` |
| `/tpd reload` | Eklentiyi yeniden yükler | `turkishprofanitydetection.admin` |
| `/tpd version` | Eklenti sürümünü gösterir | `turkishprofanitydetection.commands` |
| `/tpd stats [oyuncu]` | Bir oyuncunun küfür istatistiklerini gösterir | `turkishprofanitydetection.statistics` |
| `/tpd clear [oyuncu]` | Bir oyuncunun küfür istatistiklerini temizler | `turkishprofanitydetection.admin` |
| `/tpd admin` | Admin GUI'yi açar | `turkishprofanitydetection.admin` |

## 🔒 İzinler

| İzin | Açıklama | Varsayılan |
|------|----------|------------|
| `turkishprofanitydetection.bypass` | Mesajları kontrolden muaf tutar | op |
| `turkishprofanitydetection.admin` | Admin komutlarını ve GUI'yi kullanma izni | op |
| `turkishprofanitydetection.commands` | Komutları kullanma izni | op |
| `turkishprofanitydetection.statistics` | İstatistikleri görüntüleme izni | op |

## 🔌 API Kullanımı

### PlaceholderAPI Entegrasyonu

Eklenti PlaceholderAPI ile entegre çalışır ve aşağıdaki placeholder'ları sunar:

| Placeholder | Açıklama |
|-------------|----------|
| `%tpd_count%` | Oyuncunun toplam küfür sayısı |
| `%tpd_last_word%` | Son tespit edilen küfür kelimesi |
| `%tpd_last_category%` | Son tespit edilen küfürün kategorisi |
| `%tpd_last_level%` | Son tespit edilen küfürün şiddet seviyesi |
| `%tpd_total_server%` | Sunucudaki toplam küfür sayısı |
| `%tpd_top_player%` | En çok küfür eden oyuncu |

### Diğer Eklentilerle Entegrasyon

Bu eklentiyi diğer eklentilerle entegre etmek için API sınıflarını kullanabilirsiniz:

```java
// Eklentiyi almak
TurkishProfanityDetection tpd = (TurkishProfanityDetection) Bukkit.getPluginManager().getPlugin("TurkishProfanityDetection");

// Bir metinde küfür olup olmadığını kontrol etmek
ProfanityApiService apiService = tpd.getApiService();
apiService.checkMessage(message, player.getName(), result -> {
    if (result.isContainsProfanity()) {
        // Küfür tespit edildi
        // Özel işlemler uygulayabilirsiniz
    }
});
```

## ❓ Sık Sorulan Sorular

### Eklenti sunucumu yavaşlatır mı?
Hayır, eklenti asenkron işlem kullanarak ana sunucu iş parçacığını bloke etmez. API istekleri arka planda gerçekleşir.

### Eklenti internetsiz çalışır mı?
Hayır, küfür tespiti için API'ye erişim gereklidir. İnternet olmadan eklenti temel işlevselliğini kaybeder.

### Veritabanı kullanmak zorunlu mu?
Hayır, istatistikler dosya tabanlı olarak da saklanabilir. Ancak MySQL kullanmak performans ve veri güvenliği açısından önerilir.

### Küfür tespitinde hata var, ne yapmalıyım?
API'nin yanlış tespitleri olabilir. Bu durumda loglara bakıp sorunları Kynux Discord sunucusu üzerinden bildirebilirsiniz.

### PlaceholderAPI zorunlu mu?
Hayır, eklenti PlaceholderAPI olmadan da çalışır, ancak placeholder'ları kullanmak için PlaceholderAPI gereklidir.

## 📝 Sorun Giderme

### API Bağlantı Sorunları
- API URL'sinin doğru olduğundan emin olun
- Timeout değerini artırmayı deneyin
- Sunucunuzun API'ye erişebildiğinden emin olun

### Veritabanı Bağlantı Sorunları
- MySQL bağlantı bilgilerinin doğru olduğundan emin olun
- MySQL sunucusunun çalıştığından emin olun
- Firewall ayarlarınızı kontrol edin

### Performans Sorunları
- `data-retention-days` değerini düşürün
- Gereksiz log ayarlarını kapatın
- Discord webhook entegrasyonunu devre dışı bırakın

## 📊 Sürüm Notları

### Versiyon 1.0.0
- İlk kararlı sürüm
- Kapsamlı performans iyileştirmeleri
- API v2 desteği eklendi
- Gelişmiş yapay zeka modeli entegrasyonu
- Veritabanı sorgu optimizasyonları
- Admin GUI tamamen yenilendi
- PlaceholderAPI entegrasyonu geliştirildi

### Versiyon 0.0.3
- MySQL desteği eklendi, veritabanı bağlantı havuzu iyileştirildi
- İstatistikler artık sunucu yeniden başlatıldığında kaybolmuyor
- API bağlantı zaman aşımı sorunları giderildi
- Eşzamanlı mesaj işleme iyileştirildi
- Mesaj önbelleği ve hız sınırlaması eklendi

## 🤝 Destek ve İletişim

- Discord: [kynux.dev](https://discord.gg/kynux)
- Web Sitesi: [https://kynux.cloud](https://kynux.cloud)
- GitHub: [github.com/kynuxdev/turkish-profanity-detection-plugin](https://github.com/kynuxdev/turkish-profanity-detection-plugin)
- E-posta: support@kynux.cloud

---

<p align="center">
  <small>© 2025 KynuxCloud | Tüm hakları saklıdır.</small>
</p>
